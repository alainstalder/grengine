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

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DefaultFileSourceTest {

    @Test
    void testConstructFromFilePlusGetters() throws IOException {

        // given

        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 22");

        // when

        final FileSource s = new DefaultFileSource(file);

        // then

        assertThat(s.getId(), is(file.getCanonicalPath()));
        assertThat(s.getFile().getPath(), is(file.getCanonicalPath()));
        assertThat(s.getLastModified(), is(file.lastModified()));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultFileSource[ID=" + s.getId() + "]"));
    }
    
    @Test
    void testConstructFromFileWithFileNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> new DefaultFileSource(null),
                "File is null.");
    }

    @Test
    void testConstructorFromFileExceptionGetCanonicalFile() {

        // given

        final File file = new File(TestUtil.FileThatThrowsInGetCanonicalFile.ABSOLUTE_PATH);

        // when

        final FileSource s = new DefaultFileSource(new TestUtil.FileThatThrowsInGetCanonicalFile());

        // then

        assertThat(s.getFile().getPath(), is(file.getPath()));
        assertThat(s.getFile().getPath().contains(".."), is(true));
    }

    @Test
    void testEquals() throws Exception {

        // given

        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        final File file2 = new File(dir, "MyScript2.groovy");

        // when

        final FileSource s = new DefaultFileSource(file);

        // then

        assertThat(s, is(new DefaultFileSource(file)));
        assertThat(s, is(new DefaultFileSource(file.getAbsoluteFile())));
        assertThat(s.equals(new DefaultFileSource(file2)), is(false));
        assertThat(s.equals("different class"), is(false));
        assertThat(s.equals(null), is(false));
    }

}
