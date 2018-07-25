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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DefaultCodeTest {

    @Test
    void testConstructPlusGetters() {

        // given

        final MockSource m1 = new MockSource("id1");
        final MockSource m2 = new MockSource("id2");
        final MockSource mNotPartOfCode = new MockSource("id3");
        final String name1 = "MainClassName1";
        final String name2 = "MainClassName2";
        final Set<String> names1 = new HashSet<>();
        names1.add("Side1");
        names1.add("MainClassName1");
        final Set<String> names2 = new HashSet<>();
        names2.add("Side2");
        names2.add("MainClassName2");
        final CompiledSourceInfo i1 = new CompiledSourceInfo(m1, name1, names1, 11);
        final CompiledSourceInfo i2 = new CompiledSourceInfo(m2, name2, names2, 22);

        final String sourcesName = "sourcesName";

        final Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(m1, i1);
        infoMap.put(m2, i2);

        final Map<String,Bytecode> bytecodeMap = new HashMap<>();
        final String name1sub = name1 + "#Sub";
        bytecodeMap.put(name1, new Bytecode(name1, new byte[] { 1, 2, 3 }));
        bytecodeMap.put(name1sub, new Bytecode(name1sub, new byte[] { 4, 5, 6 }));
        bytecodeMap.put(name2, new Bytecode(name2, new byte[] { 7, 8, 9 }));

        // when

        final Code code = new DefaultCode(sourcesName, infoMap, bytecodeMap);

        // then

        assertThat(code.getSourcesName(), is(sourcesName));
        assertThat(code.getSourceSet(), is(infoMap.keySet()));
        assertThat(code.getClassNameSet(), is(bytecodeMap.keySet()));

        assertThat(code.getBytecode(name1).getBytes()[0], is((byte)1));
        assertThat(code.getBytecode("SomeOtherClassName"), is(nullValue()));
        assertThat(code.getBytecode(null), is(nullValue()));

        assertThat(code.isForSource(m1), is(true));
        assertThat(code.isForSource(m2), is(true));
        assertThat(code.isForSource(mNotPartOfCode), is(false));
        assertThat(code.isForSource(null), is(false));

        assertThat(code.getMainClassName(m1), is(name1));
        assertThat(code.getMainClassName(m2), is(name2));
        assertThrows(IllegalArgumentException.class,
                () -> code.getMainClassName(mNotPartOfCode),
                "Source is not for this code. Source: " + mNotPartOfCode);
        assertThrows(IllegalArgumentException.class,
                () -> code.getMainClassName(null),
                "Source is not for this code. Source: null");

        assertThat(code.getClassNames(m1), is(names1));
        assertThat(code.getClassNames(m2), is(names2));
        assertThrows(IllegalArgumentException.class,
                () -> code.getClassNames(mNotPartOfCode),
                "Source is not for this code. Source: " + mNotPartOfCode);
        assertThrows(IllegalArgumentException.class,
                () -> code.getClassNames(null),
                "Source is not for this code. Source: null");

        assertThat(code.getLastModifiedAtCompileTime(m1), is(11L));
        assertThat(code.getLastModifiedAtCompileTime(m2), is(22L));
        assertThrows(IllegalArgumentException.class,
                () -> code.getLastModifiedAtCompileTime(mNotPartOfCode),
                "Source is not for this code. Source: " + mNotPartOfCode);
        assertThrows(IllegalArgumentException.class,
                () -> code.getLastModifiedAtCompileTime(null),
                "Source is not for this code. Source: null");

        assertThat(code.toString(), is("DefaultCode[sourcesName='sourcesName', sources:2, classes:3]"));
    }
    
    @Test
    void testConstructSourcesNameNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> new DefaultCode(null, new HashMap<>(), new HashMap<>()),
                "Sources name is null.");
    }
    
    @Test
    void testConstructCompiledSourceInfoMapNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> new DefaultCode("name", null, new HashMap<>()),
                "Compiled source info map is null.");
    }
    
    @Test
    void testConstructBytecodeMapNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> new DefaultCode("name", new HashMap<>(), null),
                "Bytecode map is null9999.");
    }

}
