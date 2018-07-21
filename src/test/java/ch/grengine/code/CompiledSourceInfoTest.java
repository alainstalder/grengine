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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class CompiledSourceInfoTest {

    @Test
    public void testConstructPlusGetters() {

        // given

        Source m1 = new MockSource("id1");
        String name = "MainClassName";
        Set<String> names = new HashSet<>();
        names.add("Side");
        names.add("MainClassName");

        // when

        CompiledSourceInfo info = new CompiledSourceInfo(m1, name, names, 55);

        // then

        assertThat(info.getSource(), is(m1));
        assertThat(info.getMainClassName(), is(name));
        assertThat(info.getClassNames(), is(names));
        assertThat(info.getLastModifiedAtCompileTime(), is(55L));
        assertThat(info.toString().startsWith("CompiledSourceInfo[source=MockSource[ID='id1', lastModified=0], " +
                "mainClassName=MainClassName, classNames=" + names + ", lastModifiedAtCompileTime="), is(true));
    }
    
    @Test
    public void testConstructSourceNull() {

        // when/then

        assertThrows(() -> new CompiledSourceInfo(null, "",
                        new HashSet<>(), 0),
                NullPointerException.class,
                "Source is null.");
    }
    
    @Test
    public void testConstructMainClassNameNull() {


        // when/then

        assertThrows(() -> new CompiledSourceInfo(new MockSource("id1"), null,
                        new HashSet<>(), 0),
                NullPointerException.class,
                "Main class name is null.");
    }
    
    @Test
    public void testConstructClassNamesNull() {


        // when/then

        assertThrows(() -> new CompiledSourceInfo(new MockSource("id1"), "",
                        null, 0),
                NullPointerException.class,
                "Class names are null.");
    }

}
