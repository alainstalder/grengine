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

package ch.artecat.grengine.engine;

import ch.artecat.grengine.TestUtil;
import ch.artecat.grengine.code.Code;
import ch.artecat.grengine.code.groovy.DefaultGroovyCompiler;
import ch.artecat.grengine.except.ClassNameConflictException;
import ch.artecat.grengine.except.LoadException;
import ch.artecat.grengine.load.ClassReleaser;
import ch.artecat.grengine.load.DefaultClassReleaser;
import ch.artecat.grengine.load.DefaultTopCodeCacheFactory;
import ch.artecat.grengine.load.LoadMode;
import ch.artecat.grengine.load.RecordingClassReleaser;
import ch.artecat.grengine.load.TopCodeCacheFactory;
import ch.artecat.grengine.source.DefaultSourceFactory;
import ch.artecat.grengine.source.DefaultTextSource;
import ch.artecat.grengine.source.MockFile;
import ch.artecat.grengine.source.MockFileSource;
import ch.artecat.grengine.source.Source;
import ch.artecat.grengine.source.SourceFactory;
import ch.artecat.grengine.source.SourceUtil;
import ch.artecat.grengine.sources.Sources;
import ch.artecat.grengine.sources.SourcesUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import groovy.lang.Script;
import org.junit.jupiter.api.Test;

import static ch.artecat.grengine.TestUtil.assertThrowsMessageIs;
import static ch.artecat.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


class LayeredEngineTest {

    @Test
    void testConstructDefaults() {

        // given
        
        final LayeredEngine.Builder builder = new LayeredEngine.Builder();

        // when

        final LayeredEngine engine = builder.build();

        // then

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
    void testConstructAllDefined() {

        // given

        final LayeredEngine.Builder builder = new LayeredEngine.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final TopCodeCacheFactory topCodeCacheFactory = new DefaultTopCodeCacheFactory();
        final ClassReleaser releaser = new RecordingClassReleaser();

        // when

        final LayeredEngine engine = builder
                .setParent(parent)
                .setLoadMode(LoadMode.PARENT_FIRST)
                .setWithTopCodeCache(false)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .setTopCodeCacheFactory(topCodeCacheFactory)
                .setClassReleaser(releaser)
                .setAllowSameClassNamesInMultipleCodeLayers(false)
                .setAllowSameClassNamesInParentAndCodeLayers(false)
                .build();

        // then

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
    void testModifyBuilderAfterUse() {

        // given

        final LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.build();

        // when/then

        assertThrowsMessageIs(IllegalStateException.class,
                () -> builder.setLoadMode(LoadMode.CURRENT_FIRST),
                "Builder already used.");
    }

    @Test
    void testClose() throws Exception {

        // given

        final LayeredEngine.Builder builder = new LayeredEngine.Builder();
        final RecordingClassReleaser releaser = new RecordingClassReleaser();

        final LayeredEngine engine = builder
                .setClassReleaser(releaser)
                .build();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final List<Sources> sourcesList = Collections.singletonList(sources);

        engine.setCodeLayersBySource(sourcesList);

        final Loader loaderAttached = engine.newAttachedLoader();
        final Loader loaderDetached = engine.newDetachedLoader();

        final Class<?> clazz1a = engine.loadClass(loaderAttached, "Class1");
        final Class<?> clazz2a = engine.loadClass(loaderAttached, "Class2");
        clazz2a.getConstructor().newInstance();
        final Class<?> clazz1d = engine.loadClass(loaderDetached, "Class1");
        final Class<?> clazz2d = engine.loadClass(loaderDetached, "Class2");
        clazz2d.getConstructor().newInstance();

        // when

        engine.close();

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

    private Source s1;
    private Source s2;
    private Source s3;
    private Source s4;
    private List<Sources> sourcesLayers;
    private List<Code> codeLayers;
    
    private void prepareCode(final File dir, final int offs) throws Exception {
        final MockFile f1 = new MockFile(dir, "Script1.groovy");
        s1 = new MockFileSource(f1);
        final MockFile f2 = new MockFile(dir, "Script2.groovy");
        s2 = new MockFileSource(f2);
        final MockFile f3 = new MockFile(dir, "Script3.groovy");
        s3 = new MockFileSource(f3);
        s4 = new DefaultTextSource(
                "package groovy.util\npublic class Expando extends Script { public def run() { return 400 } }");

        final Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s3, s4);
        final Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "test");
        final Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s1, s2, s3);
        final Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "test");
        sourcesLayers = Arrays.asList(sources1, sources2);
        
        TestUtil.setFileText(f3, "public class Script3 extends Script { public def run() { return 300 } }");
        final Code code1 = new DefaultGroovyCompiler().compile(sources1);
        
        TestUtil.setFileText(f1, "return " + offs + "+100");
        TestUtil.setFileText(f2, "public class Script2 extends Script { public def run() { return Sub.get() } }\n" +
                "public class Sub { static int x=" + offs + "+200; static def get() { return x++ } }");
        TestUtil.setFileText(f3, "public class Script3 extends Script { public def run() { return 333 } }");
        assertThat(f1.setLastModified(offs), is(true));
        assertThat(f2.setLastModified(offs), is(true));
        final Code code2 = new DefaultGroovyCompiler().compile(sources2);
        
        codeLayers = Arrays.asList(code1, code2);
    }
    
    @Test
    void testFromCodeLayersNoTopCodeCache() throws Exception {

        // given

        final File dir = createTestDir();
        prepareCode(dir, 1000);

        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .build();
        
        engine.setCodeLayers(codeLayers);

        final Loader loader = engine.getLoader();
        final Loader attachedLoader2 = engine.newAttachedLoader();
        final Loader detachedLoader = engine.newDetachedLoader();

        // when

        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.getConstructor().newInstance();
        Script script12 = (Script)clazz12.getConstructor().newInstance();
        Script script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));

        // when

        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.getConstructor().newInstance();
        Script script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(1200));
        assertThat(script21.run(), is(1201));
        assertThat(script21.run(), is(1202));
        assertThat(script22.run(), is(1200));
        assertThat(script22.run(), is(1201));
        assertThat(script22.run(), is(1202));

        // when (layers current first, so top version counts)

        final Class<?> clazz31 = engine.loadMainClass(loader, s3);
        final Class<?> clazz32 = engine.loadMainClass(attachedLoader2, s3);
        final Script script31 = (Script)clazz31.getConstructor().newInstance();
        final Script script32 = (Script)clazz32.getConstructor().newInstance();

        // then

        assertThat(script31.run(), is(333));
        assertThat(script31.run(), is(333));
        assertThat(script32.run(), is(333));
        assertThat(script32.run(), is(333));

        // when

        final Class<?> clazz4 = engine.loadMainClass(loader, s4);
        final Script script4 = (Script)clazz4.getConstructor().newInstance();

        // then

        assertThat(script4.run(), is(400));

        // when (new loaders for s3+s4 because classes already loaded)

        final Loader attachedLoader3 = engine.newAttachedLoader();
        final Loader attachedLoader4 = engine.newAttachedLoader();
        @SuppressWarnings("rawtypes")
        final Class classSub21 = engine.loadClass(attachedLoader3, s2, "Sub");
        @SuppressWarnings("rawtypes")
        final Class classSub22 = engine.loadClass(attachedLoader4, s2, "Sub");

        // then

        assertThat(classSub21, not(sameInstance(classSub22)));
        clazz31.getConstructor().newInstance();
        clazz32.getConstructor().newInstance();

        // when (current layer first, so layer version counts)

        final Class<?> clazz4direct = engine.loadClass(attachedLoader3, "groovy.util.Expando");
        final Script script4direct = (Script)clazz4direct.getConstructor().newInstance();

        // then

        assertThat(script4direct.run(), is(400));

        // when

        prepareCode(dir, 2000);
        
        // no change even though file modification date has changed
        // (could not load differently from static because already loaded, but from top code cache)
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));

        // when

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();
        script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(2100));
        assertThat(script11.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));

        // when
        
        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.getConstructor().newInstance();
        script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(2200));
        assertThat(script21.run(), is(2201));
        assertThat(script21.run(), is(2202));
        assertThat(script22.run(), is(2200));
        assertThat(script22.run(), is(2201));
        assertThat(script22.run(), is(2202));

        // when/then (extra: try to load class that does not exist)

        assertThrowsMessageIs(LoadException.class,
                () -> engine.loadClass(loader, "DoesNotExist235134"),
                "Could not load class 'DoesNotExist235134'. " +
                        "Cause: java.lang.ClassNotFoundException: DoesNotExist235134");
    }
    
    @Test
    void testFromCodeLayersNoTopCodeCacheParentFirst() throws Exception {

        // given

        final File dir = createTestDir();
        prepareCode(dir, 1000);

        LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .setLoadMode(LoadMode.PARENT_FIRST)
                .build();

        engine.setCodeLayers(codeLayers);

        final Loader loader = engine.getLoader();
        final Loader attachedLoader2 = engine.newAttachedLoader();

        // when (layers parent first, so lower version counts)

        final Class<?> clazz31 = engine.loadMainClass(loader, s3);
        final Class<?> clazz32 = engine.loadMainClass(attachedLoader2, s3);
        final Script script31 = (Script)clazz31.getConstructor().newInstance();
        final Script script32 = (Script)clazz32.getConstructor().newInstance();

        // then

        assertThat(script31.run(), is(300));
        assertThat(script31.run(), is(300));
        assertThat(script32.run(), is(300));
        assertThat(script32.run(), is(300));
        
        // when (class exists in parent, but not source based, so uses one in layer)

        final Class<?> clazz4 = engine.loadMainClass(loader, s4);
        final Script script4 = (Script)clazz4.getConstructor().newInstance();

        // then

        assertThat(script4.run(), is(400));

        // when (new loaders for s3+s4 because classes already loaded)

        Loader attachedLoader3 = engine.newAttachedLoader();
        Loader attachedLoader4 = engine.newAttachedLoader();
        @SuppressWarnings("rawtypes")
        final Class classSub21 = engine.loadClass(attachedLoader3, s2, "Sub");
        @SuppressWarnings("rawtypes")
        final Class classSub22 = engine.loadClass(attachedLoader4, s2, "Sub");

        // then

        assertThat(classSub21, not(sameInstance(classSub22)));
        clazz31.getConstructor().newInstance();
        clazz32.getConstructor().newInstance();
        
        // when (current layer first, so layer version counts)

        Class<?> clazz4direct = engine.loadClass(attachedLoader3, "groovy.util.Expando");

        // then

        assertThat(clazz4direct.getConstructor().newInstance(), not(instanceOf(Script.class)));
    }
    
    @Test
    void testFromCodeLayersNoTopCodeCacheClassNameConflictChecks() throws Exception {
        
        // given

        final File dir = createTestDir();
        prepareCode(dir, 1000);

        // when

        final LayeredEngine engine1 = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .setAllowSameClassNamesInMultipleCodeLayers(false)
                .setAllowSameClassNamesInParentAndCodeLayers(true)
                .build();

        // then

        assertThrowsMessageIs(ClassNameConflictException.class,
                () -> engine1.setCodeLayers(codeLayers),
                "Found 1 class name conflict(s). Duplicate classes in code layers: [Script3], " +
                        "classes in code layers and parent: (not checked)");

        // when

        final LayeredEngine engine2 = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .setAllowSameClassNamesInMultipleCodeLayers(true)
                .setAllowSameClassNamesInParentAndCodeLayers(false)
                .build();

        // then

        assertThrowsMessageIs(ClassNameConflictException.class,
                () -> engine2.setCodeLayers(codeLayers),
                "Found 1 class name conflict(s). Duplicate classes in code layers: (not checked), " +
                        "classes in code layers and parent: [groovy.util.Expando]");

        // when

        final LayeredEngine engine3 = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .setAllowSameClassNamesInMultipleCodeLayers(false)
                .setAllowSameClassNamesInParentAndCodeLayers(false)
                .build();

        // then

        assertThrowsMessageIs(ClassNameConflictException.class,
                () -> engine3.setCodeLayers(codeLayers),
                "Found 2 class name conflict(s). Duplicate classes in code layers: [Script3], " +
                        "classes in code layers and parent: [groovy.util.Expando]");
    }

    
    @Test
    void testFromSourcesLayersNoTopCodeCache() throws Exception {

        // given

        final File dir = createTestDir();
        prepareCode(dir, 1000);

        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .build();
        
        engine.setCodeLayersBySource(sourcesLayers);
        
        final Loader loader = engine.getLoader();
        final Loader attachedLoader2 = engine.newAttachedLoader();
        final Loader detachedLoader = engine.newDetachedLoader();

        // when
        
        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.getConstructor().newInstance();
        Script script12 = (Script)clazz12.getConstructor().newInstance();
        Script script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));

        // when
        
        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.getConstructor().newInstance();
        Script script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(1200));
        assertThat(script21.run(), is(1201));
        assertThat(script21.run(), is(1202));
        assertThat(script22.run(), is(1200));
        assertThat(script22.run(), is(1201));
        assertThat(script22.run(), is(1202));

        // when
        
        prepareCode(dir,2000);
        
        // no change even though file modification date has change
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));

        // when

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayersBySource(sourcesLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();
        script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(2100));
        assertThat(script11.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));

        // when

        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.getConstructor().newInstance();
        script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(2200));
        assertThat(script21.run(), is(2201));
        assertThat(script21.run(), is(2202));
        assertThat(script22.run(), is(2200));
        assertThat(script22.run(), is(2201));
        assertThat(script22.run(), is(2202));
    }
    
    @Test
    void testFromCodeLayersTopCodeCacheParentFirst() throws Exception {

        // given

        final File dir = createTestDir();
        prepareCode(dir, 1000);

        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .build();
        
        engine.setCodeLayers(codeLayers);
        
        final Loader loader = engine.getLoader();
        final Loader attachedLoader2 = engine.newAttachedLoader();
        final Loader detachedLoader = engine.newDetachedLoader();

        // when
        
        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.getConstructor().newInstance();
        Script script12 = (Script)clazz12.getConstructor().newInstance();
        Script script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));

        // when

        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.getConstructor().newInstance();
        Script script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(1200));
        assertThat(script21.run(), is(1201));
        assertThat(script21.run(), is(1202));
        assertThat(script22.run(), is(1200));
        assertThat(script22.run(), is(1201));
        assertThat(script22.run(), is(1202));

        // when

        prepareCode(dir, 2000);
        
        // no change even though file modification date has change
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));


        // when

        // now replace code layers, then changes must become available (except if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();
        script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(2100));
        assertThat(script11.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));


        // when

        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.getConstructor().newInstance();
        script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(2200));
        assertThat(script21.run(), is(2201));
        assertThat(script21.run(), is(2202));
        assertThat(script22.run(), is(2200));
        assertThat(script22.run(), is(2201));
        assertThat(script22.run(), is(2202));
    }

    @Test
    void testFromCodeLayersTopCodeCacheCurrentFirst() throws Exception {

        // given

        final File dir = createTestDir();
        prepareCode(dir, 1000);

        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .build();
        
        engine.setCodeLayers(codeLayers);

        final Loader loader = engine.getLoader();
        final Loader attachedLoader2 = engine.newAttachedLoader();
        final Loader detachedLoader = engine.newDetachedLoader();

        // when

        Class<?> clazz11 = engine.loadMainClass(loader, s1);
        Class<?> clazz12 = engine.loadMainClass(attachedLoader2, s1);
        Class<?> clazz1D = engine.loadMainClass(detachedLoader, s1);
        Script script11 = (Script)clazz11.getConstructor().newInstance();
        Script script12 = (Script)clazz12.getConstructor().newInstance();
        Script script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(1100));
        assertThat(script11.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script12.run(), is(1100));
        assertThat(script1D.run(), is(1100));
        assertThat(script1D.run(), is(1100));

        // when

        Class<?> clazz21 = engine.loadMainClass(loader, s2);
        Class<?> clazz22 = engine.loadMainClass(attachedLoader2, s2);
        Script script21 = (Script)clazz21.getConstructor().newInstance();
        Script script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(1200));
        assertThat(script21.run(), is(1201));
        assertThat(script21.run(), is(1202));
        assertThat(script22.run(), is(1200));
        assertThat(script22.run(), is(1201));
        assertThat(script22.run(), is(1202));

        // when

        prepareCode(dir, 2000);
        
        // file modification date has changed,
        // change must become available immediately via top code cache
        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(2100));
        assertThat(script11.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script12.run(), is(2100));

        // when

        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.getConstructor().newInstance();
        script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(2200));
        assertThat(script21.run(), is(2201));
        assertThat(script21.run(), is(2202));
        assertThat(script22.run(), is(2200));
        assertThat(script22.run(), is(2201));
        assertThat(script22.run(), is(2202));

        // when

        // now replace code layers, changes must remain available (even if loader is detached)
        engine.setCodeLayers(codeLayers);

        clazz11 = engine.loadMainClass(loader, s1);
        clazz12 = engine.loadMainClass(attachedLoader2, s1);
        clazz1D = engine.loadMainClass(detachedLoader, s1);
        script11 = (Script)clazz11.getConstructor().newInstance();
        script12 = (Script)clazz12.getConstructor().newInstance();
        script1D = (Script)clazz1D.getConstructor().newInstance();

        // then

        assertThat(script11.run(), is(2100));
        assertThat(script11.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script12.run(), is(2100));
        assertThat(script1D.run(), is(2100));
        assertThat(script1D.run(), is(2100));

        // when

        clazz21 = engine.loadMainClass(loader, s2);
        clazz22 = engine.loadMainClass(attachedLoader2, s2);
        script21 = (Script)clazz21.getConstructor().newInstance();
        script22 = (Script)clazz22.getConstructor().newInstance();

        // then

        assertThat(script21.run(), is(2200));
        assertThat(script21.run(), is(2201));
        assertThat(script21.run(), is(2202));
        assertThat(script22.run(), is(2200));
        assertThat(script22.run(), is(2201));
        assertThat(script22.run(), is(2202));
    }

    @Test
    void testSetCodeLayersNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new LayeredEngine.Builder().build().setCodeLayers(null),
                "Code layers are null.");
    }

    @Test
    void testSetCodeLayersBySourceNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new LayeredEngine.Builder().build().setCodeLayersBySource(null),
                "Sources layers are null.");
    }

    @Test
    void testAsClassLoader() throws Exception {

        // given

        final LayeredEngine engine = new LayeredEngine.Builder().build();

        // when

        final ClassLoader classLoader = engine.asClassLoader(engine.getLoader());

        // then

        assertThat(classLoader, notNullValue());
        assertThat(classLoader.getClass().getName(),
                is("ch.artecat.grengine.engine.LayeredEngine$LoaderBasedClassLoader"));

        assertThat(classLoader.loadClass("java.util.Calendar").getName(),
                is("java.util.Calendar"));

        // when/then

        assertThrowsMessageIs(ClassNotFoundException.class,
                () -> classLoader.loadClass("NoSuchClass"),
                "NoSuchClass");
    }

    @Test
    void testAsClassLoaderFindClass() throws Exception {

        // given

        final LayeredEngine engine = new LayeredEngine.Builder().build();

        final ClassLoader classLoader = engine.asClassLoader(engine.getLoader());

        // when

        final Method findClassMethod = classLoader.getClass().getDeclaredMethod("findClass", String.class);

        // then

        assertThat(((Class<?>)findClassMethod.invoke(classLoader, "java.util.Calendar")).getName(),
                is("java.util.Calendar"));

        // when/then

        try {
            findClassMethod.invoke(classLoader, "NoSuchClass");
            fail("did not throw");
        } catch (InvocationTargetException e) {
            assertThat(e.getCause(), instanceOf(ClassNotFoundException.class));
            assertThat(e.getCause().getMessage(), is("NoSuchClass"));
        }
    }

    @Test
    void testAsClassLoaderLoaderNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new LayeredEngine.Builder().build().asClassLoader(null),
                "Loader is null.");
    }

    @Test
    void testAsClassLoaderLoaderForOtherEngine() {

        // given

        final LayeredEngine engine2 = new LayeredEngine.Builder().build();


        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> new LayeredEngine.Builder().build().asClassLoader(engine2.getLoader()),
                "Engine ID does not match (loader created by a different engine).");
    }

}
