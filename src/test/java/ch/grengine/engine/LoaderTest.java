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

import ch.grengine.code.Code;
import ch.grengine.code.DefaultCode;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.load.BytecodeClassLoader;
import ch.grengine.load.LoadMode;
import ch.grengine.load.RecordingClassReleaser;
import ch.grengine.load.SourceClassLoader;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.util.HashMap;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class LoaderTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructAndGetSetSourceClassLoader() throws Exception {

        EngineId engineId1 = new EngineId();
        EngineId engineId2 = new EngineId();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        SourceClassLoader classLoader1 = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        SourceClassLoader classLoader2 = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);

        Loader loader = new Loader(engineId1, 17, true, classLoader1);

        assertThat(loader.getNumber(), is(17L));
        assertThat(loader.isAttached(), is(true));

        assertThat(loader.getSourceClassLoader(engineId1), is(classLoader1));
        try {
            loader.getSourceClassLoader(engineId2);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Engine ID does not match (loader created by a different engine)."));
        }

        try {
            loader.setSourceClassLoader(engineId2, classLoader2);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Engine ID does not match (loader created by a different engine)."));
        }
        loader.setSourceClassLoader(engineId1, classLoader2);
        assertThat(loader.getSourceClassLoader(engineId1), is(classLoader2));
        try {
            loader.getSourceClassLoader(engineId2);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Engine ID does not match (loader created by a different engine)."));
        }

        System.out.println(loader);
        assertThat(loader.toString().startsWith("Loader[engineId=ch.grengine.engine.EngineId@"), is(true));
        assertThat(loader.toString().endsWith(", number=17, isAttached=true]"), is(true));
        Loader detachedLoader = new Loader(engineId1, 17, false, classLoader1);
        assertThat(detachedLoader.isAttached(), is(false));
        assertThat(detachedLoader.toString().startsWith("Loader[engineId=ch.grengine.engine.EngineId@"), is(true));
        assertThat(detachedLoader.toString().endsWith(", number=17, isAttached=false]"), is(true));
    }
    
    @Test
    public void testConstructEngineIdNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        try {
            new Loader(null, 0, false, classLoader);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Engine ID is null."));
        }
    }
    
    @Test
    public void testConstructSourceClassLoaderNull() throws Exception {
        try {
            new Loader(new EngineId(), 0, false, null);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Source class loader is null."));
        }
    }

    @Test
    public void testConstructWithClassReleaserAndClose() throws Exception {

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, loadMode, code);

        EngineId engineId = new EngineId();

        RecordingClassReleaser releaser = new RecordingClassReleaser();

        Loader loader = new Loader(engineId, 17, true, releaser, classLoader);

        Class<?> clazz1 = loader.getSourceClassLoader(engineId).loadClass("Class1");
        Class<?> clazz2 = loader.getSourceClassLoader(engineId).loadClass("Class2");
        clazz2.newInstance();

        loader.close();

        assertThat(releaser.classes.contains(clazz1), is(true));
        assertThat(releaser.classes.contains(clazz2), is(true));
        assertThat(releaser.classes.size(), is(3));
        assertThat(releaser.countClassesWithName("Class1"), is(1));
        assertThat(releaser.countClassesWithName("Class2"), is(1));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(1));
    }
    
    @Test
    public void testEquals() {
        long number = 15;
        EngineId id = new EngineId();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        BytecodeClassLoader classLoader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        BytecodeClassLoader classLoader2 = new BytecodeClassLoader(parent.getParent(), LoadMode.PARENT_FIRST, code);
        
        Loader loader = new Loader(id, number, true, classLoader);
        assertThat(loader.equals(new Loader(id, number, true, classLoader)), is(true));
        assertThat(loader.equals(new Loader(id, number, false, classLoader)), is(true));
        assertThat(loader.equals(new Loader(id, number, true, classLoader2)), is(true));
        assertThat(loader.equals(new Loader(id, 33, true, classLoader)), is(false));
        assertThat(loader.equals(new Loader(new EngineId(), number, true, classLoader)), is(false));
        assertThat(loader.equals("different class"), is(false));
        assertThat(loader.equals(null), is(false));
    }

}
