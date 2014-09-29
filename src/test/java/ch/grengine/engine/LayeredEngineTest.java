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

package ch.grengine.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import groovy.lang.Script;

import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
import ch.grengine.code.CodeUtil;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.except.ClassNameConflictException;
import ch.grengine.except.LoadException;
import ch.grengine.load.DefaultTopCodeCacheFactory;
import ch.grengine.load.LoadMode;
import ch.grengine.load.TopCodeCacheFactory;
import ch.grengine.source.DefaultTextSource;
import ch.grengine.source.MockFile;
import ch.grengine.source.MockFileSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;


public class LayeredEngineTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructDefaults() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        LayeredEngine engine = builder.build();
        
        assertEquals(builder, engine.getBuilder());
        assertEquals(Thread.currentThread().getContextClassLoader(), engine.getBuilder().getParent());
        assertEquals(LoadMode.CURRENT_FIRST, engine.getBuilder().getLoadMode());
        assertTrue(engine.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.PARENT_FIRST, engine.getBuilder().getTopLoadMode());
        assertNotNull(engine.getBuilder().getTopCodeCacheFactory());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers());
        assertTrue(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers());
    }

    @Test
    public void testConstructAllDefined() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        builder.setParent(parent);
        builder.setLoadMode(LoadMode.PARENT_FIRST);
        builder.setWithTopCodeCache(false);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);
        TopCodeCacheFactory topCodeCacheFactory = new DefaultTopCodeCacheFactory();
        builder.setTopCodeCacheFactory(topCodeCacheFactory);
        builder.setAllowSameClassNamesInMultipleCodeLayers(false);
        builder.setAllowSameClassNamesInParentAndCodeLayers(false);
        
        LayeredEngine engine = builder.build();
        
        assertEquals(builder, engine.getBuilder());
        assertEquals(parent, engine.getBuilder().getParent());
        assertEquals(LoadMode.PARENT_FIRST, engine.getBuilder().getLoadMode());
        assertFalse(engine.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.CURRENT_FIRST, engine.getBuilder().getTopLoadMode());
        assertEquals(topCodeCacheFactory, engine.getBuilder().getTopCodeCacheFactory());
        assertFalse(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers());
        assertFalse(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers());
    }

    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.build();
        try {
            builder.setLoadMode(LoadMode.CURRENT_FIRST);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }
    
    private MockFile f1;
    private Source s1;
    private MockFile f2;
    private Source s2;
    private MockFile f3;
    private Source s3;
    private Source s4;
    private List<Sources> sourcesLayers;
    private List<Code> codeLayers;
    
    private void prepareCode(int offs) throws Exception {
        f1 = new MockFile(tempFolder.getRoot(), "Script1.groovy");
        s1 = new MockFileSource(f1);
        f2 = new MockFile(tempFolder.getRoot(), "Script2.groovy");
        s2 = new MockFileSource(f2);
        f3 = new MockFile(tempFolder.getRoot(), "Script3.groovy");
        s3 = new MockFileSource(f3);
        s4 = new DefaultTextSource(
                "package org.junit\npublic class Assume extends Script { public def run() { return 400 } }");
        
        Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s3, s4);
        Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "test");
        Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s1, s2, s3);
        Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "test");
        sourcesLayers = SourcesUtil.sourcesArrayToList(sources1, sources2);
        
        TestUtil.setFileText(f3, "public class Script3 extends Script { public def run() { return 300 } }");
        Code code1 = new DefaultGroovyCompiler().compile(sources1);
        
        TestUtil.setFileText(f1, "return " + offs + "+100");
        TestUtil.setFileText(f2, "public class Script2 extends Script { public def run() { return Sub.get() } }\n" +
                "public class Sub { static int x=" + offs + "+200; static def get() { return x++ } }");
        TestUtil.setFileText(f3, "public class Script3 extends Script { public def run() { return 333 } }");
        f1.setLastModified(offs);
        f2.setLastModified(offs);
        Code code2 = new DefaultGroovyCompiler().compile(sources2);
        
        codeLayers = CodeUtil.codeArrayToList(code1, code2);

    }
    
    @Test
    public void testFromCodeLayersNoTopCodeCache() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        
        LayeredEngine engine = builder.build();
        
        prepareCode(1000);
        engine.setCodeLayers(codeLayers);
        
        Loader loader = engine.getLoader();
        Loader attachedLoader2 = engine.newAttachedLoader();
        Loader detachedLoader = engine.newDetachedLoader();
        
        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.newInstance();
        Script script12 = (Script)clazz12.newInstance();
        Script script1D = (Script)clazz1D.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertEquals(1200, script21.run());
        assertEquals(1201, script21.run());
        assertEquals(1202, script21.run());
        assertEquals(1200, script22.run());
        assertEquals(1201, script22.run());
        assertEquals(1202, script22.run());
        
        // layers current first, so top version counts
        Class<?> clazz31 = engine.loadMainClass(loader, s3);
        Class<?> clazz32 = engine.loadMainClass(attachedLoader2, s3);
        Script script31 = (Script)clazz31.newInstance();
        Script script32 = (Script)clazz32.newInstance();
        assertEquals(333, script31.run());
        assertEquals(333, script31.run());
        assertEquals(333, script32.run());
        assertEquals(333, script32.run());
        
        Class<?> clazz4 = engine.loadMainClass(loader, s4);
        Script script4 = (Script)clazz4.newInstance();
        assertEquals(400, script4.run());
        
        // new loaders for s3+s4 because classes already loaded
        Loader attachedLoader3 = engine.newAttachedLoader();
        Loader attachedLoader4 = engine.newAttachedLoader();
        
        Class<?> classSub21 = engine.loadClass(attachedLoader3, s2, "Sub");
        Class<?> classSub22 = engine.loadClass(attachedLoader4, s2, "Sub");
        assertTrue(classSub21 != classSub22);
        clazz31.newInstance();
        clazz32.newInstance();
        
        // current layer first, so layer version counts
        Class<?> clazz4direct = engine.loadClass(attachedLoader3, "org.junit.Assume");
        Script script4direct = (Script)clazz4direct.newInstance();
        assertEquals(400, script4direct.run());

        prepareCode(2000);
        
        // no change even though file modification date has changed
        // (could not load differently from static because already loaded, but from top code cache)
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertEquals(2100, script11.run());
        assertEquals(2100, script11.run());
        assertEquals(2100, script12.run());
        assertEquals(2100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertEquals(2200, script21.run());
        assertEquals(2201, script21.run());
        assertEquals(2202, script21.run());
        assertEquals(2200, script22.run());
        assertEquals(2201, script22.run());
        assertEquals(2202, script22.run());
        
        // extra: try to load class that does not exist
        try {
            engine.loadClass(loader, "DoesNotExist235134");
            fail();
        } catch (LoadException e) {
            assertEquals("Could not load class 'DoesNotExist235134'. " +
                    "Cause: java.lang.ClassNotFoundException: DoesNotExist235134", e.getMessage());
        }
    }
    
    @Test
    public void testFromCodeLayersNoTopCodeCacheParentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        builder.setLoadMode(LoadMode.PARENT_FIRST);
        
        LayeredEngine engine = builder.build();
        
        prepareCode(1000);
        engine.setCodeLayers(codeLayers);
        
        Loader loader = engine.getLoader();
        Loader attachedLoader2 = engine.newAttachedLoader();
                
        // layers parent first, so lower version counts
        Class<?> clazz31 = engine.loadMainClass(loader, s3);
        Class<?> clazz32 = engine.loadMainClass(attachedLoader2, s3);
        Script script31 = (Script)clazz31.newInstance();
        Script script32 = (Script)clazz32.newInstance();
        assertEquals(300, script31.run());
        assertEquals(300, script31.run());
        assertEquals(300, script32.run());
        assertEquals(300, script32.run());
        
        // class exists in parent, but not source based, so uses one in layer
        Class<?> clazz4 = engine.loadMainClass(loader, s4);
        Script script4 = (Script)clazz4.newInstance();
        assertEquals(400, script4.run());
       
        
        // new loaders for s3+s4 because classes already loaded
        Loader attachedLoader3 = engine.newAttachedLoader();
        Loader attachedLoader4 = engine.newAttachedLoader();
        
        Class<?> classSub21 = engine.loadClass(attachedLoader3, s2, "Sub");
        Class<?> classSub22 = engine.loadClass(attachedLoader4, s2, "Sub");
        assertTrue(classSub21 != classSub22);
        clazz31.newInstance();
        clazz32.newInstance();
        
        // current layer first, so layer version counts
        Class<?> clazz4direct = engine.loadClass(attachedLoader3, "org.junit.Assume");
        try {
            Script script4direct = (Script)clazz4direct.newInstance();
            fail();
            script4direct.run();
        } catch (ClassCastException e) {
            // expected
        }
    }
    
    @Test
    public void testFromCodeLayersNoTopCodeCacheClassNameConflictChecks() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        builder.setAllowSameClassNamesInMultipleCodeLayers(false);
        builder.setAllowSameClassNamesInParentAndCodeLayers(true);
        
        LayeredEngine engine = builder.build();
        
        prepareCode(1000);
        
        try {
            engine.setCodeLayers(codeLayers);
            fail();
        } catch (ClassNameConflictException e) {
            assertEquals("Found 1 class name conflict(s). Duplicate classes in code layers: [Script3], " +
                    "classes in code layers and parent: (not checked)", e.getMessage());
        }
        
        builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        builder.setAllowSameClassNamesInMultipleCodeLayers(true);
        builder.setAllowSameClassNamesInParentAndCodeLayers(false);
        
        engine = builder.build();
        
        try {
            engine.setCodeLayers(codeLayers);
            fail();
        } catch (ClassNameConflictException e) {
            assertEquals("Found 1 class name conflict(s). Duplicate classes in code layers: (not checked), " +
                    "classes in code layers and parent: [org.junit.Assume]", e.getMessage());
        }
        
        builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        builder.setAllowSameClassNamesInMultipleCodeLayers(false);
        builder.setAllowSameClassNamesInParentAndCodeLayers(false);
        
        engine = builder.build();
        
        try {
            engine.setCodeLayers(codeLayers);
            fail();
        } catch (ClassNameConflictException e) {
            assertEquals("Found 2 class name conflict(s). Duplicate classes in code layers: [Script3], " +
                    "classes in code layers and parent: [org.junit.Assume]", e.getMessage());
        }
        
    }

    
    @Test
    public void testFromSourcesLayersNoTopCodeCache() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        
        LayeredEngine engine = builder.build();
        
        prepareCode(1000);
        engine.setCodeLayersBySource(sourcesLayers);
        
        Loader loader = engine.getLoader();
        Loader attachedLoader2 = engine.newAttachedLoader();
        Loader detachedLoader = engine.newDetachedLoader();
        
        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.newInstance();
        Script script12 = (Script)clazz12.newInstance();
        Script script1D = (Script)clazz1D.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertEquals(1200, script21.run());
        assertEquals(1201, script21.run());
        assertEquals(1202, script21.run());
        assertEquals(1200, script22.run());
        assertEquals(1201, script22.run());
        assertEquals(1202, script22.run());
        
        prepareCode(2000);
        
        // no change even though file modification date has change
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayersBySource(sourcesLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertEquals(2100, script11.run());
        assertEquals(2100, script11.run());
        assertEquals(2100, script12.run());
        assertEquals(2100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertEquals(2200, script21.run());
        assertEquals(2201, script21.run());
        assertEquals(2202, script21.run());
        assertEquals(2200, script22.run());
        assertEquals(2201, script22.run());
        assertEquals(2202, script22.run());
    }
    
    @Test
    public void testFromCodeLayersTopCodeCacheParentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(true);
        
        LayeredEngine engine = builder.build();
        
        prepareCode(1000);
        engine.setCodeLayers(codeLayers);
        
        Loader loader = engine.getLoader();
        Loader attachedLoader2 = engine.newAttachedLoader();
        Loader detachedLoader = engine.newDetachedLoader();
        
        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.newInstance();
        Script script12 = (Script)clazz12.newInstance();
        Script script1D = (Script)clazz1D.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertEquals(1200, script21.run());
        assertEquals(1201, script21.run());
        assertEquals(1202, script21.run());
        assertEquals(1200, script22.run());
        assertEquals(1201, script22.run());
        assertEquals(1202, script22.run());
        
        prepareCode(2000);
        
        // no change even though file modification date has change
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertEquals(2100, script11.run());
        assertEquals(2100, script11.run());
        assertEquals(2100, script12.run());
        assertEquals(2100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertEquals(2200, script21.run());
        assertEquals(2201, script21.run());
        assertEquals(2202, script21.run());
        assertEquals(2200, script22.run());
        assertEquals(2201, script22.run());
        assertEquals(2202, script22.run());
    }

    @Test
    public void testFromCodeLayersTopCodeCacheCurrentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(true);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);
        
        LayeredEngine engine = builder.build();
        
        prepareCode(1000);
        engine.setCodeLayers(codeLayers);
        
        Loader loader = engine.getLoader();
        Loader attachedLoader2 = engine.newAttachedLoader();
        Loader detachedLoader = engine.newDetachedLoader();
        
        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.newInstance();
        Script script12 = (Script)clazz12.newInstance();
        Script script1D = (Script)clazz1D.newInstance();
        assertEquals(1100, script11.run());
        assertEquals(1100, script11.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script12.run());
        assertEquals(1100, script1D.run());
        assertEquals(1100, script1D.run());
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertEquals(1200, script21.run());
        assertEquals(1201, script21.run());
        assertEquals(1202, script21.run());
        assertEquals(1200, script22.run());
        assertEquals(1201, script22.run());
        assertEquals(1202, script22.run());
        
        prepareCode(2000);
        
        // file modification date has changed,
        // change must become available immediately via top code cache
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertEquals(2100, script11.run());
        assertEquals(2100, script11.run());
        assertEquals(2100, script12.run());
        assertEquals(2100, script12.run());
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertEquals(2200, script21.run());
        assertEquals(2201, script21.run());
        assertEquals(2202, script21.run());
        assertEquals(2200, script22.run());
        assertEquals(2201, script22.run());
        assertEquals(2202, script22.run());

        // now replace code layers, changes must remain available (even if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertEquals(2100, script11.run());
        assertEquals(2100, script11.run());
        assertEquals(2100, script12.run());
        assertEquals(2100, script12.run());
        assertEquals(2100, script1D.run());
        assertEquals(2100, script1D.run());
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertEquals(2200, script21.run());
        assertEquals(2201, script21.run());
        assertEquals(2202, script21.run());
        assertEquals(2200, script22.run());
        assertEquals(2201, script22.run());
        assertEquals(2202, script22.run());
    }

    @Test
    public void testSetCodeLayersNull() throws Exception {
        try {
            new LayeredEngine.Builder().build().setCodeLayers(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Code layers are null.", e.getMessage());
        }
    }

    @Test
    public void testSetCodeLayersBySourceNull() throws Exception {
        try {
            new LayeredEngine.Builder().build().setCodeLayersBySource(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Sources layers are null.", e.getMessage());
        }
    }

}
