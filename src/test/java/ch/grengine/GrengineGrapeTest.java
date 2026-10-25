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
import ch.grengine.except.CompileException;
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
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static ch.grengine.TestUtil.grapeActivateWithCopy;
import static ch.grengine.TestUtil.grapeDeactivateWithCopy;
import static ch.grengine.TestUtil.hasIssue12458MavenGrapeEngine;
import static ch.grengine.TestUtil.hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


class GrengineGrapeTest {

    private static final String GRAPE_SCRIPT = "" +
            "@Grab('com.google.guava:guava:18.0')\n" +
            "import com.google.common.base.Ascii\n" +
            "println \"Grape: 'C' is upper case: ${Ascii.isUpperCase('C' as char)}\"";

    @Test
    void testHelloWorldWithGrape_unwrapped() {

        printVersions();
        try {
            Grengine.Grape.newGrengine().run(GRAPE_SCRIPT);
            assertThat(hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus(), is(false));
            assertThat(hasIssue12458MavenGrapeEngine(), is(false));
        } catch (CompileException e) {
            if (!hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus()) { throw e; }
            assertThat(e.getMessage(), containsString("Unsupported class file major version"));
        } catch (CreateException e) {
            if (!hasIssue12458MavenGrapeEngine()) { throw e; }
            final Throwable c = e.getCause();
            assertThat(c, instanceOf(ExceptionInInitializerError.class));
            final Throwable cc = c.getCause();
            assertThat(cc, instanceOf(RuntimeException.class));
            assertThat(cc.getMessage(), is("No suitable ClassLoader found for grab"));
        }
    }

    @Test
    void testHelloWorldWithGrape_wrapped() {

        printVersions();
        try {
            Grengine.Grape.activate();
            Grengine.Grape.newGrengine().run(GRAPE_SCRIPT);
            assertThat(hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus(), is(false));
        } catch (CompileException e) {
            if (!hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus()) { throw e; }
            assertThat(e.getMessage(), containsString("Unsupported class file major version"));
        } finally {
            Grengine.Grape.deactivate();
        }
    }

    private static void printVersions() {
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Groovy: " + GroovyClassLoader.class.getPackage().getImplementationVersion());
    }

    @Test
    void testNoGrapeByDefault() {
        try {
            new Grengine().run(GRAPE_SCRIPT);
            fail("must throw");
        } catch (CreateException | CompileException e) {
            //e.printStackTrace();
            if (hasIssue12467AsmResolvingOfGroovy2WithJdk25Plus()) {
                assertThat(e, instanceOf(CompileException.class));
                assertThat(e.getMessage(), containsString("Unsupported class file major version"));
            } else {
                assertThat(e, instanceOf(CreateException.class));
                final Throwable c = e.getCause();
                assertThat(c, instanceOf(ExceptionInInitializerError.class));
                final Throwable cc = c.getCause();
                assertThat(cc.getClass().getCanonicalName(), is(RuntimeException.class.getCanonicalName()));
                assertThat(cc.getMessage(), is("No suitable ClassLoader found for grab"));
            }
        }
    }

    @Test
    void testConstructEmpty() {

        // when

        final Grengine gren = Grengine.Grape.newGrengine();

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        final CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

    }

    @Test
    void testConstructEmpty_parent() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        final CompilerConfiguration config = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));

    }

    @Test
    void testConstructEmpty_config() {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(config);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructEmpty_parent_config() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, config);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    void testConstructDir() {

        // when

        final Grengine gren = Grengine.Grape.newGrengine(new File("."));

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        final CompilerConfiguration config1 = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config1.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
        final CompilerConfiguration config2 = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config2.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructDir_parent() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, new File("."));

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        final CompilerConfiguration config1 = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config1.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
        final CompilerConfiguration config2 = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config2.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructDir_config() {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(config, new File("."));

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructDir_parent_config() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, config, new File("."));

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    void testConstructDirDirMode() {

        // when

        final Grengine gren = Grengine.Grape.newGrengine(new File("."), DirMode.NO_SUBDIRS);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        CompilerConfiguration config1 = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config1.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
        CompilerConfiguration config2 = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config2.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructDirDirMode_parent() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, new File("."), DirMode.NO_SUBDIRS);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        final CompilerConfiguration config1 = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config1.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
        final CompilerConfiguration config2 = getCompilerConfigFromDirBasedSources(gren);
        assertThat(config2.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructDirDirMode_config() {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(config, new File("."), DirMode.NO_SUBDIRS);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructDirDirMode_parent_config() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, config, new File("."), DirMode.NO_SUBDIRS);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromDirBasedSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }


    @Test
    void testConstructUrl() throws Exception {

        // given

        final List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        final Grengine gren = Grengine.Grape.newGrengine(urls);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        final CompilerConfiguration config1 = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config1.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
        final CompilerConfiguration config2 = getCompilerConfigFromFixedSetSources(gren);
        assertThat(config2.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructUrl_parent() throws Exception {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();
        final List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, urls);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), sameInstance(parent));

        final CompilerConfiguration config1 = getCompilerConfigFromEngineTopCodeCache(engine);
        assertThat(config1.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
        final CompilerConfiguration config2 = getCompilerConfigFromFixedSetSources(gren);
        assertThat(config2.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructUrl_config() throws Exception {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();
        final List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        final Grengine gren = Grengine.Grape.newGrengine(config, urls);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), instanceOf(GroovyClassLoader.class));
        assertThat(engine.getBuilder().getParent().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(getCompilerConfigFromEngineTopCodeCache(engine), sameInstance(config));
        assertThat(getCompilerConfigFromFixedSetSources(gren), sameInstance(config));
        assertThat(config.getCompilationCustomizers().get(0).getClass().getSimpleName(),
                is("GrapeCompilationCustomizer"));
    }

    @Test
    void testConstructUrl_parent_config() throws Exception {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();
        final List<URL> urls = Collections.singletonList(new File(".").toURI().toURL());

        // when

        final Grengine gren = Grengine.Grape.newGrengine(parent, config, urls);

        // then

        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
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
    void testActivateDeactivate() {

        try {
            assertThat(Grape.getInstance(), notNullValue());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is(not("GrengineGrapeEngine")));

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

            assertThat(Grape.getInstance(), notNullValue());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is(not("GrengineGrapeEngine")));

            // when

            Grengine.Grape.deactivate();

            // then

            assertThat(Grape.getInstance(), notNullValue());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is(not("GrengineGrapeEngine")));

        } finally {
            Grengine.Grape.deactivate();
        }

    }

    @Test
    void testActivateDeactivate_differentGrengineClasses() {

        try {
            assertThat(Grape.getInstance(), notNullValue());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is(not("GrengineGrapeEngine")));

            // when

            Grengine.Grape.activate();
            GrapeEngine engine = Grape.getInstance();

            // then

            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));

            // when

            grapeActivateWithCopy();

            // then

            assertThat(Grape.getInstance().getClass().getSimpleName(), is("GrengineGrapeEngine"));
            assertThat(Grape.getInstance(), sameInstance(engine));

            // when

            grapeDeactivateWithCopy();

            // then

            assertThat(Grape.getInstance(), notNullValue());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is(not("GrengineGrapeEngine")));

            // when

            Grengine.Grape.deactivate();

            // then

            assertThat(Grape.getInstance(), notNullValue());
            assertThat(Grape.getInstance().getClass().getSimpleName(), is(not("GrengineGrapeEngine")));

        } finally {
            grapeDeactivateWithCopy();
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateOk_defaultToDefaultLock_sameGrengineClasses() {
        try {

            // given
            Grengine.Grape.activate();
            GrapeEngine engine = Grape.getInstance();

            // when
            Grengine.Grape.activate();

            // then (stays at previous instance with compatible lock)
            assertThat(Grape.getInstance() == engine, is(true));

        } finally {
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateOk_defaultToDefaultLock_differentGrengineClasses() {
        try {

            // given
            grapeActivateWithCopy();
            GrapeEngine engine = Grape.getInstance();

            // when
            Grengine.Grape.activate();

            // then (stays at previous instance with compatible lock)
            assertThat(Grape.getInstance() == engine, is(true));

        } finally {
            grapeDeactivateWithCopy();
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateOk_specificToSameSpecificLock_sameGrengineClasses() {
        try {

            // given
            Grengine.Grape.activate(String.class);
            GrapeEngine engine = Grape.getInstance();

            // when
            Grengine.Grape.activate(String.class);

            // then (stays at previous instance with same lock)
            assertThat(Grape.getInstance() == engine, is(true));

        } finally {
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateOk_specificToSameSpecificLock_differentGrengineClasses() {
        try {

            // given
            grapeActivateWithCopy(String.class);
            GrapeEngine engine = Grape.getInstance();

            // when
            Grengine.Grape.activate(String.class);

            // then (stays at previous instance with same lock)
            assertThat(Grape.getInstance() == engine, is(true));

        } finally {
            grapeDeactivateWithCopy();
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateFails_defaultToSpecificLock_sameGrengineClasses() {
        try {

            // given
            Grengine.Grape.activate();

            // when/then
            assertThrowsMessageIs(IllegalStateException.class,
                    () -> Grengine.Grape.activate(String.class),
                    "Unable to wrap GrapeEngine in Grape.class because it is " +
                            "already wrapped by a Grengine using an incompatible lock.");
        } finally {
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateFails_defaultToSpecificLock_differentGrengineClasses() {
        try {

            // given
            grapeActivateWithCopy();

            // when/then
            assertThrowsMessageIs(IllegalStateException.class,
                    () -> Grengine.Grape.activate(String.class),
                    "Unable to wrap GrapeEngine in Grape.class because it is " +
                            "already wrapped by a Grengine using an incompatible lock.");
        } finally {
            grapeDeactivateWithCopy();
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateFails_specificToDefaultLock_sameGrengineClasses() {
        try {

            // given
            Grengine.Grape.activate(String.class);

            // when/then
            assertThrowsMessageIs(IllegalStateException.class,
                    Grengine.Grape::activate,
                    "Unable to wrap GrapeEngine in Grape.class because it is " +
                            "already wrapped by a Grengine using an incompatible lock.");
        } finally {
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateFails_specificToDefaultLock_differentGrengineClasses() {
        try {

            // given
            grapeActivateWithCopy(String.class);

            // when/then
            assertThrowsMessageIs(IllegalStateException.class,
                    Grengine.Grape::activate,
                    "Unable to wrap GrapeEngine in Grape.class because it is " +
                            "already wrapped by a Grengine using an incompatible lock.");
        } finally {
            grapeDeactivateWithCopy();
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateFails_specificToOtherSpecificLock_sameGrengineClasses() {
        try {

            // given
            Grengine.Grape.activate(String.class);

            // when/then
            assertThrowsMessageIs(IllegalStateException.class,
                    () -> Grengine.Grape.activate(File.class),
                    "Unable to wrap GrapeEngine in Grape.class because it is " +
                            "already wrapped by a Grengine using an incompatible lock.");
        } finally {
            Grengine.Grape.deactivate();
        }
    }

    @Test
    void testActivateFails_specificToOtherSpecificLock_differentGrengineClasses() {
        try {

            // given
            grapeActivateWithCopy(String.class);

            // when/then
            assertThrowsMessageIs(IllegalStateException.class,
                    () -> Grengine.Grape.activate(File.class),
                    "Unable to wrap GrapeEngine in Grape.class because it is " +
                            "already wrapped by a Grengine using an incompatible lock.");
        } finally {
            grapeDeactivateWithCopy();
            Grengine.Grape.deactivate();
        }
    }

}

