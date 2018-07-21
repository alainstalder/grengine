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

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class DefaultTextSourceTest {

    @Test
    public void testConstructFromTextPlusGetters() {

        // given

        final String text = "println 55";

        // when

        final TextSource s = new DefaultTextSource(text);

        // then

        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + s.getText() + "']"));
    }

    @Test
    public void testConstructFromTextAndNamePlusGetters() {

        // given

        final String text = "println 55";
        final String name = "FirstScript";

        // when

        final TextSource s = new DefaultTextSource(text, name);

        // then

        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text) + "/" + name));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        System.out.println(s);
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + s.getText() + "']"));
    }

    @Test
    public void testConstructFromTextWithTextNull() {

        // when/then

        assertThrows(() -> new DefaultTextSource(null),
                NullPointerException.class,
                "Text is null.");
    }

    @Test
    public void testConstructFromTextAndNameWithTextNull() {

        // when/then

        assertThrows(() -> new DefaultTextSource(null, "name"),
                NullPointerException.class,
                "Text is null.");
    }

    @Test
    public void testConstructFromTextAndNameWithNameNull() {

        // when/then

        assertThrows(() -> new DefaultTextSource("println 33", null),
                NullPointerException.class,
                "Desired class name is null.");
    }

    @Test
    public void testLongText() {

        // given

        final String text = "println " + TestUtil.multiply("1", 300);

        // when

        final TextSource s = new DefaultTextSource(text);

        // then

        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        final String expectedText = "println " + TestUtil.multiply("1", 188) + "[..]";
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText + "']"));
    }

    @Test
    public void testTextWithLineBreaks() {

        // given

        final String text = "class Class1 {\nstatic class Sub {} }\r\nclass Side {}";

        // when

        final TextSource s = new DefaultTextSource(text);

        // then

        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        final String expectedText = "class Class1 {%n" + "static class Sub {} }%n" + "class Side {}";
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText + "']"));
    }

    @Test
    public void testEquals() {

        // given

        final String text = "println 11";
        final String text2 = "println 22";

        // when

        final TextSource s = new DefaultTextSource(text);

        // then

        assertThat(s, is(new DefaultTextSource(text)));
        assertThat(s.equals(new DefaultTextSource(text2)), is(false));
        assertThat(s.equals(new DefaultTextSource(text2)), is(false));
        assertThat(s.equals("different class"), is(false));
        assertThat(s.equals(null), is(false));
    }

}
