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
import ch.grengine.source.DefaultSourceFactory.ContentTrackingUrlSource;
import ch.grengine.source.DefaultSourceFactory.LastModifiedTrackingFileSource;
import ch.grengine.source.DefaultSourceFactory.SourceIdTrackingTextSource;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DefaultSourceFactoryTest {

    @Test
    void testFromTextNoNameNoIdTracking() {

        // given

        final SourceFactory sf = new DefaultSourceFactory();
        final String text = "println 'hello'";

        // when

        final Source s = sf.fromText(text);

        // then

        assertThat(s, instanceOf(DefaultTextSource.class));
        assertThat(s, not(instanceOf(SourceIdTrackingTextSource.class)));
        assertThat(s, is(new DefaultTextSource(text)));

        // when

        final Source s2 = sf.fromText(text);

        // then

        assertThat(s, is(s2));
        assertThat(s.getId(), not(sameInstance(s2.getId())));
        assertThat(text, is(((TextSource) s).getText()));
    }

    @Test
    void testFromTextNoNameWithIdTracking() {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        final String text = "println 'hello'";

        // when

        final Source s = sf.fromText(text);

        // then

        assertThat(s, not(instanceOf(DefaultTextSource.class)));
        assertThat(s, instanceOf(SourceIdTrackingTextSource.class));
        assertThat(s, is(new DefaultTextSource(text)));

        // when

        final Source s2 = sf.fromText(text);

        // then

        assertThat(s, is(s2));
        assertThat(s.getId(), sameInstance(s2.getId()));
        assertThat(text, is(((TextSource) s).getText()));
        assertThat(s.toString(), is("SourceIdTrackingTextSource[ID=" + s.getId() + ", text='" + text + "']"));
    }

    @Test
    void testFromTextWithNameNoIdTracking() {

        // given

        final SourceFactory sf = new DefaultSourceFactory();
        final String text = "println 'hello'";
        final String name = "MyScript";

        // when

        final Source s = sf.fromText(text, name);

        // then

        assertThat(s, instanceOf(DefaultTextSource.class));
        assertThat(s, not(instanceOf(SourceIdTrackingTextSource.class)));
        assertThat(s, is(new DefaultTextSource(text, name)));
        assertThat(s.equals(new DefaultTextSource(text)), is(false));

        // when

        final Source s2 = sf.fromText(text, name);

        // then

        assertThat(s, is(s2));
        assertThat(s.getId(), not(sameInstance(s2.getId())));
        assertThat(text, is(((TextSource) s).getText()));
    }

    @Test
    void testFromTextWithNameWithIdTracking() {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        final String text = "println 'hello'";
        final String name = "MyScript";

        // when

        final Source s = sf.fromText(text, name);

        // then

        assertThat(s, not(instanceOf(DefaultTextSource.class)));
        assertThat(s, instanceOf(SourceIdTrackingTextSource.class));
        assertThat(s, is(new DefaultTextSource(text, name)));
        assertThat(s.equals(new DefaultTextSource(text)), is(false));

        // when

        final Source s2 = sf.fromText(text, name);

        // then

        assertThat(s, is(s2));
        // not the same, cached is only the part of the ID without the desired name
        assertThat(s.getId(), not(sameInstance(s2.getId())));
        assertThat(text, is(((TextSource) s).getText()));
    }
    
    @Test
    void testFromFileNoLastModifiedTracking() throws Exception {

        // given

        final SourceFactory sf = new DefaultSourceFactory();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        final long lastMod = file.lastModified();

        // when

        final Source s = sf.fromFile(file);

        // then

        assertThat(s, instanceOf(DefaultFileSource.class));
        assertThat(s, not(instanceOf(LastModifiedTrackingFileSource.class)));
        assertThat(s, is(new DefaultFileSource(file)));
        assertThat(s.getLastModified(), is(lastMod));
        assertThat(s.getLastModified(), is(lastMod));
    }
        
    @Test
    void testFromFileWithLastModifiedTracking() throws Exception {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackFileSourceLastModified(true)
                .setFileLastModifiedTrackingLatencyMs(50)
                .build();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        final long lastMod = file.lastModified();

        // when

        final Source s = sf.fromFile(file);

        // then

        assertThat(s, instanceOf(DefaultFileSource.class));
        assertThat(s, instanceOf(LastModifiedTrackingFileSource.class));
        assertThat(s, is(new DefaultFileSource(file)));
        assertThat(s.getLastModified(), is(lastMod));
        assertThat(s.getLastModified(), is(lastMod));
       
        while (s.getLastModified() == lastMod) {
            TestUtil.setFileText(file, "println 2");
            Thread.sleep(50);
        }
    }
        
    @Test
    void testFromUrlNoTracking() throws Exception {

        // given

        final SourceFactory sf = new DefaultSourceFactory();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        final URL url = file.toURI().toURL();

        // when

        final Source s = sf.fromUrl(url);

        // then

        assertThat(s, instanceOf(DefaultUrlSource.class));
        assertThat(s, not(instanceOf(ContentTrackingUrlSource.class)));
        assertThat(s, is(new DefaultUrlSource(url)));
        assertThat(s.getLastModified(), is(0L));
    }
    
    @Test
    void testFromUrlWithTrackingNoLatency() throws Exception {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackUrlContent(true)
                .setUrlTrackingLatencyMs(0)
                .build();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        final URL url = file.toURI().toURL();

        // when

        final Source s = sf.fromUrl(url);

        // then

        assertThat(s, instanceOf(DefaultUrlSource.class));
        assertThat(s, instanceOf(ContentTrackingUrlSource.class));
        assertThat(s, is(new DefaultUrlSource(url)));
        assertThat(s.getLastModified(), is(not(0L)));
        
        // wait a bit, last modified must remain unchanged
        long lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (set same script text, last modified must remain unchanged)

        TestUtil.setFileText(file, "println 1");

        // then

        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (set different script text, last modified must change)

        TestUtil.setFileText(file, "println 2");

        // then

        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));

        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (clear cache, last modified must change (url must be loaded))

        sf.clearCache();

        // then

        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
        
        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (delete file, last modified must change)

        assertThat(file.delete(), is(true));

        // then

        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
        
        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (set script text back to original, last modified must change)

        TestUtil.setFileText(file, "println 1");

        // then

        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
    }
    
    @Test
    void testFromUrlWithTrackingWithLatency() throws Exception {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackUrlContent(true)
                .setUrlTrackingLatencyMs(50)
                .build();
        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        final URL url = file.toURI().toURL();

        // when

        final Source s = sf.fromUrl(url);

        // then

        assertThat(s, instanceOf(DefaultUrlSource.class));
        assertThat(s, instanceOf(ContentTrackingUrlSource.class));
        assertThat(s, is(new DefaultUrlSource(url)));
        assertThat(s.getLastModified(), is(not(0L)));
        
        // wait a bit, last modified must remain unchanged
        final long lastModifiedOld = s.getLastModified();
        Thread.sleep(80);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (set same script text, last modified must remain unchanged)

        TestUtil.setFileText(file, "println 1");

        // then

        Thread.sleep(80);
        assertThat(s.getLastModified(), is(lastModifiedOld));

        // when (set different script text, last modified must change)

        TestUtil.setFileText(file, "println 2");

        // then

        Thread.sleep(80);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
    }
    
    @Test
    void testConstructWithTextSourceIdTrackingFromTextWithTextNull() {

        // given

        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();

        // when/then

        assertThrows(NullPointerException.class,
                () -> sf.fromText(null),
                "Text is null.");
    }
    
    @Test
    void testConstructWithTextSourceIdTrackingFromTextAndNameWithTextNull() {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();

        // when/then

        assertThrows(NullPointerException.class,
                () -> sf.fromText(null, "name"),
                "Text is null.");
    }

    @Test
    void testConstructWithTextSourceIdTrackingFromTextAndNameWithNameNull() {

        // given

        final DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();

        // when/then

        assertThrows(NullPointerException.class,
                () -> sf.fromText("println 33", null),
                "Desired class name is null.");
    }


    @Test
    void testModifyBuilderAfterUseAndGetBuilder() {

        // given

        final DefaultSourceFactory.Builder builder = new DefaultSourceFactory.Builder();
        final DefaultSourceFactory sf = builder.build();

        // when/then

        assertThrows(IllegalStateException.class,
                () -> builder.setTrackUrlContent(false),
                "Builder already used.");

        // extra test, get builder
        assertThat(sf.getBuilder(), is(builder));
    }

}
