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

import ch.grengine.source.MockSource;
import ch.grengine.source.SourceUtil;
import ch.grengine.TestUtil;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.DefaultFileSource;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class CompositeSourcesTest {

    @Test
    void testConstructDefaults() throws Exception {

        // given

        final MockSource m1 = new MockSource("id1");
        final FixedSetSources s1 = new FixedSetSources.Builder(SourceUtil.sourceArrayToSourceSet(m1)).build();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 33");
        final DirBasedSources s2 =  new DirBasedSources.Builder(dir).build();
        final List<Sources> sourcesList = Arrays.asList(s1, s2);

        // when

        final CompositeSources.Builder builder = new CompositeSources.Builder(sourcesList);
        final CompositeSources s = builder.build();

        // then

        Thread.sleep(30);
        assertThat(s.getBuilder(), CoreMatchers.is(builder));
        assertThat(s.getBuilder().getSourcesCollection().size(), is(2));
        assertThat(s.getName(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));

        assertThat(s.getBuilder().getName(), is(s.getName()));
        assertThat(s.getBuilder().getCompilerFactory(), is(s.getCompilerFactory()));
        assertThat(s.getBuilder().getLatencyMs(), CoreMatchers.is(CompositeSources.Builder.DEFAULT_LATENCY_MS));
        assertThat(s.getLastModified() < System.currentTimeMillis(), is(true));

        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(new DefaultFileSource(file)), is(true));
    }
    
    @Test
    void testConstructAllDefined() throws Exception {

        // given

        final MockSource m1 = new MockSource("id1");
        final FixedSetSources s1 = new FixedSetSources.Builder(
                SourceUtil.sourceArrayToSourceSet(m1)).build();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 33");
        final DirBasedSources s2 =  new DirBasedSources.Builder(dir).build();
        final List<Sources> sourcesList = Arrays.asList(s1, s2);
        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();

        // when

        final CompositeSources.Builder builder = new CompositeSources.Builder(sourcesList);
        final CompositeSources s = builder
                .setName("composite")
                .setCompilerFactory(compilerFactory)
                .setLatencyMs(200)
                .build();

        // then

        Thread.sleep(30);
        assertThat(s.getBuilder(), CoreMatchers.is(builder));
        assertThat(s.getBuilder().getSourcesCollection().size(), is(2));
        assertThat(s.getName(), is("composite"));
        assertThat(s.getCompilerFactory(), is(compilerFactory));

        assertThat(s.getBuilder().getName(), is(s.getName()));
        assertThat(s.getBuilder().getCompilerFactory(), is(s.getCompilerFactory()));
        assertThat(s.getBuilder().getLatencyMs(), is(200L));
        assertThat(s.getLastModified() < System.currentTimeMillis(), is(true));

        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(new DefaultFileSource(file)), is(true));
    }
    
    @Test
    void testConstructSourcesCollectionNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new CompositeSources.Builder(null),
                "Sources collection is null.");
    }

    @Test
    void testModifyBuilderAfterUse() {

        // given

        final CompositeSources.Builder builder = new CompositeSources.Builder(new LinkedList<>());
        builder.build();


        // when/then

        assertThrowsMessageIs(IllegalStateException.class,
                () -> builder.setName("name"),
                "Builder already used.");
    }
    
    @Test
    void testLastModified() throws Exception {

        // given

        final MockSource m1 = new MockSource("id1");
        final FixedSetSources s1 = new FixedSetSources.Builder(SourceUtil.sourceArrayToSourceSet(m1))
            .setLatencyMs(50).build();
        final MockSource m2 = new MockSource("id2");
        final FixedSetSources s2 = new FixedSetSources.Builder(SourceUtil.sourceArrayToSourceSet(m2))
            .setLatencyMs(50).build();
        final List<Sources> sourcesList = Arrays.asList(s1, s2);

        // when

        final CompositeSources.Builder builder = new CompositeSources.Builder(sourcesList);
        final CompositeSources s = builder
                .setLatencyMs(50)
                .build();

        // then

        assertThat(s.getBuilder().getSourcesCollection().size(), is(2));
        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(m2), is(true));

        // when

        m2.setLastModified(1);

        // then

        final long lastMod = s.getLastModified();
        Thread.sleep(30);
        assertThat(lastMod, is(s.getLastModified()));
        Thread.sleep(120);
        final long lastMod2 = s.getLastModified();
        assertThat(lastMod2 > lastMod, is(true));
        Thread.sleep(120);
        assertThat(lastMod2, is(s.getLastModified()));

        // when
        
        m1.setLastModified(1);

        // then

        Thread.sleep(120);
        final long lastMod3 = s.getLastModified();
        assertThat(lastMod3 > lastMod2, is(true));
        Thread.sleep(120);
        assertThat(lastMod3, is(s.getLastModified()));
    }
    
}
