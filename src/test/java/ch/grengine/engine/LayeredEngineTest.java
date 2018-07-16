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

package ch.grengine.engine;

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.except.ClassNameConflictException;
import ch.grengine.except.LoadException;
import ch.grengine.load.ClassReleaser;
import ch.grengine.load.DefaultClassReleaser;
import ch.grengine.load.DefaultTopCodeCacheFactory;
import ch.grengine.load.LoadMode;
import ch.grengine.load.RecordingClassReleaser;
import ch.grengine.load.TopCodeCacheFactory;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.DefaultTextSource;
import ch.grengine.source.MockFile;
import ch.grengine.source.MockFileSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import groovy.lang.Script;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class LayeredEngineTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructDefaults() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        LayeredEngine engine = builder.build();

        assertThat(engine.getBuilder(), is(builder));
        assertThat(engine.getBuilder().getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(engine.getBuilder().getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(engine.getBuilder().isWithTopCodeCache(), is(true));
        assertThat(engine.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(engine.getBuilder().getTopCodeCacheFactory(), is(notNullValue()));
        assertThat(engine.getBuilder().getClassReleaser(), instanceOf(DefaultClassReleaser.class));
        assertThat(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers(), is(true));
        assertThat(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers(), is(true));
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
        ClassReleaser releaser = new RecordingClassReleaser();
        builder.setClassReleaser(releaser);
        builder.setAllowSameClassNamesInMultipleCodeLayers(false);
        builder.setAllowSameClassNamesInParentAndCodeLayers(false);
        
        LayeredEngine engine = builder.build();

        assertThat(engine.getBuilder(), is(builder));
        assertThat(engine.getBuilder().getParent(), is(parent));
        assertThat(engine.getBuilder().getLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(engine.getBuilder().isWithTopCodeCache(), is(false));
        assertThat(engine.getBuilder().getTopLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(engine.getBuilder().getTopCodeCacheFactory(), is(topCodeCacheFactory));
        assertThat(engine.getBuilder().getClassReleaser(), is(releaser));
        assertThat(engine.getBuilder().isAllowSameClassNamesInMultipleCodeLayers(), is(false));
        assertThat(engine.getBuilder().isAllowSameClassNamesInParentAndCodeLayers(), is(false));
    }

    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.build();
        try {
            builder.setLoadMode(LoadMode.CURRENT_FIRST);
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }

    @Test
    public void testClose() throws Exception {
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        RecordingClassReleaser releaser = new RecordingClassReleaser();
        builder.setClassReleaser(releaser);

        LayeredEngine engine = builder.build();

        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        List<Sources> sourcesList = Arrays.asList(sources);

        engine.setCodeLayersBySource(sourcesList);

        Loader loaderAttached = engine.newAttachedLoader();
        Loader loaderDetached = engine.newDetachedLoader();

        Class<?> clazz1a = engine.loadClass(loaderAttached, "Class1");
        Class<?> clazz2a = engine.loadClass(loaderAttached, "Class2");
        clazz2a.newInstance();
        Class<?> clazz1d = engine.loadClass(loaderDetached, "Class1");
        Class<?> clazz2d = engine.loadClass(loaderDetached, "Class2");
        clazz2d.newInstance();

        engine.close();

        assertThat(releaser.classes.contains(clazz1a), is(true));
        assertThat(releaser.classes.contains(clazz2a), is(true));
        assertThat(releaser.classes.contains(clazz1d), is(true));
        assertThat(releaser.classes.contains(clazz2d), is(true));
        assertThat(releaser.classes.size(), is(6));
        assertThat(releaser.countClassesWithName("Class1"), is(2));
        assertThat(releaser.countClassesWithName("Class2"), is(2));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(2));
    }

    private Source s1;
    private Source s2;
    private Source s3;
    private Source s4;
    private List<Sources> sourcesLayers;
    private List<Code> codeLayers;
    
    private void prepareCode(int offs) throws Exception {
        MockFile f1 = new MockFile(tempFolder.getRoot(), "Script1.groovy");
        s1 = new MockFileSource(f1);
        MockFile f2 = new MockFile(tempFolder.getRoot(), "Script2.groovy");
        s2 = new MockFileSource(f2);
        MockFile f3 = new MockFile(tempFolder.getRoot(), "Script3.groovy");
        s3 = new MockFileSource(f3);
        s4 = new DefaultTextSource(
                "package org.junit\npublic class Assume extends Script { public def run() { return 400 } }");
        
        Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s3, s4);
        Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "test");
        Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s1, s2, s3);
        Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "test");
        sourcesLayers = Arrays.asList(sources1, sources2);
        
        TestUtil.setFileText(f3, "public class Script3 extends Script { public def run() { return 300 } }");
        Code code1 = new DefaultGroovyCompiler().compile(sources1);
        
        TestUtil.setFileText(f1, "return " + offs + "+100");
        TestUtil.setFileText(f2, "public class Script2 extends Script { public def run() { return Sub.get() } }\n" +
                "public class Sub { static int x=" + offs + "+200; static def get() { return x++ } }");
        TestUtil.setFileText(f3, "public class Script3 extends Script { public def run() { return 333 } }");
        f1.setLastModified(offs);
        f2.setLastModified(offs);
        Code code2 = new DefaultGroovyCompiler().compile(sources2);
        
        codeLayers = Arrays.asList(code1, code2);

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
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(1200));
        assertThat((Integer)script21.run(), is(1201));
        assertThat((Integer)script21.run(), is(1202));
        assertThat((Integer)script22.run(), is(1200));
        assertThat((Integer)script22.run(), is(1201));
        assertThat((Integer)script22.run(), is(1202));
        
        // layers current first, so top version counts
        Class<?> clazz31 = engine.loadMainClass(loader, s3);
        Class<?> clazz32 = engine.loadMainClass(attachedLoader2, s3);
        Script script31 = (Script)clazz31.newInstance();
        Script script32 = (Script)clazz32.newInstance();
        assertThat((Integer)script31.run(), is(333));
        assertThat((Integer)script31.run(), is(333));
        assertThat((Integer)script32.run(), is(333));
        assertThat((Integer)script32.run(), is(333));
        
        Class<?> clazz4 = engine.loadMainClass(loader, s4);
        Script script4 = (Script)clazz4.newInstance();
        assertThat((Integer)script4.run(), is(400));
        
        // new loaders for s3+s4 because classes already loaded
        Loader attachedLoader3 = engine.newAttachedLoader();
        Loader attachedLoader4 = engine.newAttachedLoader();

        @SuppressWarnings("rawtypes")
        Class classSub21 = engine.loadClass(attachedLoader3, s2, "Sub");
        @SuppressWarnings("rawtypes")
        Class classSub22 = engine.loadClass(attachedLoader4, s2, "Sub");
        assertThat(classSub21, not(sameInstance(classSub22)));
        clazz31.newInstance();
        clazz32.newInstance();
        
        // current layer first, so layer version counts
        Class<?> clazz4direct = engine.loadClass(attachedLoader3, "org.junit.Assume");
        Script script4direct = (Script)clazz4direct.newInstance();
        assertThat((Integer)script4direct.run(), is(400));

        prepareCode(2000);
        
        // no change even though file modification date has changed
        // (could not load differently from static because already loaded, but from top code cache)
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(2200));
        assertThat((Integer)script21.run(), is(2201));
        assertThat((Integer)script21.run(), is(2202));
        assertThat((Integer)script22.run(), is(2200));
        assertThat((Integer)script22.run(), is(2201));
        assertThat((Integer)script22.run(), is(2202));
        
        // extra: try to load class that does not exist
        try {
            engine.loadClass(loader, "DoesNotExist235134");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Could not load class 'DoesNotExist235134'. " +
                    "Cause: java.lang.ClassNotFoundException: DoesNotExist235134"));
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
        assertThat((Integer)script31.run(), is(300));
        assertThat((Integer)script31.run(), is(300));
        assertThat((Integer)script32.run(), is(300));
        assertThat((Integer)script32.run(), is(300));
        
        // class exists in parent, but not source based, so uses one in layer
        Class<?> clazz4 = engine.loadMainClass(loader, s4);
        Script script4 = (Script)clazz4.newInstance();
        assertThat((Integer)script4.run(), is(400));
       
        
        // new loaders for s3+s4 because classes already loaded
        Loader attachedLoader3 = engine.newAttachedLoader();
        Loader attachedLoader4 = engine.newAttachedLoader();

        @SuppressWarnings("rawtypes")
        Class classSub21 = engine.loadClass(attachedLoader3, s2, "Sub");
        @SuppressWarnings("rawtypes")
        Class classSub22 = engine.loadClass(attachedLoader4, s2, "Sub");
        assertThat(classSub21, not(sameInstance(classSub22)));
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
            assertThat(e.getMessage(), is("Found 1 class name conflict(s). Duplicate classes in code layers: [Script3], " +
                    "classes in code layers and parent: (not checked)"));
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
            assertThat(e.getMessage(), is("Found 1 class name conflict(s). Duplicate classes in code layers: (not checked), " +
                    "classes in code layers and parent: [org.junit.Assume]"));
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
            assertThat(e.getMessage(), is("Found 2 class name conflict(s). Duplicate classes in code layers: [Script3], " +
                    "classes in code layers and parent: [org.junit.Assume]"));
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
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(1200));
        assertThat((Integer)script21.run(), is(1201));
        assertThat((Integer)script21.run(), is(1202));
        assertThat((Integer)script22.run(), is(1200));
        assertThat((Integer)script22.run(), is(1201));
        assertThat((Integer)script22.run(), is(1202));
        
        prepareCode(2000);
        
        // no change even though file modification date has change
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayersBySource(sourcesLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(2200));
        assertThat((Integer)script21.run(), is(2201));
        assertThat((Integer)script21.run(), is(2202));
        assertThat((Integer)script22.run(), is(2200));
        assertThat((Integer)script22.run(), is(2201));
        assertThat((Integer)script22.run(), is(2202));
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
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(1200));
        assertThat((Integer)script21.run(), is(1201));
        assertThat((Integer)script21.run(), is(1202));
        assertThat((Integer)script22.run(), is(1200));
        assertThat((Integer)script22.run(), is(1201));
        assertThat((Integer)script22.run(), is(1202));
        
        prepareCode(2000);
        
        // no change even though file modification date has change
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(2200));
        assertThat((Integer)script21.run(), is(2201));
        assertThat((Integer)script21.run(), is(2202));
        assertThat((Integer)script22.run(), is(2200));
        assertThat((Integer)script22.run(), is(2201));
        assertThat((Integer)script22.run(), is(2202));
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
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script11.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script12.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        assertThat((Integer)script1D.run(), is(1100));
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.newInstance();
        Script script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(1200));
        assertThat((Integer)script21.run(), is(1201));
        assertThat((Integer)script21.run(), is(1202));
        assertThat((Integer)script22.run(), is(1200));
        assertThat((Integer)script22.run(), is(1201));
        assertThat((Integer)script22.run(), is(1202));
        
        prepareCode(2000);
        
        // file modification date has changed,
        // change must become available immediately via top code cache
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(2200));
        assertThat((Integer)script21.run(), is(2201));
        assertThat((Integer)script21.run(), is(2202));
        assertThat((Integer)script22.run(), is(2200));
        assertThat((Integer)script22.run(), is(2201));
        assertThat((Integer)script22.run(), is(2202));

        // now replace code layers, changes must remain available (even if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.newInstance();
        script12 = (Script)clazz12.newInstance();
        script1D = (Script)clazz1D.newInstance();
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script11.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script12.run(), is(2100));
        assertThat((Integer)script1D.run(), is(2100));
        assertThat((Integer)script1D.run(), is(2100));
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.newInstance();
        script22 = (Script)clazz22.newInstance();
        assertThat((Integer)script21.run(), is(2200));
        assertThat((Integer)script21.run(), is(2201));
        assertThat((Integer)script21.run(), is(2202));
        assertThat((Integer)script22.run(), is(2200));
        assertThat((Integer)script22.run(), is(2201));
        assertThat((Integer)script22.run(), is(2202));
    }

    @Test
    public void testSetCodeLayersNull() throws Exception {
        try {
            new LayeredEngine.Builder().build().setCodeLayers(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Code layers are null."));
        }
    }

    @Test
    public void testSetCodeLayersBySourceNull() throws Exception {
        try {
            new LayeredEngine.Builder().build().setCodeLayersBySource(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Sources layers are null."));
        }
    }

    @Test
    public void testAsClassLoader() throws Exception {

        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        LayeredEngine engine = builder.build();

        ClassLoader classLoader = engine.asClassLoader(engine.getLoader());
        assertThat(classLoader, notNullValue());
        assertThat(classLoader.getClass().getName(),
                is("ch.grengine.engine.LayeredEngine$LoaderBasedClassLoader"));

        assertThat(classLoader.loadClass("java.util.Calendar").getName(),
                is("java.util.Calendar"));

        try {
            classLoader.loadClass("NoSuchClass");
            fail();
        } catch (ClassNotFoundException e) {
            assertThat(e.getMessage(), is("NoSuchClass"));
        }
    }

    @Test
    public void testAsClassLoaderFindClass() throws Exception {

        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        LayeredEngine engine = builder.build();

        ClassLoader classLoader = engine.asClassLoader(engine.getLoader());
        Method findClassMethod = classLoader.getClass().getDeclaredMethod("findClass", String.class);

        assertThat(((Class<?>)findClassMethod.invoke(classLoader, "java.util.Calendar")).getName(),
                is("java.util.Calendar"));

        try {
            findClassMethod.invoke(classLoader, "NoSuchClass");
            fail();
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(ClassNotFoundException.class));
            assertThat(e.getCause().getMessage(), is("NoSuchClass"));
        }
    }

    @Test
    public void testAsClassLoaderLoaderNull() throws Exception {
        try {
            new LayeredEngine.Builder().build().asClassLoader(null);
            fail();
        } catch (NullPointerException e) {
            assertThat(e.getMessage(), is("Loader is null."));
        }
    }

    @Test
    public void testAsClassLoaderLoaderForOtherEngine() throws Exception {
        LayeredEngine engine2 = new LayeredEngine.Builder().build();
        try {
            new LayeredEngine.Builder().build().asClassLoader(engine2.getLoader());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(
                    "Engine ID does not match (loader created by a different engine)."));
        }
    }

}
