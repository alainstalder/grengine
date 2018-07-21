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

package ch.grengine;

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static ch.grengine.TestUtil.assertThrowsContains;
import static ch.grengine.TestUtil.assertThrowsStartsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class TestUtilTest {

    @Test
    public void testAssertThrowsDoesNotThrow() {
        try {
            assertThrows(() -> {},
                    NullPointerException.class,
                    "Must not be null.");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("expected to throw"));
        }
    }

    @Test
    public void testAssertThrowsThrowsWrongExceptionClass() {
        try {
            assertThrows(() -> { throw new IllegalArgumentException(); },
                    NullPointerException.class,
                    "Must not be null.");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("" +
                    "Expected java.lang.NullPointerException but got java.lang.IllegalArgumentException\n" +
                    "Expected: is <true>\n" +
                    "     but: was <false>"));
        }
    }

    @Test
    public void testAssertThrowsThrowsWrongExceptionClassSuperClassExpected() {
        try {
            assertThrows(() -> { throw new IllegalArgumentException(); },
                    Exception.class,
                    "Must not be null.");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("" +
                    "Expected java.lang.Exception but got java.lang.IllegalArgumentException\n" +
                    "Expected: is <true>\n" +
                    "     but: was <false>"));
        }
    }

    @Test
    public void testAssertThrowsThrowsWrongExceptionMessageActualNull() {
        try {
            assertThrows(() -> { throw new NullPointerException(); },
                    NullPointerException.class,
                    "Must not be null.");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("\n" +
                    "Expected: is \"Must not be null.\"\n" +
                    "     but: was null"));
        }
    }

    @Test
    public void testAssertThrowsThrowsWrongExceptionMessageExpectedNull() {
        try {
            assertThrows(() -> { throw new NullPointerException("Must not be null."); },
                    NullPointerException.class,
                    null);
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("\n" +
                    "Expected: is null\n" +
                    "     but: was \"Must not be null.\""));
        }
    }

    @Test
    public void testAssertThrowsThrowsWrongExceptionMessageBothNotNull() {
        try {
            assertThrows(() -> { throw new NullPointerException("Argument must not be null."); },
                    NullPointerException.class,
                    "Must not be null.");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("\n" +
                    "Expected: is \"Must not be null.\"\n" +
                    "     but: was \"Argument must not be null.\""));
        }
    }

    @Test
    public void testAssertThrowsBothExceptionMessagesNull() {
        try {
            assertThrows(() -> { throw new NullPointerException(); },
                    NullPointerException.class,
                    null);
        } catch (AssertionError e) {
            fail();
        }
    }

    @Test
    public void testAssertThrowsMessagesCompare() {
        assertThrows(() -> { throw new NullPointerException("null"); },
                NullPointerException.class,
                "null");
        try {
            assertThrows(() -> { throw new NullPointerException("null ..."); },
                    NullPointerException.class,
                    "null");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("\n" +
                    "Expected: is \"null\"\n" +
                    "     but: was \"null ...\""));
        }
    }

    @Test
    public void testAssertThrowsContainsMessagesCompare() {
        assertThrowsContains(() -> { throw new NullPointerException("null"); },
                NullPointerException.class,
                "null");
        assertThrowsContains(() -> { throw new NullPointerException("null ..."); },
                NullPointerException.class,
                "null");
        assertThrowsContains(() -> { throw new NullPointerException("... null ..."); },
                NullPointerException.class,
                "null");
    }

    @Test
    public void testAssertThrowsStartsWithMessagesCompare() {
        assertThrowsStartsWith(() -> { throw new NullPointerException("null"); },
                NullPointerException.class,
                "null");
        assertThrowsStartsWith(() -> { throw new NullPointerException("null ..."); },
                NullPointerException.class,
                "null");
        try {
            assertThrowsStartsWith(() -> { throw new NullPointerException("... null ..."); },
                    NullPointerException.class,
                    "null");
            fail();
        } catch (AssertionError e) {
            assertThat(e.getMessage(), is("\n" +
                    "Expected: a string starting with \"null\"\n" +
                    "     but: was \"... null ...\""));
        }
    }

}
