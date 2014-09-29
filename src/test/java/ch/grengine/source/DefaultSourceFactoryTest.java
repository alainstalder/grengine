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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;


public class DefaultSourceFactoryTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testFromText() {
        SourceFactory sf = new DefaultSourceFactory();
        String text = "println 'hello'";
        Source s = sf.fromText(text);
        assertTrue(s instanceof DefaultTextSource);
        assertEquals(new DefaultTextSource(text), s);
    }

    @Test
    public void testFromTextWithName() {
        SourceFactory sf = new DefaultSourceFactory();
        String text = "println 'hello'";
        String name = "MyScript";
        Source s = sf.fromText(text, name);
        assertTrue(s instanceof DefaultTextSource);
        assertEquals(new DefaultTextSource(text, name), s);
        assertFalse(s.equals(new DefaultTextSource(text)));
    }
    
    @Test
    public void testFromFile() {
        SourceFactory sf = new DefaultSourceFactory();
        File file = new File("MyScript.groovy");
        Source s = sf.fromFile(file);
        assertTrue(s instanceof DefaultFileSource);
        assertEquals(new DefaultFileSource(file), s);
    }
    
    @Test
    public void testFromUrlNoTracking() throws Exception {
        SourceFactory sf = new DefaultSourceFactory();
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        TestUtil.setFileText(file, "println 1");
        URL url = file.toURI().toURL();
        Source s = sf.fromUrl(url);
        assertTrue(s instanceof DefaultUrlSource);
        assertFalse(s instanceof DefaultSourceFactory.TrackingUrlSource);
        assertEquals(new DefaultUrlSource(url), s);
        assertEquals(0, s.getLastModified());
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
        assertTrue(s instanceof DefaultUrlSource);
        assertTrue(s instanceof DefaultSourceFactory.TrackingUrlSource);
        assertEquals(new DefaultUrlSource(url), s);
        assertFalse(s.getLastModified() == 0);
        
        // wait a bit, last modified must remain unchanged
        long lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertEquals(lastModifiedOld, s.getLastModified());
        // set same script text, last modified must remain unchanged
        TestUtil.setFileText(file, "println 1");
        Thread.sleep(30);
        assertEquals(lastModifiedOld, s.getLastModified());
        // set different script text, last modified must change
        TestUtil.setFileText(file, "println 2");
        Thread.sleep(30);
        assertTrue(lastModifiedOld != s.getLastModified());

        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertEquals(lastModifiedOld, s.getLastModified());
        // clear chache, last modified must change (url must be loaded)
        sf.clearCache();
        Thread.sleep(30);
        assertTrue(lastModifiedOld != s.getLastModified());
        
        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertEquals(lastModifiedOld, s.getLastModified());
        // delete file, last modified must change
        assertTrue(file.delete());
        Thread.sleep(30);
        assertTrue(lastModifiedOld != s.getLastModified());
        
        // wait a bit, last modified must remain unchanged
        lastModifiedOld = s.getLastModified();
        Thread.sleep(30);
        assertEquals(lastModifiedOld, s.getLastModified());
        // set script text back to original, last modified must change
        TestUtil.setFileText(file, "println 1");
        Thread.sleep(30);
        assertTrue(lastModifiedOld != s.getLastModified());
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
        assertTrue(s instanceof DefaultUrlSource);
        assertTrue(s instanceof DefaultSourceFactory.TrackingUrlSource);
        assertEquals(new DefaultUrlSource(url), s);
        assertFalse(s.getLastModified() == 0);
        
        // wait a bit, last modified must remain unchanged
        long lastModifiedOld = s.getLastModified();
        Thread.sleep(80);
        assertEquals(lastModifiedOld, s.getLastModified());
        // set same script text, last modified must remain unchanged
        TestUtil.setFileText(file, "println 1");
        Thread.sleep(80);
        assertEquals(lastModifiedOld, s.getLastModified());
        // set different script text, last modified must change
        TestUtil.setFileText(file, "println 2");
        Thread.sleep(80);
        assertTrue(lastModifiedOld != s.getLastModified());
    }

    @Test
    public void testModifyBuilderAfterUseAndGetBuilder() {
        DefaultSourceFactory.Builder builder = new DefaultSourceFactory.Builder();
        DefaultSourceFactory sf = builder.build();
        try {
            builder.setTrackUrlContent(false);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
        // extra test, get builder
        assertEquals(builder, sf.getBuilder());
    }

}
