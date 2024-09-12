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
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static ch.grengine.TestUtil.assertThrowsMessageStartsWith;
import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class SourceUtilTest {

    @Test
    void testConstructor() {
        new SourceUtil();
    }
    
    @Test
    void testTextsToSourceSetNoNamesWithFactoryWithCollection() {

        // given

        final String text1 = "println 1";
        final String text2 = "println 2";
        final List<String> texts = Arrays.asList(text1, text2);

        // when

        final Set<Source> sourceSet = SourceUtil.textsToSourceSet(new DefaultSourceFactory(), texts);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultTextSource(text1)), is(true));
        assertThat(sourceSet.contains(new DefaultTextSource(text2)), is(true));
    }
    
    @Test
    void testTextsToSourceSetNoNamesWithFactoryWithArrayVarargs() {

        // given

        final String text1 = "println 1";
        final String text2 = "println 2";

        // when

        final Set<Source> sourceSet = SourceUtil.textsToSourceSet(new DefaultSourceFactory(), text1, text2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultTextSource(text1)), is(true));
        assertThat(sourceSet.contains(new DefaultTextSource(text2)), is(true));
    }

    @Test
    void testTextsToSourceSetNoNamesNoFactoryWithCollection() {

        // given

        final String text1 = "println 1";
        final String text2 = "println 2";
        final List<String> texts = Arrays.asList(text1, text2);

        // when

        final Set<Source> sourceSet = SourceUtil.textsToSourceSet(texts);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultTextSource(text1)), is(true));
        assertThat(sourceSet.contains(new DefaultTextSource(text2)), is(true));
    }
    
    @Test
    void testTextsToSourceSetNoNamesNoFactoryWithArrayVarargs() {

        // given

        final String text1 = "println 1";
        final String text2 = "println 2";

        // when

        final Set<Source> sourceSet = SourceUtil.textsToSourceSet(text1, text2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultTextSource(text1)), is(true));
        assertThat(sourceSet.contains(new DefaultTextSource(text2)), is(true));
    }
    
    @Test
    void testTextsToSourceSetWithNamesWithFactory() {

        // given

        final String text1 = "println 1";
        final String text2 = "println 2";
        final String name1 = "Script1";
        final String name2 = "Script2";
        final Map<String,String> texts = new HashMap<>();
        texts.put(name1, text1);
        texts.put(name2, text2);

        // when

        final Set<Source> sourceSet = SourceUtil.textsToSourceSet(new DefaultSourceFactory(), texts);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultTextSource(text1, name1)), is(true));
        assertThat(sourceSet.contains(new DefaultTextSource(text2, name2)), is(true));
    }

    @Test
    void testTextsToSourceSetWithNamesNoFactory() {

        // given

        final String text1 = "println 1";
        final String text2 = "println 2";
        final String name1 = "Script1";
        final String name2 = "Script2";
        final Map<String,String> texts = new HashMap<>();
        texts.put(name1, text1);
        texts.put(name2, text2);

        // when

        final Set<Source> sourceSet = SourceUtil.textsToSourceSet(texts);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultTextSource(text1, name1)), is(true));
        assertThat(sourceSet.contains(new DefaultTextSource(text2, name2)), is(true));
    }
    
    
    @Test
    void testFilesToSourceSetWithFactoryWithCollection() {

        // given

        final File file1 = new File("Script1.groovy");
        final File file2 = new File("Script2.groovy");
        final List<File> files = Arrays.asList(file1, file2);

        // when

        final Set<Source> sourceSet = SourceUtil.filesToSourceSet(new DefaultSourceFactory(), files);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultFileSource(file1)), is(true));
        assertThat(sourceSet.contains(new DefaultFileSource(file2)), is(true));
    }

    @Test
    void testFilesToSourceSetWithFactoryWithArrayVarargs() {

        // given

        final File file1 = new File("Script1.groovy");
        final File file2 = new File("Script2.groovy");

        // when

        final Set<Source> sourceSet = SourceUtil.filesToSourceSet(new DefaultSourceFactory(), file1, file2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultFileSource(file1)), is(true));
        assertThat(sourceSet.contains(new DefaultFileSource(file2)), is(true));
    }

    @Test
    void testFilesToSourceSetNoFactoryWithCollection() {

        // given

        final File file1 = new File("Script1.groovy");
        final File file2 = new File("Script2.groovy");
        final List<File> files = Arrays.asList(file1, file2);

        // when

        final Set<Source> sourceSet = SourceUtil.filesToSourceSet(files);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultFileSource(file1)), is(true));
        assertThat(sourceSet.contains(new DefaultFileSource(file2)), is(true));
    }

    @Test
    void testFilesToSourceSetNoFactoryWithArrayVarargs() {

        // given

        final File file1 = new File("Script1.groovy");
        final File file2 = new File("Script2.groovy");

        // when

        final Set<Source> sourceSet = SourceUtil.filesToSourceSet(file1, file2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultFileSource(file1)), is(true));
        assertThat(sourceSet.contains(new DefaultFileSource(file2)), is(true));
    }

    
    @Test
    void testUrlsToSourceSetWithFactoryWithCollection() throws Exception {

        // given

        final URL url1 = new URL("http://foo.bar/Script1.groovy");
        final URL url2 = new URL("http://foo.bar/Script2.groovy");
        final List<URL> urls = Arrays.asList(url1, url2);

        // when

        final Set<Source> sourceSet = SourceUtil.urlsToSourceSet(new DefaultSourceFactory(), urls);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultUrlSource(url1)), is(true));
        assertThat(sourceSet.contains(new DefaultUrlSource(url2)), is(true));
    }

    @Test
    void testUrlsToSourceSetWithFactoryWithArrayVarargs() throws Exception {

        // given

        final URL url1 = new URL("http://foo.bar/Script1.groovy");
        final URL url2 = new URL("http://foo.bar/Script2.groovy");

        // when

        final Set<Source> sourceSet = SourceUtil.urlsToSourceSet(new DefaultSourceFactory(), url1, url2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultUrlSource(url1)), is(true));
        assertThat(sourceSet.contains(new DefaultUrlSource(url2)), is(true));
    }

    @Test
    void testUrlsToSourceSetNoFactoryWithCollection() throws Exception {

        // given

        final URL url1 = new URL("http://foo.bar/Script1.groovy");
        final URL url2 = new URL("http://foo.bar/Script2.groovy");
        final List<URL> urls = Arrays.asList(url1, url2);

        // when

        final Set<Source> sourceSet = SourceUtil.urlsToSourceSet(urls);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultUrlSource(url1)), is(true));
        assertThat(sourceSet.contains(new DefaultUrlSource(url2)), is(true));
    }

    @Test
    void testUrlsToSourceSetNoFactoryWithArrayVarargs() throws Exception {

        // given

        final URL url1 = new URL("http://foo.bar/Script1.groovy");
        final URL url2 = new URL("http://foo.bar/Script2.groovy");

        // when

        final Set<Source> sourceSet = SourceUtil.urlsToSourceSet(url1, url2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(new DefaultUrlSource(url1)), is(true));
        assertThat(sourceSet.contains(new DefaultUrlSource(url2)), is(true));
    }

    
    @Test
    void testSourceToSourceSet() {

        // given

        final Source source = new DefaultTextSource("println 1");

        // when

        final Set<Source> sourceSet = SourceUtil.sourceToSourceSet(source);

        // then

        assertThat(sourceSet.size(), is(1));
        assertThat(sourceSet.contains(source), is(true));
    }
    
    @Test
    void testSourceCollectionToSourceSet() {

        // given

        final Source source1 = new DefaultTextSource("println 1");
        final Source source2 = new DefaultTextSource("println 2");
        final List<Source> sourceList = Arrays.asList(source1, source2);

        // when

        final Set<Source> sourceSet = SourceUtil.sourceCollectionToSourceSet(sourceList);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(source1), is(true));
        assertThat(sourceSet.contains(source2), is(true));
    }
    
    @Test
    void testSourceArrayVarargsToSourceSet() {

        // given

        final Source source1 = new DefaultTextSource("println 1");
        final Source source2 = new DefaultTextSource("println 2");

        // when

        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(source1, source2);

        // then

        assertThat(sourceSet.size(), is(2));
        assertThat(sourceSet.contains(source1), is(true));
        assertThat(sourceSet.contains(source2), is(true));
    }
    

    @Test
    void testMd5() {

        // when/then (RFC 1321 test vectors)

        assertThat(SourceUtil.md5(""), is("D41D8CD98F00B204E9800998ECF8427E"));
        assertThat(SourceUtil.md5("a"), is("0CC175B9C0F1B6A831C399E269772661"));
        assertThat(SourceUtil.md5("abc"), is("900150983CD24FB0D6963F7D28E17F72"));
        assertThat(SourceUtil.md5("message digest"), is("F96B697D7CB7938D525A2F31AAF161D0"));
        assertThat(SourceUtil.md5("abcdefghijklmnopqrstuvwxyz"), is("C3FCD3D76192E4007DFB496CCA67E13B"));
        assertThat(SourceUtil.md5("ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz0123456789"), is("D174AB98D277D9F5A5611C2C9F419D9F"));
        assertThat(SourceUtil.md5("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), is("57EDF4A22BE3C955AC49DA2E2107B67A"));
    }
    
    @Test
    void testHash() {

        // when/then

        assertThat(SourceUtil.hash("abc", "MD5"), is("900150983CD24FB0D6963F7D28E17F72"));
    }
    
    @Test
    void testHashUnsupportedDigestAlgorithm() {

        // when/then

        assertThrowsMessageIs(UnsupportedOperationException.class,
                () -> SourceUtil.hash("abc", "BatHash"),
                "No message digest BatHash.");
    }
    
    @Test
    void testGetTextStartNoLineBreaksNullText() {

        // when/then

        assertThat(SourceUtil.getTextStartNoLineBreaks(null, 0), is(nullValue()));
    }
    
    @Test
    void testGetTextStartNoLineBreaksNegMaxLen() {

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> SourceUtil.getTextStartNoLineBreaks("hello", -1),
                "Max len (-1) is negative.");
    }
    
    @Test
    void testGetTextStartNoLineBreaksMaxLenLessThan10() {

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> SourceUtil.getTextStartNoLineBreaks("hello", 9),
                "Max len (9) must be at least 10.");
    }

    @Test
    void testGetTextStartNoLineBreaks() {
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello", 10), is("hello"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello1", 10), is("hello1"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello12", 10), is("hello12"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello123", 10), is("hello123"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello1234", 10), is("hello1234"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello12345", 10), is("hello12345"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello123456", 10), is("hello1[..]"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello1234567", 10), is("hello1[..]"));

        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\n", 10), is("hello%n"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\r", 10), is("hello%n"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\r\n", 10), is("hello%n"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\n\r", 10), is("hello%n%n"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\n1", 10), is("hello%n1"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\n12", 10), is("hello%n12"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\n123", 10), is("hello%n123"));
        assertThat(SourceUtil.getTextStartNoLineBreaks("hello\n1234", 10), is("hello%[..]"));
    }

    @Test
    void testReadUrlText() throws Exception {

        // given

        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        final String text = "println 55";
        TestUtil.setFileText(file, text);
        final URL url = file.toURI().toURL();

        // when

        final String textRead = SourceUtil.readUrlText(url, "UTF-8");

        // then

        assertThat(textRead, is(text));
    }
    
    @Test
    void testReadUrlTextEmptyText() throws Exception {

        // given

        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        final String text = "";
        TestUtil.setFileText(file, text);
        final URL url = file.toURI().toURL();

        // when

        final String textRead = SourceUtil.readUrlText(url, "UTF-8");

        // then

        assertThat(textRead, is(text));
    }
    
    @Test
    void testReadUrlTextNothingThere() throws Exception {

        // given

        final File dir = createTestDir();
        final File file = new File(dir, "MyScript.groovy");
        final URL url = file.toURI().toURL();

        // when/then

        assertThrowsMessageStartsWith(IOException.class,
                () -> SourceUtil.readUrlText(url, "UTF-8"),
                "Could not open stream for URL '" + url + "':");
    }

}
