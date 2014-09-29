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

package ch.grengine.code;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.source.MockSource;
import ch.grengine.source.Source;


public class DefaultCodeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructPlusGetters() {
        
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        String name1 = "MainClassName1";
        String name2 = "MainClassName2";
        Set<String> names1 = new HashSet<String>();
        names1.add("Side1");
        names1.add("MainClassName1");
        Set<String> names2 = new HashSet<String>();
        names2.add("Side2");
        names2.add("MainClassName2");
        CompiledSourceInfo i1 = new CompiledSourceInfo(m1, name1, names1, 11);
        CompiledSourceInfo i2 = new CompiledSourceInfo(m2, name2, names2, 22);
        
        String sourcesName = "sourcesname";

        Map<Source,CompiledSourceInfo> infos = new HashMap<Source,CompiledSourceInfo>();
        infos.put(m1, i1);
        infos.put(m2, i2);
        
        Map<String,Bytecode> bytecodes = new HashMap<String,Bytecode>();
        String name1sub = name1 + "#Sub";
        bytecodes.put(name1, new Bytecode(name1, new byte[] { 1, 2, 3 }));
        bytecodes.put(name1sub, new Bytecode(name1sub, new byte[] { 4, 5, 6 }));
        bytecodes.put(name2, new Bytecode(name2, new byte[] { 7, 8, 9 }));

        Code code = new DefaultCode(sourcesName, infos, bytecodes);
        
        assertEquals(sourcesName, code.getSourcesName());
        assertEquals(infos.keySet(), code.getSourceSet());
        assertEquals(bytecodes.keySet(), code.getClassNameSet());
        
        assertEquals(1, code.getBytecode(name1).getBytes()[0]);
        assertNull(code.getBytecode("SomeOtherClassName"));
        assertNull(code.getBytecode(null));
        
        MockSource m3 = new MockSource("id3");
        assertTrue(code.isForSource(m1));
        assertTrue(code.isForSource(m2));
        assertFalse(code.isForSource(m3));
        assertFalse(code.isForSource(null));
        
        assertEquals(name1, code.getMainClassName(m1));
        assertEquals(name2, code.getMainClassName(m2));
        try {
            code.getMainClassName(m3);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is not for this code. Source: " + m3, e.getMessage());
        }
        try {
            code.getMainClassName(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is not for this code. Source: null", e.getMessage());
        }

        assertEquals(names1, code.getClassNames(m1));
        assertEquals(names2, code.getClassNames(m2));
        try {
            code.getClassNames(m3);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is not for this code. Source: " + m3, e.getMessage());
        }
        try {
            code.getClassNames(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is not for this code. Source: null", e.getMessage());
        }

        assertEquals(11, code.getLastModifiedAtCompileTime(m1));
        assertEquals(22, code.getLastModifiedAtCompileTime(m2));
        try {
            code.getLastModifiedAtCompileTime(m3);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is not for this code. Source: " + m3, e.getMessage());
        }
        try {
            code.getLastModifiedAtCompileTime(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is not for this code. Source: null", e.getMessage());
        }
        
        assertEquals("DefaultCode[sourcesName='sourcesname', sources:2, classes:3]", code.toString());
    }
    
    @Test
    public void testConstructSourcesNameNull() throws Exception {
        try {
            new DefaultCode(null, new HashMap<Source,CompiledSourceInfo>(), new HashMap<String,Bytecode>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Sources name is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructCompiledSourceInfosNull() throws Exception {
        try {
            new DefaultCode("name", null, new HashMap<String,Bytecode>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiled source infos is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructBytecodesNull() throws Exception {
        try {
            new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Bytecodes is null.", e.getMessage());
        }
    }

}
