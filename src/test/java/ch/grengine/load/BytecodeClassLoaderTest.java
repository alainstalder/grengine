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
import ch.grengine.code.Bytecode;
import ch.grengine.code.Code;
import ch.grengine.code.CompiledSourceInfo;
import ch.grengine.code.DefaultCode;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.except.LoadException;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.createTestDir;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BytecodeClassLoaderTest {
    
    @Test
    void testConstructAndGetters() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());

        // when

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);

        // then

        assertThat(loader.getParent(), is(parent));
        assertThat(loader.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getCode(), is(code));
    }
    
    @Test
    void testConstructParentNull() {

        // given

        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());

        // when/then

        assertThrows(NullPointerException.class,
                () -> new BytecodeClassLoader(null, LoadMode.CURRENT_FIRST, code),
                "Parent class loader is null.");
    }
    
    @Test
    void testConstructLoadModeNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());

        // when/then

        assertThrows(NullPointerException.class,
                () -> new BytecodeClassLoader(parent, null, code),
                "Load mode is null.");
    }
    
    @Test
    void testConstructCodeNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        // when/then

        assertThrows(NullPointerException.class,
                () -> new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, null),
                "Code is null.");
    }


    private void testParentRegularClassLoader(final LoadMode loadMode) throws Exception {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {\n" +
                "static class Sub {} }\n" +
                "class Side {}");
        final Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        final Source s3 = f.fromText("class Class3 {}");
        final Source s4 = f.fromText("package groovy.util\nclass Expando { def marker12345() { return 1 } }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader1 = new BytecodeClassLoader(parent, loadMode, code);

        // when/then (findBytecodeClassLoaderBySource(source))

        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(s1);
        assertThat(loader1, is(loaderFound));
        loaderFound = loader1.findBytecodeClassLoaderBySource(s2);
        assertThat(loader1, is(loaderFound));
        loaderFound = loader1.findBytecodeClassLoaderBySource(s3);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader1.findBytecodeClassLoaderBySource(s4);
        assertThat(loader1, is(loaderFound));

        // when/then (loadMainClass(source))

        Class<?> clazz = loader1.loadMainClass(s1);
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader1.loadMainClass(s2);
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        assertThrows(LoadException.class,
                () -> loader1.loadMainClass(s3),
                "Source not found: " + s3.toString());
        clazz = loader1.loadMainClass(s4);
        assertThat(clazz.getName(), is("groovy.util.Expando"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");

        // when/then (loadClass(source, name))

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader2 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader2.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader2.loadClass(s1, "Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader2.loadClass(s1, "Side");
        assertThat(clazz.getName(), is("Side"));
        // wrong source, not found
        assertThrows(LoadException.class,
                () -> loader2.loadClass(s1, "groovy.util.Expando"),
                "Class 'groovy.util.Expando' not found for source. Source: " + s1.toString());
        clazz = loader2.loadClass(s4, "groovy.util.Expando");
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");

        clazz = loader2.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader3 = new BytecodeClassLoader(parent, loadMode, code);

        assertThrows(LoadException.class,
                () -> loader3.loadClass(s3, "Class1"),
                "Source not found: " + s3.toString());

        // when/then (loadClass(name))

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader4 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader4.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader4.loadClass("Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader4.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz = loader4.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader4.loadClass("groovy.util.Expando");

        if (loadMode == LoadMode.PARENT_FIRST) {
            // make sure the Java version of the class was loaded
            clazz.getDeclaredMethod("createMap");
        } else {
            // make sure the Groovy version of the class was loaded
            clazz.getDeclaredMethod("marker12345");
        }
    }

    @Test
    void testParentRegularClassLoaderParentFirst() throws Exception {
        testParentRegularClassLoader(LoadMode.PARENT_FIRST);
    }

    @Test
    void testParentRegularClassLoaderCurrentFirst() throws Exception {
        testParentRegularClassLoader(LoadMode.CURRENT_FIRST);
    }


    private void testParentSourceClassLoader(final LoadMode loadMode) throws Exception {

        // given

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final File dir = createTestDir();
        final File f1 = new File(dir, "Class1.groovy");
        TestUtil.setFileText(f1, "class Class1 { def marker11() { return 11 } }");
        final Source s1 = f.fromFile(f1);
        final Set<Source> sourceSetParent = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sourcesParent = SourcesUtil.sourceSetToSources(sourceSetParent, "testParent");
        final Code codeParent = c.compile(sourcesParent);
        final ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(),
                loadMode, codeParent);

        TestUtil.setFileText(f1, "class Class1 { def marker22() { return 22 } }");
        final Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        final Source s3 = f.fromText("class Class3 {}");
        final Source s4 = f.fromText("package groovy.util\nclass Expando { def marker12345() { return 1 } }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader1 = new BytecodeClassLoader(parent, loadMode, code);

        // when/then (findBytecodeClassLoaderBySource(source))

        BytecodeClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(s1);
        if (loadMode == LoadMode.PARENT_FIRST) {
            assertThat(parent, is(loaderFound));
        } else {
            assertThat(loader1, is(loaderFound));
        }

        loaderFound = loader1.findBytecodeClassLoaderBySource(s2);
        assertThat(loader1, is(loaderFound));
        loaderFound = loader1.findBytecodeClassLoaderBySource(s3);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader1.findBytecodeClassLoaderBySource(s4);
        assertThat(loader1, is(loaderFound));

        // when/then (loadMainClass(source))

        Class<?> clazz = loader1.loadMainClass(s1);
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader1.loadMainClass(s2);
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        assertThrows(LoadException.class,
                () -> loader1.loadMainClass(s3),
                "Source not found: " + s3.toString());
        clazz = loader1.loadMainClass(s4);
        assertThat(clazz.getName(), is("groovy.util.Expando"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");

        // when/then (loadClass(source, name))

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader2 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader2.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        // wrong source, not found
        assertThrows(LoadException.class,
                () -> loader2.loadClass(s1, "groovy.util.Expando"),
                "Class 'groovy.util.Expando' not found for source. Source: " + s1.toString());
        clazz = loader2.loadClass(s4, "groovy.util.Expando");
        assertThat(clazz.getName(), is("groovy.util.Expando"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");

        clazz = loader2.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader3 = new BytecodeClassLoader(parent, loadMode, code);

        assertThrows(LoadException.class,
                () -> loader3.loadClass(s3, "Class1"),
                "Source not found: " + s3.toString());

        // when/then (loadClass(name))

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader4 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader4.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader4.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        clazz = loader4.loadClass("groovy.util.Expando");
        if (loadMode == LoadMode.PARENT_FIRST) {
            // make sure the Java version of the class was loaded
            clazz.getDeclaredMethod("createMap");
        } else {
            // make sure the Groovy version of the class was loaded
            clazz.getDeclaredMethod("marker12345");
        }
    }

    @Test
    void testParentSourceClassLoaderParentFirst() throws Exception {
        testParentSourceClassLoader(LoadMode.PARENT_FIRST);
    }
    
    
    @Test
    void testParentIsSourceClassLoaderCurrentFirst() throws Exception {
        testParentSourceClassLoader(LoadMode.CURRENT_FIRST);
    }


    @Test
    void testLoadClassWithResolveCurrentFirst() throws Exception {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);

        // when/then (load class with resolve (protected method))

        loader.loadClass("Class1", true);
    }

    @Test
    void testStaticLoadMainClassBySource() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");

        // when/then (case of loading where parent class loader is not a SourceClassLoader)

        assertThrows(LoadException.class,
                () -> BytecodeClassLoader.loadMainClassBySource(parent, s1),
                "Source not found: " + s1.toString());

        // when/then (fabricate inconsistent code)

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final Set<String> classNames1 = new HashSet<>();
        classNames1.add("Class1");
        final CompiledSourceInfo info = new CompiledSourceInfo(s1, "Class1", classNames1, 0);
        final Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(s1, info);
        final Map<String,Bytecode> bytecodeMapEmpty = new HashMap<>();
        final Code inconsistentCode = new DefaultCode(code.getSourcesName(), infoMap, bytecodeMapEmpty);

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, inconsistentCode);

        assertThrows(LoadException.class,
                () -> BytecodeClassLoader.loadMainClassBySource(loader, s1),
                "Inconsistent code: " + inconsistentCode + "." +
                        " Main class 'Class1' not found for source. Source: " + s1.toString());
    }
    
    @Test
    void testStaticLoadMainBySourceAndName() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");

        // when/then (case of loading where parent class loader is not a SourceClassLoader)

        assertThrows(LoadException.class,
                () -> BytecodeClassLoader.loadClassBySourceAndName(parent, s1, "Class1"),
                "Source not found: " + s1.toString());

        // when/then (fabricate inconsistent code)

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final Set<String> classNames1 = new HashSet<>();
        classNames1.add("Class33NoBytecode");
        final CompiledSourceInfo info = new CompiledSourceInfo(s1, "Class1", classNames1, 0);
        final Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(s1, info);
        final Map<String,Bytecode> bytecodeMapEmpty = new HashMap<>();
        final Code inconsistentCode = new DefaultCode(code.getSourcesName(), infoMap, bytecodeMapEmpty);

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, inconsistentCode);

        assertThrows(LoadException.class,
                () -> BytecodeClassLoader.loadClassBySourceAndName(loader, s1, "Class33NoBytecode"),
                "Inconsistent code: " + inconsistentCode + "." +
                        " Class 'Class33NoBytecode' not found for source. Source: " + s1.toString());
    }
    
    @Test
    void testClone() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);

        // when

        final BytecodeClassLoader clone = loader.clone();

        // then

        assertThat(clone, not(sameInstance(loader)));
        assertThat(clone.getParent(), is(loader.getParent()));
        assertThat(clone.getLoadMode(), is(loader.getLoadMode()));
        assertThat(clone.getCode(), is(loader.getCode()));
    }

    @Test
    void testReleaseClasses() throws Exception {
        testReleaseClasses(false);
    }

    @Test
    void testReleaseClassesThrows() throws Exception {
        testReleaseClasses(true);
    }

    private void testReleaseClasses(boolean throwAfterReleasing) throws Exception {

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

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);

        final Class<?> clazz1 = loader.loadClass("Class1");
        final Class<?> clazz2 = loader.loadClass("Class2");
        clazz2.getConstructor().newInstance();

        final RecordingClassReleaser releaser = new RecordingClassReleaser();
        releaser.throwAfterReleasing = throwAfterReleasing;

        // when

        loader.releaseClasses(releaser);

        // then

        assertThat(releaser.classes.contains(clazz1), is(true));
        assertThat(releaser.classes.contains(clazz2), is(true));
        assertThat(releaser.classes.size(), is(3));
        assertThat(releaser.countClassesWithName("Class1"), is(1));
        assertThat(releaser.countClassesWithName("Class2"), is(1));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(1));
    }

    private static class ThrowingBytecodeClassLoader extends BytecodeClassLoader {
        
        ThrowingBytecodeClassLoader(BytecodeClassLoader loader) {
            super(loader.getParent(), loader.getLoadMode(), loader.getCode());
        }

        Class<?> defineClass(String name, byte[] bytes) {
            throw new RuntimeException("unit test");
        }
        
    }
    
    @Test
    void testThrowsInSynchronizedBlock() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final ThrowingBytecodeClassLoader throwingLoader = new ThrowingBytecodeClassLoader(loader);

        // when/then

        assertThrows(RuntimeException.class,
                () -> throwingLoader.loadClass("Class1"),
                "unit test");
    }

}
