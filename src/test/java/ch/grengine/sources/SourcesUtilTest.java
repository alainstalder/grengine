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

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class SourcesUtilTest {

    @Test
    public void testConstructor() {
        new SourcesUtil();
    }

    @Test
    public void testSourceToSourcesDefaultCompilerFactory() {

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
    public void testSourceToSourcesDefaultCompilerFactorySourceNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceToSources(null),
                NullPointerException.class,
                "Source is null.");
    }

    @Test
    public void testSourceToSourcesSpecificCompilerFactory() {

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
    public void testSourceToSourcesSpecificCompilerFactorySourceNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceToSources(null, new DefaultGroovyCompilerFactory()),
                NullPointerException.class,
                "Source is null.");
    }
    
    @Test
    public void testSourceToSourcesSpecificCompilerFactoryNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceToSources(new MockSource("id1"), null),
                NullPointerException.class,
                "Compiler factory is null.");
    }

    
    @Test
    public void testSourceSetToSourcesDefaultCompilerFactory() {

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
    public void testSourceSetToSourcesDefaultCompilerFactorySourceSetNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceSetToSources(null, "myName"),
                NullPointerException.class,
                "Source set is null.");
    }

    @Test
    public void testSourceSetToSourcesDefaultCompilerFactorySourceNameNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceSetToSources(new HashSet<>(), null),
                NullPointerException.class,
                "Name is null.");
    }
    
    @Test
    public void testSourceSetToSourcesSpecificCompilerFactory() {

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
    public void testSourceSetToSourcesSpecificCompilerFactorySourceSetNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceSetToSources(null, "myName",
                new DefaultGroovyCompilerFactory()),
                NullPointerException.class,
                "Source set is null.");
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactorySourceNameNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceSetToSources(new HashSet<>(), null,
                new DefaultGroovyCompilerFactory()),
                NullPointerException.class,
                "Name is null.");
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactoryNull() {

        // when/then

        assertThrows(() -> SourcesUtil.sourceSetToSources(new HashSet<>(), "myName", null),
                NullPointerException.class,
                "Compiler factory is null.");
    }

}
