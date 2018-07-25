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

package ch.grengine.sources;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.MockSource;
import ch.grengine.source.SourceUtil;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class SourcesUtilTest {

    @Test
    void testConstructor() {
        new SourcesUtil();
    }

    @Test
    void testSourceToSourcesDefaultCompilerFactory() {

        // given

        final MockSource m = new MockSource("id1");

        // when

        final Sources s = SourcesUtil.sourceToSources(m);

        // then

        assertThat(s.getName(), is("id1"));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
        assertThat(s.getSourceSet().size(), is(1));
        assertThat(s.getSourceSet().contains(m), is(true));
    }

    @Test
    void testSourceToSourcesDefaultCompilerFactorySourceNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceToSources(null),
                "Source is null.");
    }

    @Test
    void testSourceToSourcesSpecificCompilerFactory() {

        // given

        final MockSource m = new MockSource("id1");
        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();

        // when

        final Sources s = SourcesUtil.sourceToSources(m, compilerFactory);

        // then

        assertThat(s.getName(), is("id1"));
        assertThat(s.getCompilerFactory(), is(compilerFactory));
        assertThat(s.getSourceSet().size(), is(1));
        assertThat(s.getSourceSet().contains(m), is(true));
    }
    
    @Test
    void testSourceToSourcesSpecificCompilerFactorySourceNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceToSources(null, new DefaultGroovyCompilerFactory()),
                "Source is null.");
    }
    
    @Test
    void testSourceToSourcesSpecificCompilerFactoryNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceToSources(new MockSource("id1"), null),
                "Compiler factory is null.");
    }

    
    @Test
    void testSourceSetToSourcesDefaultCompilerFactory() {

        // given

        final MockSource m1 = new MockSource("id1");
        final MockSource m2 = new MockSource("id2");

        // when

        final Sources s = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(m1, m2), "myName");

        // then

        assertThat(s.getName(), is("myName"));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(m2), is(true));
    }

    @Test
    void testSourceSetToSourcesDefaultCompilerFactorySourceSetNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceSetToSources(null, "myName"),
                "Source set is null.");
    }

    @Test
    void testSourceSetToSourcesDefaultCompilerFactorySourceNameNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceSetToSources(new HashSet<>(), null),
                "Name is null.");
    }
    
    @Test
    void testSourceSetToSourcesSpecificCompilerFactory() {

        // given

        final MockSource m1 = new MockSource("id1");
        final MockSource m2 = new MockSource("id2");
        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();

        // when

        final Sources s = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(m1, m2),
                "myName", compilerFactory);

        // then

        assertThat(s.getName(), is("myName"));
        assertThat(s.getCompilerFactory(), is(compilerFactory));
        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(m2), is(true));
    }

    @Test
    void testSourceSetToSourcesSpecificCompilerFactorySourceSetNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceSetToSources(null, "myName",
                        new DefaultGroovyCompilerFactory()),
                "Source set is null.");
    }

    @Test
    void testSourceSetToSourcesSpecificCompilerFactorySourceNameNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceSetToSources(new HashSet<>(), null,
                        new DefaultGroovyCompilerFactory()),
                "Name is null.");
    }

    @Test
    void testSourceSetToSourcesSpecificCompilerFactoryNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> SourcesUtil.sourceSetToSources(new HashSet<>(), "myName", null),
                "Compiler factory is null.");
    }

}
