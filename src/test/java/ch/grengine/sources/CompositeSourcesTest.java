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

import ch.grengine.TestUtil;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.DefaultFileSource;
import ch.grengine.source.MockSource;
import ch.grengine.source.SourceUtil;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class CompositeSourcesTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructDefaults() throws Exception {
        MockSource m1 = new MockSource("id1");
        FixedSetSources s1 = new FixedSetSources.Builder(SourceUtil.sourceArrayToSourceSet(m1)).build();
        File dir = tempFolder.getRoot();
        File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 33");
        DirBasedSources s2 =  new DirBasedSources.Builder(dir).build();
        List<Sources> sourcesList = SourcesUtil.sourcesArrayToList(s1, s2);
        CompositeSources.Builder builder = new CompositeSources.Builder(sourcesList);
        CompositeSources s = builder.build();
        
        Thread.sleep(30);
        assertThat(s.getBuilder(), is(builder));
        assertThat(s.getBuilder().getSourcesCollection().size(), is(2));
        assertThat(s.getName(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));

        assertThat(s.getBuilder().getName(), is(s.getName()));
        assertThat(s.getBuilder().getCompilerFactory(), is(s.getCompilerFactory()));
        assertThat(s.getBuilder().getLatencyMs(), is(CompositeSources.Builder.DEFAULT_LATENCY_MS));
        assertThat(s.getLastModified() < System.currentTimeMillis(), is(true));

        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(new DefaultFileSource(file)), is(true));
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        MockSource m1 = new MockSource("id1");
        FixedSetSources s1 = new FixedSetSources.Builder(
                SourceUtil.sourceArrayToSourceSet(m1)).build();
        File dir = tempFolder.getRoot();
        File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 33");
        DirBasedSources s2 =  new DirBasedSources.Builder(dir).build();
        List<Sources> sourcesList = SourcesUtil.sourcesArrayToList(s1, s2);
        CompositeSources.Builder builder = new CompositeSources.Builder(sourcesList);
        builder.setName("composite");
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        builder.setCompilerFactory(compilerFactory);
        builder.setLatencyMs(200);
        CompositeSources s = builder.build();
        
        Thread.sleep(30);
        assertThat(s.getBuilder(), is(builder));
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
    public void testConstructSourcesCollectionNull() throws Exception {
        try {
            new CompositeSources.Builder(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Sources collection is null."));
        }
    }

    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        CompositeSources.Builder builder = new CompositeSources.Builder(new LinkedList<>());
        builder.build();
        try {
            builder.setName("name");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }
    
    @Test
    public void testLastModified() throws Exception {
        MockSource m1 = new MockSource("id1");
        FixedSetSources s1 = new FixedSetSources.Builder(SourceUtil.sourceArrayToSourceSet(m1))
            .setLatencyMs(50).build();
        MockSource m2 = new MockSource("id2");
        FixedSetSources s2 = new FixedSetSources.Builder(SourceUtil.sourceArrayToSourceSet(m2))
            .setLatencyMs(50).build();
        List<Sources> sourcesList = SourcesUtil.sourcesArrayToList(s1, s2);
        CompositeSources.Builder builder = new CompositeSources.Builder(sourcesList);
        CompositeSources s = builder.setLatencyMs(50).build();

        assertThat(s.getBuilder().getSourcesCollection().size(), is(2));
        assertThat(s.getSourceSet().size(), is(2));
        assertThat(s.getSourceSet().contains(m1), is(true));
        assertThat(s.getSourceSet().contains(m2), is(true));

        m2.setLastModified(1);
        long lastMod = s.getLastModified();
        Thread.sleep(30);
        assertThat(lastMod, is(s.getLastModified()));
        Thread.sleep(120);
        long lastMod2 = s.getLastModified();
        assertThat(lastMod2 > lastMod, is(true));
        Thread.sleep(120);
        assertThat(lastMod2, is(s.getLastModified()));
        
        m1.setLastModified(1);
        Thread.sleep(120);
        long lastMod3 = s.getLastModified();
        assertThat(lastMod3 > lastMod2, is(true));
        Thread.sleep(120);
        assertThat(lastMod3, is(s.getLastModified()));
    }
    
}
