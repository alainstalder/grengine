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

package ch.grengine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import groovy.lang.GroovyClassLoader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.engine.LayeredEngine;
import ch.grengine.except.CreateException;
import ch.grengine.sources.DirBasedSources;

/**
 * Tests interaction of Grengine and Groovy Grape.
 * 
 * In principle, a GroovyClassLoader (or a RootLoader) must be present
 * somewhere up the class loader parent hierarchy when using Grape to
 * pull external dependencies, else a RuntimeException is thrown with
 * message "No suitable ClassLoader found for grab".
 * 
 * With Grengine, a GroovyClassLoader is normally only used for compilation,
 * but not at runtime, so if Grape is needed, a GroovyClassLoader should
 * be set as parent of the LayeredEngine (or somewhere further up).
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
    public void testGrapeSingleScript_WithoutGroovyClassLoader() throws Exception {
        
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
    public void testGrapeSingleScript_WithGroovyClassLoader() throws Exception {
        
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();

        LayeredEngine layeredEngine = new LayeredEngine.Builder()
            .setParent(groovyClassLoader)
            .build();
        
        Grengine gren = new Grengine.Builder()
            .setEngine(layeredEngine)
            .build();
        
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
    public void testTwoScripts_WithGroovyClassLoader() throws Exception {
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

        LayeredEngine layeredEngine = new LayeredEngine.Builder()
            .setParent(groovyClassLoader)
            .build();
        
        Grengine gren = new Grengine.Builder()
            .setEngine(layeredEngine)
            .setSourcesLayers(new DirBasedSources.Builder(dir).build())
            .build();
        
        assertNull(gren.getLastUpdateException());
        assertEquals(true, gren.run(f1));
        assertEquals(true, gren.run("return Util.isUpperCase('C' as char)"));
    }

}

