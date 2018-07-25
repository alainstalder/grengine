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

import ch.grengine.code.Code;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class LayeredClassLoaderTest {
    
    @Test
    void testConstructFromCodeLayersDefaults() {

        // given
        
        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();

        // when

        final LayeredClassLoader loader1 = builder.buildFromCodeLayers();

        // then

        assertThat(loader1.getBuilder(), is(builder));
        assertThat(loader1.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(loader1.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader1.getCodeLayers().isEmpty(), is(true));
        assertThat(loader1.getTopCodeCache(), is(nullValue()));

        assertThat(loader1.getBuilder().getParent(), is(loader1.getParent()));
        assertThat(loader1.getBuilder().getLoadMode(), is(loader1.getLoadMode()));
        assertThat(loader1.getBuilder().getSourcesLayers().isEmpty(), is(true));
        assertThat(loader1.getBuilder().getCodeLayers(), is(loader1.getCodeLayers()));
        assertThat(loader1.getBuilder().isWithTopCodeCache(), is(false));
        assertThat(loader1.getBuilder().getTopLoadMode(), is(LoadMode.PARENT_FIRST));
        assertThat(loader1.getBuilder().getTopCodeCache(), is(nullValue()));

        // when (extra: constructor with explicitly from code layers)

        final LayeredClassLoader loader2 = new LayeredClassLoader(builder, false);

        // then

        assertThat(loader2.getBuilder(), is(builder));
        assertThat(loader2.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(loader2.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader2.getCodeLayers().isEmpty(), is(true));
        assertThat(loader2.getTopCodeCache(), is(nullValue()));
    }

    @Test
    void testConstructFromCodeLayersAllSet() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final List<Code> codeLayers = getTestCodeLayers(parent);
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();

        // when

        final LayeredClassLoader loader = builder
                .setParent(parent)
                .setLoadMode(LoadMode.PARENT_FIRST)
                .setCodeLayers(codeLayers)
                .setWithTopCodeCache(true, topCodeCache)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .buildFromCodeLayers();

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
    void testConstructFromSourcesLayersDefaults() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();

        // when

        final LayeredClassLoader loader = builder.buildFromSourcesLayers();

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
    void testConstructFromSourcesLayersAllSet() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final List<Sources> sourcesLayers = getTestSourcesLayers();
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();

        // when

        final LayeredClassLoader loader = builder
                .setParent(parent)
                .setLoadMode(LoadMode.PARENT_FIRST)
                .setSourcesLayers(sourcesLayers)
                .setWithTopCodeCache(true, topCodeCache)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .buildFromSourcesLayers();

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
    void testSetLayersWithVarargs() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();

        // when

        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final List<Code> codeLayers = getTestCodeLayers(parent);
        final List<Sources> sourcesLayers = getTestSourcesLayers();

        // then

        assertThat(codeLayers.size(), is(2));
        assertThat(sourcesLayers.size(), is(2));

        // when

        final Code code1 = codeLayers.get(0);
        final Code code2 = codeLayers.get(1);
        final Sources sources1 = sourcesLayers.get(0);
        final Sources sources2 = sourcesLayers.get(1);
        
        builder.setCodeLayers(code1, code2);
        final List<Code> codeLayersRead = builder.getCodeLayers();

        // then

        assertThat(codeLayersRead.size(), is(2));
        assertThat(codeLayersRead.get(0), is(code1));
        assertThat(codeLayersRead.get(1), is(code2));

        // when

        builder.setSourcesLayers(sources1, sources2);
        final List<Sources> sourcesLayersRead = builder.getSourcesLayers();

        // then

        assertThat(sourcesLayersRead.size(), is(2));
        assertThat(sourcesLayersRead.get(0), is(sources1));
        assertThat(sourcesLayersRead.get(1), is(sources2));
    }
    
    @Test
    void testModifyBuilderAfterUse() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        builder.buildFromCodeLayers();

        // when/then

        assertThrows(IllegalStateException.class,
                () -> builder.setLoadMode(LoadMode.CURRENT_FIRST),
                "Builder already used.");
    }
    
    
    @Test
    void testClone_NoTopCodeCache() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final LayeredClassLoader loader = builder.buildFromCodeLayers();

        // when

        final LayeredClassLoader clone = loader.clone();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(nullValue()));
    }
    
    @Test
    void testClone_WithTopCodeCache() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LayeredClassLoader loader = builder
                .setWithTopCodeCache(true, topCodeCache)
                .buildFromCodeLayers();

        // when

        final LayeredClassLoader clone = loader.clone();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(topCodeCache));
    }
    
    @Test
    void testCloneWithSeparateTopCodeCache_NoTopCodeCache() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final LayeredClassLoader loader = builder.buildFromCodeLayers();

        // when

        final LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getTopCodeCache(), is(nullValue()));
    }
    
    @Test
    void testCloneWithSeparateTopCodeCache_WithTopCodeCache() {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();
        final LayeredClassLoader loader = builder
                .setWithTopCodeCache(true, topCodeCache)
                .buildFromCodeLayers();

        // when

        final LayeredClassLoader clone = loader.cloneWithSeparateTopCodeCache();

        // then

        assertThat(clone.getBuilder(), is(loader.getBuilder()));
        assertThat(clone.getCodeLayers(), is(loader.getCodeLayers()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(topCodeCache, not(sameInstance(clone.getTopCodeCache())));
        assertThat(clone.getTopCodeCache(), is(notNullValue()));
        assertThat(clone.getTopCodeCache(), instanceOf(DefaultTopCodeCache.class));
    }

    @Test
    void testReleaseClasses() throws Exception {

        // given

        final LayeredClassLoader.Builder builder = new LayeredClassLoader.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final TopCodeCache topCodeCache = new DefaultTopCodeCache.Builder(parent).build();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final List<Sources> sourcesList = Collections.singletonList(sources);

        final LayeredClassLoader loader = builder
                .setWithTopCodeCache(true, topCodeCache)
                .setSourcesLayers(sourcesList)
                .buildFromSourcesLayers();

        final Class<?> clazz1 = loader.loadClass("Class1");
        final Class<?> clazz2 = loader.loadClass("Class2");
        clazz2.getConstructor().newInstance();

        final Source s4 = f.fromText("class Class4 {}");
        Class<?> clazz4 = loader.loadMainClass(s4);

        final Source s5 = f.fromText("class Class4 { int get() { return 1 } }");
        Class<?> clazz5 = loader.loadMainClass(s5);

        final RecordingClassReleaser releaser = new RecordingClassReleaser();

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
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("public class Twice { public def get() { return Inner1.get() }\n" +
                "public class Inner1 { static def get() { return 1 } } }");
        final Source s2 = f.fromText("public class Twice { public def get() { return Inner2.get() }\n" +
                "public class Inner2 { static def get() { return 2 } } }");
        final Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s1);
        final Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s2);
        final Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "sources1");
        final Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "sources2");
        return Arrays.asList(sources1, sources2);
    }
    
    private static List<Code> getTestCodeLayers(final ClassLoader parent) {
        final List<Sources> sourcesLayers = getTestSourcesLayers();
        final DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);
        final Code code1 = c.compile(sourcesLayers.get(0));
        final Code code2 = c.compile(sourcesLayers.get(1));
        return Arrays.asList(code1, code2);
    }

}
