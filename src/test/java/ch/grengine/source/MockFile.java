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

import static org.junit.Assert.fail;

import java.io.File;

import ch.grengine.TestUtil;


// so far, mocks only last modified
public class MockFile extends File {
    
    private static final long serialVersionUID = -1009288987865961842L;
    
    private File lastModifiedFile;
    
    public MockFile(String pathname) {
        super(pathname);
        lastModifiedFile = new File(super.getAbsoluteFile() + ".lastModified");
        if (!lastModifiedFile.exists()) {
            setLastModified(0);
        }
    }
        
    public MockFile(File file, String name) {
        super(file, name);
        lastModifiedFile = new File(super.getAbsoluteFile() + ".lastModified");
        setLastModified(0);
    }
        
    @Override
    public long lastModified() {
        try {
            return Long.parseLong(TestUtil.getFileText(lastModifiedFile));
        } catch (Exception e) {
            fail(e.toString());
            return 0;
        }            
    }
    
    @Override
    public boolean setLastModified(long lastModified) {
        try {
            TestUtil.setFileText(lastModifiedFile, Long.toString(lastModified));
            return true;
        } catch (Exception e) {
            fail(e.toString());
            return false;
        }
    }

}
