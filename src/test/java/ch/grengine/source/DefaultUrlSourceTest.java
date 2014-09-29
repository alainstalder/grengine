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
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;


public class DefaultUrlSourceTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromUrlPlusGetters() throws Exception {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 22");
        URL url = file.toURI().toURL();
        UrlSource s = new DefaultUrlSource(url);
        assertEquals(url.toString(), s.getId());
        assertEquals(url, s.getUrl());
        assertEquals(0,  s.getLastModified());
        System.out.println(s);
        assertEquals("DefaultUrlSource[ID=" + s.getId() + "]", s.toString());
    }
    
    @Test
    public void testConstructFromUrlWithUrlNull() {
        try {
            new DefaultUrlSource(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("URL is null.", e.getMessage());
        }
    }
        
    @Test
    public void testEquals() throws Exception {
        URL url = new File(tempFolder.getRoot(), "MyScript.groovy").toURI().toURL();
        URL url2 = new File(tempFolder.getRoot(), "MyScript2.groovy").toURI().toURL();
        assertEquals(new DefaultUrlSource(url), new DefaultUrlSource(url));
        assertFalse(new DefaultUrlSource(url).equals(new DefaultUrlSource(url2)));
        assertFalse(new DefaultUrlSource(url).equals("different class"));
        assertFalse(new DefaultUrlSource(url).equals(null));
    }

}
