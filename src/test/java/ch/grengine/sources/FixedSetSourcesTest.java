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

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.MockSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;


public class FixedSetSourcesTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructDefaults() throws Exception {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Set<Source> set = SourceUtil.sourceArrayToSourceSet(m1, m2);
        FixedSetSources.Builder builder = new FixedSetSources.Builder(set);
        FixedSetSources s = builder.build();
        
        Thread.sleep(30);
        assertEquals(builder, s.getBuilder());
        assertEquals(set, s.getSourceSet());
        assertNotNull(s.getName());
        assertNotNull(s.getCompilerFactory());
        assertTrue(s.getCompilerFactory() instanceof DefaultGroovyCompilerFactory);
        assertEquals(s.getSourceSet(), s.getBuilder().getSourceSet());
        assertEquals(s.getName(), s.getBuilder().getName());
        assertEquals(s.getSourceSet(), s.getBuilder().getSourceSet());
        assertEquals(FixedSetSources.Builder.DEFAULT_LATENCY_MS, s.getBuilder().getLatencyMs());
        assertTrue(s.getLastModified() < System.currentTimeMillis());
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Set<Source> set = SourceUtil.sourceArrayToSourceSet(m1, m2);
        FixedSetSources.Builder builder = new FixedSetSources.Builder(set);
        CompilerFactory factory = new DefaultGroovyCompilerFactory();
        builder.setName("fixed").setCompilerFactory(factory).setLatencyMs(200);
        FixedSetSources s = builder.build();
        
        Thread.sleep(30);
        assertEquals(builder, s.getBuilder());
        assertEquals(set, s.getSourceSet());
        assertEquals("fixed", s.getName());
        assertEquals(factory, s.getCompilerFactory());
        assertEquals(s.getSourceSet(), s.getBuilder().getSourceSet());
        assertEquals(s.getName(), s.getBuilder().getName());
        assertEquals(s.getSourceSet(), s.getBuilder().getSourceSet());
        assertEquals(200, s.getBuilder().getLatencyMs());
        assertTrue(s.getLastModified() < System.currentTimeMillis());
    }
    
    @Test
    public void testConstructSourceSetNull() throws Exception {
        try {
            new FixedSetSources.Builder(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source set is null.", e.getMessage());
        }
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Set<Source> set = SourceUtil.sourceArrayToSourceSet(m1, m2);
        FixedSetSources.Builder builder = new FixedSetSources.Builder(set);
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
        MockSource m2 = new MockSource("id2");
        Set<Source> set = SourceUtil.sourceArrayToSourceSet(m1, m2);
        FixedSetSources.Builder builder = new FixedSetSources.Builder(set);
        FixedSetSources s = builder.setLatencyMs(50).build();
        
        m2.setLastModified(1);
        long lastMod = s.getLastModified();
        Thread.sleep(30);
        assertEquals(s.getLastModified(), lastMod);
        Thread.sleep(30);
        long lastMod2 = s.getLastModified();
        assertTrue(lastMod2 > lastMod);
        Thread.sleep(60);
        assertEquals(s.getLastModified(), lastMod2);
    }

}
