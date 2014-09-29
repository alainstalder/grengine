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

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;


public class MockFileSourceTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMockFileSource() throws Exception {
        File f = new File(tempFolder.getRoot(), "file");
        TestUtil.setFileText(f, "dummy");
        File fMod = new File(tempFolder.getRoot(), "file.lastModified");
        MockFileSource s = new MockFileSource(f);
        
        assertTrue(f.exists());
        assertTrue(fMod.exists());
        assertEquals("0", TestUtil.getFileText(fMod));
        assertEquals(0, s.getLastModified());
        assertEquals(0, s.getFile().lastModified());
        assertTrue(f.lastModified() != 0);
        
        assertTrue(((MockFile)s.getFile()).setLastModified(100));
        assertEquals("100", TestUtil.getFileText(fMod));
        assertEquals(100, s.getLastModified());
        assertEquals(100, s.getFile().lastModified());
        assertTrue(f.lastModified() != 100);
    }
}
