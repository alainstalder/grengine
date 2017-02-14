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
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;

import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class SourcesUtilTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructor() {
        new SourcesUtil();
    }

    @Test
    public void testSourceToSourcesDefaultCompilerFactory() {
        MockSource m = new MockSource("id1");
        Sources s = SourcesUtil.sourceToSources(m);
        assertThat(s.getName(), is("id1"));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
        assertThat(s.getSourceSet().size(), is(1));
        assertThat(s.getSourceSet().contains(m), is(true));
    }

    @Test
    public void testSourceToSourcesDefaultCompilerFactorySourceNull() throws Exception {
        try {
            SourcesUtil.sourceToSources(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is null."));
        }
    }

    @Test
    public void testSourceToSourcesSpecificCompilerFactory() {
        MockSource m = new MockSource("id1");
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        Sources s = SourcesUtil.sourceToSources(m, compilerFactory);
        assertThat(s.getName(), is("id1"));
        assertThat(s.getCompilerFactory(), is(compilerFactory));
        assertThat(s.getSourceSet().size(), is(1));
        assertThat(s.getSourceSet().contains(m), is(true));
    }
    
    @Test
    public void testSourceToSourcesSpecificCompilerFactorySourceNull() throws Exception {
        try {
            SourcesUtil.sourceToSources(null, new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source is null."));
        }
    }
    
    @Test
    public void testSourceToSourcesSpecificCompilerFactoryNull() throws Exception {
        try {
            SourcesUtil.sourceToSources(new MockSource("id1"), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler factory is null."));
        }
    }

    
    @Test
    public void testSourceSetToSourcesDefaultCompilerFactory() {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Sources s = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(m1, m2), "myName");
        assertThat(s.getName(), is("myName"));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(m2), is(true));
    }

    @Test
    public void testSourceSetToSourcesDefaultCompilerFactorySourceSetNull() {
        try {
            SourcesUtil.sourceSetToSources(null, "myName");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source set is null."));
        }
    }

    @Test
    public void testSourceSetToSourcesDefaultCompilerFactorySourceNameNull() {
        try {
            SourcesUtil.sourceSetToSources(new HashSet<Source>(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Name is null."));
        }
    }
    
    @Test
    public void testSourceSetToSourcesSpecificCompilerFactory() {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        Sources s = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(m1, m2), "myName",
                compilerFactory);
        assertThat(s.getName(), is("myName"));
        assertThat(s.getCompilerFactory(), is(compilerFactory));
        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(m2), is(true));
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactorySourceSetNull() {
        try {
            SourcesUtil.sourceSetToSources(null, "myName", new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source set is null."));
        }
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactorySourceNameNull() {
        try {
            SourcesUtil.sourceSetToSources(new HashSet<Source>(), null, new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Name is null."));
        }
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactoryNull() {
        try {
            SourcesUtil.sourceSetToSources(new HashSet<Source>(), "myName", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler factory is null."));
        }
    }
    
    
    @Test
    public void testSourcesArrayToList() {
        Sources s1 = SourcesUtil.sourceToSources(new MockSource("id1"));
        Sources s2 = SourcesUtil.sourceToSources(new MockSource("id2"));
        Sources[] sArrayEmpty = new Sources[0];
        Sources[] sArrayAll = new Sources[] { s1, s2 };

        assertThat(SourcesUtil.sourcesArrayToList(sArrayEmpty).size(), is(0));
        assertThat(SourcesUtil.sourcesArrayToList(sArrayAll).size(), is(2));
        assertThat(SourcesUtil.sourcesArrayToList(sArrayAll).get(0), is(s1));
        assertThat(SourcesUtil.sourcesArrayToList(sArrayAll).get(1), is(s2));
        assertThat(SourcesUtil.sourcesArrayToList(s1).size(), is(1));
        assertThat(SourcesUtil.sourcesArrayToList(s1).get(0), is(s1));
        assertThat(SourcesUtil.sourcesArrayToList(s2, s1).size(), is(2));
        assertThat(SourcesUtil.sourcesArrayToList(s2, s1).get(0), is(s2));
        assertThat(SourcesUtil.sourcesArrayToList(s2, s1).get(1), is(s1));
    }

    @Test
    public void testSourcesArrayToListSourcesNull() {
        try {
            SourcesUtil.sourcesArrayToList((Sources[])null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Sources array is null."));
        }
    }

}
