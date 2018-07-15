/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.code;

import ch.grengine.source.MockSource;
import ch.grengine.source.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultCodeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructPlusGetters() {
        
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        String name1 = "MainClassName1";
        String name2 = "MainClassName2";
        Set<String> names1 = new HashSet<>();
        names1.add("Side1");
        names1.add("MainClassName1");
        Set<String> names2 = new HashSet<>();
        names2.add("Side2");
        names2.add("MainClassName2");
        CompiledSourceInfo i1 = new CompiledSourceInfo(m1, name1, names1, 11);
        CompiledSourceInfo i2 = new CompiledSourceInfo(m2, name2, names2, 22);
        
        String sourcesName = "sourcesName";

        Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(m1, i1);
        infoMap.put(m2, i2);
        
        Map<String,Bytecode> bytecodeMap = new HashMap<>();
        String name1sub = name1 + "#Sub";
        bytecodeMap.put(name1, new Bytecode(name1, new byte[] { 1, 2, 3 }));
        bytecodeMap.put(name1sub, new Bytecode(name1sub, new byte[] { 4, 5, 6 }));
        bytecodeMap.put(name2, new Bytecode(name2, new byte[] { 7, 8, 9 }));

        Code code = new DefaultCode(sourcesName, infoMap, bytecodeMap);

        assertThat(code.getSourcesName(), is(sourcesName));
        assertThat(code.getSourceSet(), is(infoMap.keySet()));
        assertThat(code.getClassNameSet(), is(bytecodeMap.keySet()));

        assertThat(code.getBytecode(name1).getBytes()[0], is((byte)1));
        assertThat(code.getBytecode("SomeOtherClassName"), is(nullValue()));
        assertThat(code.getBytecode(null), is(nullValue()));
        
        MockSource m3 = new MockSource("id3");
        assertThat(code.isForSource(m1), is(true));
        assertThat(code.isForSource(m2), is(true));
        assertThat(code.isForSource(m3), is(false));
        assertThat(code.isForSource(null), is(false));

        assertThat(code.getMainClassName(m1), is(name1));
        assertThat(code.getMainClassName(m2), is(name2));
        try {
            code.getMainClassName(m3);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is not for this code. Source: " + m3));
        }
        try {
            code.getMainClassName(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is not for this code. Source: null"));
        }

        assertThat(code.getClassNames(m1), is(names1));
        assertThat(code.getClassNames(m2), is(names2));
        try {
            code.getClassNames(m3);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is not for this code. Source: " + m3));
        }
        try {
            code.getClassNames(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is not for this code. Source: null"));
        }

        assertThat(code.getLastModifiedAtCompileTime(m1), is(11L));
        assertThat(code.getLastModifiedAtCompileTime(m2), is(22L));
        try {
            code.getLastModifiedAtCompileTime(m3);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is not for this code. Source: " + m3));
        }
        try {
            code.getLastModifiedAtCompileTime(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is not for this code. Source: null"));
        }

        assertThat(code.toString(), is("DefaultCode[sourcesName='sourcesName', sources:2, classes:3]"));
    }
    
    @Test
    public void testConstructSourcesNameNull() throws Exception {
        try {
            new DefaultCode(null, new HashMap<>(), new HashMap<>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Sources name is null."));
        }
    }
    
    @Test
    public void testConstructCompiledSourceInfoMapNull() throws Exception {
        try {
            new DefaultCode("name", null, new HashMap<>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiled source info map is null."));
        }
    }
    
    @Test
    public void testConstructBytecodeMapNull() throws Exception {
        try {
            new DefaultCode("name", new HashMap<>(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Bytecode map is null."));
        }
    }

}
