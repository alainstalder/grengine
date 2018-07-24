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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class TestUtil {
        
    public static class FileThatThrowsInGetCanonicalFile extends File {
        private static final long serialVersionUID = -3224104992041563195L;
        public static final String ABSOLUTE_PATH = "/fallback/../to/absolute/path";
        public FileThatThrowsInGetCanonicalFile() { super(ABSOLUTE_PATH); }
        @Override public File getCanonicalFile() throws IOException { throw new IOException(); }
        @Override public File getAbsoluteFile() { return this; }
    }

    @FunctionalInterface
    public interface CodeExpectedToThrow {
        void run() throws Throwable;
    }

    private enum MessageAssertionMode { IS, CONTAINS, STARTS_WITH }

    /**
     * Asserts that the given code throws the expected throwable with the expected message.
     *
     * @param code Code to run that is expected to throw
     * @param expectedClass expected throwable class
     * @param expectedMessage expected throwable message, may be null
     */
    public static void assertThrows(final CodeExpectedToThrow code,
                                    final Class<? extends Throwable> expectedClass,
                                    final String expectedMessage) {
        assertThrowsInternal(code, expectedClass, expectedMessage, MessageAssertionMode.IS);
    }

    /**
     * Asserts that the given code throws the expected throwable with the expected message.
     *
     * @param code Code to run that is expected to throw
     * @param expectedClass expected throwable class
     * @param expectedMessagePart text expected to be contained in throwable message
     */
    public static void assertThrowsContains(final CodeExpectedToThrow code,
                                            final Class<? extends Throwable> expectedClass,
                                            final String expectedMessagePart) {
        assertThrowsInternal(code, expectedClass, expectedMessagePart, MessageAssertionMode.CONTAINS);
    }

    /**
     * Asserts that the given code throws the expected throwable and the message.
     *
     * @param code Code to run that is expected to throw
     * @param expectedClass expected throwable class
     * @param expectedMessageStart text expected to be at the start of the throwable message
     */
    public static void assertThrowsStartsWith(final CodeExpectedToThrow code,
                                              final Class<? extends Throwable> expectedClass,
                                              final String expectedMessageStart) {
        assertThrowsInternal(code, expectedClass, expectedMessageStart, MessageAssertionMode.STARTS_WITH);
    }

    private static void assertThrowsInternal(final CodeExpectedToThrow code,
                                             final Class<? extends Throwable> expectedClass,
                                             final String expectedMessage,
                                             final MessageAssertionMode mode) {
        boolean thrown;
        try {
            code.run();
            thrown = false;
        } catch (Throwable t) {
            thrown = true;
            assertThat("Expected " + expectedClass.getName() + " but got " + t.getClass().getName(),
                    t.getClass().equals(expectedClass), is(true));
            switch (mode) {
                case IS:
                    assertThat(t.getMessage(), is(expectedMessage));
                    break;
                case CONTAINS:
                    assertThat(t.getMessage(), containsString(expectedMessage));
                    break;
                case STARTS_WITH:
                    assertThat(t.getMessage(), startsWith(expectedMessage));
                    break;
            }
        }
        if (!thrown) {
            fail("expected to throw");
        }
    }


    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> argsToMap(final Object... args) {
        assertThat(args.length % 2 == 0, is(true));
        final Map<K,V> map = new HashMap<>();
        boolean isKey = true;
        K key = null;
        for (Object arg : args) {
            if (isKey) {
                key = (K)arg;
            } else {
                map.put(key, (V)arg);
            }
            isKey = !isKey;
        }
        return map;
    }
    
    public static String multiply(final String s, final int nTimes) {
        final StringBuilder out = new StringBuilder();
        for (int i=0; i<nTimes; i++) {
            out.append(s);
        }
        return out.toString();
    }
    
    public static void setFileText(final File file, final String text)
            throws FileNotFoundException, UnsupportedEncodingException {
        final PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(text);
        writer.close();
    }
    
    public static String getFileText(final File file) throws FileNotFoundException {
        try (final Scanner scan = new Scanner(file, StandardCharsets.UTF_8.name())) {
            scan.useDelimiter("\\A");
            return scan.hasNext() ? scan.next() : "";
        }
    }

}
