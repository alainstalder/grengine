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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;


public class MockFileTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testMockFile() throws Exception {

        // given

        File f = new File(tempFolder.getRoot(), "file");
        TestUtil.setFileText(f, "dummy");
        File fMod = new File(tempFolder.getRoot(), "file.lastModified");

        // when

        MockFile mock = new MockFile(f.getAbsolutePath());

        // then

        assertThat(f.exists(), is(true));
        assertThat(fMod.exists(), is(true));
        assertThat(TestUtil.getFileText(fMod), is("0"));
        assertThat(mock.lastModified(), is(0L));
        assertThat(f.lastModified(), is(not(0L)));

        // when

        assertThat(mock.setLastModified(100), is(true));

        // then

        assertThat(TestUtil.getFileText(fMod), is("100"));
        assertThat(mock.lastModified(), is(100L));
        assertThat(f.lastModified(), is(not(100L)));
    }

}
