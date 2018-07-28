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
import ch.grengine.source.DefaultTextSource;
import ch.grengine.source.MockFile;
import ch.grengine.source.MockFileSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsStartsWith;
import static ch.grengine.TestUtil.createTestDir;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.CodeLayersType.CODE_LAYERS_CURRENT_FIRST;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.CodeLayersType.CODE_LAYERS_PARENT_FIRST;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.ParentClassLoaderType.PARENT_IS_REGULAR_CLASS_LOADER;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.ParentClassLoaderType.PARENT_IS_SOURCE_CLASS_LOADER;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.SourcesChangedState.SOURCES_CHANGED;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.SourcesChangedState.SOURCES_UNCHANGED;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.TopCodeCacheType.TOP_CODE_CACHE_CURRENT_FIRST;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.TopCodeCacheType.TOP_CODE_CACHE_OFF;
import static ch.grengine.load.LayeredClassLoaderMatrixTest.TopCodeCacheType.TOP_CODE_CACHE_PARENT_FIRST;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;


class LayeredClassLoaderMatrixTest {

    // whether the parent class loader of the layered class loader to test
    // is a source class loader or not
    enum ParentClassLoaderType {
        PARENT_IS_SOURCE_CLASS_LOADER,
        PARENT_IS_REGULAR_CLASS_LOADER
    }

    // current first: try to load from top code layer first, down to parent
    // parent first:  try to load from parent first, up to top code layer
    enum CodeLayersType {
        CODE_LAYERS_CURRENT_FIRST,
        CODE_LAYERS_PARENT_FIRST
    }

    // off:           no top code cache, try to load directly from code layers / parent
    // current first: try to load first from top code cache, then from code layers / parent
    // parent first:  try to load first from code layers / parent, then from top code cache
    enum TopCodeCacheType {
        TOP_CODE_CACHE_OFF,
        TOP_CODE_CACHE_CURRENT_FIRST,
        TOP_CODE_CACHE_PARENT_FIRST
    }

    // whether sources have changed their state (modification date)
    // after compiling code layers; if yes, they are loaded from top code cache
    // instead of from layers, depending on other settings
    enum SourcesChangedState {
        SOURCES_CHANGED,
        SOURCES_UNCHANGED
    }


    static class TestContext {

        ParentClassLoaderType parentClassLoaderType;
        CodeLayersType codeLayersType;
        TopCodeCacheType topCodeCacheType;
        SourcesChangedState sourcesChangedState;

        MockFile fMain;
        Source sMain;
        MockFile fExpando;
        Source sExpando;
        Source sNotInCodeLayers;
        Code codeParent;
        List<Code> codeLayers;

        ClassLoader parent;
        LoadMode layerLoadMode;
        boolean isWithTopLoadMode;
        TopCodeCache topCodeCache;
        LoadMode topLoadMode;

        LayeredClassLoader.Builder builder;
    }

    private void testGeneric(final ParentClassLoaderType parentClassLoaderType,
                             final CodeLayersType codeLayersType, final TopCodeCacheType topCodeCacheType,
                             final SourcesChangedState sourcesChangedState) throws Exception {

        TestContext ctx = new TestContext();
        ctx.parentClassLoaderType = parentClassLoaderType;
        ctx.codeLayersType = codeLayersType;
        ctx.topCodeCacheType = topCodeCacheType;
        ctx.sourcesChangedState = sourcesChangedState;

        prepareCode(ctx);
        prepareBuilder(ctx);

        testFindBytecodeClassLoaderBySource(ctx);
        testLoadMainClassBySource(ctx);
        testLoadClassBySourceAndName(ctx);
        testLoadClassByName(ctx);
    }

    private void prepareCode(final TestContext ctx) throws Exception {
        final File dir = createTestDir();
        ctx.fMain = new MockFile(dir, "Main.groovy");
        ctx.sMain = new MockFileSource(ctx.fMain);
        ctx.fExpando = new MockFile(dir, "Expando.groovy");
        ctx.sExpando = new MockFileSource(ctx.fExpando);
        ctx.sNotInCodeLayers = new DefaultTextSource("class NotInCodeLayers {}");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(ctx.sMain, ctx.sExpando);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        
        // code parent class loader if source class loader
        TestUtil.setFileText(ctx.fMain, "class Main { def methodParent() {} }\nclass Side { def methodParent() {} }");
        TestUtil.setFileText(ctx.fExpando, "package groovy.util\nclass Expando  { def methodParent() {} }");
        ctx.codeParent = new DefaultGroovyCompiler().compile(sources);
        
        // code layer 0
        TestUtil.setFileText(ctx.fMain, "class Main { def methodLayer0() {} }\nclass Side { def methodLayer0() {} }");
        TestUtil.setFileText(ctx.fExpando, "package groovy.util\nclass Expando  { def methodLayer0() {} }");
        final Code codeLayer0 = new DefaultGroovyCompiler().compile(sources);
        
        // code layer 1
        TestUtil.setFileText(ctx.fMain, "class Main { def methodLayer1() {} }\nclass Side { def methodLayer1() {} }");
        TestUtil.setFileText(ctx.fExpando, "package groovy.util\nclass Expando  { def methodLayer1() {} }");
        final Code codeLayer1 = new DefaultGroovyCompiler().compile(sources);

        ctx.codeLayers = Arrays.asList(codeLayer0, codeLayer1);
        
        // prepare files for top code cache
        TestUtil.setFileText(ctx.fMain, "class Main { def methodTop() {} }\nclass Side { def methodTop() {} }");
        TestUtil.setFileText(ctx.fExpando, "package groovy.util\nclass Expando  { def methodTop() {} }");

        if (ctx.sourcesChangedState == SOURCES_CHANGED) {
            assertThat(ctx.fMain.setLastModified(100), is(true));
            assertThat(ctx.fExpando.setLastModified(100), is(true));
        }
    }

    private void prepareBuilder(final TestContext ctx) {

        switch (ctx.parentClassLoaderType) {
            case PARENT_IS_SOURCE_CLASS_LOADER:
                ctx.parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                        LoadMode.CURRENT_FIRST, ctx.codeParent);
                break;
            case PARENT_IS_REGULAR_CLASS_LOADER:
                ctx.parent = Thread.currentThread().getContextClassLoader();
                break;
        }

        switch (ctx.codeLayersType) {
            case CODE_LAYERS_CURRENT_FIRST:
                ctx.layerLoadMode = LoadMode.CURRENT_FIRST;
                break;
            case CODE_LAYERS_PARENT_FIRST:
                ctx.layerLoadMode = LoadMode.PARENT_FIRST;
                break;
        }

        switch (ctx.topCodeCacheType) {
            case TOP_CODE_CACHE_OFF:
                ctx.isWithTopLoadMode = false;
                ctx.topCodeCache = null;
                ctx.topLoadMode = null;
                break;
            case TOP_CODE_CACHE_CURRENT_FIRST:
                ctx.isWithTopLoadMode = true;
                ctx.topCodeCache = new DefaultTopCodeCache.Builder(ctx.parent).build();
                ctx.topLoadMode = LoadMode.CURRENT_FIRST;
                break;
            case TOP_CODE_CACHE_PARENT_FIRST:
                ctx.isWithTopLoadMode = true;
                ctx.topCodeCache = new DefaultTopCodeCache.Builder(ctx.parent).build();
                ctx.topLoadMode = LoadMode.PARENT_FIRST;
                break;
        }

        ctx.builder = new LayeredClassLoader.Builder()
                .setParent(ctx.parent)
                .setLoadMode(ctx.layerLoadMode)
                .setCodeLayers(ctx.codeLayers)
                .setWithTopCodeCache(ctx.isWithTopLoadMode, ctx.topCodeCache)
                .setTopLoadMode(ctx.topLoadMode);
    }

    private static String getExpectedMethod(final TestContext ctx, final boolean loadBySource) {
        if (ctx.topCodeCacheType == TOP_CODE_CACHE_CURRENT_FIRST
                && ctx.sourcesChangedState == SOURCES_CHANGED
                && loadBySource) {
            return "methodTop";
        }
        switch (ctx.codeLayersType) {
            case CODE_LAYERS_CURRENT_FIRST:
                return "methodLayer1";
            case CODE_LAYERS_PARENT_FIRST:
                switch (ctx.parentClassLoaderType) {
                    case PARENT_IS_REGULAR_CLASS_LOADER:
                        return "methodLayer0";
                    case PARENT_IS_SOURCE_CLASS_LOADER:
                        return "methodParent";
                }
        }
        throw new RuntimeException("never gets here");
    }

    private void testFindBytecodeClassLoaderBySource(final TestContext ctx) {

        // given

        final LayeredClassLoader loader = ctx.builder.buildFromCodeLayers();

        final boolean expectParentClassLoader =
                ctx.parentClassLoaderType == PARENT_IS_SOURCE_CLASS_LOADER &&
                ctx.codeLayersType == CODE_LAYERS_PARENT_FIRST;

        // when

        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(ctx.sMain);

        // then

        assertThat(loaderFound, is(notNullValue()));
        if (expectParentClassLoader) {
            assertThat(loaderFound, sameInstance(ctx.parent));
        } else {
            assertThat(loaderFound, not(sameInstance(ctx.parent)));
        }

        // when

        loaderFound = loader.findBytecodeClassLoaderBySource(ctx.sExpando);

        // then

        assertThat(loaderFound, is(notNullValue()));
        if (expectParentClassLoader) {
            assertThat(loaderFound, sameInstance(ctx.parent));
        } else {
            assertThat(loaderFound, not(sameInstance(ctx.parent)));
        }

        // when

        loaderFound = loader.findBytecodeClassLoaderBySource(ctx.sNotInCodeLayers);

        // then

        assertThat(loaderFound, is(nullValue()));
    }

    private void testLoadMainClassBySource(final TestContext ctx) throws Exception {

        // given

        final LayeredClassLoader loader = ctx.builder.buildFromCodeLayers();
        final String expectedMethod = getExpectedMethod(ctx, true);
        final boolean expectLoadIfNotInCodeLayers = ctx.topCodeCacheType != TOP_CODE_CACHE_OFF;

        // when

        Class<?> clazz = loader.loadMainClass(ctx.sMain);

        // then

        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        clazz = loader.loadMainClass(ctx.sMain);

        // then

        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        if (ctx.topCodeCache != null) {
            ctx.topCodeCache.clear();
        }
        clazz = loader.loadMainClass(ctx.sMain);

        // then

        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        clazz = loader.loadMainClass(ctx.sExpando);

        // when

        assertThat(clazz.getName(), is("groovy.util.Expando"));
        clazz.getDeclaredMethod(expectedMethod);

        // when/then

        if (expectLoadIfNotInCodeLayers) {
            assertThat(loader.loadMainClass(ctx.sNotInCodeLayers), notNullValue());
        } else {
            assertThrowsStartsWith(LoadException.class,
                    () -> loader.loadMainClass(ctx.sNotInCodeLayers),
                    "Source not found: ");
        }

    }

    private void testLoadClassBySourceAndName(final TestContext ctx) throws Exception {

        // given

        final LayeredClassLoader loader = ctx.builder.buildFromCodeLayers();
        final String expectedMethod = getExpectedMethod(ctx, true);
        final boolean expectLoadIfNotInCodeLayers = ctx.topCodeCacheType != TOP_CODE_CACHE_OFF;

        // when

        Class<?> clazz = loader.loadClass(ctx.sMain, "Main");

        // then

        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        clazz = loader.loadClass(ctx.sMain, "Main");

        // then

        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        clazz = loader.loadClass(ctx.sMain, "Side");

        // then

        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod(expectedMethod);


        // when/then (wrong source, not found)
        assertThrowsStartsWith(LoadException.class,
                () -> loader.loadClass(ctx.sMain, "groovy.util.Expando"),
                "Class 'groovy.util.Expando' not found for source. Source: " + ctx.sMain.toString());

        // when (correct source)

        clazz = loader.loadClass(ctx.sExpando, "groovy.util.Expando");

        // then

        assertThat(clazz.getName(), is("groovy.util.Expando"));
        clazz.getDeclaredMethod(expectedMethod);

        // when/then

        if (expectLoadIfNotInCodeLayers) {
            assertThat(loader.loadClass(ctx.sNotInCodeLayers, "NotInCodeLayers"), notNullValue());
        } else {
            assertThrowsStartsWith(LoadException.class,
                    () -> loader.loadClass(ctx.sNotInCodeLayers, "NotInCodeLayers"),
                    "Source not found: ");
        }

    }

    private void testLoadClassByName(final TestContext ctx) throws Exception {

        // given

        final LayeredClassLoader loader = ctx.builder.buildFromCodeLayers();
        final String expectedMethod = getExpectedMethod(ctx, false);
        final boolean expectedLoadGroovyUtilExpando =
                ctx.codeLayersType == CODE_LAYERS_PARENT_FIRST
                && ctx.parentClassLoaderType == PARENT_IS_REGULAR_CLASS_LOADER;
        final String expectedExpandoMethod = expectedLoadGroovyUtilExpando ?
                "createMap" : expectedMethod;


        // when

        Class<?> clazz = loader.loadClass("Main");

        // then

        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        clazz = loader.loadClass("Side");

        // then

        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod(expectedMethod);

        // when

        clazz = loader.loadClass("groovy.util.Expando");

        // then

        clazz.getDeclaredMethod(expectedExpandoMethod);

        // when/then

        assertThrowsStartsWith(ClassNotFoundException.class,
                () -> loader.loadClass("NotInCodeLayers"),
                "NotInCodeLayers");


        // when/then (extra: load class with resolve (protected method))

        loader.loadClass("Main", true);
    }



    // top off, top sources changed
    @Test
    void testParentRegularClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_CHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_CHANGED);
    }
    @Test
    void testParentRegularClassLoader_LayersCurrentFirst_TopOff_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_CHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersCurrentFirst_TopOff_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_CHANGED);
    }

    // top parent first, top sources changed
    @Test
    void testParentRegularClassLoader_LayersParentFirst_TopParentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_CHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_CHANGED);
    }
    @Test
    void testParentRegularClassLoader_LayersCurrentFirst_TopParentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_CHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_CHANGED);
    }

    // top current first, top sources changed
    @Test
    void testParentRegularClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_CHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_CHANGED);
    }
    @Test
    void testParentRegularClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_CHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesChanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_CHANGED);
    }


    // top off, top sources unchanged
    @Test
    void testParentRegularClassLoader_LayersParentFirst_TopOff_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_UNCHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersParentFirst_TopOff_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_UNCHANGED);
    }
    @Test
    void testParentRegularClassLoader_LayersCurrentFirst_TopOff_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_UNCHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersCurrentFirst_TopOff_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_OFF, SOURCES_UNCHANGED);
    }

    // top parent first, top sources unchanged
    @Test
    void testParentRegularClassLoader_LayersParentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_UNCHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersParentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_UNCHANGED);
    }
    @Test
    void testParentRegularClassLoader_LayersCurrentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_UNCHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersCurrentFirst_TopParentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_PARENT_FIRST, SOURCES_UNCHANGED);
    }

    // top current first, top sources unchanged
    @Test
    void testParentRegularClassLoader_LayersParentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_UNCHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_PARENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_UNCHANGED);
    }
    @Test
    void testParentRegularClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_REGULAR_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_UNCHANGED);
    }
    @Test
    void testParentSourceClassLoader_LayersCurrentFirst_TopCurrentFirst_SourcesUnchanged() throws Exception {
        testGeneric(PARENT_IS_SOURCE_CLASS_LOADER, CODE_LAYERS_CURRENT_FIRST,
                TOP_CODE_CACHE_CURRENT_FIRST, SOURCES_UNCHANGED);
    }


    @Test
    void testExtraNoCodeLayers_ParentRegularClassLoader_LayersParentFirst_TopOff_SourcesChanged() throws Exception {

        // given

        final TestContext ctx = new TestContext();
        ctx.parentClassLoaderType = PARENT_IS_REGULAR_CLASS_LOADER;
        ctx.codeLayersType = CODE_LAYERS_PARENT_FIRST;
        ctx.topCodeCacheType = TOP_CODE_CACHE_OFF;
        ctx.sourcesChangedState = SOURCES_CHANGED;

        prepareCode(ctx);
        ctx.codeLayers = new LinkedList<>();
        prepareBuilder(ctx);

        final LayeredClassLoader loader = ctx.builder.buildFromCodeLayers();

        // when/then (findBytecodeClassLoaderBySource(source))

        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(ctx.sMain);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(ctx.sExpando);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(ctx.sNotInCodeLayers);
        assertThat(loaderFound, is(nullValue()));
    }

    @Test
    void testExtraSourcesChangedTop_ParentSourceClassLoader_LayersParentFirst_TopCurrentFirst_SourcesChanged() throws Exception {

        // given

        final TestContext ctx = new TestContext();
        ctx.parentClassLoaderType = PARENT_IS_SOURCE_CLASS_LOADER;
        ctx.codeLayersType = CODE_LAYERS_PARENT_FIRST;
        ctx.topCodeCacheType = TOP_CODE_CACHE_CURRENT_FIRST;
        ctx.sourcesChangedState = SOURCES_CHANGED;

        prepareCode(ctx);
        prepareBuilder(ctx);

        final LayeredClassLoader loader1 = ctx.builder.buildFromCodeLayers();

        // when/then (loadMainClass(source))

        Class<?> clazz = loader1.loadMainClass(ctx.sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(ctx.fMain.setLastModified(55555), is(true));

        clazz = loader1.loadMainClass(ctx.sMain);
        assertThat(clazz.getName(), is("Main"));
        clazz.getDeclaredMethod("methodTop");

        // when/then (loadClass(source, name))

        final LayeredClassLoader loader2 = ctx.builder.buildFromCodeLayers();

        clazz = loader2.loadClass(ctx.sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");

        assertThat(ctx.fMain.setLastModified(77777), is(true));

        clazz = loader2.loadClass(ctx.sMain, "Side");
        assertThat(clazz.getName(), is("Side"));
        clazz.getDeclaredMethod("methodTop");
    }

}
