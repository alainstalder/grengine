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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import groovy.grape.GrapeEngine;
import groovy.lang.GroovySystem;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.function.Executable;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


public class TestUtil {

    public static final int JDK_MAJOR_VERSION;
    static {
        String v = System.getProperty("java.specification.version");
        JDK_MAJOR_VERSION = Integer.parseInt(v.startsWith("1.") ? v.substring(2) : v);
    }
    public static final int GROOVY_MAJOR_VERSION = Integer.parseInt(GroovySystem.getVersion().split("\\.")[0]);

    public static final GrengineCopyClassLoader GRENGINE_COPY_CLASS_LOADER =
            new GrengineCopyClassLoader(TestUtil.class.getClassLoader());

    public static class FileThatThrowsInGetCanonicalFile extends File {
        @Serial
        private static final long serialVersionUID = 8273644872772335157L;
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

    public static String serializeObject(final Object obj) {
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
             final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return fail(e);
        }
    }

    public static Object deserializeObject(final String s) {
        final byte[] data = Base64.getDecoder().decode(s);
        try (final ByteArrayInputStream bais = new ByteArrayInputStream(data);
             final ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return fail(e);
        }
    }

    /**
     * Activates Grape with separately loaded Grengine classes,
     * with the default lock.
     */
    public static void grapeActivateWithCopy() {
        callCopy(Grengine.Grape.class, "activate", new Class<?>[]{});
    }

    /**
     * Activates Grape with separately loaded Grengine classes,
     * with the given lock.
     *
     * @param lock lock to use; uses default lock if null
     */
    public static void grapeActivateWithCopy(final Object lock) {
        callCopy(Grengine.Grape.class, "activate", new Class<?>[]{Object.class}, lock);
    }

    /**
     * Deactivates Grape with separately loaded Grengine classes.
     */
    public static void grapeDeactivateWithCopy() {
        callCopy(Grengine.Grape.class, "deactivate", new Class<?>[]{});
    }

    public static Object callCopy(final Class<?> clazz, final String methodName,
            final Class<?>[] methodArgClasses, final Object... callArgs) {
        try {
            final Method method = GRENGINE_COPY_CLASS_LOADER.loadClass(clazz.getName())
                    .getDeclaredMethod(methodName, methodArgClasses);
            method.setAccessible(true);
            return method.invoke(null, callArgs);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            return fail(e);
        } catch (ReflectiveOperationException e) {
            return fail(e);
        }
    }

    // ASM of Groovy 2.5 cannot load JDK 25 bytecode, which is involved in some unit tests
    // here if run under JDK 25 (or later). The workaround is to turn off ASM resolving,
    // which however comes at a certain performance cost.
    // https://issues.apache.org/jira/browse/GROOVY-12467
    // "There aren't any plans to do another 2.5.X release."
    public static boolean hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus() {
        return JDK_MAJOR_VERSION >= 25 && GROOVY_MAJOR_VERSION < 3;
    }

    // Since Groovy 6, Grape engines are an optional Maven dependency.
    public static boolean hasGroovyGrapeAsOptionalDependency() {
        return GROOVY_MAJOR_VERSION >= 6;
    }

    // The Maven GrapeEngine from groovy-grape-maven has a reported bug
    // https://issues.apache.org/jira/browse/GROOVY-12458 as of Groovy 6.0.0.
    // As of 2026-10-22 the bug is fixed in the Groovy master branch, but at
    // is not yet visible into which version(s) a fix would come.
    // Once a fix is released, the check here can be made more specific.
    public static boolean hasIssue12458MavenGrapeEngine() {
        final GrapeEngine engine = groovy.grape.Grape.getInstance();
        return engine != null && "groovy.grape.maven.GrapeMaven".equals(engine.getClass().getName());
    }

}
