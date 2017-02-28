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

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.except.CreateException;
import ch.grengine.load.DefaultTopCodeCacheFactory;
import ch.grengine.sources.DirBasedSources;
import ch.grengine.sources.DirMode;
import ch.grengine.sources.FixedSetSources;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import groovy.grape.Grape;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests the respective class.
 * 
 * @author Alain Stalder
 *
 */
public class GrengineGrapeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testHelloWorldWithGrape() throws Exception {
        Grengine.Grape.newGrengine().run("@Grab('com.google.guava:guava:18.0')\n"
                + "import com.google.common.base.Ascii\n" +
                "println \"Grape: 'C' is upper case: ${Ascii.isUpperCase('C' as char)}\"");
    }

    @Test
    public void testNoGrapeByDefault() throws Exception {
        try {
            new Grengine().run("@Grab('com.google.guava:guava:18.0')\n"
                    + "import com.google.common.base.Ascii\n" +
                    "println \"Grape: 'C' is upper case: ${Ascii.isUpperCase('C' as char)}\"");
            fail("must throw");
        } catch (Throwable t) {
            //t.printStackTrace();
            assertThat(t, instanceOf(CreateException.class));
            t = t.getCause();
            assertThat(t, instanceOf(ExceptionInInitializerError.class));
            t = t.getCause();
            assertThat(t.getClass().getCanonicalName(), is(RuntimeException.class.getCanonicalName()));
            assertThat(t.getMessage(), is("No suitable ClassLoader found for grab"));
        }
    }

    @Test
    public void testConstructEmpty() throws Exception {

        Grengine gren = Grengine.Grape.newGrengine();

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

    }

    @Test
    public void testConstructEmpty_parent() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        Grengine gren = Grengine.Grape.newGrengine(parent);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

    }

    @Test
    public void testConstructEmpty_config() throws Exception {

        CompilerConfiguration config = new CompilerConfiguration();
        Grengine gren = Grengine.Grape.newGrengine(config);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructEmpty_parent_config() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Grengine gren = Grengine.Grape.newGrengine(parent, config);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    public void testConstructDir() throws Exception {

        Grengine gren = Grengine.Grape.newGrengine(new File("."));

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDir_parent() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        Grengine gren = Grengine.Grape.newGrengine(parent, new File("."));

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDir_config() throws Exception {

        CompilerConfiguration config = new CompilerConfiguration();
        Grengine gren = Grengine.Grape.newGrengine(config, new File("."));

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDir_parent_config() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Grengine gren = Grengine.Grape.newGrengine(parent, config, new File("."));

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    public void testConstructDirDirMode() throws Exception {

        Grengine gren = Grengine.Grape.newGrengine(new File("."), DirMode.NO_SUBDIRS);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDirDirMode_parent() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        Grengine gren = Grengine.Grape.newGrengine(parent, new File("."), DirMode.NO_SUBDIRS);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDirDirMode_config() throws Exception {

        CompilerConfiguration config = new CompilerConfiguration();
        Grengine gren = Grengine.Grape.newGrengine(config, new File("."), DirMode.NO_SUBDIRS);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDirDirMode_parent_config() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Grengine gren = Grengine.Grape.newGrengine(parent, config, new File("."), DirMode.NO_SUBDIRS);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    public void testConstructUrl() throws Exception {

        List<URL> urls = Arrays.asList(new File(".").toURI().toURL());
        Grengine gren = Grengine.Grape.newGrengine(urls);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromFixedSetSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructUrl_parent() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        List<URL> urls = Arrays.asList(new File(".").toURI().toURL());
        Grengine gren = Grengine.Grape.newGrengine(parent, urls);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromFixedSetSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructUrl_config() throws Exception {

        CompilerConfiguration config = new CompilerConfiguration();
        List<URL> urls = Arrays.asList(new File(".").toURI().toURL());
        Grengine gren = Grengine.Grape.newGrengine(config, urls);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromFixedSetSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructUrl_parent_config() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        List<URL> urls = Arrays.asList(new File(".").toURI().toURL());
        Grengine gren = Grengine.Grape.newGrengine(parent, config, urls);

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance((ClassLoader)parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromFixedSetSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    private CompilerConfiguration getCompilerConfigFromEngineTopCodeCache(LayeredEngine engine) {
        CompilerFactory compilerFactory = ((DefaultTopCodeCacheFactory)engine.getBuilder().getTopCodeCacheFactory())
                .getCompilerFactory();
        return ((DefaultGroovyCompilerFactory)compilerFactory).getCompilerConfiguration();
    }

    private CompilerConfiguration getCompilerConfigFromDirBasedSources(Grengine gren) {
        DirBasedSources sources = (DirBasedSources)gren.getBuilder().getSourcesLayers().get(0);
        CompilerFactory compilerFactory = sources.getBuilder().getCompilerFactory();
        return ((DefaultGroovyCompilerFactory)compilerFactory).getCompilerConfiguration();
    }

    private CompilerConfiguration getCompilerConfigFromFixedSetSources(Grengine gren) {
        FixedSetSources sources = (FixedSetSources)gren.getBuilder().getSourcesLayers().get(0);
        CompilerFactory compilerFactory = sources.getBuilder().getCompilerFactory();
        return ((DefaultGroovyCompilerFactory)compilerFactory).getCompilerConfiguration();
    }


    @Test
    public void testConstructGrape() throws Exception {
        new Grengine.Grape();
    }

    @Test
    public void testActivateDeactivate() throws Exception {

        try {
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

            Grengine.Grape.activate();
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));

            Grengine.Grape.activate();
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));

            Grengine.Grape.deactivate();
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

            Grengine.Grape.deactivate();
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

        } finally {
            Grengine.Grape.deactivate();
        }

    }

    @Test
    public void testActivateDifferentLock() throws Exception {

        try {
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

            Grengine.Grape.activate(new Object());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));

            try {
                Grengine.Grape.activate(new Object());
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(),
                        is("Attempt to change lock for wrapped Grape class (unwrap first)."));
            }

        } finally {
            Grengine.Grape.deactivate();
        }

    }





}

