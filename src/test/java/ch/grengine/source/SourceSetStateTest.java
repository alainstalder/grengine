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

package ch.grengine.source;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class SourceSetStateTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
        
    @Test
    public void testConstructAndUpdate() throws Exception {
        
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Set<Source> set = SourceUtil.sourceArrayToSourceSet(m1, m2);
        SourceSetState s1 = new SourceSetState(set);
        Thread.sleep(30);
        assertEquals(set, s1.getSourceSet());
        assertEquals(s1.getLastModified(), s1.getLastChecked());
        assertTrue(s1.getLastModified() < System.currentTimeMillis());
        
        // same set with last modified unchanged
        SourceSetState s2 = s1.update(set);
        Thread.sleep(30);
        assertEquals(set, s2.getSourceSet());
        assertEquals(s1.getLastModified(), s2.getLastModified());
        assertTrue(s1.getLastChecked() < s2.getLastChecked());
        assertTrue(s2.getLastModified() < System.currentTimeMillis());
        
        // same set with second source different last modified
        m2.setLastModified(1);
        SourceSetState s3 = s2.update(set);
        Thread.sleep(30);
        assertEquals(set, s3.getSourceSet());
        assertTrue(s2.getLastModified() < s3.getLastModified());
        assertEquals(s3.getLastChecked(), s3.getLastModified());
        assertTrue(s3.getLastModified() < System.currentTimeMillis());
        
        // additional source
        MockSource m3 = new MockSource("id3");
        Set<Source> setNew = SourceUtil.sourceArrayToSourceSet(m1, m2, m3);
        SourceSetState s4 = s3.update(setNew);
        Thread.sleep(30);
        assertEquals(setNew, s4.getSourceSet());
        assertTrue(s3.getLastModified() < s4.getLastModified());
        assertEquals(s4.getLastChecked(), s4.getLastModified());
        assertTrue(s4.getLastModified() < System.currentTimeMillis());

        // empty source set
        Set<Source> setEmpty = new HashSet<Source>();
        SourceSetState s5 = s4.update(setEmpty);
        Thread.sleep(30);
        assertEquals(setEmpty, s5.getSourceSet());
        assertTrue(s4.getLastModified() < s5.getLastModified());
        assertEquals(s5.getLastChecked(), s5.getLastModified());
        assertTrue(s5.getLastModified() < System.currentTimeMillis());

        // different source set of same size
        Set<Source> setDifferent = SourceUtil.sourceArrayToSourceSet(m1, m3);
        SourceSetState s6 = s3.update(setDifferent);
        Thread.sleep(30);
        assertEquals(setDifferent, s6.getSourceSet());
        assertTrue(s3.getLastModified() < s6.getLastModified());
        assertEquals(s6.getLastChecked(), s6.getLastModified());
        assertTrue(s6.getLastModified() < System.currentTimeMillis());
    }
    
    @Test
    public void testConstructWithSourceSetNull() {
        try {
            new SourceSetState(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source set is null.", e.getMessage());
        }
    }
    
    @Test
    public void testUpdateWithSourceSetNull() {
        try {
            new SourceSetState(new HashSet<Source>()).update(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("New source set is null.", e.getMessage());
        }
    }


}
