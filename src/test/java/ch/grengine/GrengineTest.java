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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static ch.grengine.TestUtil.assertThrows;
import static ch.grengine.TestUtil.assertThrowsContains;
import static ch.grengine.TestUtil.assertThrowsStartsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests the respective class.
 * 
 * @author Alain Stalder
 *
 */
public class GrengineTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testHelloWorld() {
        new Grengine().run("println 'hello world'");
    }

    @Test
    public void testHelloWorldWithClose() {
        Grengine gren = new Grengine();
        gren.run("print 'hello world '; [1,2,3].each { print it }; println()");
        gren.close();
    }

    @Test
    public void testConstructDefaults() {

        // given

        Grengine.Builder builder = new Grengine.Builder();

        // when

        Grengine gren = builder.build();

        // then

        assertThat(gren.getBuilder(), is(builder));

        assertThat(gren.getEngine(), is(notNullValue()));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        LayeredEngine engine = (LayeredEngine)gren.getEngine();
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
    public void testConstructAllDefined() {

        // given

        Grengine.Builder builder = new Grengine.Builder();
        Engine engine = new LayeredEngine.Builder().build();
        builder.setEngine(engine);
        SourceFactory sourceFactory = new DefaultSourceFactory();
        builder.setSourceFactory(sourceFactory);
        List<Sources> sourcesLayers = new LinkedList<>();
        builder.setSourcesLayers();
        builder.setSourcesLayers(sourcesLayers);
        UpdateExceptionNotifier notifier = new MockUpdateExceptionNotifier(null);
        builder.setUpdateExceptionNotifier(notifier);
        builder.setLatencyMs(99);

        // when

        Grengine gren = builder.build();

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
    public void testModifyBuilderAfterUse() {

        // given

        Grengine.Builder builder = new Grengine.Builder();
        builder.build();

        // when/then

        assertThrows(() -> builder.setLatencyMs(999),
                IllegalStateException.class,
                "Builder already used.");
    }

    @Test
    public void testConstructEmpty() {

        // when

        Grengine gren = new Grengine();

        // then

        assertThat(gren.getEngine(), is(notNullValue()));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        LayeredEngine engine = (LayeredEngine)gren.getEngine();
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
    public void testConstructEmpty_WithParent() {

        // given

        ClassLoader parent = new GroovyClassLoader();

        // when

        Grengine gren = new Grengine(parent);

        // when

        assertThat(gren.getEngine(), is(notNullValue()));
        assertThat(gren.getEngine(), is(gren.getBuilder().getEngine()));
        LayeredEngine engine = (LayeredEngine)gren.getEngine();
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
    public void testConstructFromDirWithoutSubDirs() throws Exception {

        // given

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(dir);

        // then

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
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
        assertThrowsContains(() -> gren.run(fSub3),
                CompileException.class,
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsContains(() -> gren.loadClass("ScriptSub1"),
                LoadException.class,
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithoutSubDirs_WithParent() throws Exception {

        // given

        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(parent, dir);

        // then

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
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
        assertThrowsContains(() -> gren.run(fSub3),
                CompileException.class,
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsContains(() -> gren.loadClass("ScriptSub1"),
                LoadException.class,
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithSubDirs() throws Exception {

        // given

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        assertThat(gren.run(fSub1), is(1));
        assertThat(gren.run(fSub2), is(2));
        assertThat(gren.run(fSub3), is(2));

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithoutSubDirsNoTopCodeCache() throws Exception {

        // given

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine.Builder()
                .setSourcesLayers(new DirBasedSources.Builder(dir).setDirMode(DirMode.NO_SUBDIRS).build())
                .setEngine(new LayeredEngine.Builder().setWithTopCodeCache(false).build())
                .build();

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        // all not found because not in static layers and no top code cache
        assertThrowsContains(() -> gren.run(fSub1),
                LoadException.class,
                "Source not found: ");
        assertThrowsContains(() -> gren.run(fSub2),
                LoadException.class,
                "Source not found: ");
        assertThrowsContains(() -> gren.run(fSub3),
                LoadException.class,
                "Source not found: ");

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsContains(() -> gren.loadClass("ScriptSub1"),
                LoadException.class,
                "Could not load class 'ScriptSub1'. Cause: ");
        // also not found, because there is no top code cache
        assertThrowsContains(() -> gren.loadClass(sSub1, "ScriptSub1"),
                LoadException.class,
                "Source not found: ");
    }

    @Test
    public void testConstructFromCompilerConfig() {

        // given

        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = new Grengine(config);

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));
    }

    @Test
    public void testConstructFromCompilerConfig_WithParent() {

        // given

        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        // when

        Grengine gren = new Grengine(parent, config);

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));
    }


    @Test
    public void testConstructFromDirWithoutSubDirs_WithCompilerConfig() throws Exception {

        // given

        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(config, dir);

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
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
        assertThrowsContains(() -> gren.run(fSub3),
                CompileException.class,
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsContains(() -> gren.loadClass("ScriptSub1"),
                LoadException.class,
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithoutSubDirs_WithCompilerConfig_WithParent() throws Exception {

        // given

        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(parent, config, dir);

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
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
        assertThrowsContains(() -> gren.run(fSub3),
                CompileException.class,
                "unable to resolve class ScriptSub2");

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        assertThrowsContains(() -> gren.loadClass("ScriptSub1"),
                LoadException.class,
                "Could not load class 'ScriptSub1'. Cause: ");
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithSubDirs_WithCompilerConfiguration() throws Exception {

        // given

        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(config, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
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
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithSubDirs_WithCompilerConfiguration_WithParent() throws Exception {

        // given

        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(parent, config, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
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
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithSubDirs_WithParent() throws Exception {

        // given

        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        assertThat(subDir.mkdir(), is(true));
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        // when

        Grengine gren = new Grengine(parent, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // then

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        assertThat(gren.run(f2), is(1));
        assertThat(gren.run(fSub1), is(1));
        assertThat(gren.run(fSub2), is(2));
        assertThat(gren.run(fSub3), is(2));

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass("ScriptSub1");
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromUrls() throws Exception {

        // given

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        // when

        Grengine gren = new Grengine(Collections.singletonList(u1));

        // then

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren.run(f2), is(1));

        // when

        gren = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // in static layers
        assertThat(gren.run(f2), is(1));
    }

    @Test
    public void testConstructFromUrls_WithParent() throws Exception {

        // given

        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        // when

        Grengine gren = new Grengine(parent, Collections.singletonList(u1));

        // then

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren.run(f2), is(1));

        // when

        gren = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // in static layers
        assertThat(gren.run(f2), is(1));
    }


    @Test
    public void testConstructFromUrls_WithCompilerConfiguration() throws Exception {

        // given

        CompilerConfiguration config = new CompilerConfiguration();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        // when

        Grengine gren = new Grengine(config, Collections.singletonList(u1));

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren.run(f2), is(1));

        // when

        gren = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // in static layers
        assertThat(gren.run(f2), is(1));
    }
    @Test
    public void testConstructFromUrls_WithCompilerConfiguration_WithParent() throws Exception {

        // given

        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        // when

        Grengine gren = new Grengine(parent, config, Collections.singletonList(u1));

        // then

        // check that same compiler configuration
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        DefaultTopCodeCacheFactory defaultTopCodeCacheFactory =
                (DefaultTopCodeCacheFactory)layeredEngine.getBuilder().getTopCodeCacheFactory();
        DefaultGroovyCompilerFactory compilerFactory =
                (DefaultGroovyCompilerFactory)defaultTopCodeCacheFactory.getCompilerFactory();
        assertThat(config, is(compilerFactory.getCompilerConfiguration()));
        // check parent
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat(gren.run(f2), is(1));

        // when

        gren = new Grengine(Arrays.asList(u1, u2));

        // then

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(gren.run(f1), is(1));
        // in static layers
        assertThat(gren.run(f2), is(1));
    }


    @Test
    public void testConstructFromCompilerConfigNull() {

        // when/then

        assertThrows(() -> new Grengine((CompilerConfiguration)null),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

    @Test
    public void testConstructFromParentParentNull() {

        // when/then

        assertThrows(() -> new Grengine((ClassLoader)null),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromDir_DirNull() {

        // when/then

        assertThrows(() -> new Grengine((File)null),
                NullPointerException.class,
                "Directory is null.");
    }

    @Test
    public void testConstructFromDir_ParentNull() {

        // when/then

        assertThrows(() -> new Grengine((ClassLoader)null, new File(".")),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromDir_CompilerConfigurationNull() {

        // when/then

        assertThrows(() -> new Grengine((CompilerConfiguration)null, new File(".")),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

    @Test
    public void testConstructFromDirAndCompilerConfiguration_ParentNull() {

        // when/then

        assertThrows(() -> new Grengine(null, new CompilerConfiguration(), new File(".")),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromDirWithDirMode_DirNull() {

        // when/then

        assertThrows(() -> new Grengine(null, DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Directory is null.");
    }

    @Test
    public void testConstructFromDirWithDirMode_DirModeNull() {

        // when/then

        assertThrows(() -> new Grengine(new File("."), null),
                NullPointerException.class,
                "Dir mode is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrows(() -> new Grengine((CompilerConfiguration)null, new File("."), DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_DirNull() {

        // when/then

        assertThrows(() -> new Grengine(new CompilerConfiguration(), null, DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Directory is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_DirModeNull() {

        // when/then

        assertThrows(() -> new Grengine(new CompilerConfiguration(), new File("."), null),
                NullPointerException.class,
                "Dir mode is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_ParentNull() {

        // when/then

        assertThrows(() -> new Grengine((ClassLoader)null, new File("."), DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_DirNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), null, DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Directory is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_DirModeNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), new File("."), null),
                NullPointerException.class,
                "Dir mode is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_ParentNull() {

        // when/then

        assertThrows(() -> new Grengine(null, new CompilerConfiguration(), new File("."),
                        DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), null, new File("."),
                        DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), null,
                        DirMode.NO_SUBDIRS),
                NullPointerException.class,
                "Directory is null.");
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirModeNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), new File("."),
                        null),
                NullPointerException.class,
                "Dir mode is null.");
    }

    @Test
    public void testConstructFromUrls_UrlsNull() {

        // when/then

        assertThrows(() -> new Grengine((Collection<URL>)null),
                NullPointerException.class,
                "URL collection is null.");
    }

    @Test
    public void testConstructFromUrlsAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrows(() -> new Grengine((CompilerConfiguration)null, new LinkedList<>()),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

    @Test
    public void testConstructFromUrlsAndCompilerConfiguration_UrlsNull() {

        // when/then

        assertThrows(() -> new Grengine(new CompilerConfiguration(), (Collection<URL>)null),
                NullPointerException.class,
                "URL collection is null.");
    }

    @Test
    public void testConstructFromUrlsAndParent_ParentNull() {

        // when/then

        assertThrows(() -> new Grengine((ClassLoader)null, new LinkedList<>()),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromUrlsAndParent_UrlsNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), (Collection<URL>)null),
                NullPointerException.class,
                "URL collection is null.");
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_ParentNull() {

        // when/then

        assertThrows(() -> new Grengine(null, new CompilerConfiguration(), new LinkedList<>()),
                NullPointerException.class,
                "Parent class loader is null.");
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_CompilerConfigurationNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), null, new LinkedList<>()),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_UrlsNull() {

        // when/then

        assertThrows(() -> new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), (Collection<URL>)null),
                NullPointerException.class,
                "URL collection is null.");
    }


    @Test
    public void testClose() throws Exception {

        // given

        Grengine.Builder builder = new Grengine.Builder();

        LayeredEngine.Builder engineBuilder = new LayeredEngine.Builder();

        RecordingClassReleaser releaser = new RecordingClassReleaser();
        engineBuilder.setClassReleaser(releaser);

        LayeredEngine engine = engineBuilder.build();

        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        List<Sources> sourcesList = Collections.singletonList(sources);

        engine.setCodeLayersBySource(sourcesList);

        builder.setEngine(engine);

        Grengine gren = builder.build();

        Loader loaderAttached = gren.newAttachedLoader();
        Loader loaderDetached = gren.newDetachedLoader();

        Class<?> clazz1a = gren.loadClass(loaderAttached, "Class1");
        Class<?> clazz2a = gren.loadClass(loaderAttached, "Class2");
        clazz2a.getConstructor().newInstance();
        Class<?> clazz1d = gren.loadClass(loaderDetached, "Class1");
        Class<?> clazz2d = gren.loadClass(loaderDetached, "Class2");
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
    public void testAsClassLoaderBasic() throws Exception {

        // given

        File dir = tempFolder.getRoot();
        File testFile = new File(dir, "Test.groovy");
        TestUtil.setFileText(testFile, "class Test { static int get55() { 55 } }");

        // when


        Grengine gren = new Grengine(tempFolder.getRoot());

        // then

        assertThat(gren.run("Test.get55()"), is(55));


        // when

        GroovyShell shell = new GroovyShell(gren.asClassLoader());

        // then

        assertThat(shell.evaluate("Test.get55()"), is(55));


        // when

        shell = new GroovyShell(gren.asClassLoader(gren.getLoader()));

        // then

        assertThat(shell.evaluate("Test.get55()"), is(55));

        // make sure can load from parent loaders
        shell.evaluate("new StringBuffer().append('x')");

        gren.close();
    }

    @Test
    public void testAsClassLoaderLoaderNull() {

        // when/then

        assertThrows(() -> new Grengine().asClassLoader(null),
                NullPointerException.class,
                "Loader is null.");
    }

    @Test
    public void testAsClassLoaderLoaderForOtherEngine() {

        // when/then

        assertThrows(() -> new Grengine().asClassLoader(new Grengine().getLoader()),
                IllegalArgumentException.class,
                "Engine ID does not match (loader created by a different engine).");
    }

    @Test
    public void testMatrixSource() throws Exception {

        // when

        Grengine gren = new Grengine();

        // then

        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));

        // when

        TextSource st = (TextSource)gren.source("hello");

        // then

        assertThat(st.getText(), is("hello"));

        // when

        TextSource stn = (TextSource)gren.source("hello", "World");

        // then

        assertThat(stn.getText(), is("hello"));

        // when

        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        FileSource fs = (FileSource)gren.source(f);

        // then

        assertThat(fs.getFile().getPath(), is(f.getCanonicalPath()));

        // when

        URL u = f.toURI().toURL();
        UrlSource us = (UrlSource)gren.source(u);

        // then

        assertThat(us.getUrl(), is(u));
    }

    @Test
    public void testMatrixLoad() throws Exception {

        // given

        Grengine gren = new Grengine();

        // when

        TextSource st = (TextSource)gren.source("return 'text'");
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        File fu = new File(tempFolder.getRoot(), "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        URL u = fu.toURI().toURL();

        // then

        assertThat(((Script) gren.load("return 'text'").getConstructor().newInstance()).run(), is("text"));
        assertThat(((Script) gren.load(
                "return 'text-with-name'", "Script0").getConstructor().newInstance()).run(), is("text-with-name"));
        assertThat(((Script) gren.load(f).getConstructor().newInstance()).run(), is("file"));
        assertThat(((Script) gren.load(u).getConstructor().newInstance()).run(), is("url"));
        assertThat(((Script) gren.load(st).getConstructor().newInstance()).run(), is("text"));

        // when

        Loader loader = gren.newAttachedLoader();

        // then

        assertThat(((Script) gren.load(loader, "return 'text'").getConstructor().newInstance()).run(), is("text"));
        assertThat(((Script) gren.load(loader,
                "return 'text-with-name'", "Script0").getConstructor().newInstance()).run(), is("text-with-name"));
        assertThat(((Script) gren.load(loader, f).getConstructor().newInstance()).run(), is("file"));
        assertThat(((Script) gren.load(loader, u).getConstructor().newInstance()).run(), is("url"));
        assertThat(((Script) gren.load(loader, st).getConstructor().newInstance()).run(), is("text"));

        // when

        Loader loader2 = gren.newDetachedLoader();

        // then

        assertThat(((Script) gren.load(loader2, "return 'text'").getConstructor().newInstance()).run(), is("text"));
        assertThat(((Script) gren.load(loader2,
                "return 'text-with-name'", "Script0").getConstructor().newInstance()).run(), is("text-with-name"));
        assertThat(((Script) gren.load(loader2, f).getConstructor().newInstance()).run(), is("file"));
        assertThat(((Script) gren.load(loader2, u).getConstructor().newInstance()).run(), is("url"));
        assertThat(((Script) gren.load(loader2, st).getConstructor().newInstance()).run(), is("text"));
    }

    @Test
    public void testMatrixCreate() throws Exception {

        // given

        Grengine gren = new Grengine();

        // when

        TextSource st = (TextSource)gren.source("return 'text'");
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        File fu = new File(tempFolder.getRoot(), "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        URL u = fu.toURI().toURL();

        Loader loader = gren.newAttachedLoader();

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

        Class<?> clazz = gren.load(st);

        // then

        assertThat((gren.create(clazz)).run(), is("text"));

        // when/then

        assertThrowsStartsWith(() -> gren.create(String.class),
                CreateException.class,
                "Could not create script for class java.lang.String. " +
                        "Cause: java.lang.ClassCastException: ");
        assertThrowsStartsWith(() -> gren.create(loader, "class NotAScript {}"),
                CreateException.class,
                "Could not create script for class 'NotAScript' from source ");
    }

    @Test
    public void testMatrixBinding() {

        // given

        Grengine gren = new Grengine();

        // when

        Binding b = gren.binding();

        // then

        assertThat(b.getVariables().size(), is(0));

        // when

        b = gren.binding("aa", 11);

        // then

        assertThat(b.getVariables().size(), is(1));
        assertThat(b.getVariables().get("aa"), is(11));

        // when

        b = gren.binding("aa", 11, "bb", 22);

        // then

        assertThat(b.getVariables().size(), is(2));
        assertThat(b.getVariables().get("aa"), is(11));
        assertThat(b.getVariables().get("bb"), is(22));

        // when

        b = gren.binding("aa", 11, "bb", 22, "cc", 33);

        // then

        assertThat(b.getVariables().size(), is(3));
        assertThat(b.getVariables().get("aa"), is(11));
        assertThat(b.getVariables().get("bb"), is(22));
        assertThat(b.getVariables().get("cc"), is(33));

        // when

        b = gren.binding("aa", 11, "bb", 22, "cc", 33,
                "dd", 44);

        // then

        assertThat(b.getVariables().size(), is(4));
        assertThat(b.getVariables().get("aa"), is(11));
        assertThat(b.getVariables().get("bb"), is(22));
        assertThat(b.getVariables().get("cc"), is(33));
        assertThat(b.getVariables().get("dd"), is(44));

        // when

        b = gren.binding("aa", 11, "bb", 22, "cc", 33,
                "dd", 44, "ee", 55);

        // then

        assertThat(b.getVariables().size(), is(5));
        assertThat(b.getVariables().get("aa"), is(11));
        assertThat(b.getVariables().get("bb"), is(22));
        assertThat(b.getVariables().get("cc"), is(33));
        assertThat(b.getVariables().get("dd"), is(44));
        assertThat(b.getVariables().get("ee"), is(55));

        // when

        b = gren.binding("aa", 11, "bb", 22, "cc", 33,
                "dd", 44, "ee", 55, "ff", 66);

        // then

        assertThat(b.getVariables().size(), is(6));
        assertThat(b.getVariables().get("aa"), is(11));
        assertThat(b.getVariables().get("bb"), is(22));
        assertThat(b.getVariables().get("cc"), is(33));
        assertThat(b.getVariables().get("dd"), is(44));
        assertThat(b.getVariables().get("ee"), is(55));
        assertThat(b.getVariables().get("ff"), is(66));

        // when/then

        assertThrows(() -> gren.binding("aa", 11, "bb", 22,
                "cc", 33, "dd", 44, "ee", 55, "ff"),
                IllegalArgumentException.class,
                "Odd number of arguments.");
        assertThrows(() -> gren.binding("aa", 11, "bb", 22,
                "cc", 33, "dd", 44, "ee", 55, 7777, 66),
                IllegalArgumentException.class,
                "Argument 11 is not a string.");
        assertThat(gren.run("return x", gren.binding("x", 22)), is(22));
    }

    @Test
    public void testMatrixRun() throws Exception {

        // given

        Grengine gren = new Grengine();

        // when

        TextSource st = (TextSource)gren.source("return 'text'");
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        File fu = new File(tempFolder.getRoot(), "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        URL u = fu.toURI().toURL();

        Loader loader = gren.newAttachedLoader();

        // then

        assertThat(gren.run("return 'text'"), is("text"));
        assertThat(gren.run("return 'text-with-name'", "Script0"), is("text-with-name"));
        assertThat(gren.run(f), is("file"));
        assertThat(gren.run(u), is("url"));
        assertThat(gren.run(st), is("text"));

        assertThat(gren.run(loader, "return 'text'"), is("text"));
        assertThat(gren.run(loader, "return 'text-with-name'", "Script0"), is("text-with-name"));
        assertThat(gren.run(loader, f), is("file"));
        assertThat(gren.run(loader, u), is("url"));
        assertThat(gren.run(loader, st), is("text"));

        // when

        Map<String,Object> map = new HashMap<>();
        map.put("x", 99);
        st = (TextSource)gren.source("return x");
        // replace file and url so that immediately up to date
        f = new File(tempFolder.getRoot(), "ScriptX.groovy");
        TestUtil.setFileText(f, "return x");
        u = f.toURI().toURL();

        // then

        assertThat(gren.run("return x", map), is(99));
        assertThat(gren.run("return x", "Script0", map), is(99));
        assertThat(gren.run(f, map), is(99));
        assertThat(gren.run(u, map), is(99));
        assertThat(gren.run(st, map), is(99));

        assertThat(gren.run(loader, "return x", map), is(99));
        assertThat(gren.run(loader, "return x", "Script0", map), is(99));
        assertThat(gren.run(loader, f, map), is(99));
        assertThat(gren.run(loader, u, map), is(99));
        assertThat(gren.run(loader, st, map), is(99));

        // when

        Binding binding = new Binding(map);

        // then

        assertThat(gren.run("return x", binding), is(99));
        assertThat(gren.run("return x", "Script0", binding), is(99));
        assertThat(gren.run(f, binding), is(99));
        assertThat(gren.run(u, binding), is(99));
        assertThat(gren.run(st, binding), is(99));

        assertThat(gren.run(loader, "return x", binding), is(99));
        assertThat(gren.run(loader, "return x", "Script0", binding), is(99));
        assertThat(gren.run(loader, f, binding), is(99));
        assertThat(gren.run(loader, u, binding), is(99));
        assertThat(gren.run(loader, st, binding), is(99));

        // when

        Script script = gren.create(st);

        // then

        assertThat(gren.run(gren.create("return 99")), is(99));
        assertThat(gren.run(script, map), is(99));
        assertThat(gren.run(script, binding), is(99));
    }


    @Test
    public void testUpdateExceptionsCompileException() throws Exception {

        // given

        MockTextSource s1 = new MockTextSource("return 0");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(0)
                .setName("except")
                .build();
        List<Sources> sourcesLayers = Collections.singletonList(sources);

        MockUpdateExceptionNotifier notifier = new MockUpdateExceptionNotifier(null);

        // when

        Grengine gren = new Grengine.Builder()
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
        GrengineException e = gren.getLastUpdateException();

        // then

        assertThat(e, is(notNullValue()));
        assertThat(e, instanceOf(CompileException.class));
        assertThat(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: org.codehaus.groovy.control.MultipleCompilationErrorsException: "), is(true));
        assertThat(e, is(notifier.getLastUpdateException()));

        // when

        s1.setThrowAtGetText(new RuntimeException("unit test"));
        s1.setText("return 22");
        s1.setLastModified(222);

        Thread.sleep(30);

        gren.run(s1);
        e = gren.getLastUpdateException();

        // then

        assertThat(e, is(notNullValue()));
        assertThat(e, instanceOf(CompileException.class));
        assertThat(e.getMessage(), is("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: java.lang.RuntimeException: unit test"));
        assertThat(e, is(notifier.getLastUpdateException()));

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
    public void testUpdateExceptionsOtherException() {

        // given

        Source s1 = new DefaultTextSource("package org.junit\nclass Assume {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(0)
                .setName("except")
                .build();
        List<Sources> sourcesLayers = Collections.singletonList(sources);

        // when

        final Grengine gren = new Grengine.Builder()
                .setEngine(new LayeredEngine.Builder()
                        .setWithTopCodeCache(false)
                        .setAllowSameClassNamesInParentAndCodeLayers(false)
                        .build())
                .setSourcesLayers(sourcesLayers)
                .setLatencyMs(0)
                .build();
        GrengineException e = gren.getLastUpdateException();

        // then

        assertThat(e, is(notNullValue()));
        assertThat(e, not(instanceOf(CompileException.class)));
        assertThat(e.getMessage().startsWith("Failed to update Grengine. " +
                "Cause: ch.grengine.except.ClassNameConflictException: Found 1 class name conflict(s)"), is(true));
    }

}

