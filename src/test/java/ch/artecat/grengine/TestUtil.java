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

package ch.artecat.grengine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestUtil {

    @SuppressWarnings("serial")
    public static class FileThatThrowsInGetCanonicalFile extends File {
        public static final String ABSOLUTE_PATH = "/fallback/../to/absolute/path";
        public FileThatThrowsInGetCanonicalFile() { super(ABSOLUTE_PATH); }
        @Override public File getCanonicalFile() throws IOException { throw new IOException(); }
        @Override public File getAbsoluteFile() { return this; }
    }

    /**
     * Create empty test directory, which will be automatically deleted when the JVM exits.
     *
     * @return directory, never null
     *
     * @throws IOException if failed to create directory
     */
    public static File createTestDir() throws IOException {
        final File testDir = Files.createTempDirectory("gren").toFile();
        FileUtils.forceDeleteOnExit(testDir);
        return testDir;
    }

    /**
     * Set file text, using UTF-8 charset.
     *
     * @param file file
     * @param text text
     *
     * @throws IOException if failed to write to the file
     */
    public static void setFileText(final File file, final String text) throws IOException {
        FileUtils.writeStringToFile(file, text, StandardCharsets.UTF_8);
    }

    /**
     * Get file text, using UTF-8 charset.
     *
     * @param file file
     * @return file text, never null
     *
     * @throws IOException if failed to read from the file
     */
    public static String getFileText(final File file) throws IOException {
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

    /**
     * Asserts that the given code throws the expected throwable
     * and that its message is the expected text.
     *
     * @param expectedType expected throwable class
     * @param executable code to run that is expected to throw
     * @param expectedMessage expected throwable message text
     */
    public static <T extends Throwable> void assertThrowsMessageIs(
            final Class<T> expectedType, final Executable executable, final String expectedMessage) {
        final Throwable t = assertThrows(expectedType, executable);
        assertThat(t.getMessage(), is(expectedMessage));
    }

    /**
     * Asserts that the given code throws the expected throwable
     * and that its message contains the expected text.
     *
     * @param expectedType expected throwable class
     * @param executable code to run that is expected to throw
     * @param expectedMessagePart text expected to be contained in throwable message
     */
    public static <T extends Throwable> void assertThrowsMessageContains(
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
    public static <T extends Throwable> void assertThrowsMessageStartsWith(
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
    public static String repeatString(final String s, final int n) {
        return new String(new char[n]).replace("\0", s);
    }

    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static void toRuntimeException(ThrowingRunnable run) {
        try {
            run.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
