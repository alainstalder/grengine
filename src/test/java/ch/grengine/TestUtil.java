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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestUtil {
        
    public static class FileThatThrowsInGetCanonicalFile extends File {
        private static final long serialVersionUID = -3224104992041563195L;
        public static final String ABSOLUTE_PATH = "/fallback/../to/absolute/path";
        public FileThatThrowsInGetCanonicalFile() { super(ABSOLUTE_PATH); }
        @Override public File getCanonicalFile() throws IOException { throw new IOException(); }
        @Override public File getAbsoluteFile() { return this; }
    }

    public static File createTestDir() throws IOException {
        final File testDir = Files.createTempDirectory("gren").toFile();
        FileUtils.forceDeleteOnExit(testDir);
        return testDir;
    }

    public static void setFileText(final File file, final String text) throws IOException {
        FileUtils.writeStringToFile(file, text, StandardCharsets.UTF_8);
    }

    public static String getFileText(final File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * Asserts that the given code throws the expected throwable
     * and that its message contains the expected text.
     *
     * @param expectedType expected throwable class
     * @param executable code to run that is expected to throw
     * @param expectedMessagePart text expected to be contained in throwable message
     */
    public static <T extends Throwable> void assertThrowsContains(
            final Class<T> expectedType, final Executable executable, final String expectedMessagePart) {
        final Throwable t = assertThrows(expectedType, executable);
        assertThat(t.getMessage(), containsString(expectedMessagePart));
    }

    /**
     * Asserts that the given code throws the expected throwable
     * and that its message starts with the expected text.
     *
     * @param expectedType expected throwable class
     * @param executable code to run that is expected to throw
     * @param expectedMessageStart text expected to start the throwable message
     */
    public static <T extends Throwable> void assertThrowsStartsWith(
            final Class<T> expectedType, final Executable executable, final String expectedMessageStart) {
        final Throwable t = assertThrows(expectedType, executable);
        assertThat(t.getMessage(), startsWith(expectedMessageStart));
    }

    /**
     * Repeats the given string n times.
     *
     * @param s string to repeat
     * @param n number of times to repeat
     * @return string repeated n times
     */
    public static String multiply(final String s, final int n) {
        final StringBuilder out = new StringBuilder();
        for (int i=0; i<n; i++) {
            out.append(s);
        }
        return out.toString();
    }

}
