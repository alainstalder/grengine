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
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultUrlSourceTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromUrlPlusGetters() throws Exception {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 22");
        URL url = file.toURI().toURL();
        UrlSource s = new DefaultUrlSource(url);
        assertThat(s.getId(), is(url.toString()));
        assertThat(s.getUrl(), is(url));
        assertThat(s.getLastModified(), is(0L));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultUrlSource[ID=" + s.getId() + "]"));
    }
    
    @Test
    public void testConstructFromUrlWithUrlNull() {
        try {
            new DefaultUrlSource(null);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("URL is null."));
        }
    }
        
    @Test
    public void testEquals() throws Exception {
        URL url = new File(tempFolder.getRoot(), "MyScript.groovy").toURI().toURL();
        URL url2 = new File(tempFolder.getRoot(), "MyScript2.groovy").toURI().toURL();
        assertThat(new DefaultUrlSource(url), is(new DefaultUrlSource(url)));
        assertThat(new DefaultUrlSource(url).equals(new DefaultUrlSource(url2)), is(false));
        assertThat(new DefaultUrlSource(url).equals("different class"), is(false));
        assertThat(new DefaultUrlSource(url).equals(null), is(false));
    }

}
