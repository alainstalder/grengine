/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.Bytecode;
import ch.grengine.code.Code;
import ch.grengine.code.CompiledSourceInfo;
import ch.grengine.code.DefaultCode;
import ch.grengine.load.BytecodeClassLoader;
import ch.grengine.load.LoadMode;
import ch.grengine.source.Source;


public class LoaderTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructAndGetSetSourceClassLoader() throws Exception {
        
       EngineId engineId1 = new EngineId();
       EngineId engineId2 = new EngineId();
       ClassLoader parent = Thread.currentThread().getContextClassLoader();
       Code code = new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), 
               new HashMap<String,Bytecode>());
       BytecodeClassLoader classLoader1 = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
       BytecodeClassLoader classLoader2 = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
       
       Loader loader = new Loader(engineId1, 17, true, classLoader1);
       
       assertEquals(17, loader.getNumber());
       
       assertEquals(classLoader1, loader.getSourceClassLoader(engineId1));
       try {
           loader.getSourceClassLoader(engineId2);
       } catch (IllegalArgumentException e) {
           assertEquals("Engine ID does not match (loader created by a different engine).", e.getMessage());
       }
       
       try {
           loader.setSourceClassLoader(engineId2, classLoader2);
       } catch (IllegalArgumentException e) {
           assertEquals("Engine ID does not match (loader created by a different engine).", e.getMessage());
       }
       loader.setSourceClassLoader(engineId1, classLoader2);
       assertEquals(classLoader2, loader.getSourceClassLoader(engineId1));
       try {
           loader.getSourceClassLoader(engineId2);
       } catch (IllegalArgumentException e) {
           assertEquals("Engine ID does not match (loader created by a different engine).", e.getMessage());
       }
       
       System.out.println(loader);
       assertTrue(loader.toString().startsWith("Loader[engineId=ch.grengine.engine.EngineId@"));
       assertTrue(loader.toString().endsWith(", number=17, isAttached=true]"));
       Loader detachedLoader = new Loader(engineId1, 17, false, classLoader1);
       assertTrue(detachedLoader.toString().startsWith("Loader[engineId=ch.grengine.engine.EngineId@"));
       assertTrue(detachedLoader.toString().endsWith(", number=17, isAttached=false]"));
    }
    
    @Test
    public void testConstructEngineIdNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        try {
            new Loader(null, 0, false, classLoader);
        } catch (IllegalArgumentException e) {
            assertEquals("Engine ID is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructSourceClassLoaderNull() throws Exception {
        try {
            new Loader(new EngineId(), 0, false, null);
        } catch (IllegalArgumentException e) {
            assertEquals("Source class loader is null.", e.getMessage());
        }
    }
    
    @Test
    public void testEquals() {
        long number = 15;
        EngineId id = new EngineId();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        BytecodeClassLoader classLoader2 = new BytecodeClassLoader(parent.getParent(), LoadMode.PARENT_FIRST, code);
        
        Loader loader = new Loader(id, number, true, classLoader);
        assertTrue(loader.equals(new Loader(id, number, true, classLoader)));
        assertTrue(loader.equals(new Loader(id, number, false, classLoader)));
        assertTrue(loader.equals(new Loader(id, number, true, classLoader2)));
        assertFalse(loader.equals(new Loader(id, 33, true, classLoader)));
        assertFalse(loader.equals(new Loader(new EngineId(), number, true, classLoader)));
        assertFalse(loader.equals("different class"));
        assertFalse(loader.equals(null));
    }

}
