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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultSourceFactoryTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testFromTextNoNameNoIdTracking() {
        SourceFactory sf = new DefaultSourceFactory();
        String text = "println 'hello'";
        Source s = sf.fromText(text);
        assertThat(s, instanceOf(DefaultTextSource.class));
        assertThat(s, not(instanceOf(SourceIdTrackingTextSource.class)));
        assertThat(s, is((Source)new DefaultTextSource(text)));
        Source s2 = sf.fromText(text);
        assertThat(s, is(s2));
        assertThat(s.getId(), not(sameInstance(s2.getId())));
        assertThat(text, is(((TextSource) s).getText()));
    }

    @Test
    public void testFromTextNoNameWithIdTracking() {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        String text = "println 'hello'";
        Source s = sf.fromText(text);
        assertThat(s, not(instanceOf(DefaultTextSource.class)));
        assertThat(s, instanceOf(SourceIdTrackingTextSource.class));
        assertThat(s, is((Source)new DefaultTextSource(text)));
        Source s2 = sf.fromText(text);
        assertThat(s, is(s2));
        assertThat(s.getId(), sameInstance(s2.getId()));
        assertThat(text, is(((TextSource) s).getText()));
        assertThat(s.toString(), is("SourceIdTrackingTextSource[ID=" + s.getId() + ", text='" + text + "']"));
    }

    @Test
    public void testFromTextWithNameNoIdTracking() {
        SourceFactory sf = new DefaultSourceFactory();
        String text = "println 'hello'";
        String name = "MyScript";
        Source s = sf.fromText(text, name);
        assertThat(s, instanceOf(DefaultTextSource.class));
        assertThat(s, not(instanceOf(SourceIdTrackingTextSource.class)));
        assertThat(s, is((Source)new DefaultTextSource(text, name)));
        assertThat(s.equals(new DefaultTextSource(text)), is(false));
        Source s2 = sf.fromText(text, name);
        assertThat(s, is(s2));
        assertThat(s.getId(), not(sameInstance(s2.getId())));
        assertThat(text, is(((TextSource) s).getText()));
    }

    @Test
    public void testFromTextWithNameWithIdTracking() {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        String text = "println 'hello'";
        String name = "MyScript";
        Source s = sf.fromText(text, name);
        assertThat(s, not(instanceOf(DefaultTextSource.class)));
        assertThat(s, instanceOf(SourceIdTrackingTextSource.class));
        assertThat(s, is((Source)new DefaultTextSource(text, name)));
        assertThat(s.equals(new DefaultTextSource(text)), is(false));
        Source s2 = sf.fromText(text, name);
        assertThat(s, is(s2));
        // not the same, cached is only the part of the ID without the desired name
        assertThat(s.getId(), not(sameInstance(s2.getId())));
        assertThat(text, is(((TextSource) s).getText()));
    }
    
    @Test
    public void testFromFileNoLastModifiedTracking() throws Exception {
        SourceFactory sf = new DefaultSourceFactory();
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        long lastMod = file.lastModified();
        Source s = sf.fromFile(file);
        assertThat(s, instanceOf(DefaultFileSource.class));
        assertThat(s, not(instanceOf(LastModifiedTrackingFileSource.class)));
        assertThat(s, is((Source)new DefaultFileSource(file)));
        assertThat(s.getLastModified(), is(lastMod));
        assertThat(s.getLastModified(), is(lastMod));
    }
        
    @Test
    public void testFromFileWithLastModifiedTracking() throws Exception {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackFileSourceLastModified(true)
                .setFileLastModifiedTrackingLatencyMs(50)
                .build();
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        long lastMod = file.lastModified();
        Source s = sf.fromFile(file);
        assertThat(s, instanceOf(DefaultFileSource.class));
        assertThat(s, instanceOf(LastModifiedTrackingFileSource.class));
        assertThat(s, is((Source)new DefaultFileSource(file)));
        assertThat(s.getLastModified(), is(lastMod));
        assertThat(s.getLastModified(), is(lastMod));
       
        while (s.getLastModified() == lastMod) {
            TestUtil.setFileText(file, "println 2");
            Thread.sleep(50);
        }
    }
        
    @Test
    public void testFromUrlNoTracking() throws Exception {
        SourceFactory sf = new DefaultSourceFactory();
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        URL url = file.toURI().toURL();
        Source s = sf.fromUrl(url);
        assertThat(s, instanceOf(DefaultUrlSource.class));
        assertThat(s, not(instanceOf(ContentTrackingUrlSource.class)));
        assertThat(s, is((Source)new DefaultUrlSource(url)));
        assertThat(s.getLastModified(), is(0L));
    }
    
    @Test
    public void testFromUrlWithTrackingNoLatency() throws Exception {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackUrlContent(true)
                .setUrlTrackingLatencyMs(0)
                .build();
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        URL url = file.toURI().toURL();
        Source s = sf.fromUrl(url);
        assertThat(s, instanceOf(DefaultUrlSource.class));
        assertThat(s, instanceOf(ContentTrackingUrlSource.class));
        assertThat(s, is((Source)new DefaultUrlSource(url)));
        assertThat(s.getLastModified(), is(not(0L)));
        
        // wait a bit, last modified must remain unchanged
        long lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // set same script text, last modified must remain unchanged
        TestUtil.setFileText(file, "println 1");
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // set different script text, last modified must change
        TestUtil.setFileText(file, "println 2");
        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));

        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // clear cache, last modified must change (url must be loaded)
        sf.clearCache();
        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
        
        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // delete file, last modified must change
        assertThat(file.delete(), is(true));
        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
        
        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // set script text back to original, last modified must change
        TestUtil.setFileText(file, "println 1");
        Thread.sleep(30);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
    }
    
    @Test
    public void testFromUrlWithTrackingWithLatency() throws Exception {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackUrlContent(true)
                .setUrlTrackingLatencyMs(50)
                .build();
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        URL url = file.toURI().toURL();
        Source s = sf.fromUrl(url);
        assertThat(s, instanceOf(DefaultUrlSource.class));
        assertThat(s, instanceOf(ContentTrackingUrlSource.class));
        assertThat(s, is((Source)new DefaultUrlSource(url)));
        assertThat(s.getLastModified(), is(not(0L)));
        
        // wait a bit, last modified must remain unchanged
        long lastModifiedOld = s.getLastModified();
        Thread.sleep(80);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // set same script text, last modified must remain unchanged
        TestUtil.setFileText(file, "println 1");
        Thread.sleep(80);
        assertThat(s.getLastModified(), is(lastModifiedOld));
        // set different script text, last modified must change
        TestUtil.setFileText(file, "println 2");
        Thread.sleep(80);
        assertThat(lastModifiedOld, is(not(s.getLastModified())));
    }
    
    @Test
    public void testConstructWithTextSourceIdTrackingFromTextWithTextNull() {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        try {
            sf.fromText((String)null);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("Text is null."));
        }
    }
    
    @Test
    public void testConstructWithTextSourceIdTrackingFromTextAndNameWithTextNull() {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        try {
            sf.fromText((String)null, "name");
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("Text is null."));
        }
    }

    @Test
    public void testConstructWithTextSourceIdTrackingFromTextAndNameWithNameNull() {
        DefaultSourceFactory sf = new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .build();
        try {
            sf.fromText("println 33", (String)null);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("Desired class name is null."));
        }
    }


    @Test
    public void testModifyBuilderAfterUseAndGetBuilder() {
        DefaultSourceFactory.Builder builder = new DefaultSourceFactory.Builder();
        DefaultSourceFactory sf = builder.build();
        try {
            builder.setTrackUrlContent(false);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
        // extra test, get builder
        assertThat(sf.getBuilder(), is(builder));
    }

}
