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

package ch.artecat.grengine.code;

import ch.artecat.grengine.source.MockSource;
import ch.artecat.grengine.source.Source;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static ch.artecat.grengine.TestUtil.assertThrowsMessageIs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class DefaultSingleSourceCodeTest {

    @Test
    void testConstructPlusGetters() {

        // given
        
        final Source m1 = new MockSource("id1");
        final Source mNotPartOfCode = new MockSource("id3");
        final String name1 = "MainClassName1";
        final Set<String> names1 = new HashSet<>();
        names1.add("Side1");
        names1.add("MainClassName1");
        final CompiledSourceInfo i1 = new CompiledSourceInfo(m1, name1, names1, 11);

        final String sourcesName = "sourcesName";

        final Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(m1, i1);

        final Map<String,Bytecode> bytecodeMap = new HashMap<>();
        final String name1sub = name1 + "#Sub";
        bytecodeMap.put(name1, new Bytecode(name1, new byte[] { 1, 2, 3 }));
        bytecodeMap.put(name1sub, new Bytecode(name1sub, new byte[] { 4, 5, 6 }));

        // when

        final SingleSourceCode code = new DefaultSingleSourceCode(sourcesName, infoMap, bytecodeMap);

        // then

        assertThat(code.getSourcesName(), is(sourcesName));
        assertThat(code.getSourceSet(), is(infoMap.keySet()));
        assertThat(code.getClassNameSet(), is(bytecodeMap.keySet()));

        assertThat(code.getBytecode(name1).getBytes()[0], is((byte)1));
        assertThat(code.getBytecode("SomeOtherClassName"), is(nullValue()));
        assertThat(code.getBytecode(null), is(nullValue()));
        
        assertThat(code.isForSource(m1), is(true));
        assertThat(code.isForSource(mNotPartOfCode), is(false));
        assertThat(code.isForSource(null), is(false));

        assertThat(code.getMainClassName(m1), is(name1));
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> code.getMainClassName(mNotPartOfCode),
                "Source is not for this code. Source: " + mNotPartOfCode);
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> code.getMainClassName(null),
                "Source is not for this code. Source: null");

        assertThat(code.getClassNames(m1), is(names1));
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> code.getClassNames(mNotPartOfCode),
                "Source is not for this code. Source: " + mNotPartOfCode);
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> code.getClassNames(null),
                "Source is not for this code. Source: null");

        assertThat(code.getLastModifiedAtCompileTime(m1), is(11L));
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> code.getLastModifiedAtCompileTime(mNotPartOfCode),
                "Source is not for this code. Source: " + mNotPartOfCode);
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> code.getLastModifiedAtCompileTime(null),
                "Source is not for this code. Source: null");

        assertThat(code.getSource(), is(m1));
        assertThat(code.getMainClassName(), is(name1));
        assertThat(code.getClassNames(), is(names1));
        assertThat(code.getLastModifiedAtCompileTime(), is(11L));

        // when

        final String codeString =  code.toString();

        // then

        assertThat(codeString.startsWith("DefaultSingleSourceCode[sourcesName='sourcesName', mainClassName=MainClassName1, " +
                "classes:[MainClassName1"), is(true));
        assertThat(codeString.endsWith("classes:[MainClassName1, MainClassName1#Sub]]") ||
                codeString.endsWith("classes:[MainClassName1#Sub, MainClassName1]]"), is(true));
    }
    
    @Test
    void testConstructSourcesNameNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultSingleSourceCode(null, new HashMap<>(), new HashMap<>()),
                "Sources name is null.");
    }
    
    @Test
    void testConstructCompiledSourceInfoMapNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultSingleSourceCode("name", null, new HashMap<>()),
                "Compiled source info map is null.");
    }
    
    @Test
    void testConstructBytecodeMapNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultSingleSourceCode("name", new HashMap<>(), null),
                "Bytecode map is null.");
    }
    
    @Test
    void testConstructNotSingleSource() {

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> new DefaultSingleSourceCode("name", new HashMap<>(), new HashMap<>()),
                "Not a single source.");
    }

}
