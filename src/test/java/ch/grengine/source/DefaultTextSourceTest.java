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
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;


public class DefaultTextSourceTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromTextPlusGetters() {
        String text = "println 55";
        TextSource s = new DefaultTextSource(text);
        assertEquals("/groovy/script/Script" + SourceUtil.md5(text), s.getId());
        assertEquals(0, s.getLastModified());
        assertEquals(text, s.getText());
        System.out.println(s);
        assertEquals("DefaultTextSource[ID=" + s.getId() + ", text='" + s.getText() +"']", s.toString());
    }
    
    @Test
    public void testConstructFromTextAndNamePlusGetters() {
        String text = "println 55";
        String name = "FirstScript";
        TextSource s = new DefaultTextSource(text, name);
        assertEquals("/groovy/script/Script" + SourceUtil.md5(text) + "/" + name, s.getId());
        assertEquals(0, s.getLastModified());
        assertEquals(text, s.getText());
        System.out.println(s);
        assertEquals("DefaultTextSource[ID=" + s.getId() + ", text='" + s.getText() +"']", s.toString());
    }

    @Test
    public void testConstructFromTextWithTextNull() {
        try {
            new DefaultTextSource(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Text is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructFromTextAndNameWithTextNull() {
        try {
            new DefaultTextSource(null, "name");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Text is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromTextAndNameWithNameNull() {
        try {
            new DefaultTextSource("println 33", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Desired class name is null.", e.getMessage());
        }
    }
    
    @Test
    public void testLongText() {
        String text = "println " + TestUtil.multiply("1", 300);
        TextSource s = new DefaultTextSource(text);
        assertEquals("/groovy/script/Script" + SourceUtil.md5(text), s.getId());
        assertEquals(0, s.getLastModified());
        assertEquals(text, s.getText());
        String expectedText = "println " + TestUtil.multiply("1", 188) + "[..]";
        assertEquals("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText +"']", s.toString());
    }
    
    @Test
    public void testTextWithLinebreaks() {
        String text = "class Class1 {\nstatic class Sub {} }\r\nclass Side {}";
        TextSource s = new DefaultTextSource(text);
        assertEquals("/groovy/script/Script" + SourceUtil.md5(text), s.getId());
        assertEquals(0, s.getLastModified());
        assertEquals(text, s.getText());
        String expectedText = "class Class1 {%nstatic class Sub {} }%nclass Side {}";
        assertEquals("DefaultTextSource[ID=" + s.getId() + ", text='" + expectedText +"']", s.toString());
    }
    
    @Test
    public void testEquals() {
        String text = "println 11";
        String text2 = "println 22";
        assertEquals(new DefaultTextSource(text), new DefaultTextSource(text));
        assertFalse(new DefaultTextSource(text).equals(new DefaultTextSource(text2)));
        assertFalse(new DefaultTextSource(text).equals(new DefaultTextSource(text2)));
        assertFalse(new DefaultTextSource(text).equals("different class"));
        assertFalse(new DefaultTextSource(text).equals(null));
    }

}
