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

import ch.grengine.code.DefaultCode;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.load.BytecodeClassLoader;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;
import ch.grengine.code.Code;
import ch.grengine.load.LoadMode;
import ch.grengine.load.RecordingClassReleaser;
import ch.grengine.load.SourceClassLoader;
import ch.grengine.source.SourceFactory;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.util.HashMap;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class LoaderTest {

    @Test
    void testConstructAndGetSetSourceClassLoader() {

        // given

        final EngineId engineId1 = new EngineId();
        final EngineId engineId2 = new EngineId();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        final SourceClassLoader classLoader1 = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        final SourceClassLoader classLoader2 = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);

        // when

        final Loader loader = new Loader(engineId1, 17, true, classLoader1);

        // then

        System.out.println(loader);
        assertThat(loader.getNumber(), is(17L));
        assertThat(loader.isAttached(), is(true));
        assertThat(loader.getSourceClassLoader(engineId1), is(classLoader1));
        assertThat(loader.toString().startsWith("Loader[engineId=ch.grengine.engine.EngineId@"), is(true));
        assertThat(loader.toString().endsWith(", number=17, isAttached=true]"), is(true));

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> loader.getSourceClassLoader(engineId2),
                "Engine ID does not match (loader created by a different engine).");
        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> loader.setSourceClassLoader(engineId2, classLoader2),
                "Engine ID does not match (loader created by a different engine).");

        // when

        loader.setSourceClassLoader(engineId1, classLoader2);

        // then

        assertThat(loader.getSourceClassLoader(engineId1), is(classLoader2));

        // when/then

        assertThrowsMessageIs(IllegalArgumentException.class,
                () -> loader.getSourceClassLoader(engineId2),
                "Engine ID does not match (loader created by a different engine).");

        // when

        final Loader detachedLoader = new Loader(engineId1, 17, false, classLoader1);

        // then

        assertThat(detachedLoader.isAttached(), is(false));
        assertThat(detachedLoader.toString().startsWith("Loader[engineId=ch.grengine.engine.EngineId@"), is(true));
        assertThat(detachedLoader.toString().endsWith(", number=17, isAttached=false]"), is(true));
    }
    
    @Test
    void testConstructEngineIdNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        final BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Loader(null, 0, false, classLoader),
                "Engine ID is null.");
    }
    
    @Test
    void testConstructSourceClassLoaderNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new Loader(new EngineId(), 0, false, null),
                "Source class loader is null.");
    }

    @Test
    void testConstructWithClassReleaserAndClose() throws Exception {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, loadMode, code);

        final EngineId engineId = new EngineId();

        final RecordingClassReleaser releaser = new RecordingClassReleaser();

        final Loader loader = new Loader(engineId, 17, true, releaser, classLoader);

        final Class<?> clazz1 = loader.getSourceClassLoader(engineId).loadClass("Class1");
        final Class<?> clazz2 = loader.getSourceClassLoader(engineId).loadClass("Class2");
        clazz2.getConstructor().newInstance();

        // when

        loader.close();

        // then

        assertThat(releaser.classes.contains(clazz1), is(true));
        assertThat(releaser.classes.contains(clazz2), is(true));
        assertThat(releaser.classes.size(), is(3));
        assertThat(releaser.countClassesWithName("Class1"), is(1));
        assertThat(releaser.countClassesWithName("Class2"), is(1));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(1));
    }
    
    @Test
    void testEquals() {

        // given

        final long number = 15;
        final EngineId id = new EngineId();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        final BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        final BytecodeClassLoader classLoader2 = new BytecodeClassLoader(parent.getParent(), LoadMode.PARENT_FIRST, code);

        // when

        final Loader loader = new Loader(id, number, true, classLoader);

        // then

        assertThat(loader.equals(new Loader(id, number, true, classLoader)), is(true));
        assertThat(loader.equals(new Loader(id, number, false, classLoader)), is(true));
        assertThat(loader.equals(new Loader(id, number, true, classLoader2)), is(true));
        assertThat(loader.equals(new Loader(id, 33, true, classLoader)), is(false));
        assertThat(loader.equals(new Loader(new EngineId(), number, true, classLoader)), is(false));
        assertThat(loader.equals("different class"), is(false));
        assertThat(loader.equals(null), is(false));
    }

}
