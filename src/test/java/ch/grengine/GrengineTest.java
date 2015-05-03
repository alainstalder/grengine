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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

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

import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    public void testConstructDefaults() throws Exception {

        Grengine.Builder builder = new Grengine.Builder();
        Grengine gren = builder.build();

        assertEquals(builder, gren.getBuilder());

        assertNotNull(gren.getEngine());
        assertEquals(gren.getBuilder().getEngine(), gren.getEngine());
        assertTrue(gren.getEngine() instanceof LayeredEngine);
        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertEquals(Thread.currentThread().getContextClassLoader(), engine.getBuilder().getParent());
        assertEquals(LoadMode.CURRENT_FIRST, engine.getBuilder().getLoadMode());
        assertTrue(engine.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.PARENT_FIRST, engine.getBuilder().getTopLoadMode());
        assertNotNull(engine.getBuilder().getTopCodeCacheFactory());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers());

        assertNotNull(gren.getSourceFactory());
        assertEquals(gren.getBuilder().getSourceFactory(), gren.getSourceFactory());
        assertTrue(gren.getSourceFactory() instanceof DefaultSourceFactory);

        assertEquals(0, gren.getBuilder().getSourcesLayers().size());

        assertEquals(Grengine.Builder.DEFAULT_LATENCY_MS, gren.getBuilder().getLatencyMs());
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
        
        assertEquals(builder, gren.getBuilder());
        assertEquals(engine, gren.getEngine());
        assertEquals(gren.getBuilder().getEngine(), gren.getEngine());
        assertEquals(sourceFactory, gren.getSourceFactory());
        assertEquals(gren.getBuilder().getSourceFactory(), gren.getSourceFactory());
        assertEquals(sourcesLayers, gren.getBuilder().getSourcesLayers());
        assertEquals(notifier, gren.getBuilder().getUpdateExceptionNotifier());
        assertEquals(99, gren.getBuilder().getLatencyMs());
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        Grengine.Builder builder = new Grengine.Builder();
        builder.build();
        try {
            builder.setLatencyMs(999);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }

    @Test
    public void testConstructEmpty() throws Exception {

        Grengine gren = new Grengine();

        assertNotNull(gren.getEngine());
        assertEquals(gren.getBuilder().getEngine(), gren.getEngine());
        assertTrue(gren.getEngine() instanceof LayeredEngine);
        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertEquals(Thread.currentThread().getContextClassLoader(), engine.getBuilder().getParent());
        assertEquals(LoadMode.CURRENT_FIRST, engine.getBuilder().getLoadMode());
        assertTrue(engine.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.PARENT_FIRST, engine.getBuilder().getTopLoadMode());
        assertNotNull(engine.getBuilder().getTopCodeCacheFactory());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers());

        assertNotNull(gren.getSourceFactory());
        assertEquals(gren.getBuilder().getSourceFactory(), gren.getSourceFactory());
        assertTrue(gren.getSourceFactory() instanceof DefaultSourceFactory);

        assertEquals(0, gren.getBuilder().getSourcesLayers().size());

        assertEquals(Grengine.Builder.DEFAULT_LATENCY_MS, gren.getBuilder().getLatencyMs());
    }

    @Test
    public void testConstructEmpty_WithParent() throws Exception {

        ClassLoader parent = new GroovyClassLoader();
        Grengine gren = new Grengine(parent);

        assertNotNull(gren.getEngine());
        assertEquals(gren.getBuilder().getEngine(), gren.getEngine());
        assertTrue(gren.getEngine() instanceof LayeredEngine);
        LayeredEngine engine = (LayeredEngine)gren.getEngine();
        assertEquals(parent, engine.getBuilder().getParent());
        assertEquals(LoadMode.CURRENT_FIRST, engine.getBuilder().getLoadMode());
        assertTrue(engine.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.PARENT_FIRST, engine.getBuilder().getTopLoadMode());
        assertNotNull(engine.getBuilder().getTopCodeCacheFactory());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers());

        assertNotNull(gren.getSourceFactory());
        assertEquals(gren.getBuilder().getSourceFactory(), gren.getSourceFactory());
        assertTrue(gren.getSourceFactory() instanceof DefaultSourceFactory);

        assertEquals(0, gren.getBuilder().getSourcesLayers().size());

        assertEquals(Grengine.Builder.DEFAULT_LATENCY_MS, gren.getBuilder().getLatencyMs());
    }

    @Test
    public void testConstructFromDirWithoutSubdirs() throws Exception {
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertTrue(subDir.exists());
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(dir);

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertEquals(Thread.currentThread().getContextClassLoader(), layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertEquals(1, gren.run(fSub1));
        // found because compiled in top code cache
        assertEquals(2, gren.run(fSub2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve class ScriptSub2"));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass(gren.getLoader(), "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithoutSubdirs_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertTrue(subDir.exists());
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(parent, dir);

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertEquals(parent, layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertEquals(1, gren.run(fSub1));
        // found because compiled in top code cache
        assertEquals(2, gren.run(fSub2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve class ScriptSub2"));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass(gren.getLoader(), "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithSubdirs() throws Exception {
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertTrue(subDir.exists());
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        assertEquals(1, gren.run(fSub1));
        assertEquals(2, gren.run(fSub2));
        assertEquals(2, gren.run(fSub3));

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass(gren.getLoader(), "ScriptSub1");
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithoutSubdirsNoTopCodeCache() throws Exception {
        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertTrue(subDir.exists());
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
        
        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        // all not found because not in static layers and no top code cache
        try {
            gren.run(fSub1);
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Source not found: "));
        }
        try {
            gren.run(fSub2);
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Source not found: "));
        }
        try {
            gren.run(fSub3);
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Source not found: "));
        }
        
        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass(gren.getLoader(), "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "));
        }
        // also not found, because there is no top code cache
        try {
            gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Source not found: "));
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(Thread.currentThread().getContextClassLoader(), layeredEngine.getBuilder().getParent());
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(parent, layeredEngine.getBuilder().getParent());
    }


    @Test
    public void testConstructFromDirWithoutSubdirs_WithCompilerConfig() throws Exception {
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
        assertTrue(subDir.exists());
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(Thread.currentThread().getContextClassLoader(), layeredEngine.getBuilder().getParent());

        // check that script extensions set from compiler configuration
        assertEquals(scriptExtensions,
                ((DirBasedSources)gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertEquals(1, gren.run(fSub1));
        // found because compiled in top code cache
        assertEquals(2, gren.run(fSub2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve class ScriptSub2"));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass(gren.getLoader(), "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithoutSubdirs_WithCompilerConfig_WithParent() throws Exception {
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
        assertTrue(subDir.exists());
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(parent, layeredEngine.getBuilder().getParent());

        // check that script extensions set from compiler configuration
        assertEquals(scriptExtensions,
                ((DirBasedSources)gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        // found because compiled in top code cache and that one sees Script1 in static layer
        assertEquals(1, gren.run(fSub1));
        // found because compiled in top code cache
        assertEquals(2, gren.run(fSub2));
        // not found because compiled in top code cache and there ScriptSub2 is not visible
        // (has its own separate class loader in the top code cache)
        try {
            gren.run(fSub3);
            fail();
        } catch (CompileException e) {
            assertTrue(e.getMessage().contains("unable to resolve class ScriptSub2"));
        }

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        // not found because only in top code cache, not in static code layers
        Source sSub1 = new DefaultFileSource(fSub1);
        try {
            gren.loadClass(gren.getLoader(), "ScriptSub1");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Could not load class 'ScriptSub1'. Cause: "));
        }
        // this works, because loading by source from top code cache
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }


    @Test
    public void testConstructFromDirWithSubdirs_WithCompilerConfiguration() throws Exception {
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
        assertTrue(subDir.exists());
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(Thread.currentThread().getContextClassLoader(), layeredEngine.getBuilder().getParent());

        // check that script extensions set from compiler configuration
        assertEquals(scriptExtensions,
                ((DirBasedSources) gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        assertEquals(1, gren.run(fSub1));
        assertEquals(2, gren.run(fSub2));
        assertEquals(2, gren.run(fSub3));

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass(gren.getLoader(), "ScriptSub1");
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithSubdirs_WithCompilerConfiguration_WithParent() throws Exception {
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
        assertTrue(subDir.exists());
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(parent, layeredEngine.getBuilder().getParent());


        // check that script extensions set from compiler configuration
        assertEquals(scriptExtensions,
                ((DirBasedSources) gren.getBuilder().getSourcesLayers().get(0)).getScriptExtensions());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        assertEquals(1, gren.run(fSub1));
        assertEquals(2, gren.run(fSub2));
        assertEquals(2, gren.run(fSub3));

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass(gren.getLoader(), "ScriptSub1");
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
    }

    @Test
    public void testConstructFromDirWithSubdirs_WithParent() throws Exception {
        ClassLoader parent = new GroovyClassLoader();

        File dir = tempFolder.getRoot();
        File f1 = new File(dir, "Script1.groovy");
        TestUtil.setFileText(f1, "return 1");
        File f2 = new File(dir, "Script2.groovy");
        TestUtil.setFileText(f2, "return new Script1().run()");
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        assertTrue(subDir.exists());
        File fSub1 = new File(subDir, "ScriptSub1.groovy");
        TestUtil.setFileText(fSub1, "return new Script1().run()");
        File fSub2 = new File(subDir, "ScriptSub2.groovy");
        TestUtil.setFileText(fSub2, "return 2");
        File fSub3 = new File(subDir, "ScriptSub3.groovy");
        TestUtil.setFileText(fSub3, "return new ScriptSub2().run()");

        Grengine gren = new Grengine(parent, dir, DirMode.WITH_SUBDIRS_RECURSIVE);

        // check parent
        LayeredEngine layeredEngine = (LayeredEngine)gren.getEngine();
        assertEquals(parent, layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        assertEquals(1, gren.run(f2));
        assertEquals(1, gren.run(fSub1));
        assertEquals(2, gren.run(fSub2));
        assertEquals(2, gren.run(fSub3));

        // extra: load with class name
        Source s1 = new DefaultFileSource(f1);
        gren.loadClass(gren.getLoader(), "Script1");
        gren.loadClass(gren.getLoader(), s1, "Script1");
        Source sSub1 = new DefaultFileSource(fSub1);
        gren.loadClass(gren.getLoader(), "ScriptSub1");
        gren.loadClass(gren.getLoader(), sSub1, "ScriptSub1");
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
        assertEquals(Thread.currentThread().getContextClassLoader(), layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // created in top code cache, Script1 found in static layers
        assertEquals(1, gren.run(f2));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // in static layers
        assertEquals(1, gren.run(f2));
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
        assertEquals(parent, layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // created in top code cache, Script1 found in static layers
        assertEquals(1, gren.run(f2));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // in static layers
        assertEquals(1, gren.run(f2));
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(Thread.currentThread().getContextClassLoader(), layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // created in top code cache, Script1 found in static layers
        assertEquals(1, gren.run(f2));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // in static layers
        assertEquals(1, gren.run(f2));
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
        assertEquals(compilerFactory.getCompilerConfiguration(), config);
        // check parent
        assertEquals(parent, layeredEngine.getBuilder().getParent());

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // created in top code cache, Script1 found in static layers
        assertEquals(1, gren.run(f2));

        gren = new Grengine(Arrays.asList(u1, u2));

        assertNull(gren.getLastUpdateException());
        assertEquals(1, gren.run(f1));
        // in static layers
        assertEquals(1, gren.run(f2));
    }


    @Test
    public void testConstructFromCompilerConfigNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromParentParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDir_DirNull() throws Exception {
        try {
            new Grengine((File)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Directory is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDir_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new File("."));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDir_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null, new File("."));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirAndCompilerConfiguration_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new CompilerConfiguration(), new File("."));
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirMode_DirNull() throws Exception {
        try {
            new Grengine((File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Directory is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirMode_DirModeNull() throws Exception {
        try {
            new Grengine(new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Dir mode is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null, new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_DirNull() throws Exception {
        try {
            new Grengine(new CompilerConfiguration(), (File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Directory is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndCompilerConfiguration_DirModeNull() throws Exception {
        try {
            new Grengine(new CompilerConfiguration(), new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Dir mode is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_DirNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Directory is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParent_DirModeNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Dir mode is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new CompilerConfiguration(), new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_CompilerConfigurationNull()
            throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (CompilerConfiguration)null, new File("."), DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), (File)null, DirMode.NO_SUBDIRS);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Directory is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromDirWithDirModeAndParentAndCompilerConfiguration_DirModeNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), new File("."), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Dir mode is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrls_UrlsNull() throws Exception {
        try {
            new Grengine((Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("URL collection is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndCompilerConfiguration_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine((CompilerConfiguration)null, new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndCompilerConfiguration_UrlsNull() throws Exception {
        try {
            new Grengine(new CompilerConfiguration(), (Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("URL collection is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndParent_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndParent_UrlsNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("URL collection is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_ParentNull() throws Exception {
        try {
            new Grengine((ClassLoader)null, new CompilerConfiguration(), new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_CompilerConfigurationNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), (CompilerConfiguration)null, new LinkedList<URL>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

    @Test
    public void testConstructFromUrlsAndParentAndCompilerConfiguration_UrlsNull() throws Exception {
        try {
            new Grengine(new GroovyClassLoader(), new CompilerConfiguration(), (Collection<URL>)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("URL collection is null.", e.getMessage());
        }
    }

    
    @Test
    public void testMatrixSource() throws Exception {
        
        Grengine gren = new Grengine();
        
        assertTrue(gren.getSourceFactory() instanceof DefaultSourceFactory);
        TextSource st = (TextSource)gren.source("hello");
        assertEquals("hello", st.getText());
        
        assertTrue(gren.getSourceFactory() instanceof DefaultSourceFactory);
        TextSource stn = (TextSource)gren.source("hello", "World");
        assertEquals("hello", stn.getText());
        
        File f = new File(tempFolder.getRoot(), "Script1.groovy");
        FileSource fs = (FileSource)gren.source(f);
        assertEquals(f.getCanonicalPath(), fs.getFile().getPath());
        
        URL u = f.toURI().toURL();
        UrlSource us = (UrlSource)gren.source(u);
        assertEquals(u, us.getUrl());
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
                
        assertEquals("text", ((Script)gren.load("return 'text'").newInstance()).run());
        assertEquals("text-with-name", ((Script)gren.load(
                "return 'text-with-name'", "Script0").newInstance()).run());
        assertEquals("file", ((Script)gren.load(f).newInstance()).run());
        assertEquals("url", ((Script)gren.load(u).newInstance()).run());
        assertEquals("text", ((Script)gren.load(st).newInstance()).run());
        
        Loader loader = gren.newAttachedLoader();
        
        assertEquals("text", ((Script)gren.load(loader, "return 'text'").newInstance()).run());
        assertEquals("text-with-name", ((Script)gren.load(loader, 
                "return 'text-with-name'", "Script0").newInstance()).run());
        assertEquals("file", ((Script)gren.load(loader, f).newInstance()).run());
        assertEquals("url", ((Script)gren.load(loader, u).newInstance()).run());
        assertEquals("text", ((Script)gren.load(loader, st).newInstance()).run());
        
        Loader loader2 = gren.newDetachedLoader();
        
        assertEquals("text", ((Script)gren.load(loader2, "return 'text'").newInstance()).run());
        assertEquals("text-with-name", ((Script)gren.load(loader2, 
                "return 'text-with-name'", "Script0").newInstance()).run());
        assertEquals("file", ((Script)gren.load(loader2, f).newInstance()).run());
        assertEquals("url", ((Script)gren.load(loader2, u).newInstance()).run());
        assertEquals("text", ((Script)gren.load(loader2, st).newInstance()).run());
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
        
        assertEquals("text", ((Script)gren.create("return 'text'")).run());
        assertEquals("text-with-name", ((Script)gren.create("return 'text-with-name'", "Script0")).run());
        assertEquals("file", ((Script)gren.create(f)).run());
        assertEquals("url", ((Script)gren.create(u)).run());
        assertEquals("text", ((Script)gren.create(st)).run());
        
        assertEquals("text", ((Script)gren.create(loader, "return 'text'")).run());
        assertEquals("text-with-name", ((Script)gren.create(loader, "return 'text-with-name'", "Script0")).run());
        assertEquals("file", ((Script)gren.create(loader, f)).run());
        assertEquals("url", ((Script)gren.create(loader, u)).run());
        assertEquals("text", ((Script)gren.create(loader, st)).run());
        
        Class<?> clazz = gren.load(st);
        assertEquals("text", ((Script)gren.create(clazz)).run());
        
        try {
            gren.create(String.class);
        } catch (CreateException e) {
            assertTrue(e.getMessage().startsWith("Could not create script for class java.lang.String. " +
                    "Cause: java.lang.ClassCastException: "));
        }
        
        try {
            gren.create(loader, "class NotAScript {}");
        } catch (CreateException e) {
            assertTrue(e.getMessage().startsWith("Could not create script for class 'NotAScript' from source "));
        }
    }

    @Test
    public void testMatrixBinding() throws Exception {
        
        Grengine gren = new Grengine();
        
        Binding b = gren.binding();
        assertEquals(0, b.getVariables().size());
        
        b = gren.binding("aa", 11);
        assertEquals(1, b.getVariables().size());
        assertEquals(11, b.getVariables().get("aa"));
        
        b = gren.binding("aa", 11, "bb", 22);
        assertEquals(2, b.getVariables().size());
        assertEquals(11, b.getVariables().get("aa"));
        assertEquals(22, b.getVariables().get("bb"));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33);
        assertEquals(3, b.getVariables().size());
        assertEquals(11, b.getVariables().get("aa"));
        assertEquals(22, b.getVariables().get("bb"));
        assertEquals(33, b.getVariables().get("cc"));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44);
        assertEquals(4, b.getVariables().size());
        assertEquals(11, b.getVariables().get("aa"));
        assertEquals(22, b.getVariables().get("bb"));
        assertEquals(33, b.getVariables().get("cc"));
        assertEquals(44, b.getVariables().get("dd"));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55);
        assertEquals(5, b.getVariables().size());
        assertEquals(11, b.getVariables().get("aa"));
        assertEquals(22, b.getVariables().get("bb"));
        assertEquals(33, b.getVariables().get("cc"));
        assertEquals(44, b.getVariables().get("dd"));
        assertEquals(55, b.getVariables().get("ee"));
        
        b = gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55, "ff", 66);
        assertEquals(6, b.getVariables().size());
        assertEquals(11, b.getVariables().get("aa"));
        assertEquals(22, b.getVariables().get("bb"));
        assertEquals(33, b.getVariables().get("cc"));
        assertEquals(44, b.getVariables().get("dd"));
        assertEquals(55, b.getVariables().get("ee"));
        assertEquals(66, b.getVariables().get("ff"));
        
        try {
            gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55, "ff");
        } catch (IllegalArgumentException e) {
            assertEquals("Odd number of arguments.", e.getMessage());
        }
        
        try {
            gren.binding("aa", 11, "bb", 22, "cc", 33, "dd", 44, "ee", 55, 7777, 66);
        } catch (IllegalArgumentException e) {
            assertEquals("Argument 11 is not a string.", e.getMessage());
        }
        
        assertEquals(22, gren.run("return x", gren.binding("x", 22)));
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
        
        assertEquals("text", gren.run("return 'text'"));
        assertEquals("text-with-name", gren.run("return 'text-with-name'", "Script0"));
        assertEquals("file", gren.run(f));
        assertEquals("url", gren.run(u));
        assertEquals("text", gren.run(st));
        
        assertEquals("text", gren.run(loader, "return 'text'"));
        assertEquals("text-with-name", gren.run(loader, "return 'text-with-name'", "Script0"));
        assertEquals("file", gren.run(loader, f));
        assertEquals("url", gren.run(loader, u));
        assertEquals("text", gren.run(loader, st));
        
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("x", 99);
        st = (TextSource)gren.source("return x");
        // replace file and url so that immediately up to date
        f = new File(tempFolder.getRoot(), "ScriptX.groovy");
        TestUtil.setFileText(f, "return x");
        u = f.toURI().toURL();

        assertEquals(99, gren.run("return x", map));
        assertEquals(99, gren.run("return x", "Script0", map));
        assertEquals(99, gren.run(f, map));
        assertEquals(99, gren.run(u, map));
        assertEquals(99, gren.run(st, map));

        assertEquals(99, gren.run(loader, "return x", map));
        assertEquals(99, gren.run(loader, "return x", "Script0", map));
        assertEquals(99, gren.run(loader, f, map));
        assertEquals(99, gren.run(loader, u, map));
        assertEquals(99, gren.run(loader, st, map));
        
        Binding binding = new Binding(map);
        
        assertEquals(99, gren.run("return x", binding));
        assertEquals(99, gren.run("return x", "Script0", binding));
        assertEquals(99, gren.run(f, binding));
        assertEquals(99, gren.run(u, binding));
        assertEquals(99, gren.run(st, binding));

        assertEquals(99, gren.run(loader, "return x", binding));
        assertEquals(99, gren.run(loader, "return x", "Script0", binding));
        assertEquals(99, gren.run(loader, f, binding));
        assertEquals(99, gren.run(loader, u, binding));
        assertEquals(99, gren.run(loader, st, binding));
        
        Script script = gren.create(st);
        assertEquals(99, gren.run(gren.create("return 99")));
        assertEquals(99, gren.run(script, map));
        assertEquals(99, gren.run(script, binding));
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
        
        assertEquals(0, gren.run(s1));
        assertNull(gren.getLastUpdateException());
        assertNull(notifier.getLastUpdateException());
        
        s1.setText("&%&%");
        s1.setLastModified(99);
        
        Thread.sleep(30);
        
        gren.run(s1);
        GrengineException e = gren.getLastUpdateException();
        assertNotNull(e);
        assertTrue(e instanceof CompileException);
        assertTrue(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: org.codehaus.groovy.control.MultipleCompilationErrorsException: "));
        assertEquals(notifier.getLastUpdateException(), e);
        
        s1.setThrowAtGetText(new RuntimeException("unit test"));
        s1.setText("return 22");
        s1.setLastModified(222);
        
        Thread.sleep(30);
        
        gren.run(s1);
        e = gren.getLastUpdateException();
        assertNotNull(e);
        assertTrue(e instanceof CompileException);
        assertEquals("Compile failed for sources FixedSetSources[name='except']. " +
                "Cause: java.lang.RuntimeException: unit test",
                e.getMessage());
        assertEquals(notifier.getLastUpdateException(), e);
        
        s1.setThrowAtGetText(null);
        s1.setText("return 33");
        s1.setLastModified(333);
        
        Thread.sleep(30);
        
        assertEquals(33, gren.run(s1));
        assertNull(gren.getLastUpdateException());
        assertNull(notifier.getLastUpdateException());
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
        assertNotNull(e);
        assertFalse(e instanceof CompileException);
        assertTrue(e.getMessage().startsWith("Failed to update Grengine. " +
                "Cause: ch.grengine.except.ClassNameConflictException: Found 1 class name conflict(s)"));
    }

}

