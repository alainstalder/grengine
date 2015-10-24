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

import ch.grengine.TestUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultTextSourceTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructFromTextPlusGetters() {
        String text = "println 55";
        TextSource s = new DefaultTextSource(text);
        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + s.getText() + "']"));
    }

    @Test
    public void testConstructFromTextAndNamePlusGetters() {
        String text = "println 55";
        String name = "FirstScript";
        TextSource s = new DefaultTextSource(text, name);
        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text) + "/" + name));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + s.getText() + "']"));
    }

    @Test
    public void testConstructFromTextWithTextNull() {
        try {
            new DefaultTextSource(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Text is null."));
        }
    }

    @Test
    public void testConstructFromTextAndNameWithTextNull() {
        try {
            new DefaultTextSource(null, "name");
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Text is null."));
        }
    }

    @Test
    public void testConstructFromTextAndNameWithNameNull() {
        try {
            new DefaultTextSource("println 33", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Desired class name is null."));
        }
    }

    @Test
    public void testLongText() {
        String text = "println " + TestUtil.multiply("1", 300);
        TextSource s = new DefaultTextSource(text);
        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        String expectedText = "println " + TestUtil.multiply("1", 188) + "[..]";
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText + "']"));
    }

    @Test
    public void testTextWithLinebreaks() {
        String text = "class Class1 {\nstatic class Sub {} }\r\nclass Side {}";
        TextSource s = new DefaultTextSource(text);
        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        String expectedText = "class Class1 {%nstatic class Sub {} }%nclass Side {}";
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText + "']"));
    }

    @Test
    public void testEquals() {
        String text = "println 11";
        String text2 = "println 22";
        assertThat(new DefaultTextSource(text), is(new DefaultTextSource(text)));
        assertThat(new DefaultTextSource(text).equals(new DefaultTextSource(text2)), is(false));
        assertThat(new DefaultTextSource(text).equals(new DefaultTextSource(text2)), is(false));
        assertThat(new DefaultTextSource(text).equals("different class"), is(false));
        assertThat(new DefaultTextSource(text).equals(null), is(false));
    }

}
