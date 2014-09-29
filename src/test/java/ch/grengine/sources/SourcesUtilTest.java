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

import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.MockSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;


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
        assertEquals("id1", s.getName());
        assertNotNull(s.getCompilerFactory());
        assertTrue(s.getCompilerFactory() instanceof DefaultGroovyCompilerFactory);
        assertEquals(1, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m));
    }

    @Test
    public void testSourceToSourcesDefaultCompilerFactorySourceNull() throws Exception {
        try {
            SourcesUtil.sourceToSources(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is null.", e.getMessage());
        }
    }

    @Test
    public void testSourceToSourcesSpecificCompilerFactory() {
        MockSource m = new MockSource("id1");
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        Sources s = SourcesUtil.sourceToSources(m, compilerFactory);
        assertEquals("id1", s.getName());
        assertEquals(compilerFactory, s.getCompilerFactory());
        assertEquals(1, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m));
    }
    
    @Test
    public void testSourceToSourcesSpecificCompilerFactorySourceNull() throws Exception {
        try {
            SourcesUtil.sourceToSources(null, new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is null.", e.getMessage());
        }
    }
    
    @Test
    public void testSourceToSourcesSpecificCompilerFactoryNull() throws Exception {
        try {
            SourcesUtil.sourceToSources(new MockSource("id1"), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler factory is null.", e.getMessage());
        }
    }

    
    @Test
    public void testSourceSetToSourcesDefaultCompilerFactory() {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Sources s = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(m1, m2), "myname");
        assertEquals("myname", s.getName());
        assertNotNull(s.getCompilerFactory());
        assertTrue(s.getCompilerFactory() instanceof DefaultGroovyCompilerFactory);
        assertEquals(2, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m1));
        assertTrue(s.getSourceSet().contains(m2));
    }

    @Test
    public void testSourceSetToSourcesDefaultCompilerFactorySourceSetNull() {
        try {
            SourcesUtil.sourceSetToSources(null, "myname");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source set is null.", e.getMessage());
        }
    }

    @Test
    public void testSourceSetToSourcesDefaultCompilerFactorySourceNameNull() {
        try {
            SourcesUtil.sourceSetToSources(new HashSet<Source>(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Name is null.", e.getMessage());
        }
    }
    
    @Test
    public void testSourceSetToSourcesSpecificCompilerFactory() {
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        Sources s = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(m1, m2), "myname",
                compilerFactory);
        assertEquals("myname", s.getName());
        assertEquals(compilerFactory, s.getCompilerFactory());
        assertEquals(2, s.getSourceSet().size());
        assertTrue(s.getSourceSet().contains(m1));
        assertTrue(s.getSourceSet().contains(m2));
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactorySourceSetNull() {
        try {
            SourcesUtil.sourceSetToSources(null, "myname", new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source set is null.", e.getMessage());
        }
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactorySourceNameNull() {
        try {
            SourcesUtil.sourceSetToSources(new HashSet<Source>(), null, new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Name is null.", e.getMessage());
        }
    }

    @Test
    public void testSourceSetToSourcesSpecificCompilerFactoryNull() {
        try {
            SourcesUtil.sourceSetToSources(new HashSet<Source>(), "myname", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler factory is null.", e.getMessage());
        }
    }
    
    
    @Test
    public void testSourcesArrayToList() {
        Sources s1 = SourcesUtil.sourceToSources(new MockSource("id1"));
        Sources s2 = SourcesUtil.sourceToSources(new MockSource("id2"));
        Sources[] sArrayEmpty = new Sources[0];
        Sources[] sArrayAll = new Sources[] { s1, s2 };
        
        assertEquals(0, SourcesUtil.sourcesArrayToList(sArrayEmpty).size());
        assertEquals(2, SourcesUtil.sourcesArrayToList(sArrayAll).size());
        assertEquals(s1, SourcesUtil.sourcesArrayToList(sArrayAll).get(0));
        assertEquals(s2, SourcesUtil.sourcesArrayToList(sArrayAll).get(1));
        assertEquals(1, SourcesUtil.sourcesArrayToList(s1).size());
        assertEquals(s1, SourcesUtil.sourcesArrayToList(s1).get(0));
        assertEquals(2, SourcesUtil.sourcesArrayToList(s2, s1).size());
        assertEquals(s2, SourcesUtil.sourcesArrayToList(s2, s1).get(0));
        assertEquals(s1, SourcesUtil.sourcesArrayToList(s2, s1).get(1));
    }

    @Test
    public void testSourcesArrayToListSourcesNull() {
        try {
            SourcesUtil.sourcesArrayToList((Sources[])null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Sources array is null.", e.getMessage());
        }
    }

}
