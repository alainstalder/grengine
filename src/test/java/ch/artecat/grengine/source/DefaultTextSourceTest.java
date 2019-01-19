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

package ch.artecat.grengine.source;

import ch.artecat.grengine.TestUtil;

import org.junit.jupiter.api.Test;

import static ch.artecat.grengine.TestUtil.assertThrowsMessageIs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class DefaultTextSourceTest {

    @Test
    void testConstructFromTextPlusGetters() {

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
    void testConstructFromTextAndNamePlusGetters() {

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
    void testConstructFromTextWithTextNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultTextSource(null),
                "Text is null.");
    }

    @Test
    void testConstructFromTextAndNameWithTextNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultTextSource(null, "name"),
                "Text is null.");
    }

    @Test
    void testConstructFromTextAndNameWithNameNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultTextSource("println 33", null),
                "Desired class name is null.");
    }

    @Test
    void testLongText() {

        // given

        final String text = "println " + TestUtil.repeatString("1", 300);

        // when

        final TextSource s = new DefaultTextSource(text);

        // then

        assertThat(s.getId(), is("/groovy/script/Script" + SourceUtil.md5(text)));
        assertThat(s.getLastModified(), is(0L));
        assertThat(s.getText(), is(text));
        final String expectedText = "println " + TestUtil.repeatString("1", 188) + "[..]";
        assertThat(s.toString(), is("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText + "']"));
    }

    @Test
    void testTextWithLineBreaks() {

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
    void testEquals() {

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
