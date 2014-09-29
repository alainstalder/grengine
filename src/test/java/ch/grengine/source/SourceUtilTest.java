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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;


public class SourceUtilTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructor() {
        new SourceUtil();
    }
    
    @Test
    public void testTextsToSourceSetNoNamesWithFactoryWithCollection() {
        String text1 = "println 1";
        String text2 = "println 2";
        List<String> texts = TestUtil.argsToList(text1, text2);
        Set<Source> sourceSet = SourceUtil.textsToSourceSet(new DefaultSourceFactory(), texts);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultTextSource(text1)));
        assertTrue(sourceSet.contains(new DefaultTextSource(text2)));
    }
    
    @Test
    public void testTextsToSourceSetNoNamesWithFactoryWithArrayVarargs() {
        String text1 = "println 1";
        String text2 = "println 2";
        Set<Source> sourceSet = SourceUtil.textsToSourceSet(new DefaultSourceFactory(), text1, text2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultTextSource(text1)));
        assertTrue(sourceSet.contains(new DefaultTextSource(text2)));
    }

    @Test
    public void testTextsToSourceSetNoNamesNoFactoryWithCollection() {
        String text1 = "println 1";
        String text2 = "println 2";
        List<String> texts = TestUtil.argsToList(text1, text2);
        Set<Source> sourceSet = SourceUtil.textsToSourceSet(texts);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultTextSource(text1)));
        assertTrue(sourceSet.contains(new DefaultTextSource(text2)));
    }
    
    @Test
    public void testTextsToSourceSetNoNamesNoFactoryWithArrayVarargs() {
        String text1 = "println 1";
        String text2 = "println 2";
        Set<Source> sourceSet = SourceUtil.textsToSourceSet(text1, text2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultTextSource(text1)));
        assertTrue(sourceSet.contains(new DefaultTextSource(text2)));
    }
    
    @Test
    public void testTextsToSourceSetWithNamesWithFactory() {
        String text1 = "println 1";
        String text2 = "println 2";
        String name1 = "Script1";
        String name2 = "Script2";
        Map<String,String> texts = TestUtil.argsToMap(name1, text1, name2, text2);
        Set<Source> sourceSet = SourceUtil.textsToSourceSet(new DefaultSourceFactory(), texts);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultTextSource(text1, name1)));
        assertTrue(sourceSet.contains(new DefaultTextSource(text2, name2)));
    }

    @Test
    public void testTextsToSourceSetWithNamesNoFactory() {
        String text1 = "println 1";
        String text2 = "println 2";
        String name1 = "Script1";
        String name2 = "Script2";
        Map<String,String> texts = TestUtil.argsToMap(name1, text1, name2, text2);
        Set<Source> sourceSet = SourceUtil.textsToSourceSet(texts);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultTextSource(text1, name1)));
        assertTrue(sourceSet.contains(new DefaultTextSource(text2, name2)));
    }
    
    
    @Test
    public void testFilesToSourceSetWithFactoryWithCollection() {
        File file1 = new File("Script1.groovy");
        File file2 = new File("Script2.groovy");
        List<File> files = TestUtil.argsToList(file1, file2);
        Set<Source> sourceSet = SourceUtil.filesToSourceSet(new DefaultSourceFactory(), files);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultFileSource(file1)));
        assertTrue(sourceSet.contains(new DefaultFileSource(file2)));
    }

    @Test
    public void testFilesToSourceSetWithFactoryWithArrayVarargs() {
        File file1 = new File("Script1.groovy");
        File file2 = new File("Script2.groovy");
        Set<Source> sourceSet = SourceUtil.filesToSourceSet(new DefaultSourceFactory(), file1, file2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultFileSource(file1)));
        assertTrue(sourceSet.contains(new DefaultFileSource(file2)));
    }

    @Test
    public void testFilesToSourceSetNoFactoryWithCollection() {
        File file1 = new File("Script1.groovy");
        File file2 = new File("Script2.groovy");
        List<File> files = TestUtil.argsToList(file1, file2);
        Set<Source> sourceSet = SourceUtil.filesToSourceSet(files);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultFileSource(file1)));
        assertTrue(sourceSet.contains(new DefaultFileSource(file2)));
    }

    @Test
    public void testFilesToSourceSetNoFactoryWithArrayVarargs() {
        File file1 = new File("Script1.groovy");
        File file2 = new File("Script2.groovy");
        Set<Source> sourceSet = SourceUtil.filesToSourceSet(file1, file2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultFileSource(file1)));
        assertTrue(sourceSet.contains(new DefaultFileSource(file2)));
    }

    
    @Test
    public void testUrlsToSourceSetWithFactoryWithCollection() throws Exception {
        URL url1 = new URL("http://foo.bar/Script1.groovy");
        URL url2 = new URL("http://foo.bar/Script2.groovy");
        List<URL> urls = TestUtil.argsToList(url1, url2);
        Set<Source> sourceSet = SourceUtil.urlsToSourceSet(new DefaultSourceFactory(), urls);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultUrlSource(url1)));
        assertTrue(sourceSet.contains(new DefaultUrlSource(url2)));
    }

    @Test
    public void testUrlsToSourceSetWithFactoryWithArrayVarargs() throws Exception {
        URL url1 = new URL("http://foo.bar/Script1.groovy");
        URL url2 = new URL("http://foo.bar/Script2.groovy");
        Set<Source> sourceSet = SourceUtil.urlsToSourceSet(new DefaultSourceFactory(), url1, url2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultUrlSource(url1)));
        assertTrue(sourceSet.contains(new DefaultUrlSource(url2)));
    }

    @Test
    public void testUrlsToSourceSetNoFactoryWithCollection() throws Exception {
        URL url1 = new URL("http://foo.bar/Script1.groovy");
        URL url2 = new URL("http://foo.bar/Script2.groovy");
        List<URL> urls = TestUtil.argsToList(url1, url2);
        Set<Source> sourceSet = SourceUtil.urlsToSourceSet(urls);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultUrlSource(url1)));
        assertTrue(sourceSet.contains(new DefaultUrlSource(url2)));
    }

    @Test
    public void testUrlsToSourceSetNoFactoryWithArrayVarargs() throws Exception {
        URL url1 = new URL("http://foo.bar/Script1.groovy");
        URL url2 = new URL("http://foo.bar/Script2.groovy");
        Set<Source> sourceSet = SourceUtil.urlsToSourceSet(url1, url2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(new DefaultUrlSource(url1)));
        assertTrue(sourceSet.contains(new DefaultUrlSource(url2)));
    }

    
    @Test
    public void testSourceToSourceSet() {
        Source source = new DefaultTextSource("println 1");
        Set<Source> sourceSet = SourceUtil.sourceToSourceSet(source);
        assertEquals(1, sourceSet.size());
        assertTrue(sourceSet.contains(source));
    }
    
    @Test
    public void testSourceCollectionToSourceSet() {
        Source source1 = new DefaultTextSource("println 1");
        Source source2 = new DefaultTextSource("println 2");
        List<Source> sourceList = TestUtil.argsToList(source1, source2);
        Set<Source> sourceSet = SourceUtil.sourceCollectionToSourceSet(sourceList);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(source1));
        assertTrue(sourceSet.contains(source2));
    }
    
    @Test
    public void testSourceArrayVarargsToSourceSet() {
        Source source1 = new DefaultTextSource("println 1");
        Source source2 = new DefaultTextSource("println 2");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(source1, source2);
        assertEquals(2, sourceSet.size());
        assertTrue(sourceSet.contains(source1));
        assertTrue(sourceSet.contains(source2));
    }
    

    @Test
    public void testMd5() {
        // RFC 1321 test vectors
        assertEquals("D41D8CD98F00B204E9800998ECF8427E", SourceUtil.md5(""));
        assertEquals("0CC175B9C0F1B6A831C399E269772661", SourceUtil.md5("a"));
        assertEquals("900150983CD24FB0D6963F7D28E17F72", SourceUtil.md5("abc"));
        assertEquals("F96B697D7CB7938D525A2F31AAF161D0", SourceUtil.md5("message digest"));
        assertEquals("C3FCD3D76192E4007DFB496CCA67E13B", SourceUtil.md5("abcdefghijklmnopqrstuvwxyz"));
        assertEquals("D174AB98D277D9F5A5611C2C9F419D9F",
                SourceUtil.md5("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"));
        assertEquals("57EDF4A22BE3C955AC49DA2E2107B67A",
                SourceUtil.md5("12345678901234567890123456789012345678901234567890123456789012345678901234567890"));
    }
    
    @Test
    public void testHash() {
        assertEquals("900150983CD24FB0D6963F7D28E17F72", SourceUtil.hash("abc", "MD5"));
    }
    
    @Test
    public void testHashUnsupportedDigestAlgorithm() {
        try {
            SourceUtil.hash("abc", "BatHash");
            fail();
        } catch (UnsupportedOperationException e) {
            assertEquals("No message digest BatHash.", e.getMessage());
        }
    }
    
    @Test
    public void testGetTextStartNoLineBreaksNullText() {
        assertNull(SourceUtil.getTextStartNoLinebreaks(null, 0));
    }
    
    @Test
    public void testGetTextStartNoLineBreaksNegMaxLen() {
        try {
            SourceUtil.getTextStartNoLinebreaks("hello", -1);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Max len (-1) is negative.", e.getMessage());
        }
    }
    
    @Test
    public void testGetTextStartNoLineBreaksMaxLenLessThan10() {
        try {
            SourceUtil.getTextStartNoLinebreaks("hello", 9);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Max len (9) must be at least 10.", e.getMessage());
        }
    }

    @Test
    public void testGetTextStartNoLineBreaks() {
        assertEquals("hello", SourceUtil.getTextStartNoLinebreaks("hello", 10));
        assertEquals("hello1", SourceUtil.getTextStartNoLinebreaks("hello1", 10));
        assertEquals("hello12", SourceUtil.getTextStartNoLinebreaks("hello12", 10));
        assertEquals("hello123", SourceUtil.getTextStartNoLinebreaks("hello123", 10));
        assertEquals("hello1234", SourceUtil.getTextStartNoLinebreaks("hello1234", 10));
        assertEquals("hello12345", SourceUtil.getTextStartNoLinebreaks("hello12345", 10));
        assertEquals("hello1[..]", SourceUtil.getTextStartNoLinebreaks("hello123456", 10));
        assertEquals("hello1[..]", SourceUtil.getTextStartNoLinebreaks("hello1234567", 10));
        
        assertEquals("hello%n", SourceUtil.getTextStartNoLinebreaks("hello\n", 10));
        assertEquals("hello%n", SourceUtil.getTextStartNoLinebreaks("hello\r", 10));
        assertEquals("hello%n", SourceUtil.getTextStartNoLinebreaks("hello\r\n", 10));
        assertEquals("hello%n%n", SourceUtil.getTextStartNoLinebreaks("hello\n\r", 10));
        assertEquals("hello%n1", SourceUtil.getTextStartNoLinebreaks("hello\n1", 10));
        assertEquals("hello%n12", SourceUtil.getTextStartNoLinebreaks("hello\n12", 10));
        assertEquals("hello%n123", SourceUtil.getTextStartNoLinebreaks("hello\n123", 10));
        assertEquals("hello%[..]", SourceUtil.getTextStartNoLinebreaks("hello\n1234", 10));
    }
    
    @Test
    public void testReadUrlText() throws Exception {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        String text = "println 55";
        TestUtil.setFileText(file, text);
        URL url = file.toURI().toURL();
        String textRead = SourceUtil.readUrlText(url, "UTF-8");
        assertEquals(text, textRead);
    }
    
    @Test
    public void testReadUrlTextEmptyText() throws Exception {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        String text = "";
        TestUtil.setFileText(file, text);
        URL url = file.toURI().toURL();
        String textRead = SourceUtil.readUrlText(url, "UTF-8");
        assertEquals(text, textRead);
    }
    
    @Test
    public void testReadUrlTextNothingThere() throws Exception {
        File file = new File(tempFolder.getRoot(), "MyScript.groovy");
        URL url = file.toURI().toURL();
        try {
           SourceUtil.readUrlText(url, "UTF-8");
           fail();
        } catch(IOException e) {
            assertTrue(e.getMessage().startsWith("Could not open stream for URL '" + url + "':"));
        }
    }

}
