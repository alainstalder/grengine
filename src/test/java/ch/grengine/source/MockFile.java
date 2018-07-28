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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


// so far, mocks only last modified
@SuppressWarnings("serial")
public class MockFile extends File {

    private final File lastModifiedFile;
    
    public MockFile(final String pathname) {
        super(pathname);
        lastModifiedFile = new File(super.getAbsoluteFile() + ".lastModified");
        if (!lastModifiedFile.exists()) {
            assertThat(setLastModified(0), is(true));
        }
    }

    public MockFile(final File file, final String name) {
        super(file, name);
        lastModifiedFile = new File(super.getAbsoluteFile() + ".lastModified");
        assertThat(setLastModified(0), is(true));
    }

    @Override
    public long lastModified() {
        try {
            return Long.parseLong(TestUtil.getFileText(lastModifiedFile));
        } catch (Exception e) {
            fail(e.toString());
            // never gets here...
            return 0;
        }            
    }
    
    @Override
    public boolean setLastModified(final long lastModified) {
        try {
            TestUtil.setFileText(lastModifiedFile, Long.toString(lastModified));
            return true;
        } catch (Exception e) {
            fail(e.toString());
            // never gets here...
            return false;
        }
    }

}
