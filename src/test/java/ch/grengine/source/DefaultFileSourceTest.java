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

package ch.grengine.source;

import ch.grengine.TestUtil;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultFileSourceTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromFilePlusGetters() throws IOException {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 22");
        FileSource s = new DefaultFileSource(file);
        assertThat(s.getId(), is(file.getCanonicalPath()));
        assertThat(s.getFile().getPath(), is(file.getCanonicalPath()));
        assertThat(s.getLastModified(), is(file.lastModified()));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultFileSource[ID=" + s.getId() + "]"));
    }
    
    @Test
    public void testConstructFromFileWithFileNull() {
        try {
            new DefaultFileSource(null);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("File is null."));
        }
    }

    @Test
    public void testConstructorFromFileExceptionGetCanonicalFile() {
        FileSource s = new DefaultFileSource(new TestUtil.FileThatThrowsInGetCanonicalFile());
        File file = new File(TestUtil.FileThatThrowsInGetCanonicalFile.ABSOLUTE_PATH);
        assertThat(s.getFile().getPath(), is(file.getPath()));
        assertThat(s.getFile().getPath().contains(".."), is(true));
    }

    @Test
    public void testEquals() {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        File file2 = new File(tempFolder.getRoot(), "MyScript2.groovy");
        assertThat(new DefaultFileSource(file), is(new DefaultFileSource(file)));
        assertThat(new DefaultFileSource(file), is(new DefaultFileSource(file.getAbsoluteFile())));
        assertThat(new DefaultFileSource(file).equals(new DefaultFileSource(file2)), is(false));
        assertThat(new DefaultFileSource(file).equals("different class"), is(false));
        assertThat(new DefaultFileSource(file).equals(null), is(false));
    }

}
