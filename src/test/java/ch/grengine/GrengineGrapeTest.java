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
import java.util.Collections;
import java.util.List;

import groovy.grape.Grape;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Test;

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

    @Test
    public void testHelloWorldWithGrape() {
        Grengine.Grape.newGrengine().run("@Grab('com.google.guava:guava:18.0')\n"
                + "import com.google.common.base.Ascii\n" +
                "println \"Grape: 'C' is upper case: ${Ascii.isUpperCase('C' as char)}\"");
    }

    @Test
    public void testNoGrapeByDefault() {
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
    public void testConstructEmpty() {

        // when

        Grengine gren = Grengine.Grape.newGrengine();

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

    }

    @Test
    public void testConstructEmpty_parent() {

        // given

        GroovyClassLoader parent = new GroovyClassLoader();

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

    }

    @Test
    public void testConstructEmpty_config() {

        // given

        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = Grengine.Grape.newGrengine(config);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructEmpty_parent_config() {

        // given

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, config);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    public void testConstructDir() {

        // when

        Grengine gren = Grengine.Grape.newGrengine(new File("."));

        // then

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
    public void testConstructDir_parent() {

        // given

        GroovyClassLoader parent = new GroovyClassLoader();

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, new File("."));

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDir_config() {

        // given

        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = Grengine.Grape.newGrengine(config, new File("."));

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDir_parent_config() {

        // given

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, config, new File("."));

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    public void testConstructDirDirMode() {

        // when

        Grengine gren = Grengine.Grape.newGrengine(new File("."), DirMode.NO_SUBDIRS);

        // then

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
    public void testConstructDirDirMode_parent() {

        // given

        GroovyClassLoader parent = new GroovyClassLoader();

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, new File("."), DirMode.NO_SUBDIRS);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDirDirMode_config() {

        // given

        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = Grengine.Grape.newGrengine(config, new File("."), DirMode.NO_SUBDIRS);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructDirDirMode_parent_config() {

        // given

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, config, new File("."), DirMode.NO_SUBDIRS);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    public void testConstructUrl() throws Exception {

        // given

        List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        Grengine gren = Grengine.Grape.newGrengine(urls);

        // then

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

        // given

        GroovyClassLoader parent = new GroovyClassLoader();
        List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, urls);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

        config = getCompilerConfigFromFixedSetSources(gren);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    public void testConstructUrl_config() throws Exception {

        // given

        CompilerConfiguration config = new CompilerConfiguration();
        List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        Grengine gren = Grengine.Grape.newGrengine(config, urls);

        // then

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

        // given

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        Grengine gren = Grengine.Grape.newGrengine(parent, config, urls);

        // then

        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromFixedSetSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    private static CompilerConfiguration getCompilerConfigFromEngineTopCodeCache(LayeredEngine engine) {
        CompilerFactory compilerFactory = ((DefaultTopCodeCacheFactory)engine.getBuilder().getTopCodeCacheFactory())
                .getCompilerFactory();
        return ((DefaultGroovyCompilerFactory)compilerFactory).getCompilerConfiguration();
    }

    private static CompilerConfiguration getCompilerConfigFromDirBasedSources(Grengine gren) {
        DirBasedSources sources = (DirBasedSources)gren.getBuilder().getSourcesLayers().get(0);
        CompilerFactory compilerFactory = sources.getBuilder().getCompilerFactory();
        return ((DefaultGroovyCompilerFactory)compilerFactory).getCompilerConfiguration();
    }

    private static CompilerConfiguration getCompilerConfigFromFixedSetSources(Grengine gren) {
        FixedSetSources sources = (FixedSetSources)gren.getBuilder().getSourcesLayers().get(0);
        CompilerFactory compilerFactory = sources.getBuilder().getCompilerFactory();
        return ((DefaultGroovyCompilerFactory)compilerFactory).getCompilerConfiguration();
    }


    @Test
    public void testConstructGrape() {
        new Grengine.Grape();
    }

    @Test
    public void testActivateDeactivate() {

        try {
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

            // when

            Grengine.Grape.activate();

            // then

            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));

            // when

            Grengine.Grape.activate();

            // then

            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));

            // when

            Grengine.Grape.deactivate();

            // then

            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

            // when

            Grengine.Grape.deactivate();

            // then

            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

        } finally {
            Grengine.Grape.deactivate();
        }

    }

    @Test
    public void testActivateDifferentLock() {

        try {
            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrapeIvy"));

            // when

            Grengine.Grape.activate(new Object());

            // then

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

