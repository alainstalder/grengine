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

import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.engine.Engine;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.engine.Loader;
import ch.grengine.except.CompileException;
import ch.grengine.except.CreateException;
import ch.grengine.except.GrengineException;
import ch.grengine.except.LoadException;
import ch.grengine.load.DefaultTopCodeCacheFactory;
import ch.grengine.load.LoadMode;
import ch.grengine.load.RecordingClassReleaser;
import ch.grengine.source.DefaultFileSource;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.DefaultTextSource;
import ch.grengine.source.FileSource;
import ch.grengine.source.MockTextSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.source.TextSource;
import ch.grengine.source.UrlSource;
import ch.grengine.sources.DirBasedSources;
import ch.grengine.sources.DirMode;
import ch.grengine.sources.FixedSetSources;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageContains;
import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static ch.grengine.TestUtil.assertThrowsMessageStartsWith;
import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class GrengineTest {
    
    @Test
    void testHelloWorld() {
        new Grengine().run("println 'hello world'");
    }

    @Test
    void testHelloWorldWithClose() {
        final Grengine gren = new Grengine();
        gren.run("print 'hello world '; [1,2,3].each { print it }; println()");
        gren.close();
    }

    @Test
    void testConstructDefaults() {

        // given

        final Grengine.Builder builder = new Grengine.Builder();

        // when

        final Grengine gren = builder.build();

        // then

        assertThat(gren.getBuilder(), is(builder));

        assertThat(gren.getEngine(), is(notNullValue()));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(engine.getBuilder().getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(engine.getBuilder().isWithTopCodeCache(), is(true));
        assertThat(engine.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(engine.getBuilder().getTopCodeCacheFactory(), is(notNullValue()));
        assertThat(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers(), is(true));
        assertThat(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers(), is(true));

        assertThat(gren.getSourceFactory(), is(notNullValue()));
        assertThat(gren.getSourceFactory(), is(gren.getBuilder().getSourceFactory()));
        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));

        assertThat(gren.getBuilder().getSourcesLayers().size(), is(0));

        assertThat(gren.getBuilder().getLatencyMs(), is(Grengine.Builder.DEFAULT_LATENCY_MS));
    }

    @Test
    void testConstructAllDefined() {

        // given

        final Grengine.Builder builder = new Grengine.Builder();
        final Engine engine = new LayeredEngine.Builder().build();
        final SourceFactory sourceFactory = new DefaultSourceFactory();
        final List<Sources> sourcesLayers = new LinkedList<>();
        final UpdateExceptionNotifier notifier = new MockUpdateExceptionNotifier(null);

        // when

        final Grengine gren = builder
                .setEngine(engine)
                .setSourceFactory(sourceFactory)
                .setSourcesLayers()
                .setSourcesLayers(sourcesLayers)
                .setUpdateExceptionNotifier(notifier)
                .setLatencyMs(99)
                .build();

        // then

        assertThat(gren.getBuilder(), is(builder));
        assertThat(gren.getEngine(), is(engine));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        assertThat(gren.getSourceFactory(), is(sourceFactory));
        assertThat(gren.getSourceFactory(), is(gren.getBuilder().getSourceFactory()));
        assertThat(gren.getBuilder().getSourcesLayers(), is(sourcesLayers));
        assertThat(gren.getBuilder().getUpdateExceptionNotifier(), is(notifier));
        assertThat(gren.getBuilder().getLatencyMs(), is(99L));
    }

    @Test
    void testModifyBuilderAfterUse() {

        // given

        final Grengine.Builder builder = new Grengine.Builder();
        builder.build();

        // when/then

        assertThrowsMessageIs(IllegalStateException.class,
                () -> builder.setLatencyMs(999),
                "Builder already used.");
    }

    @Test
    void testConstructEmpty() {

        // when

        final Grengine gren = new Grengine();

        // then

        assertThat(gren.getEngine(), is(notNullValue()));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(engine.getBuilder().getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(engine.getBuilder().isWithTopCodeCache(), is(true));
        assertThat(engine.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(engine.getBuilder().getTopCodeCacheFactory(), is(notNullValue()));
        assertThat(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers(), is(true));
        assertThat(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers(), is(true));

        assertThat(gren.getSourceFactory(), is(notNullValue()));
        assertThat(gren.getSourceFactory(), is(gren.getBuilder().getSourceFactory()));
        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));

        assertThat(gren.getBuilder().getSourcesLayers().size(), is(0));

        assertThat(gren.getBuilder().getLatencyMs(), is(Grengine.Builder.DEFAULT_LATENCY_MS));
    }

    @Test
    void testConstructEmpty_WithParent() {

        // given

        final ClassLoader parent = new GroovyClassLoader();

        // when

        final Grengine gren = new Grengine(parent);

        // when

        assertThat(gren.getEngine(), is(notNullValue()));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        final LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertThat(engine.getBuilder().getParent(), is(parent));
        assertThat(engine.getBuilder().getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(engine.getBuilder().isWithTopCodeCache(), is(true));
        assertThat(engine.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(engine.getBuilder().getTopCodeCacheFactory(), is(notNullValue()));
        assertThat(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers(), is(true));
        assertThat(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers(), is(true));

        assertThat(gren.getSourceFactory(), is(notNullValue()));
        assertThat(gren.getSourceFactory(), is(gren.getBuilder().getSourceFactory()));
        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));

        assertThat(gren.getBuilder().getSourcesLayers().size(), is(0));

        assertThat(gren.getBuilder().getLatencyMs(), is(Grengine.Builder.DEFAULT_LATENCY_MS));
    }

    @Test
    void testConstructFromDirWithoutSubDirs() throws Exception {

        // given

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(dir);

        // then

        // check parent
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat(gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat(gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        assertThrowsMessageContains(CompileException.class,
                () -> gren.run(fSub3),
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        final Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsMessageContains(LoadException.class,
                () -> gren.loadClass("ScriptSub1"),
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    void testConstructFromDirWithoutSubDirs_WithParent() throws Exception {

        // given

        final ClassLoader parent = new GroovyClassLoader();

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(parent, dir);

        // then

        // check parent
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat(gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat(gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        assertThrowsMessageContains(CompileException.class,
                () -> gren.run(fSub3),
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        final Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsMessageContains(LoadException.class,
                () -> gren.loadClass("ScriptSub1"),
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    void testConstructFromDirWithSubDirs() throws Exception {

        // given

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        assertThat(gren.run(fSub1), is(1));
        assertThat(gren.run(fSub2), is(2));
        assertThat(gren.run(fSub3), is(2));

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        final Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    void testConstructFromDirWithoutSubDirsNoTopCodeCache() throws Exception {

        // given

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine.Builder()
                .setSourcesLayers(new DirBasedSources.Builder(dir).setDirMode(DirMode.NO_SUBDIRS).build())
                .setEngine(new LayeredEngine.Builder().setWithTopCodeCache(false).build())
                .build();

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        // all not found because not in static layers and no top code cache
        assertThrowsMessageContains(LoadException.class,
                () -> gren.run(fSub1),
                "Source not found: ");
        assertThrowsMessageContains(LoadException.class,
                () -> gren.run(fSub2),
                "Source not found: ");
        assertThrowsMessageContains(LoadException.class,
                () -> gren.run(fSub3),
                "Source not found: ");

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        final Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsMessageContains(LoadException.class,
                () -> gren.loadClass("ScriptSub1"),
                "Could not load class 'ScriptSub1'. Cause: ");
        // also not found, because there is no top code cache
        assertThrowsMessageContains(LoadException.class,
                () -> gren.loadClass(sSub1, "ScriptSub1"),
                "Source not found: ");
    }

    @Test
    void testConstructFromCompilerConfig() {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = new Grengine(config);

        // then

        // check that same compiler configuration
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        final DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        final DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));
    }

    @Test
    void testConstructFromCompilerConfig_WithParent() {

        // given

        final ClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final Grengine gren = new Grengine(parent, config);

        // then

        // check that same compiler configuration
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        final DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        final DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));
    }


    @Test
    void testConstructFromDirWithoutSubDirs_WithCompilerConfig() throws Exception {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();
        final Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(config, dir);

        // then

        // check that same compiler configuration
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        final DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        final DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        // check that script extensions set from compiler configuration
        assertThat(((DirBasedSources) gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions(), is(scriptExtensions));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat(gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat(gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        assertThrowsMessageContains(CompileException.class,
                () -> gren.run(fSub3),
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        final Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsMessageContains(LoadException.class,
                () -> gren.loadClass("ScriptSub1"),
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    void testConstructFromDirWithoutSubDirs_WithCompilerConfig_WithParent() throws Exception {

        // given

        final ClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();
        final Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(parent, config, dir);

        // then

        // check that same compiler configuration
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        final DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        final DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        // check that script extensions set from compiler configuration
        assertThat(((DirBasedSources) gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions(), is(scriptExtensions));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat(gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat(gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        assertThrowsMessageContains(CompileException.class,
                () -> gren.run(fSub3),
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        final Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsMessageContains(LoadException.class,
                () -> gren.loadClass("ScriptSub1"),
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    void testConstructFromDirWithSubDirs_WithCompilerConfiguration() throws Exception {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();
        final Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(config, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        // check that same compiler configuration
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        final DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        final DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        // check that script extensions set from compiler configuration
        assertThat(((DirBasedSources) gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions(), is(scriptExtensions));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        assertThat(gren.run(fSub1), is(1));
        assertThat(gren.run(fSub2), is(2));
        assertThat(gren.run(fSub3), is(2));

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        final Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    void testConstructFromDirWithSubDirs_WithCompilerConfiguration_WithParent() throws Exception {

        // given

        final ClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();
        final Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(parent, config, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        // check that same compiler configuration
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        final DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        final DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));


        // check that script extensions set from compiler configuration
        assertThat(((DirBasedSources) gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions(), is(scriptExtensions));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        assertThat(gren.run(fSub1), is(1));
        assertThat(gren.run(fSub2), is(2));
        assertThat(gren.run(fSub3), is(2));

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        final Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    void testConstructFromDirWithSubDirs_WithParent() throws Exception {

        // given

        final ClassLoader parent = new GroovyClassLoader();

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        final File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        final File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        final File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        final Grengine gren = new Grengine(parent, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        // check parent
        final LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        assertThat(gren.run(fSub1), is(1));
        assertThat(gren.run(fSub2), is(2));
        assertThat(gren.run(fSub3), is(2));

        // extra: load with class name
        final Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        final Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    void testConstructFromUrls() throws Exception {

        // given

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final URL u1 = f1.toURI().toURL();
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final URL u2 = f1.toURI().toURL();

        // when

        final Grengine gren1 = new Grengine(Collections.singletonList(u1));

        // then

        // check parent
        final LayeredEngine layeredEngine = (LayeredEngine)gren1.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren1.getLastUpdateException(), is(nullValue()));
        assertThat(gren1.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren1.run(f2), is(1));

        // when

        final Grengine gren2 = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren2.getLastUpdateException(), is(nullValue()));
        assertThat(gren2.run(f1), is(1));
        // in static layers
        assertThat(gren2.run(f2), is(1));
    }

    @Test
    void testConstructFromUrls_WithParent() throws Exception {

        // given

        final ClassLoader parent = new GroovyClassLoader();

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final URL u1 = f1.toURI().toURL();
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final URL u2 = f1.toURI().toURL();

        // when

        final Grengine gren1 = new Grengine(parent, Collections.singletonList(u1));

        // then

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren1.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren1.getLastUpdateException(), is(nullValue()));
        assertThat(gren1.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren1.run(f2), is(1));

        // when

        final Grengine gren2 = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren2.getLastUpdateException(), is(nullValue()));
        assertThat(gren2.run(f1), is(1));
        // in static layers
        assertThat(gren2.run(f2), is(1));
    }


    @Test
    void testConstructFromUrls_WithCompilerConfiguration() throws Exception {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final URL u1 = f1.toURI().toURL();
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final URL u2 = f1.toURI().toURL();

        // when

        final Grengine gren1 = new Grengine(config, Collections.singletonList(u1));

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren1.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren1.getLastUpdateException(), is(nullValue()));
        assertThat(gren1.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren1.run(f2), is(1));

        // when

        final Grengine gren2 = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren2.getLastUpdateException(), is(nullValue()));
        assertThat(gren2.run(f1), is(1));
        // in static layers
        assertThat(gren2.run(f2), is(1));
    }
    @Test
    void testConstructFromUrls_WithCompilerConfiguration_WithParent() throws Exception {

        // given

        final ClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();

        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        final URL u1 = f1.toURI().toURL();
        final File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        final URL u2 = f1.toURI().toURL();

        // when

        final Grengine gren1 = new Grengine(parent, config, Collections.singletonList(u1));

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren1.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren1.getLastUpdateException(), is(nullValue()));
        assertThat(gren1.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren1.run(f2), is(1));

        // when

        final Grengine gren2 = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren2.getLastUpdateException(), is(nullValue()));
        assertThat(gren2.run(f1), is(1));
        // in static layers
        assertThat(gren2.run(f2), is(1));
    }


    @Test
    void testConstructFromCompilerConfigNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((CompilerConfiguration)null),
                "Compiler configuration is null.");
    }

    @Test
    void testConstructFromParentParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((ClassLoader)null),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromDir_DirNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((File)null),
                "Directory is null.");
    }

    @Test
    void testConstructFromDir_ParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((ClassLoader)null, new File(".")),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromDir_CompilerConfigurationNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((CompilerConfiguration)null, new File(".")),
                "Compiler configuration is null.");
    }

    @Test
    void testConstructFromDirAndCompilerConfiguration_ParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(null, new CompilerConfiguration(), new File(".")),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromDirWithDirMode_DirNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(null, DirMode.NO_SUBDIRS),
                "Directory is null.");
    }

    @Test
    void testConstructFromDirWithDirMode_DirModeNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new File("."), null),
                "Dir mode is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((CompilerConfiguration)null, new File("."), DirMode.NO_SUBDIRS),
                "Compiler configuration is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndCompilerConfiguration_DirNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new CompilerConfiguration(), null, DirMode.NO_SUBDIRS),
                "Directory is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndCompilerConfiguration_DirModeNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new CompilerConfiguration(), new File("."), null),
                "Dir mode is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParent_ParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((ClassLoader)null, new File("."), DirMode.NO_SUBDIRS),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParent_DirNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), null, DirMode.NO_SUBDIRS),
                "Directory is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParent_DirModeNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), new File("."), null),
                "Dir mode is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_ParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(null, new CompilerConfiguration(), new File("."),
                        DirMode.NO_SUBDIRS),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), null, new File("."),
                        DirMode.NO_SUBDIRS),
                "Compiler configuration is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), null,
                        DirMode.NO_SUBDIRS),
                "Directory is null.");
    }

    @Test
    void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirModeNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), new File("."),
                        null),
                "Dir mode is null.");
    }

    @Test
    void testConstructFromUrls_UrlsNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((Collection<URL>)null),
                "URL collection is null.");
    }

    @Test
    void testConstructFromUrlsAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((CompilerConfiguration)null, new LinkedList<>()),
                "Compiler configuration is null.");
    }

    @Test
    void testConstructFromUrlsAndCompilerConfiguration_UrlsNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new CompilerConfiguration(), (Collection<URL>)null),
                "URL collection is null.");
    }

    @Test
    void testConstructFromUrlsAndParent_ParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine((ClassLoader)null, new LinkedList<>()),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromUrlsAndParent_UrlsNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), (Collection<URL>)null),
                "URL collection is null.");
    }

    @Test
    void testConstructFromUrlsAndParentAndCompilerConfiguration_ParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(null, new CompilerConfiguration(), new LinkedList<>()),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromUrlsAndParentAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), null, new LinkedList<>()),
                "Compiler configuration is null.");
    }

    @Test
    void testConstructFromUrlsAndParentAndCompilerConfiguration_UrlsNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), (Collection<URL>)null),
                "URL collection is null.");
    }


    @Test
    void testClose() throws Exception {

        // given

        final Grengine.Builder builder = new Grengine.Builder();

        final LayeredEngine.Builder engineBuilder = new LayeredEngine.Builder();

        final RecordingClassReleaser releaser = new RecordingClassReleaser();
        engineBuilder.setClassReleaser(releaser);

        final LayeredEngine engine = engineBuilder.build();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final List<Sources> sourcesList = Collections.singletonList(sources);

        engine.setCodeLayersBySource(sourcesList);

        builder.setEngine(engine);

        final Grengine gren = builder.build();

        final Loader loaderAttached = gren.newAttachedLoader();
        final Loader loaderDetached = gren.newDetachedLoader();

        final Class<?> clazz1a = gren.loadClass(loaderAttached, "Class1");
        final Class<?> clazz2a = gren.loadClass(loaderAttached, "Class2");
        clazz2a.getConstructor().newInstance();
        final Class<?> clazz1d = gren.loadClass(loaderDetached, "Class1");
        final Class<?> clazz2d = gren.loadClass(loaderDetached, "Class2");
        clazz2d.getConstructor().newInstance();

        // when

        gren.close();

        // then

        assertThat(releaser.classes.contains(clazz1a), is(true));
        assertThat(releaser.classes.contains(clazz2a), is(true));
        assertThat(releaser.classes.contains(clazz1d), is(true));
        assertThat(releaser.classes.contains(clazz2d), is(true));
        assertThat(releaser.classes.size(), is(6));
        assertThat(releaser.countClassesWithName("Class1"), is(2));
        assertThat(releaser.countClassesWithName("Class2"), is(2));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(2));
    }

    @Test
    void testAsClassLoaderBasic() throws Exception {

        // given

        final File dir = createTestDir();
        final File testFile = new File(dir, "Test.groovy");
        TestUtil.setFileText(testFile, "class Test { static int get55() { 55 } }");

        // when


        final Grengine gren = new Grengine(dir);

        // then

        assertThat(gren.run("Test.get55()"), is(55));


        // when

        final GroovyShell shell1 = new GroovyShell(gren.asClassLoader());

        // then

        assertThat(shell1.evaluate("Test.get55()"), is(55));


        // when

        final GroovyShell shell2 = new GroovyShell(gren.asClassLoader(gren.getLoader()));

        // then

        assertThat(shell2.evaluate("Test.get55()"), is(55));

        // make sure can load from parent loaders
        shell2.evaluate("new StringBuffer().append('x')");

        gren.close();
    }

    @Test
    void testAsClassLoaderLoaderNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Grengine().asClassLoader(null),
                "Loader is null.");
    }

    @Test
    void testAsClassLoaderLoaderForOtherEngine() {

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> new Grengine().asClassLoader(new Grengine().getLoader()),
                "Engine ID does not match (loader created by a different engine).");
    }

    @Test
    void testMatrixSource() throws Exception {

        // when

        final Grengine gren = new Grengine();

        // then

        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));

        // when

        final TextSource st = (TextSource)gren.source("hello");

        // then

        assertThat(st.getText(), is("hello"));

        // when

        final TextSource stn = (TextSource)gren.source("hello", "World");

        // then

        assertThat(stn.getText(), is("hello"));

        // when

        final File dir = createTestDir();
        final File f = new File(dir, "Script1.groovy");
        final FileSource fs = (FileSource)gren.source(f);

        // then

        assertThat(fs.getFile().getPath(), is(f.getCanonicalPath()));

        // when

        final URL u = f.toURI().toURL();
        final UrlSource us = (UrlSource)gren.source(u);

        // then

        assertThat(us.getUrl(), is(u));
    }

    @Test
    void testMatrixLoad() throws Exception {

        // given

        final Grengine gren = new Grengine();

        // when

        final TextSource st = (TextSource)gren.source("return 'text'");
        final File dir = createTestDir();
        final File f = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        final File fu = new File(dir, "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        final URL u = fu.toURI().toURL();

        // then

        assertThat(((Script) gren.load("return 'text'").getConstructor().newInstance()).run(), is("text"));
        assertThat(((Script) gren.load(
                "return 'text-with-name'", "Script0").getConstructor().newInstance()).run(), is("text-with-name"));
        assertThat(((Script) gren.load(f).getConstructor().newInstance()).run(), is("file"));
        assertThat(((Script) gren.load(u).getConstructor().newInstance()).run(), is("url"));
        assertThat(((Script) gren.load(st).getConstructor().newInstance()).run(), is("text"));

        // when

        final Loader loader1 = gren.newAttachedLoader();

        // then

        assertThat(((Script) gren.load(loader1, "return 'text'").getConstructor().newInstance()).run(), is("text"));
        assertThat(((Script) gren.load(loader1,
                "return 'text-with-name'", "Script0").getConstructor().newInstance()).run(), is("text-with-name"));
        assertThat(((Script) gren.load(loader1, f).getConstructor().newInstance()).run(), is("file"));
        assertThat(((Script) gren.load(loader1, u).getConstructor().newInstance()).run(), is("url"));
        assertThat(((Script) gren.load(loader1, st).getConstructor().newInstance()).run(), is("text"));

        // when

        final Loader loader2 = gren.newDetachedLoader();

        // then

        assertThat(((Script) gren.load(loader2, "return 'text'").getConstructor().newInstance()).run(), is("text"));
        assertThat(((Script) gren.load(loader2,
                "return 'text-with-name'", "Script0").getConstructor().newInstance()).run(), is("text-with-name"));
        assertThat(((Script) gren.load(loader2, f).getConstructor().newInstance()).run(), is("file"));
        assertThat(((Script) gren.load(loader2, u).getConstructor().newInstance()).run(), is("url"));
        assertThat(((Script) gren.load(loader2, st).getConstructor().newInstance()).run(), is("text"));
    }

    @Test
    void testMatrixCreate() throws Exception {

        // given

        final Grengine gren = new Grengine();

        // when

        final TextSource st = (TextSource)gren.source("return 'text'");
        final File dir = createTestDir();
        final File f = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        final File fu = new File(dir, "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        final URL u = fu.toURI().toURL();

        final Loader loader = gren.newAttachedLoader();

        // then

        assertThat((gren.create("return 'text'")).run(), is("text"));
        assertThat((gren.create("return 'text-with-name'", "Script0")).run(), is("text-with-name"));
        assertThat((gren.create(f)).run(), is("file"));
        assertThat((gren.create(u)).run(), is("url"));
        assertThat((gren.create(st)).run(), is("text"));

        assertThat((gren.create(loader, "return 'text'")).run(), is("text"));
        assertThat((gren.create(loader, "return 'text-with-name'", "Script0")).run(), is("text-with-name"));
        assertThat((gren.create(loader, f)).run(), is("file"));
        assertThat((gren.create(loader, u)).run(), is("url"));
        assertThat((gren.create(loader, st)).run(), is("text"));

        // when

        final Class<?> clazz = gren.load(st);

        // then

        assertThat((gren.create(clazz)).run(), is("text"));

        // when/then

        assertThrowsMessageStartsWith(CreateException.class,
                () -> gren.create(String.class),
                "Could not create script for class java.lang.String. " +
                        "Cause: java.lang.ClassCastException: ");
        assertThrowsMessageStartsWith(CreateException.class,
                () -> gren.create(loader, "class NotAScript {}"),
                "Could not create script for class 'NotAScript' from source ");
    }

    @Test
    void testMatrixBinding() {

        // given

        final Grengine gren = new Grengine();

        // when

        final Binding b1 = gren.binding();

        // then

        assertThat(b1.getVariables().size(), is(0));

        // when

        final Binding b2 = gren.binding("aa", 11);

        // then

        assertThat(b2.getVariables().size(), is(1));
        assertThat(b2.getVariables().get("aa"), is(11));

        // when

        final Binding b3 = gren.binding("aa", 11, "bb", 22);

        // then

        assertThat(b3.getVariables().size(), is(2));
        assertThat(b3.getVariables().get("aa"), is(11));
        assertThat(b3.getVariables().get("bb"), is(22));

        // when

        final Binding b4 = gren.binding("aa", 11, "bb", 22, "cc", 33);

        // then

        assertThat(b4.getVariables().size(), is(3));
        assertThat(b4.getVariables().get("aa"), is(11));
        assertThat(b4.getVariables().get("bb"), is(22));
        assertThat(b4.getVariables().get("cc"), is(33));

        // when

        final Binding b5 = gren.binding("aa", 11, "bb", 22, "cc", 33,
                "dd", 44);

        // then

        assertThat(b5.getVariables().size(), is(4));
        assertThat(b5.getVariables().get("aa"), is(11));
        assertThat(b5.getVariables().get("bb"), is(22));
        assertThat(b5.getVariables().get("cc"), is(33));
        assertThat(b5.getVariables().get("dd"), is(44));

        // when

        final Binding b6 = gren.binding("aa", 11, "bb", 22, "cc", 33,
                "dd", 44, "ee", 55);

        // then

        assertThat(b6.getVariables().size(), is(5));
        assertThat(b6.getVariables().get("aa"), is(11));
        assertThat(b6.getVariables().get("bb"), is(22));
        assertThat(b6.getVariables().get("cc"), is(33));
        assertThat(b6.getVariables().get("dd"), is(44));
        assertThat(b6.getVariables().get("ee"), is(55));

        // when

        final Binding b7 = gren.binding("aa", 11, "bb", 22, "cc", 33,
                "dd", 44, "ee", 55, "ff", 66);

        // then

        assertThat(b7.getVariables().size(), is(6));
        assertThat(b7.getVariables().get("aa"), is(11));
        assertThat(b7.getVariables().get("bb"), is(22));
        assertThat(b7.getVariables().get("cc"), is(33));
        assertThat(b7.getVariables().get("dd"), is(44));
        assertThat(b7.getVariables().get("ee"), is(55));
        assertThat(b7.getVariables().get("ff"), is(66));

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> gren.binding("aa", 11, "bb", 22, "cc", 33,
                        "dd", 44, "ee", 55, "ff"),
                "Odd number of arguments.");
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> gren.binding("aa", 11, "bb", 22, "cc", 33,
                        "dd", 44, "ee", 55, 7777, 66),
                "Argument 11 is not a string.");
        assertThat(gren.run("return x", gren.binding("x", 22)), is(22));
    }

    @Test
    void testMatrixRun() throws Exception {

        // given

        final Grengine gren = new Grengine();

        // when

        final TextSource st1 = (TextSource)gren.source("return 'text'");
        final File dir = createTestDir();
        final File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 'file'");
        final File fu1 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(fu1, "return 'url'");
        final URL u1 = fu1.toURI().toURL();

        final Loader loader = gren.newAttachedLoader();

        // then

        assertThat(gren.run("return 'text'"), is("text"));
        assertThat(gren.run("return 'text-with-name'", "Script0"), is("text-with-name"));
        assertThat(gren.run(f1), is("file"));
        assertThat(gren.run(u1), is("url"));
        assertThat(gren.run(st1), is("text"));

        assertThat(gren.run(loader, "return 'text'"), is("text"));
        assertThat(gren.run(loader, "return 'text-with-name'", "Script0"), is("text-with-name"));
        assertThat(gren.run(loader, f1), is("file"));
        assertThat(gren.run(loader, u1), is("url"));
        assertThat(gren.run(loader, st1), is("text"));

        // when

        Map<String,Object> map = new HashMap<>();
        map.put("x", 99);
        final TextSource st2 = (TextSource)gren.source("return x");
        // replace file and url so that immediately up to date
        final File f2 = new File(dir, "ScriptX.groovy");
        TestUtil.setFileText(f2, "return x");
        final URL u2 = f2.toURI().toURL();

        // then

        assertThat(gren.run("return x", map), is(99));
        assertThat(gren.run("return x", "Script0", map), is(99));
        assertThat(gren.run(f2, map), is(99));
        assertThat(gren.run(u2, map), is(99));
        assertThat(gren.run(st2, map), is(99));

        assertThat(gren.run(loader, "return x", map), is(99));
        assertThat(gren.run(loader, "return x", "Script0", map), is(99));
        assertThat(gren.run(loader, f2, map), is(99));
        assertThat(gren.run(loader, u2, map), is(99));
        assertThat(gren.run(loader, st2, map), is(99));

        // when

        final Binding binding = new Binding(map);

        // then

        assertThat(gren.run("return x", binding), is(99));
        assertThat(gren.run("return x", "Script0", binding), is(99));
        assertThat(gren.run(f2, binding), is(99));
        assertThat(gren.run(u2, binding), is(99));
        assertThat(gren.run(st2, binding), is(99));

        assertThat(gren.run(loader, "return x", binding), is(99));
        assertThat(gren.run(loader, "return x", "Script0", binding), is(99));
        assertThat(gren.run(loader, f2, binding), is(99));
        assertThat(gren.run(loader, u2, binding), is(99));
        assertThat(gren.run(loader, st2, binding), is(99));

        // when

        final Script script = gren.create(st2);

        // then

        assertThat(gren.run(gren.create("return 99")), is(99));
        assertThat(gren.run(script, map), is(99));
        assertThat(gren.run(script, binding), is(99));
    }


    @Test
    void testUpdateExceptionsCompileException() throws Exception {

        // given

        final MockTextSource s1 = new MockTextSource("return 0");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(0)
                .setName("except")
                .build();
        final List<Sources> sourcesLayers = Collections.singletonList(sources);

        final MockUpdateExceptionNotifier notifier = new MockUpdateExceptionNotifier(null);

        // when

        final Grengine gren = new Grengine.Builder()
                .setEngine(new LayeredEngine.Builder().setWithTopCodeCache(false).build())
                .setSourcesLayers(sourcesLayers)
                .setUpdateExceptionNotifier(notifier)
                .setLatencyMs(0)
                .build();

        // then

        assertThat(gren.run(s1), is(0));
        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(notifier.getLastUpdateException(), is(nullValue()));

        // when

        s1.setText("&%&%");
        s1.setLastModified(99);

        Thread.sleep(30);

        gren.run(s1);
        final GrengineException e1 = gren.getLastUpdateException();

        // then

        assertThat(e1, is(notNullValue()));
        assertThat(e1, instanceOf(CompileException.class));
        assertThat(e1.getMessage().startsWith("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: org.codehaus.groovy.control.MultipleCompilationErrorsException: "), is(true));
        assertThat(e1, is(notifier.getLastUpdateException()));

        // when

        s1.setThrowAtGetText(new RuntimeException("unit test"));
        s1.setText("return 22");
        s1.setLastModified(222);

        Thread.sleep(30);

        gren.run(s1);
        final GrengineException e2 = gren.getLastUpdateException();

        // then

        assertThat(e2, is(notNullValue()));
        assertThat(e2, instanceOf(CompileException.class));
        assertThat(e2.getMessage(), is("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: java.lang.RuntimeException: unit test"));
        assertThat(e2, is(notifier.getLastUpdateException()));

        // when

        s1.setThrowAtGetText(null);
        s1.setText("return 33");
        s1.setLastModified(333);

        Thread.sleep(30);

        // then

        assertThat(gren.run(s1), is(33));
        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(notifier.getLastUpdateException(), is(nullValue()));
    }

    @Test
    void testUpdateExceptionsOtherException() {

        // given

        final Source s1 = new DefaultTextSource("package groovy.lang\nclass Tuple {}");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(0)
                .setName("except")
                .build();
        final List<Sources> sourcesLayers = Collections.singletonList(sources);

        // when

        final Grengine gren = new Grengine.Builder()
                .setEngine(new LayeredEngine.Builder()
                        .setWithTopCodeCache(false)
                        .setAllowSameClassNamesInParentAndCodeLayers(false)
                        .build())
                .setSourcesLayers(sourcesLayers)
                .setLatencyMs(0)
                .build();
        final GrengineException e = gren.getLastUpdateException();

        // then

        assertThat(e, is(notNullValue()));
        assertThat(e, not(instanceOf(CompileException.class)));
        assertThat(e.getMessage().startsWith("Failed to update Grengine. " +
                "Cause: ch.grengine.except.ClassNameConflictException: Found 1 class name conflict(s)"), is(true));
    }

}

