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

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
import ch.grengine.code.CodeUtil;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.except.CompileException;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class LayeredClassLoaderTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromCodeLayersDefaults() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();

        assertThat(loader.getBuilder(), is(builder));
        assertThat(loader.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(loader.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getCodeLayers().isEmpty(), is(true));
        assertThat(loader.getTopCodeCache(), is(nullValue()));

        assertThat(loader.getBuilder().getParent(), is(loader.getParent()));
        assertThat(loader.getBuilder().getLoadMode(), is(loader.getLoadMode()));
        assertThat(loader.getBuilder().getSourcesLayers().isEmpty(), is(true));
        assertThat(loader.getBuilder().getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(loader.getBuilder().isWithTopCodeCache(), is(false));
        assertThat(loader.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(loader.getBuilder().getTopCodeCache(), is(nullValue()));
        
        // extra: constructor with explicitly from code layers
        loader = new LayeredClassLoader(builder, false);

        assertThat(loader.getBuilder(), is(builder));
        assertThat(loader.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(loader.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getCodeLayers().isEmpty(), is(true));
        assertThat(loader.getTopCodeCache(), is(nullValue()));
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

        assertThat(loader.getBuilder(), is(builder));
        assertThat(loader.getParent(), is(parent));
        assertThat(loader.getLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(loader.getCodeLayers(), is(codeLayers));
        assertThat(loader.getTopCodeCache(), is(topCodeCache));

        assertThat(loader.getBuilder().getParent(), is(loader.getParent()));
        assertThat(loader.getBuilder().getLoadMode(), is(loader.getLoadMode()));
        assertThat(loader.getBuilder().getSourcesLayers().isEmpty(), is(true));
        assertThat(loader.getBuilder().getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(loader.getBuilder().isWithTopCodeCache(), is(true));
        assertThat(loader.getBuilder().getTopLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getBuilder().getTopCodeCache(), is(loader.getTopCodeCache()));
    }

    @Test
    public void testConstructFromSourcesLayersDefaults() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromSourcesLayers();

        assertThat(loader.getBuilder(), is(builder));
        assertThat(loader.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(loader.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getCodeLayers().isEmpty(), is(true));
        assertThat(loader.getTopCodeCache(), is(nullValue()));

        assertThat(loader.getBuilder().getParent(), is(loader.getParent()));
        assertThat(loader.getBuilder().getLoadMode(), is(loader.getLoadMode()));
        assertThat(loader.getBuilder().getSourcesLayers().isEmpty(), is(true));
        assertThat(loader.getBuilder().getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(loader.getBuilder().isWithTopCodeCache(), is(false));
        assertThat(loader.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(loader.getBuilder().getTopCodeCache(), is(nullValue()));
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

        assertThat(loader.getBuilder(), is(builder));
        assertThat(loader.getParent(), is(parent));
        assertThat(loader.getLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(loader.getCodeLayers().size(), is(sourcesLayers.size()));
        assertThat(loader.getTopCodeCache(), is(topCodeCache));

        assertThat(loader.getBuilder().getParent(), is(loader.getParent()));
        assertThat(loader.getBuilder().getLoadMode(), is(loader.getLoadMode()));
        assertThat(loader.getBuilder().getSourcesLayers(), is(sourcesLayers));
        assertThat(loader.getBuilder().getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(loader.getBuilder().isWithTopCodeCache(), is(true));
        assertThat(loader.getBuilder().getTopLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getBuilder().getTopCodeCache(), is(loader.getTopCodeCache()));
    }
    
    @Test
    public void testSetLayersWithVarargs() throws Exception {
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        List<Code> codeLayers = getTestCodeLayers(parent);
        List<Sources> sourcesLayers = getTestSourcesLayers();

        assertThat(codeLayers.size(), is(2));
        assertThat(sourcesLayers.size(), is(2));
        
        Code code1 = codeLayers.get(0);
        Code code2 = codeLayers.get(1);
        Sources sources1 = sourcesLayers.get(0);
        Sources sources2 = sourcesLayers.get(1);
        
        builder.setCodeLayers(code1, code2);
        List<Code> codeLayersRead = builder.getCodeLayers();
        assertThat(codeLayersRead.size(), is(2));
        assertThat(codeLayersRead.get(0), is(code1));
        assertThat(codeLayersRead.get(1), is(code2));
        
        builder.setSourcesLayers(sources1, sources2);
        List<Sources> sourcesLayersRead = builder.getSourcesLayers();
        assertThat(sourcesLayersRead.size(), is(2));
        assertThat(sourcesLayersRead.get(0), is(sources1));
        assertThat(sourcesLayersRead.get(1), is(sources2));
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.buildFromCodeLayers();
        try {
            builder.setLoadMode(LoadMode.CURRENT_FIRST);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }
    
    
    @Test
    public void testClone_NoTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.clone();

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(nullValue()));
    }
    
    @Test
    public void testClone_WithTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.clone();

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(topCodeCache));
    }
    
    @Test
    public void testCloneWithSeparateTopCodeCache_NoTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(nullValue()));
    }
    
    @Test
    public void testCloneWithSeparateTopCodeCache_WithTopCodeCache() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(topCodeCache, not(sameInstance(clone.getTopCodeCache())));
        assertThat(clone.getTopCodeCache(), is(notNullValue()));
        assertThat(clone.getTopCodeCache(), instanceOf(DefaultTopCodeCache.class));
    }

    @Test
    public void testReleaseClasses() throws Exception {
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);

        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        List<Sources> sourcesList = SourcesUtil.sourcesArrayToList(sources);
        builder.setSourcesLayers(sourcesList);

        LayeredClassLoader loader = builder.buildFromSourcesLayers();

        Class<?> clazz1 = loader.loadClass("Class1");
        Class<?> clazz2 = loader.loadClass("Class2");
        clazz2.newInstance();

        Source s4 = f.fromText("class Class4 {}");
        Class<?> clazz4 = loader.loadMainClass(s4);

        Source s5 = f.fromText("class Class4 { int get() { return 1 } }");
        Class<?> clazz5 = loader.loadMainClass(s5);

        RecordingClassReleaser releaser = new RecordingClassReleaser();
        loader.releaseClasses(releaser);

        assertThat(releaser.classes.contains(clazz1), is(true));
        assertThat(releaser.classes.contains(clazz2), is(true));
        assertThat(releaser.classes.contains(clazz4), is(true));
        assertThat(releaser.classes.contains(clazz5), is(true));
        assertThat(releaser.classes.size(), is(5));
        assertThat(releaser.countClassesWithName("Class1"), is(1));
        assertThat(releaser.countClassesWithName("Class2"), is(1));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(1));
        assertThat(releaser.countClassesWithName("Class4"), is(2));
    }
    
    
    private static List<Sources> getTestSourcesLayers() {
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
    
    private static List<Code> getTestCodeLayers(ClassLoader parent) throws CompileException {
        List<Sources> sourcesLayers = getTestSourcesLayers();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);
        Code code1 = c.compile(sourcesLayers.get(0));
        Code code2 = c.compile(sourcesLayers.get(1));
        return CodeUtil.codeArrayToList(code1, code2);
    }


    private MockFile fMain;
    private Source sMain;
    private MockFile fAssume;
    private Source sAssume;
    private Source sNotExists;
    private Code codeParent;
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
        Code codeLayer0 = new DefaultGroovyCompiler().compile(sources);
        
        // code layer 1
        TestUtil.setFileText(fMain, "class Main { def methodLayer1() {} }\nclass Side { def methodLayer1() {} }");
        TestUtil.setFileText(fAssume, "package org.junit\nclass Assume  { def methodLayer1() {} }");
        Code codeLayer1 = new DefaultGroovyCompiler().compile(sources);

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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."), is(true));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."), is(true));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."), is(true));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        try {
            loader.loadClass(sMain, "org.junit.Assume");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage().startsWith("Class 'org.junit.Assume' not found for source."), is(true));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadMainClass(sNotExists); } catch (LoadException e) {}

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        try {
            loader.loadClass(sMain, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + sMain.toString()));
        }
        clazz = loader.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        try { loader.loadClass(sNotExists, "NotExists"); } catch (LoadException e) {}
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
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
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotExists);
        assertThat(loaderFound, is(nullValue()));
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
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        fMain.setLastModified(55555);
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        fMain.setLastModified(77777);

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
    }


}
