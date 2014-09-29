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

package ch.grengine.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;


public class DefaultTopCodeCacheTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructAndGettersAndMore() throws Exception {
        
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultTopCodeCache.Builder builder =
                new DefaultTopCodeCache.Builder(parent).setCompilerFactory(compilerFactory);
        DefaultTopCodeCache c = builder.build();
        
        assertEquals(builder, c.getBuilder());
        assertEquals(compilerFactory, c.getCompilerFactory());
        assertEquals(parent, c.getParent());
        
        ClassLoader parentNew = Thread.currentThread().getContextClassLoader();
        c.setParent(parentNew);
        assertEquals(parentNew, c.getParent());
        
        DefaultTopCodeCache c2 = c.clone();
        assertEquals(compilerFactory, c2.getCompilerFactory());
        assertEquals(parentNew, c2.getParent());

        c = new DefaultTopCodeCache.Builder(null).build();
        assertNull(c.getParent());
        assertNotNull(c.getCompilerFactory());
        assertTrue(c.getCompilerFactory() instanceof DefaultGroovyCompilerFactory);
        c.setParent(parentNew);
        assertEquals(parentNew, c.getParent());
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultTopCodeCache.Builder builder = new DefaultTopCodeCache.Builder(null);
        builder.build();
        try {
            builder.setCompilerFactory(new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }

    @Test
    public void testSetParentNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        TopCodeCache c = new DefaultTopCodeCache.Builder(parent).build();
        
        try {
            c.setParent(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }
    
    // most functionality is tested in LayeredClassLoaderTest

}
