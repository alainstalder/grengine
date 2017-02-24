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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests the respective class.
 * 
 * @author Alain Stalder
 *
 */
public class GrengineTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testHelloWorld() throws Exception {
        new Grengine().run("println 'hello world'");
    }

    @Test
    public void testHelloWorldWithClose() throws Exception {
        Grengine gren = new Grengine();
        gren.run("print 'hello world '; [1,2,3].each { print it }; println()");
        gren.close();
    }

    @Test
    public void testConstructDefaults() throws Exception {

        Grengine.Builder builder = new Grengine.Builder();
        Grengine gren = builder.build();

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
    public void testConstructAllDefined() throws Exception {
        
        Grengine.Builder builder = new Grengine.Builder();
        Engine engine = new LayeredEngine.Builder().build();
        builder.setEngine(engine);
        SourceFactory sourceFactory = new DefaultSourceFactory();
        builder.setSourceFactory(sourceFactory);
        List<Sources> sourcesLayers = new LinkedList<Sources>();
        builder.setSourcesLayers();
        builder.setSourcesLayers(sourcesLayers);
        UpdateExceptionNotifier notifier = new MockUpdateExceptionNotifier(null);
        builder.setUpdateExceptionNotifier(notifier);
        builder.setLatencyMs(99);
        
        Grengine gren = builder.build();

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
    public void testModifyBuilderAfterUse() throws Exception {
        Grengine.Builder builder = new Grengine.Builder();
        builder.build();
        try {
            builder.setLatencyMs(999);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }

    @Test
    public void testConstructEmpty() throws Exception {

        Grengine gren = new Grengine();

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
    public void testConstructEmpty_WithParent() throws Exception {

        ClassLoader parent = new GroovyClassLoader();
        Grengine gren = new Grengine(parent);

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
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(dir);

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat((Integer)gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat((Integer)gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertThat(e.getMessage().contains("unable to resolve class ScriptSub2"), is(true));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass("ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "), is(true));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithoutSubDirs_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(parent, dir);

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat((Integer)gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat((Integer)gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertThat(e.getMessage().contains("unable to resolve class ScriptSub2"), is(true));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass("ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "), is(true));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithSubDirs() throws Exception {
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        assertThat((Integer)gren.run(fSub1), is(1));
        assertThat((Integer)gren.run(fSub2), is(2));
        assertThat((Integer)gren.run(fSub3), is(2));

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
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");
        
        Grengine gren = new Grengine.Builder()
                .setSourcesLayers(new DirBasedSources.Builder(dir).setDirMode(DirMode.NO_SUBDIRS).build())
                .setEngine(new LayeredEngine.Builder().setWithTopCodeCache(false).build())
                .build();

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        // all not found because not in static layers and no top code cache
        try {
            gren.run(fSub1);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Source not found: "), is(true));
        }
        try {
            gren.run(fSub2);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Source not found: "), is(true));
        }
        try {
            gren.run(fSub3);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Source not found: "), is(true));
        }
        
        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass("ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "), is(true));
        }
        // also not found, because there is no top code cache
        try {
            gren.loadClass(sSub1, "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Source not found: "), is(true));
        }
    }

    @Test
    public void testConstructFromCompilerConfig() throws Exception {
        CompilerConfiguration config = new CompilerConfiguration();

        Grengine gren = new Grengine(config);

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
    public void testConstructFromCompilerConfig_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        Grengine gren = new Grengine(parent, config);

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
        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<String>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(config, dir);

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
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat((Integer)gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat((Integer)gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertThat(e.getMessage().contains("unable to resolve class ScriptSub2"), is(true));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass("ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "), is(true));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithoutSubDirs_WithCompilerConfig_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<String>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(parent, config, dir);

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
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertThat((Integer)gren.run(fSub1), is(1));
        // found because compiled in top code cache
        assertThat((Integer)gren.run(fSub2), is(2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertThat(e.getMessage().contains("unable to resolve class ScriptSub2"), is(true));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass("Script1");
        gren.loadClass(s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass("ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "), is(true));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithSubDirs_WithCompilerConfiguration() throws Exception {
        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<String>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(config, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

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
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        assertThat((Integer)gren.run(fSub1), is(1));
        assertThat((Integer)gren.run(fSub2), is(2));
        assertThat((Integer)gren.run(fSub3), is(2));

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
        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        Set<String> scriptExtensions = new HashSet<String>();
        scriptExtensions.add("groovy");
        scriptExtensions.add("funky");
        config.setScriptExtensions(scriptExtensions);

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(parent, config, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

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
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        assertThat((Integer)gren.run(fSub1), is(1));
        assertThat((Integer)gren.run(fSub2), is(2));
        assertThat((Integer)gren.run(fSub3), is(2));

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
        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertThat(subDir.exists(), is(true));
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(parent, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        assertThat((Integer)gren.run(f2), is(1));
        assertThat((Integer)gren.run(fSub1), is(1));
        assertThat((Integer)gren.run(fSub2), is(2));
        assertThat((Integer)gren.run(fSub3), is(2));

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
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        Grengine gren = new Grengine(Arrays.asList(u1));

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat((Integer)gren.run(f2), is(1));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        // in static layers
        assertThat((Integer)gren.run(f2), is(1));
    }

    @Test
    public void testConstructFromUrls_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        Grengine gren = new Grengine(parent, Arrays.asList(u1));

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertThat(layeredEngine.getBuilder().getParent(), is(parent));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat((Integer)gren.run(f2), is(1));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        // in static layers
        assertThat((Integer)gren.run(f2), is(1));
    }


    @Test
    public void testConstructFromUrls_WithCompilerConfiguration() throws Exception {
        CompilerConfiguration config = new CompilerConfiguration();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        Grengine gren = new Grengine(config, Arrays.asList(u1));

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
        assertThat((Integer)gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat((Integer)gren.run(f2), is(1));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        // in static layers
        assertThat((Integer)gren.run(f2), is(1));
    }
    @Test
    public void testConstructFromUrls_WithCompilerConfiguration_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        URL u1 = f1.toURI().toURL();
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        URL u2 = f1.toURI().toURL();

        Grengine gren = new Grengine(parent, config, Arrays.asList(u1));

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
        assertThat((Integer)gren.run(f1), is(1));
        // created in top code cache, Script1 found in static layers
        assertThat((Integer)gren.run(f2), is(1));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat((Integer)gren.run(f1), is(1));
        // in static layers
        assertThat((Integer)gren.run(f2), is(1));
    }


    @Test
    public void testConstructFromCompilerConfigNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testConstructFromParentParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromDir_DirNull() throws Exception {
        try {
            new Grengine((File)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Directory is null."));
        }
    }

    @Test
    public void testConstructFromDir_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new File("."));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromDir_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null, new File("."));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testConstructFromDirAndCompilerConfiguration_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new CompilerConfiguration(), new File("."));
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirMode_DirNull() throws Exception {
        try {
            new Grengine((File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Directory is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirMode_DirModeNull() throws Exception {
        try {
            new Grengine(new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Dir mode is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null, new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_DirNull() throws Exception {
        try {
            new Grengine(new CompilerConfiguration(), (File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Directory is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_DirModeNull() throws Exception {
        try {
            new Grengine(new CompilerConfiguration(), new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Dir mode is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_DirNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Directory is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_DirModeNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Dir mode is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new CompilerConfiguration(), new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_CompilerConfigurationNull()
            throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (CompilerConfiguration)null, new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), (File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Directory is null."));
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirModeNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Dir mode is null."));
        }
    }

    @Test
    public void testConstructFromUrls_UrlsNull() throws Exception {
        try {
            new Grengine((Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("URL collection is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndCompilerConfiguration_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null, new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndCompilerConfiguration_UrlsNull() throws Exception {
        try {
            new Grengine(new CompilerConfiguration(), (Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("URL collection is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndParent_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndParent_UrlsNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("URL collection is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new CompilerConfiguration(), new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (CompilerConfiguration)null, new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_UrlsNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), (Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("URL collection is null."));
        }
    }

    @Test
    public void testClose() throws Exception {
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
        List<Sources> sourcesList = SourcesUtil.sourcesArrayToList(sources);

        engine.setCodeLayersBySource(sourcesList);

        builder.setEngine(engine);

        Grengine gren = builder.build();

        Loader loaderAttached = gren.newAttachedLoader();
        Loader loaderDetached = gren.newDetachedLoader();

        Class<?> clazz1a = gren.loadClass(loaderAttached, "Class1");
        Class<?> clazz2a = gren.loadClass(loaderAttached, "Class2");
        clazz2a.newInstance();
        Class<?> clazz1d = gren.loadClass(loaderDetached, "Class1");
        Class<?> clazz2d = gren.loadClass(loaderDetached, "Class2");
        clazz2d.newInstance();

        gren.close();

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
    public void testMatrixSource() throws Exception {
        
        Grengine gren = new Grengine();

        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));
        TextSource st = (TextSource)gren.source("hello");
        assertThat(st.getText(), is("hello"));

        assertThat(gren.getSourceFactory(), instanceOf(DefaultSourceFactory.class));
        TextSource stn = (TextSource)gren.source("hello", "World");
        assertThat(stn.getText(), is("hello"));
        
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        FileSource fs = (FileSource)gren.source(f);
        assertThat(fs.getFile().getPath(), is(f.getCanonicalPath()));
        
        URL u = f.toURI().toURL();
        UrlSource us = (UrlSource)gren.source(u);
        assertThat(us.getUrl(), is(u));
    }

    @Test
    public void testMatrixLoad() throws Exception {
        
        Grengine gren = new Grengine();
        
        TextSource st = (TextSource)gren.source("return 'text'");
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        File fu = new File(tempFolder.getRoot(), "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        URL u = fu.toURI().toURL();

        assertThat((String)((Script) gren.load("return 'text'").newInstance()).run(), is("text"));
        assertThat((String)((Script) gren.load(
                "return 'text-with-name'", "Script0").newInstance()).run(), is("text-with-name"));
        assertThat((String)((Script) gren.load(f).newInstance()).run(), is("file"));
        assertThat((String)((Script) gren.load(u).newInstance()).run(), is("url"));
        assertThat((String)((Script) gren.load(st).newInstance()).run(), is("text"));
        
        Loader loader = gren.newAttachedLoader();

        assertThat((String)((Script) gren.load(loader, "return 'text'").newInstance()).run(), is("text"));
        assertThat((String)((Script) gren.load(loader,
                "return 'text-with-name'", "Script0").newInstance()).run(), is("text-with-name"));
        assertThat((String)((Script) gren.load(loader, f).newInstance()).run(), is("file"));
        assertThat((String)((Script) gren.load(loader, u).newInstance()).run(), is("url"));
        assertThat((String)((Script) gren.load(loader, st).newInstance()).run(), is("text"));
        
        Loader loader2 = gren.newDetachedLoader();

        assertThat((String)((Script) gren.load(loader2, "return 'text'").newInstance()).run(), is("text"));
        assertThat((String)((Script) gren.load(loader2,
                "return 'text-with-name'", "Script0").newInstance()).run(), is("text-with-name"));
        assertThat((String)((Script) gren.load(loader2, f).newInstance()).run(), is("file"));
        assertThat((String)((Script) gren.load(loader2, u).newInstance()).run(), is("url"));
        assertThat((String)((Script) gren.load(loader2, st).newInstance()).run(), is("text"));
    }

    @Test
    public void testMatrixCreate() throws Exception {
        
        Grengine gren = new Grengine();
        
        TextSource st = (TextSource)gren.source("return 'text'");
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        File fu = new File(tempFolder.getRoot(), "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        URL u = fu.toURI().toURL();
        
        Loader loader = gren.newAttachedLoader();

        assertThat((String)(gren.create("return 'text'")).run(), is("text"));
        assertThat((String)(gren.create("return 'text-with-name'", "Script0")).run(), is("text-with-name"));
        assertThat((String)(gren.create(f)).run(), is("file"));
        assertThat((String)(gren.create(u)).run(), is("url"));
        assertThat((String)(gren.create(st)).run(), is("text"));

        assertThat((String)(gren.create(loader, "return 'text'")).run(), is("text"));
        assertThat((String)(gren.create(loader, "return 'text-with-name'", "Script0")).run(), is("text-with-name"));
        assertThat((String)(gren.create(loader, f)).run(), is("file"));
        assertThat((String)(gren.create(loader, u)).run(), is("url"));
        assertThat((String)(gren.create(loader, st)).run(), is("text"));
        
        Class<?> clazz = gren.load(st);
        assertThat((String)(gren.create(clazz)).run(), is("text"));
        
        try {
            gren.create(String.class);
        } catch (CreateException e) {
            assertThat(e.getMessage().startsWith("Could not create script for class java.lang.String. " +
                    "Cause: java.lang.ClassCastException: "), is(true));
        }
        
        try {
            gren.create(loader, "class NotAScript {}");
        } catch (CreateException e) {
            assertThat(e.getMessage().startsWith("Could not create script for class 'NotAScript' from source "), is(true));
        }
    }

    @Test
    public void testMatrixBinding() throws Exception {
        
        Grengine gren = new Grengine();
        
        Binding b = gren.binding();
        assertThat(b.getVariables().size(), is(0));
        
        b = gren.binding("aa", 11);
        assertThat(b.getVariables().size(), is(1));
        assertThat((Integer)b.getVariables().get("aa"), is(11));
        
        b = gren.binding("aa", 11, "bb", 22);
        assertThat(b.getVariables().size(), is(2));
        assertThat((Integer)b.getVariables().get("aa"), is(11));
        assertThat((Integer)b.getVariables().get("bb"), is(22));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33);
        assertThat(b.getVariables().size(), is(3));
        assertThat((Integer)b.getVariables().get("aa"), is(11));
        assertThat((Integer)b.getVariables().get("bb"), is(22));
        assertThat((Integer)b.getVariables().get("cc"), is(33));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44);
        assertThat(b.getVariables().size(), is(4));
        assertThat((Integer)b.getVariables().get("aa"), is(11));
        assertThat((Integer)b.getVariables().get("bb"), is(22));
        assertThat((Integer)b.getVariables().get("cc"), is(33));
        assertThat((Integer)b.getVariables().get("dd"), is(44));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55);
        assertThat(b.getVariables().size(), is(5));
        assertThat((Integer)b.getVariables().get("aa"), is(11));
        assertThat((Integer)b.getVariables().get("bb"), is(22));
        assertThat((Integer)b.getVariables().get("cc"), is(33));
        assertThat((Integer)b.getVariables().get("dd"), is(44));
        assertThat((Integer)b.getVariables().get("ee"), is(55));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55, "ff", 66);
        assertThat(b.getVariables().size(), is(6));
        assertThat((Integer)b.getVariables().get("aa"), is(11));
        assertThat((Integer)b.getVariables().get("bb"), is(22));
        assertThat((Integer)b.getVariables().get("cc"), is(33));
        assertThat((Integer)b.getVariables().get("dd"), is(44));
        assertThat((Integer)b.getVariables().get("ee"), is(55));
        assertThat((Integer)b.getVariables().get("ff"), is(66));
        
        try {
            gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55, "ff");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Odd number of arguments."));
        }
        
        try {
            gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55, 7777, 66);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Argument 11 is not a string."));
        }

        assertThat((Integer)gren.run("return x", gren.binding("x", 22)), is(22));
    }
    
    @Test
    public void testMatrixRun() throws Exception {
        
        Grengine gren = new Grengine();
        
        TextSource st = (TextSource)gren.source("return 'text'");
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        TestUtil.setFileText(f, "return 'file'");
        File fu = new File(tempFolder.getRoot(), "Script2.groovy");
        TestUtil.setFileText(fu, "return 'url'");
        URL u = fu.toURI().toURL();
        
        Loader loader = gren.newAttachedLoader();

        assertThat((String)gren.run("return 'text'"), is("text"));
        assertThat((String)gren.run("return 'text-with-name'", "Script0"), is("text-with-name"));
        assertThat((String)gren.run(f), is("file"));
        assertThat((String)gren.run(u), is("url"));
        assertThat((String)gren.run(st), is("text"));

        assertThat((String)gren.run(loader, "return 'text'"), is("text"));
        assertThat((String)gren.run(loader, "return 'text-with-name'", "Script0"), is("text-with-name"));
        assertThat((String)gren.run(loader, f), is("file"));
        assertThat((String)gren.run(loader, u), is("url"));
        assertThat((String)gren.run(loader, st), is("text"));
        
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("x", 99);
        st = (TextSource)gren.source("return x");
        // replace file and url so that immediately up to date
        f = new File(tempFolder.getRoot(), "ScriptX.groovy");
        TestUtil.setFileText(f, "return x");
        u = f.toURI().toURL();

        assertThat((Integer)gren.run("return x", map), is(99));
        assertThat((Integer)gren.run("return x", "Script0", map), is(99));
        assertThat((Integer)gren.run(f, map), is(99));
        assertThat((Integer)gren.run(u, map), is(99));
        assertThat((Integer)gren.run(st, map), is(99));

        assertThat((Integer)gren.run(loader, "return x", map), is(99));
        assertThat((Integer)gren.run(loader, "return x", "Script0", map), is(99));
        assertThat((Integer)gren.run(loader, f, map), is(99));
        assertThat((Integer)gren.run(loader, u, map), is(99));
        assertThat((Integer)gren.run(loader, st, map), is(99));
        
        Binding binding = new Binding(map);

        assertThat((Integer)gren.run("return x", binding), is(99));
        assertThat((Integer)gren.run("return x", "Script0", binding), is(99));
        assertThat((Integer)gren.run(f, binding), is(99));
        assertThat((Integer)gren.run(u, binding), is(99));
        assertThat((Integer)gren.run(st, binding), is(99));

        assertThat((Integer)gren.run(loader, "return x", binding), is(99));
        assertThat((Integer)gren.run(loader, "return x", "Script0", binding), is(99));
        assertThat((Integer)gren.run(loader, f, binding), is(99));
        assertThat((Integer)gren.run(loader, u, binding), is(99));
        assertThat((Integer)gren.run(loader, st, binding), is(99));
        
        Script script = gren.create(st);
        assertThat((Integer)gren.run(gren.create("return 99")), is(99));
        assertThat((Integer)gren.run(script, map), is(99));
        assertThat((Integer)gren.run(script, binding), is(99));
    }
    
    
    @Test
    public void testUpdateExceptionsCompileException() throws Exception {

        MockTextSource s1 = new MockTextSource("return 0");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(0)
                .setName("except")
                .build();
        List<Sources> sourcesLayers = SourcesUtil.sourcesArrayToList(sources);
        
        MockUpdateExceptionNotifier notifier = new MockUpdateExceptionNotifier(null);
        
        Grengine gren = new Grengine.Builder()
                .setEngine(new LayeredEngine.Builder().setWithTopCodeCache(false).build())
                .setSourcesLayers(sourcesLayers)
                .setUpdateExceptionNotifier(notifier)
                .setLatencyMs(0)
                .build();

        assertThat((Integer)gren.run(s1), is(0));
        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(notifier.getLastUpdateException(), is(nullValue()));
        
        s1.setText("&%&%");
        s1.setLastModified(99);
        
        Thread.sleep(30);
        
        gren.run(s1);
        GrengineException e = gren.getLastUpdateException();
        assertThat(e, is(notNullValue()));
        assertThat(e, instanceOf(CompileException.class));
        assertThat(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: org.codehaus.groovy.control.MultipleCompilationErrorsException: "), is(true));
        assertThat(e, is(notifier.getLastUpdateException()));
        
        s1.setThrowAtGetText(new RuntimeException("unit test"));
        s1.setText("return 22");
        s1.setLastModified(222);
        
        Thread.sleep(30);
        
        gren.run(s1);
        e = gren.getLastUpdateException();
        assertThat(e, is(notNullValue()));
        assertThat(e, instanceOf(CompileException.class));
        assertThat(e.getMessage(), is("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: java.lang.RuntimeException: unit test"));
        assertThat(e, is(notifier.getLastUpdateException()));
        
        s1.setThrowAtGetText(null);
        s1.setText("return 33");
        s1.setLastModified(333);
        
        Thread.sleep(30);

        assertThat((Integer)gren.run(s1), is(33));
        assertThat(gren.getLastUpdateException(), is(nullValue()));
        assertThat(notifier.getLastUpdateException(), is(nullValue()));
    }

    @Test
    public void testUpdateExceptionsOtherException() throws Exception {

        Source s1 = new DefaultTextSource("package org.junit\nclass Assume {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(0)
                .setName("except")
                .build();
        List<Sources> sourcesLayers = SourcesUtil.sourcesArrayToList(sources);
        
        final Grengine gren = new Grengine.Builder()
                .setEngine(new LayeredEngine.Builder()
                        .setWithTopCodeCache(false)
                        .setAllowSameClassNamesInParentAndCodeLayers(false)
                        .build())
                .setSourcesLayers(sourcesLayers)
                .setLatencyMs(0)
                .build();

        GrengineException e = gren.getLastUpdateException();
        assertThat(e, is(notNullValue()));
        assertThat(e, not(instanceOf(CompileException.class)));
        assertThat(e.getMessage().startsWith("Failed to update Grengine. " +
                "Cause: ch.grengine.except.ClassNameConflictException: Found 1 class name conflict(s)"), is(true));
    }

}

