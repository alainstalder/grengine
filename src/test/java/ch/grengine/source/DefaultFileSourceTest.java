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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;


public class DefaultFileSourceTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromFilePlusGetters() throws IOException {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 22");
        FileSource s = new DefaultFileSource(file);
        assertEquals(file.getCanonicalPath(), s.getId());
        assertEquals(file.getCanonicalPath(), s.getFile().getPath());
        assertEquals(file.lastModified(),  s.getLastModified());
        System.out.println(s);
        assertEquals("DefaultFileSource[ID=" + s.getId() + "]", s.toString());
    }
    
    @Test
    public void testConstructFromFileWithFileNull() {
        try {
            new DefaultFileSource(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("File is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructorFromFileExceptionGetCanonicalFile() {
        FileSource s = new DefaultFileSource(new TestUtil.FileThatThrowsInGetCanonicalFile());
        File file = new File(TestUtil.FileThatThrowsInGetCanonicalFile.ABSOLUTE_PATH);
        assertEquals(file.getPath(), s.getFile().getPath());
        assertTrue(s.getFile().getPath().contains(".."));
    }

    @Test
    public void testEquals() {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        File file2 = new File(tempFolder.getRoot(), "MyScript2.groovy");
        assertEquals(new DefaultFileSource(file), new DefaultFileSource(file));
        assertEquals(new DefaultFileSource(file.getAbsoluteFile()), new DefaultFileSource(file));
        assertFalse(new DefaultFileSource(file).equals(new DefaultFileSource(file2)));
        assertFalse(new DefaultFileSource(file).equals("different class"));
        assertFalse(new DefaultFileSource(file).equals(null));
    }

}
