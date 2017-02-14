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


public class DefaultSingleSourceCodeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructPlusGetters() {
        
        Source m1 = new MockSource("id1");
        String name1 = "MainClassName1";
        Set<String> names1 = new HashSet<String>();
        names1.add("Side1");
        names1.add("MainClassName1");
        CompiledSourceInfo i1 = new CompiledSourceInfo(m1, name1, names1, 11);
        
        String sourcesName = "sourcesname";

        Map<Source,CompiledSourceInfo> infos = new HashMap<Source,CompiledSourceInfo>();
        infos.put(m1, i1);
        
        Map<String,Bytecode> bytecodes = new HashMap<String,Bytecode>();
        String name1sub = name1 + "#Sub";
        bytecodes.put(name1, new Bytecode(name1, new byte[] { 1, 2, 3 }));
        bytecodes.put(name1sub, new Bytecode(name1sub, new byte[] { 4, 5, 6 }));

        SingleSourceCode code = new DefaultSingleSourceCode(sourcesName, infos, bytecodes);

        assertThat(code.getSourcesName(), is(sourcesName));
        assertThat(code.getSourceSet(), is(infos.keySet()));
        assertThat(code.getClassNameSet(), is(bytecodes.keySet()));

        assertThat(code.getBytecode(name1).getBytes()[0], is((byte)1));
        assertThat(code.getBytecode("SomeOtherClassName"), is(nullValue()));
        assertThat(code.getBytecode(null), is(nullValue()));
        
        Source m3 = new MockSource("id3");
        assertThat(code.isForSource(m1), is(true));
        assertThat(code.isForSource(m3), is(false));
        assertThat(code.isForSource(null), is(false));

        assertThat(code.getMainClassName(m1), is(name1));
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

        assertThat(code.getSource(), is(m1));
        assertThat(code.getMainClassName(), is(name1));
        assertThat(code.getClassNames(), is(names1));
        assertThat(code.getLastModifiedAtCompileTime(), is(11L));

        String codeString =  code.toString();
        assertThat(codeString.startsWith("DefaultSingleSourceCode[sourcesName='sourcesname', mainClassName=MainClassName1, " +
                "classes:[MainClassName1"), is(true));
        assertThat(codeString.endsWith("classes:[MainClassName1, MainClassName1#Sub]]") ||
                codeString.endsWith("classes:[MainClassName1#Sub, MainClassName1]]"), is(true));
    }
    
    @Test
    public void testConstructSourcesNameNull() throws Exception {
        try {
            new DefaultSingleSourceCode(null, new HashMap<Source,CompiledSourceInfo>(), new HashMap<String,Bytecode>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Sources name is null."));
        }
    }
    
    @Test
    public void testConstructCompiledSourceInfosNull() throws Exception {
        try {
            new DefaultSingleSourceCode("name", null, new HashMap<String,Bytecode>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiled source infos is null."));
        }
    }
    
    @Test
    public void testConstructBytecodesNull() throws Exception {
        try {
            new DefaultSingleSourceCode("name", new HashMap<Source,CompiledSourceInfo>(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Bytecodes is null."));
        }
    }
    
    @Test
    public void testConstructNotSingleSource() throws Exception {
        try {
            new DefaultSingleSourceCode("name", new HashMap<Source,CompiledSourceInfo>(),
                    new HashMap<String,Bytecode>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Not a single source."));
        }
    }

}
