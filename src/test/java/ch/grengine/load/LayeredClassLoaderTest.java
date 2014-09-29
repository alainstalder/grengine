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

package ch.grengine.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
import ch.grengine.code.CodeUtil;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.except.LoadException;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.DefaultTextSource;
import ch.grengine.source.MockFile;
import ch.grengine.source.MockFileSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;


public class LayeredClassLoaderTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromCodeLayersDefaults() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        assertEquals(builder, loader.getBuilder());
        assertEquals(Thread.currentThread().getContextClassLoader(), loader.getParent());
        assertEquals(LoadMode.CURRENT_FIRST, loader.getLoadMode());
        assertTrue(loader.getCodeLayers().isEmpty());
        assertNull(loader.getTopCodeCache());
        
        assertEquals(loader.getParent(), loader.getBuilder().getParent());
        assertEquals(loader.getLoadMode(), loader.getBuilder().getLoadMode());
        assertTrue(loader.getBuilder().getSourcesLayers().isEmpty());
        assertEquals(loader.getCodeLayers(), loader.getBuilder().getCodeLayers());
        assertFalse(loader.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.PARENT_FIRST, loader.getBuilder().getTopLoadMode());
        assertNull(loader.getBuilder().getTopCodeCache());
        
        // extra: constructor with explicitly from code layers
        loader = new LayeredClassLoader(builder, false);
        
        assertEquals(builder, loader.getBuilder());
        assertEquals(Thread.currentThread().getContextClassLoader(), loader.getParent());
        assertEquals(LoadMode.CURRENT_FIRST, loader.getLoadMode());
        assertTrue(loader.getCodeLayers().isEmpty());
        assertNull(loader.getTopCodeCache());
    }

    @Test
    public void testConstructFromCodeLayersAllSet() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        builder.setParent(parent);
        builder.setLoadMode(LoadMode.PARENT_FIRST);
        List<Code> codeLayers = getTestCodeLayers(parent);
        builder.setCodeLayers(codeLayers);
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        assertEquals(builder, loader.getBuilder());
        assertEquals(parent, loader.getParent());
        assertEquals(LoadMode.PARENT_FIRST, loader.getLoadMode());
        assertEquals(codeLayers, loader.getCodeLayers());
        assertEquals(topCodeCache, loader.getTopCodeCache());
        
        assertEquals(loader.getParent(), loader.getBuilder().getParent());
        assertEquals(loader.getLoadMode(), loader.getBuilder().getLoadMode());
        assertTrue(loader.getBuilder().getSourcesLayers().isEmpty());
        assertEquals(loader.getCodeLayers(), loader.getBuilder().getCodeLayers());
        assertTrue(loader.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.CURRENT_FIRST, loader.getBuilder().getTopLoadMode());
        assertEquals(loader.getTopCodeCache(), loader.getBuilder().getTopCodeCache());
    }

    @Test
    public void testConstructFromSourcesLayersDefaults() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromSourcesLayers();
        
        assertEquals(builder, loader.getBuilder());
        assertEquals(Thread.currentThread().getContextClassLoader(), loader.getParent());
        assertEquals(LoadMode.CURRENT_FIRST, loader.getLoadMode());
        assertTrue(loader.getCodeLayers().isEmpty());
        assertNull(loader.getTopCodeCache());
        
        assertEquals(loader.getParent(), loader.getBuilder().getParent());
        assertEquals(loader.getLoadMode(), loader.getBuilder().getLoadMode());
        assertTrue(loader.getBuilder().getSourcesLayers().isEmpty());
        assertEquals(loader.getCodeLayers(), loader.getBuilder().getCodeLayers());
        assertFalse(loader.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.PARENT_FIRST, loader.getBuilder().getTopLoadMode());
        assertNull(loader.getBuilder().getTopCodeCache());
    }

    @Test
    public void testConstructFromSourcesLayersAllSet() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        builder.setParent(parent);
        builder.setLoadMode(LoadMode.PARENT_FIRST);
        List<Sources> sourcesLayers = getTestSourcesLayers();
        builder.setSourcesLayers(sourcesLayers);
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);
        
        LayeredClassLoader loader = builder.buildFromSourcesLayers();
        
        assertEquals(builder, loader.getBuilder());
        assertEquals(parent, loader.getParent());
        assertEquals(LoadMode.PARENT_FIRST, loader.getLoadMode());
        assertTrue(loader.getCodeLayers().size() == sourcesLayers.size());
        assertEquals(topCodeCache, loader.getTopCodeCache());
        
        assertEquals(loader.getParent(), loader.getBuilder().getParent());
        assertEquals(loader.getLoadMode(), loader.getBuilder().getLoadMode());
        assertEquals(sourcesLayers, loader.getBuilder().getSourcesLayers());
        assertEquals(loader.getCodeLayers(), loader.getBuilder().getCodeLayers());
        assertTrue(loader.getBuilder().isWithTopCodeCache());
        assertEquals(LoadMode.CURRENT_FIRST, loader.getBuilder().getTopLoadMode());
        assertEquals(loader.getTopCodeCache(), loader.getBuilder().getTopCodeCache());
    }
    
    @Test
    public void testSetLayersWithVarargs() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        List<Code> codeLayers = getTestCodeLayers(parent);
        List<Sources> sourcesLayers = getTestSourcesLayers();
        
        assertEquals(2, codeLayers.size());
        assertEquals(2, sourcesLayers.size());
        
        Code code1 = codeLayers.get(0);
        Code code2 = codeLayers.get(1);
        Sources sources1 = sourcesLayers.get(0);
        Sources sources2 = sourcesLayers.get(1);
        
        builder.setCodeLayers(code1, code2);
        List<Code> codeLayersRead = builder.getCodeLayers();
        assertEquals(2, codeLayersRead.size());
        assertEquals(code1, codeLayersRead.get(0));
        assertEquals(code2, codeLayersRead.get(1));
        
        builder.setSourcesLayers(sources1, sources2);
        List<Sources> sourcesLayersRead = builder.getSourcesLayers();
        assertEquals(2, sourcesLayersRead.size());
        assertEquals(sources1, sourcesLayersRead.get(0));
        assertEquals(sources2, sourcesLayersRead.get(1));
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.buildFromCodeLayers();
        try {
            builder.setLoadMode(LoadMode.CURRENT_FIRST);
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }
    
    
    @Test
    public void testClone_NoTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.clone();
        
        assertEquals(loader.getBuilder(), clone.getBuilder());
        assertEquals(loader.getCodeLayers(), clone.getCodeLayers());
        assertEquals(loader.getLoadMode(), clone.getLoadMode());
        assertNull(clone.getTopCodeCache());
    }
    
    @Test
    public void testClone_WithTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.clone();
        
        assertEquals(loader.getBuilder(), clone.getBuilder());
        assertEquals(loader.getCodeLayers(), clone.getCodeLayers());
        assertEquals(loader.getLoadMode(), clone.getLoadMode());
        assertEquals(topCodeCache, clone.getTopCodeCache());
    }
    
    @Test
    public void testCloneWithSeparateTopCodeCache_NoTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();
        
        assertEquals(loader.getBuilder(), clone.getBuilder());
        assertEquals(loader.getCodeLayers(), clone.getCodeLayers());
        assertEquals(loader.getLoadMode(), clone.getLoadMode());
        assertNull(clone.getTopCodeCache());
    }
    
    @Test
    public void testCloneWithSeparateTopCodeCache_WithTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();
        
        assertEquals(loader.getBuilder(), clone.getBuilder());
        assertEquals(loader.getCodeLayers(), clone.getCodeLayers());
        assertEquals(loader.getLoadMode(), clone.getLoadMode());
        assertTrue(topCodeCache != clone.getTopCodeCache());
        assertNotNull(clone.getTopCodeCache());
        assertTrue(clone.getTopCodeCache() instanceof DefaultTopCodeCache);
    }
    
    
    private static List<Sources> getTestSourcesLayers() throws Exception {
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("public class Twice { public def get() { return Inner1.get() }\n" +
                "public class Inner1 { static def get() { return 1 } } }");
        Source s2 = f.fromText("public class Twice { public def get() { return Inner2.get() }\n" +
                "public class Inner2 { static def get() { return 2 } } }");
        Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s1);
        Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s2);
        Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "sources1");
        Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "sources2");
        return SourcesUtil.sourcesArrayToList(sources1, sources2);
    }
    
    private static List<Code> getTestCodeLayers(ClassLoader parent) throws Exception {
        List<Sources> sourcesLayers = getTestSourcesLayers();
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);
        Code code1 = c.compile(sourcesLayers.get(0));
        Code code2 = c.compile(sourcesLayers.get(1));
        
        List<Code> codeLayers = CodeUtil.codeArrayToList(code1, code2);
        return codeLayers;
    }


    private MockFile fMain;
    private Source sMain;
    private MockFile fAssume;
    private Source sAssume;
    private Source sNotExists;
    private Code codeParent;
    private Code codeLayer0;
    private Code codeLayer1;
    private List<Code> codeLayers;
    
    private void prepareCode(boolean setLastModifiedAtEnd) throws Exception {
        fMain = new MockFile(tempFolder.getRoot(), "Main.groovy");
        sMain = new MockFileSource(fMain);
        fAssume = new MockFile(tempFolder.getRoot(), "Assume.groovy");
        sAssume = new MockFileSource(fAssume);
        sNotExists = new DefaultTextSource("class NotExists {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(sMain, sAssume);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        
        // code parent class loader if source class loader
        TestUtil.setFileText(fMain, "class Main { def methodParent() {} }\nclass Side { def methodParent() {} }");
        TestUtil.setFileText(fAssume, "package org.junit\nclass Assume  { def methodParent() {} }");
        codeParent = new DefaultGroovyCompiler().compile(sources);
        
        // code layer 0
        TestUtil.setFileText(fMain, "class Main { def methodLayer0() {} }\nclass Side { def methodLayer0() {} }");
        TestUtil.setFileText(fAssume, "package org.junit\nclass Assume  { def methodLayer0() {} }");
        codeLayer0 = new DefaultGroovyCompiler().compile(sources);
        
        // code layer 1
        TestUtil.setFileText(fMain, "class Main { def methodLayer1() {} }\nclass Side { def methodLayer1() {} }");
        TestUtil.setFileText(fAssume, "package org.junit\nclass Assume  { def methodLayer1() {} }");
        codeLayer1 = new DefaultGroovyCompiler().compile(sources);

        codeLayers = new LinkedList<Code>();
        codeLayers.add(codeLayer0);
        codeLayers.add(codeLayer1);
        
        // prepare files for top code cache
        TestUtil.setFileText(fMain, "class Main { def methodTop() {} }\nclass Side { def methodTop() {} }");
        TestUtil.setFileText(fAssume, "package org.junit\nclass Assume  { def methodTop() {} }");
        if (setLastModifiedAtEnd) {
            fMain.setLastModified(100);
            fAssume.setLastModified(100);
        }
    }
    
    
    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopCodeCacheOff_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
        
        // extra: load class with resolve (protected method)
        loader.loadClass("Main", true);

    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopOff_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopOff_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertTrue(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopOff_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopOff_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopOff_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopOff_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
     clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer0", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound == parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
 
        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        prepareCode(false);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNotNull(loaderFound);
        assertTrue(loaderFound != parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadMainClass(sAssume);
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass(sMain, "Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString(), e.getMessage());
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);

        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1", new Class<?>[0]);
        
        try { loader.loadClass("NotExists"); } catch (ClassNotFoundException e) {}
    }
    
    @Test
    public void testExtraNoCodeLayers_ParentNotSourceClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {
        prepareCode(true);
        codeLayers = new LinkedList<Code>();
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = false;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertNull(loaderFound);
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertNull(loaderFound);
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertNull(loaderFound);
    }
    
    @Test
    public void testExtraSourcesChangedTop_ParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        prepareCode(true);
        
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                LoadMode.CURRENT_FIRST, codeParent);
        LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        boolean isWithTopLoadMode = true;
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        fMain.setLastModified(55555);
        
        clazz = loader.loadMainClass(sMain);
        assertEquals("Main", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
        
        fMain.setLastModified(77777);

        clazz = loader.loadClass(sMain, "Side");
        assertEquals("Side", clazz.getName());
        clazz.getDeclaredMethod("methodTop", new Class<?>[0]);
    }


}
