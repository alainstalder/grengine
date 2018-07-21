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

package ch.grengine.load;

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static ch.grengine.TestUtil.assertThrows;
import static ch.grengine.TestUtil.assertThrowsStartsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;


public class LayeredClassLoaderTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructFromCodeLayersDefaults() {

        // given
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();

        // when

        LayeredClassLoader loader = builder.buildFromCodeLayers();

        // then

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

        // when (extra: constructor with explicitly from code layers)

        loader = new LayeredClassLoader(builder, false);

        // then

        assertThat(loader.getBuilder(), is(builder));
        assertThat(loader.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(loader.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getCodeLayers().isEmpty(), is(true));
        assertThat(loader.getTopCodeCache(), is(nullValue()));
    }

    @Test
    public void testConstructFromCodeLayersAllSet() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        builder.setParent(parent);
        builder.setLoadMode(LoadMode.PARENT_FIRST);
        List<Code> codeLayers = getTestCodeLayers(parent);
        builder.setCodeLayers(codeLayers);
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);

        // when

        LayeredClassLoader loader = builder.buildFromCodeLayers();

        // then

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
    public void testConstructFromSourcesLayersDefaults() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();

        // when

        LayeredClassLoader loader = builder.buildFromSourcesLayers();

        // then

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
    public void testConstructFromSourcesLayersAllSet() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        builder.setParent(parent);
        builder.setLoadMode(LoadMode.PARENT_FIRST);
        List<Sources> sourcesLayers = getTestSourcesLayers();
        builder.setSourcesLayers(sourcesLayers);
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);

        // when

        LayeredClassLoader loader = builder.buildFromSourcesLayers();

        // then

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
    public void testSetLayersWithVarargs() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();

        // when

        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        List<Code> codeLayers = getTestCodeLayers(parent);
        List<Sources> sourcesLayers = getTestSourcesLayers();

        // then

        assertThat(codeLayers.size(), is(2));
        assertThat(sourcesLayers.size(), is(2));

        // when

        Code code1 = codeLayers.get(0);
        Code code2 = codeLayers.get(1);
        Sources sources1 = sourcesLayers.get(0);
        Sources sources2 = sourcesLayers.get(1);
        
        builder.setCodeLayers(code1, code2);
        List<Code> codeLayersRead = builder.getCodeLayers();

        // then

        assertThat(codeLayersRead.size(), is(2));
        assertThat(codeLayersRead.get(0), is(code1));
        assertThat(codeLayersRead.get(1), is(code2));

        // when

        builder.setSourcesLayers(sources1, sources2);
        List<Sources> sourcesLayersRead = builder.getSourcesLayers();

        // then

        assertThat(sourcesLayersRead.size(), is(2));
        assertThat(sourcesLayersRead.get(0), is(sources1));
        assertThat(sourcesLayersRead.get(1), is(sources2));
    }
    
    @Test
    public void testModifyBuilderAfterUse() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.buildFromCodeLayers();

        // when/then

        assertThrows(() -> builder.setLoadMode(LoadMode.CURRENT_FIRST),
            IllegalStateException.class,
            "Builder already used.");
    }
    
    
    @Test
    public void testClone_NoTopCodeCache() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();

        // when

        LayeredClassLoader clone = loader.clone();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(nullValue()));
    }
    
    @Test
    public void testClone_WithTopCodeCache() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        LayeredClassLoader loader = builder.buildFromCodeLayers();

        // when

        LayeredClassLoader clone = loader.clone();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(topCodeCache));
    }
    
    @Test
    public void testCloneWithSeparateTopCodeCache_NoTopCodeCache() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        LayeredClassLoader loader = builder.buildFromCodeLayers();

        // when

        LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(nullValue()));
    }
    
    @Test
    public void testCloneWithSeparateTopCodeCache_WithTopCodeCache() {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);
        LayeredClassLoader loader = builder.buildFromCodeLayers();

        // when

        LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(topCodeCache, not(sameInstance(clone.getTopCodeCache())));
        assertThat(clone.getTopCodeCache(), is(notNullValue()));
        assertThat(clone.getTopCodeCache(), instanceOf(DefaultTopCodeCache.class));
    }

    @Test
    public void testReleaseClasses() throws Exception {

        // given

        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        builder.setWithTopCodeCache(true, topCodeCache);

        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        List<Sources> sourcesList = Collections.singletonList(sources);
        builder.setSourcesLayers(sourcesList);

        LayeredClassLoader loader = builder.buildFromSourcesLayers();

        Class<?> clazz1 = loader.loadClass("Class1");
        Class<?> clazz2 = loader.loadClass("Class2");
        clazz2.getConstructor().newInstance();

        Source s4 = f.fromText("class Class4 {}");
        Class<?> clazz4 = loader.loadMainClass(s4);

        Source s5 = f.fromText("class Class4 { int get() { return 1 } }");
        Class<?> clazz5 = loader.loadMainClass(s5);

        RecordingClassReleaser releaser = new RecordingClassReleaser();

        // when

        loader.releaseClasses(releaser);

        // then

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
        return Arrays.asList(sources1, sources2);
    }
    
    private static List<Code> getTestCodeLayers(ClassLoader parent) {
        List<Sources> sourcesLayers = getTestSourcesLayers();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);
        Code code1 = c.compile(sourcesLayers.get(0));
        Code code2 = c.compile(sourcesLayers.get(1));
        return Arrays.asList(code1, code2);
    }


    private MockFile fMain;
    private Source sMain;
    private Source sAssume;
    private Source sNotInCodeLayers;
    private Code codeParent;
    private List<Code> codeLayers;
    
    private void prepareCode(boolean setLastModifiedAtEnd) throws Exception {
        fMain = new MockFile(tempFolder.getRoot(), "Main.groovy");
        sMain = new MockFileSource(fMain);
        MockFile fAssume = new MockFile(tempFolder.getRoot(), "Assume.groovy");
        sAssume = new MockFileSource(fAssume);
        sNotInCodeLayers = new DefaultTextSource("class NotInCodeLayers {}");
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

        codeLayers = new LinkedList<>();
        codeLayers.add(codeLayer0);
        codeLayers.add(codeLayer1);
        
        // prepare files for top code cache
        TestUtil.setFileText(fMain, "class Main { def methodTop() {} }\nclass Side { def methodTop() {} }");
        TestUtil.setFileText(fAssume, "package org.junit\nclass Assume  { def methodTop() {} }");
        if (setLastModifiedAtEnd) {
            assertThat(fMain.setLastModified(100), is(true));
            assertThat(fAssume.setLastModified(100), is(true));
        }
    }
    
    
    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopCodeCacheOff_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");

        // extra: load class with resolve (protected method)
        loader3.loadClass("Main", true);
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        
        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopOff_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopOff_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        
        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());

        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);

        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        
        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        assertThrowsStartsWith(() -> loader3.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader4 = builder.buildFromCodeLayers();
        
        clazz = loader4.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader4.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader4.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);

        assertThrowsStartsWith(() -> loader4.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        assertThrowsStartsWith(() -> loader3.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader4 = builder.buildFromCodeLayers();
        
        clazz = loader4.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader4.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader4.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");

        assertThrowsStartsWith(() -> loader4.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        assertThrowsStartsWith(() -> loader3.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader4 = builder.buildFromCodeLayers();
        
        clazz = loader4.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader4.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader4.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader4.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
        
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
          
        // not found by sMain with top code cache
        assertThrowsStartsWith(() -> loader3.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader4 = builder.buildFromCodeLayers();
        
        clazz = loader4.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader4.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader4.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader4.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopOff_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");
        
        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
        
        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopOff_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopOff_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopOff_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        assertThrowsStartsWith(() -> loader1.loadMainClass(sNotInCodeLayers),
                LoadException.class,
                "Source not found: ");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");
        
        assertThrowsStartsWith(() -> loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"),
                LoadException.class,
                "Source not found: ");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");
        
        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.PARENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer0");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer0");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer0");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, sameInstance(parent));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodParent");
        
        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodParent");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodParent");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodParent");
        
        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentNotSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
 
        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }

    @Test
    public void testParentSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {

        // given

        prepareCode(false);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.CURRENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader1 = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(notNullValue()));
        assertThat(loaderFound, not(sameInstance(parent)));
        loaderFound = loader1.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));

        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        topCodeCache.clear();
        clazz = loader1.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader1.loadMainClass(sAssume);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader1.loadMainClass(sNotInCodeLayers), notNullValue());

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader2 = builder.buildFromCodeLayers();
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader2.loadClass(sMain, "Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader2.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        // wrong source, not found
        assertThrowsStartsWith(() -> loader2.loadClass(sMain, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + sMain.toString());
        clazz = loader2.loadClass(sAssume, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        clazz.getDeclaredMethod("methodLayer1");

        assertThat(loader2.loadClass(sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        LayeredClassLoader loader3 = builder.buildFromCodeLayers();
        
        clazz = loader3.loadClass("Main");
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodLayer1");

        clazz = loader3.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodLayer1");
        
        clazz = loader3.loadClass("org.junit.Assume");
        clazz.getDeclaredMethod("methodLayer1");

        assertThrowsStartsWith(() -> loader3.loadClass("NotInCodeLayers"),
                ClassNotFoundException.class,
                "NotInCodeLayers");
    }
    
    @Test
    public void testExtraNoCodeLayers_ParentNotSourceClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {

        // given

        prepareCode(true);
        codeLayers = new LinkedList<>();

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = false;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = null;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(sMain);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(sAssume);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));
    }
    
    @Test
    public void testExtraSourcesChangedTop_ParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {

        // given

        prepareCode(true);

        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                LoadMode.CURRENT_FIRST, codeParent);
        final LoadMode layerLoadMode = LoadMode.PARENT_FIRST;
        final boolean isWithTopLoadMode = true;
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LoadMode topLoadMode = LoadMode.CURRENT_FIRST;
        
        LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.setParent(parent);
        builder.setLoadMode(layerLoadMode);
        builder.setCodeLayers(codeLayers);
        builder.setWithTopCodeCache(isWithTopLoadMode, topCodeCache);
        builder.setTopLoadMode(topLoadMode);
        
        LayeredClassLoader loader = builder.buildFromCodeLayers();
        
        // when/then (loadMainClass(source))
        
        Class<?> clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");
        
        assertThat(fMain.setLastModified(55555), is(true));
        
        clazz = loader.loadMainClass(sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = builder.buildFromCodeLayers();
        
        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(fMain.setLastModified(77777), is(true));

        clazz = loader.loadClass(sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
    }


}
