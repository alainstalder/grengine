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

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class DefaultUrlSourceTest {

    @Test
    void testConstructFromUrlPlusGetters() throws Exception {

        // given

        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 22");
        final URL url = file.toURI().toURL();

        // when

        final DefaultUrlSource s = new DefaultUrlSource(url);

        // then

        assertThat(s.getId(), is(url.toString()));
        assertThat(s.getUrl(), is(url));
        assertThat(s.getLastModified(), is(0L));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultUrlSource[ID=" + s.getId() + "]"));
    }
    
    @Test
    void testConstructFromUrlWithUrlNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultUrlSource(null),
                "URL is null.");
    }
        
    @Test
    void testEquals() throws Exception {

        // given

        final File dir = createTestDir();
        final URL url = new File(dir, "MyScript.groovy").toURI().toURL();
        final URL url2 = new File(dir, "MyScript2.groovy").toURI().toURL();

        // when

        final DefaultUrlSource s = new DefaultUrlSource(url);

        // then

        assertThat(s, is(new DefaultUrlSource(url)));
        assertThat(s.equals(new DefaultUrlSource(url2)), is(false));
        assertThat(s.equals("different class"), is(false));
        assertThat(s.equals(null), is(false));
    }

}
