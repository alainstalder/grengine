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

package ch.grengine.grapetest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import ch.grengine.Grengine;
import ch.grengine.TestUtil;
import groovy.grape.Grape;
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.engine.LayeredEngine;
import ch.grengine.except.CreateException;
import ch.grengine.sources.DirBasedSources;

/**
 * Simple tests related to using Groovy Grape with Grengine.
 * 
 * First of all, as of Groovy/Grape 2.4.3 and Ivy 2.4.0 (and older versions),
 * Grape/Ivy is not thread-safe, see bug GROOVY-7407. In a nutshell, you can
 * use it to compile/run Groovy scripts with a single instance of
 * GroovyClassLoader (resp. equivalently with a single instance of GroovyShell
 * which contains a single GroovyClassLoader) at a time. Any other instances
 * cannot compile/run Groovy scripts at the same time without the risk of
 * failing due to unprotected concurrent access to some things in Grape/Ivy.
 * 
 * Regarding Grengine and Groovy Grape:
 *   
 * In principle, a GroovyClassLoader (or a RootLoader) must be present
 * somewhere up the class loader parent hierarchy when using Grape to
 * pull external dependencies, else a RuntimeException is thrown with
 * message "No suitable ClassLoader found for grab".
 * 
 * With Grengine, a GroovyClassLoader is normally only used for compilation,
 * but not at runtime, so if Grape is needed, a GroovyClassLoader should
 * be set as parent (of the LayeredEngine, or somewhere further up).
 * 
 * But then you have already at least two different instances of a
 * GroovyClassLoader, one during compilation and one at runtime of Groovy
 * scripts, which will both try to access shared resources without
 * sufficient synchronization in Grape/Ivy and there is no way to
 * synchronize in Grengine. (Using the same GroovyClassLoader instance
 * is not possible because the one used during compilation accumulates
 * compiled Groovy scripts as loaded classes.)
 *
 * Until there is a fix in Groovy, you might want to use the workaround
 * in {@link WorkaroundGroovy7407WrappingGrapeEngine}, see comments there
 * and one of the tests below for how to use it.
 *
 * Link to bug GROOVY-7407: https://issues.apache.org/jira/browse/GROOVY-7407
 *
 * @author Alain Stalder
 *
 */
public class GrengineGrapeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    public static final Class<?> NO_GRAB_EXCEPTION_CLASS = RuntimeException.class;
    public static final String NO_GRAB_EXCEPTION_MESSAGE = "No suitable ClassLoader found for grab";
    
    @Test
    public void testNoGrapeByDefault() throws Exception {
        
        Grengine gren = new Grengine();
        
        try {
            gren.run("@Grab('com.google.guava:guava:18.0')\n"
                    + "import com.google.common.base.Ascii\n" +
                    "println \"Grape: 'C' is upper case: ${Ascii.isUpperCase('C' as char)}\"");
            fail("must throw");
        } catch (Throwable t) {
            //t.printStackTrace();
            assertTrue("must be true", t instanceof CreateException);
            t = t.getCause();
            assertTrue("must be true", t instanceof ExceptionInInitializerError);
            t = t.getCause();
            assertEquals("must be same", NO_GRAB_EXCEPTION_CLASS, t.getClass());
            assertEquals("must be same", NO_GRAB_EXCEPTION_MESSAGE, t.getMessage());
        }
    }

    @Test
    public void testSingleScript() throws Exception {
        
        Grengine gren = new Grengine(new GroovyClassLoader());
        
        try {
            gren.run("@Grab('com.google.guava:guava:18.0')\n"
                    + "import com.google.common.base.Ascii\n" +
                    "println \"Grape: 'C' is upper case: ${Ascii.isUpperCase('C' as char)}\"");
        } catch (Throwable t) {
            t.printStackTrace();
            fail("must not throw");
        }
    }

    @Test
    public void testTwoScripts() throws Exception {
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return Util.isUpperCase('C' as char)");
        File f2 = new File(dir, "Util.groovy");
        TestUtil.setFileText(f2, "@Grab('com.google.guava:guava:18.0')\n"
                        + "import com.google.common.base.Ascii\n"
                        + "class Util {\n"
                        + "  static boolean isUpperCase(def c) {\n"
                        + "    return Ascii.isUpperCase(c)\n"
                        + "  }\n"
                        + "}\n"
        );

        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

        Grengine gren = new Grengine(groovyClassLoader, dir);

        assertNull(gren.getLastUpdateException());
        assertEquals(true, gren.run(f1));
        assertEquals(true, gren.run("return Util.isUpperCase('C' as char)"));
    }

    // This only tests that the workaround does not break anything obvious.
    @Test
    public void testTwoScriptsWithWorkaroundGroovy7407() throws Exception {
        GrapeEngine originalInstance = Grape.getInstance();
        try {
            // Typically you would call this once early per VM, resp. at least
            // once early per ClassLoader that loaded Grape.class.
            WorkaroundGroovy7407WrappingGrapeEngine.createAndSet();

            assertTrue("must be true", originalInstance != Grape.getInstance());

            File dir = tempFolder.getRoot();
            File f1 = new File(dir, "Script1.groovy");
            TestUtil.setFileText(f1, "return Util.isUpperCase('C' as char)");
            File f2 = new File(dir, "Util.groovy");
            TestUtil.setFileText(f2, "@Grab('com.google.guava:guava:18.0')\n"
                            + "import com.google.common.base.Ascii\n"
                            + "class Util {\n"
                            + "  static boolean isUpperCase(def c) {\n"
                            + "    return Ascii.isUpperCase(c)\n"
                            + "  }\n"
                            + "}\n"
            );

            GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

            Grengine gren = new Grengine(groovyClassLoader, dir);

            assertNull(gren.getLastUpdateException());
            assertEquals(true, gren.run(f1));
            assertEquals(true, gren.run("return Util.isUpperCase('C' as char)"));
        } finally {
            // Resetting so that other unit tests don't use this workaround
            WorkaroundGroovy7407WrappingGrapeEngine.setEngine(originalInstance);
        }
    }

}

