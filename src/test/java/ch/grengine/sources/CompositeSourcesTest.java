/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.DefaultFileSource;
import ch.grengine.source.MockSource;
import ch.grengine.source.SourceUtil;


public class CompositeSourcesTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
        assertEquals(builder, s.getBuilder());
        assertEquals(2, s.getBuilder().getSourcesCollection().size());
        assertNotNull(s.getName());
        assertNotNull(s.getCompilerFactory());
        assertTrue(s.getCompilerFactory() instanceof DefaultGroovyCompilerFactory);
        
        assertEquals(s.getName(), s.getBuilder().getName());
        assertEquals(s.getCompilerFactory(), s.getBuilder().getCompilerFactory());
        assertEquals(CompositeSources.Builder.DEFAULT_LATENCY_MS, s.getBuilder().getLatencyMs());
        assertTrue(s.getLastModified() < System.currentTimeMillis());
        
        assertEquals(2, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m1));
        assertTrue(s.getSourceSet().contains(new DefaultFileSource(file)));
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
        assertEquals(builder, s.getBuilder());
        assertEquals(2, s.getBuilder().getSourcesCollection().size());
        assertEquals("composite", s.getName());
        assertEquals(compilerFactory, s.getCompilerFactory());
        
        assertEquals(s.getName(), s.getBuilder().getName());
        assertEquals(s.getCompilerFactory(), s.getBuilder().getCompilerFactory());
        assertEquals(200, s.getBuilder().getLatencyMs());
        assertTrue(s.getLastModified() < System.currentTimeMillis());
        
        assertEquals(2, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m1));
        assertTrue(s.getSourceSet().contains(new DefaultFileSource(file)));
    }
    
    @Test
    public void testConstructSourcesCollectionNull() throws Exception {
        try {
            new CompositeSources.Builder(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Sources collection is null.", e.getMessage());
        }
    }

    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        CompositeSources.Builder builder = new CompositeSources.Builder(new LinkedList<Sources>());
        builder.build();
        try {
            builder.setName("name");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
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
        
        assertEquals(2, s.getBuilder().getSourcesCollection().size());
        assertEquals(2, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m1));
        assertTrue(s.getSourceSet().contains(m2));

        m2.setLastModified(1);
        long lastMod = s.getLastModified();
        Thread.sleep(30);
        assertEquals(s.getLastModified(), lastMod);
        Thread.sleep(120);
        long lastMod2 = s.getLastModified();
        assertTrue(lastMod2 > lastMod);
        Thread.sleep(120);
        assertEquals(s.getLastModified(), lastMod2);
        
        m1.setLastModified(1);
        Thread.sleep(120);
        long lastMod3 = s.getLastModified();
        assertTrue(lastMod3 > lastMod2);
        Thread.sleep(120);
        assertEquals(s.getLastModified(), lastMod3);
    }
    
}
