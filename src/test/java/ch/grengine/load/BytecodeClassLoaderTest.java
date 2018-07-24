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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class BytecodeClassLoaderTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructAndGetters() {

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
    public void testConstructParentNull() {

        // given

        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());

        // when/then

        assertThrows(() -> new BytecodeClassLoader(null, LoadMode.CURRENT_FIRST, code),
                NullPointerException.class,
                "Parent class loader is null.");
    }
    
    @Test
    public void testConstructLoadModeNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());

        // when/then

        assertThrows(() -> new BytecodeClassLoader(parent, null, code),
                NullPointerException.class,
                "Load mode is null.");
    }
    
    @Test
    public void testConstructCodeNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        // when/then

        assertThrows(() -> new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, null),
                NullPointerException.class,
                "Code is null.");
    }

    
    @Test
    public void testParentNotSourceClassLoaderParentFirst() throws Exception {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.PARENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {\n" +
                "static class Sub {} }\n" +
                "class Side {}");
        final Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        final Source s3 = f.fromText("class Class3 {}");
        final Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
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
        assertThrows(() -> loader1.loadMainClass(s3),
                LoadException.class,
                "Source not found: " + s3.toString());
        clazz = loader1.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
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
        assertThrows(() -> loader2.loadClass(s1, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + s1.toString());
        clazz = loader2.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader3 = new BytecodeClassLoader(parent, loadMode, code);

        assertThrows(() -> loader3.loadClass(s3, "Class1"),
                LoadException.class,
                "Source not found: " + s3.toString());

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader4 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader4.loadClass(s4, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345" );
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader5 = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader5.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader5.loadClass("Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader5.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz = loader5.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader5.loadClass("org.junit.Assume");
        // make sure the Java version of the class was loaded
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
    }

    
    @Test
    public void testParentNotSourceClassLoaderCurrentFirst() throws Exception {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {\n" +
                "static class Sub {} }\n" +
                "class Side {}");
        final Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        final Source s3 = f.fromText("class Class3 {}");
        final Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
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
        assertThrows(() -> loader1.loadMainClass(s3),
                LoadException.class,
                "Source not found: " + s3.toString());
        clazz = loader1.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
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
        assertThrows(() -> loader2.loadClass(s1, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + s1.toString());
        clazz = loader2.loadClass(s4, "org.junit.Assume");
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        clazz = loader2.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader3 = new BytecodeClassLoader(parent, loadMode, code);

        assertThrows(() -> loader3.loadClass(s3, "Class1"),
                LoadException.class,
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
        clazz = loader4.loadClass("org.junit.Assume");
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
    }

    
    @Test
    public void testParentIsSourceClassLoaderParentFirst() throws Exception {

        // given

        final LoadMode loadMode = LoadMode.PARENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final File f1 = new File(tempFolder.getRoot(), "Class1.groovy");
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
        final Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader1 = new BytecodeClassLoader(parent, loadMode, code);
        
        // when/then (findBytecodeClassLoaderBySource(source))
        
        ClassLoader loaderFound = loader1.findBytecodeClassLoaderBySource(s1);
        assertThat(parent, is(loaderFound));
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
        assertThrows(() -> loader1.loadMainClass(s3),
                LoadException.class,
                "Source not found: " + s3.toString());
        clazz = loader1.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader2 = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader2.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        // wrong source, not found
        assertThrows(() -> loader2.loadClass(s1, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + s1.toString());

        clazz = loader2.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader3 = new BytecodeClassLoader(parent, loadMode, code);

        assertThrows(() -> loader3.loadClass(s3, "Class1"),
                LoadException.class,
                "Source not found: " + s3.toString());

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader4 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader4.loadClass(s4, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader5 = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader5.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader5.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader5.loadClass("org.junit.Assume");
        // make sure the Java version of the class was loaded
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
    }
    
    
    @Test
    public void testParentIsSourceClassLoaderCurrentFirst() throws Exception {

        // given

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();
        final File f1 = new File(tempFolder.getRoot(), "Class1.groovy");
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
        final Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
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
        assertThrows(() -> loader1.loadMainClass(s3),
                LoadException.class,
                "Source not found: " + s3.toString());
        clazz = loader1.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // when/then (loadClass(source, name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader2 = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader2.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        // wrong source, not found
        assertThrows(() -> loader2.loadClass(s1, "org.junit.Assume"),
                LoadException.class,
                "Class 'org.junit.Assume' not found for source. Source: " + s1.toString());

        clazz = loader2.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader3 = new BytecodeClassLoader(parent, loadMode, code);

        assertThrows(() -> loader3.loadClass(s3, "Class1"),
                LoadException.class,
                "Source not found: " + s3.toString());

        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader4 = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader4.loadClass(s4, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // when/then (loadClass(name))
        
        // new loader instance, else already loaded classes cannot be loaded differently
        final BytecodeClassLoader loader5 = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader5.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader5.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader5.loadClass("org.junit.Assume");
        // make sure the Groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
    }
    
    @Test
    public void testLoadClassWithResolveCurrentFirst() throws Exception {

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
    public void testStaticLoadMainClassBySource() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");

        // when/then (case of loading where parent class loader is not a SourceClassLoader)

        assertThrows(() -> BytecodeClassLoader.loadMainClassBySource(parent, s1),
                LoadException.class,
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

        assertThrows(() -> BytecodeClassLoader.loadMainClassBySource(loader, s1),
                LoadException.class,
                "Inconsistent code: " + inconsistentCode + "." +
                        " Main class 'Class1' not found for source. Source: " + s1.toString());
    }
    
    @Test
    public void testStaticLoadMainBySourceAndName() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("class Class1 {}");

        // when/then (case of loading where parent class loader is not a SourceClassLoader)

        assertThrows(() -> BytecodeClassLoader.loadClassBySourceAndName(parent, s1, "Class1"),
                LoadException.class,
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

        assertThrows(() -> BytecodeClassLoader.loadClassBySourceAndName(loader, s1, "Class33NoBytecode"),
                LoadException.class,
                "Inconsistent code: " + inconsistentCode + "." +
                        " Class 'Class33NoBytecode' not found for source. Source: " + s1.toString());
    }
    
    @Test
    public void testClone() {

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
    public void testReleaseClasses() throws Exception {
        testReleaseClasses(false);
    }

    @Test
    public void testReleaseClassesThrows() throws Exception {
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
    public void testThrowsInSynchronizedBlock() {

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

        assertThrows(() -> throwingLoader.loadClass("Class1"),
                RuntimeException.class,
                "unit test");
    }

}
