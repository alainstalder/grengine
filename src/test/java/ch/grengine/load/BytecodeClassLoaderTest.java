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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class BytecodeClassLoaderTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructAndGetters() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);

        assertThat(loader.getParent(), is(parent));
        assertThat(loader.getLoadMode(), is(LoadMode.CURRENT_FIRST));
        assertThat(loader.getCode(), is(code));
    }
    
    @Test
    public void testConstructParentNull() throws Exception {
        Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        try {
            new BytecodeClassLoader(null, LoadMode.CURRENT_FIRST, code);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }
    
    @Test
    public void testConstructLoadModeNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<>(),
                new HashMap<>());
        try {
            new BytecodeClassLoader(parent, null, code);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Load mode is null."));
        }
    }
    
    @Test
    public void testConstructCodeNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try {
            new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Code is null."));
        }
    }

    
    @Test
    public void testParentNotSourceClassLoaderParentFirst() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        LoadMode loadMode = LoadMode.PARENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {\n" +
                "static class Sub {} }\n" +
                "class Side {}");
        Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        Source s3 = f.fromText("class Class3 {}");
        Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(s1);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertThat(loader, is(loaderFound));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadMainClass(s2);
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        try {
            loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        clazz = loader.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadClass(s1, "Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader.loadClass(s1, "Side");
        assertThat(clazz.getName(), is("Side"));
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + s1.toString()));
        }
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader.loadClass(s4, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345" );
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadClass("Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the Java version of the class was loaded
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
    }

    
    @Test
    public void testParentNotSourceClassLoaderCurrentFirst() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {\n" +
                "static class Sub {} }\n" +
                "class Side {}");
        Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        Source s3 = f.fromText("class Class3 {}");
        Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(s1);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertThat(loader, is(loaderFound));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadMainClass(s2);
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        try {
            loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        clazz = loader.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadClass(s1, "Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader.loadClass(s1, "Side");
        assertThat(clazz.getName(), is("Side"));
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + s1.toString()));
        }
        clazz = loader.loadClass(s4, "org.junit.Assume");
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadClass("Class1$Sub");
        assertThat(clazz.getName(), is("Class1$Sub"));
        clazz = loader.loadClass("Side");
        assertThat(clazz.getName(), is("Side"));
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
    }

    
    @Test
    public void testParentIsSourceClassLoaderParentFirst() throws Exception {
        
        LoadMode loadMode = LoadMode.PARENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        File f1 = new File(tempFolder.getRoot(), "Class1.groovy");
        TestUtil.setFileText(f1, "class Class1 { def marker11() { return 11 } }");
        Source s1 = f.fromFile(f1);
        Set<Source> sourceSetParent = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sourcesParent = SourcesUtil.sourceSetToSources(sourceSetParent, "testParent");
        Code codeParent = c.compile(sourcesParent);
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                loadMode, codeParent);
        
        TestUtil.setFileText(f1, "class Class1 { def marker22() { return 22 } }");
        Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        Source s3 = f.fromText("class Class3 {}");
        Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        ClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(s1);
        assertThat(parent, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertThat(loader, is(loaderFound));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadMainClass(s2);
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        try {
            loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        clazz = loader.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + s1.toString()));
        }
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader.loadClass(s4, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the Java version of the class was loaded
        clazz.getDeclaredMethod("assumeNoException", Throwable.class);
    }
    
    
    @Test
    public void testParentIsSourceClassLoaderCurrentFirst() throws Exception {
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        File f1 = new File(tempFolder.getRoot(), "Class1.groovy");
        TestUtil.setFileText(f1, "class Class1 { def marker11() { return 11 } }");
        Source s1 = f.fromFile(f1);
        Set<Source> sourceSetParent = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sourcesParent = SourcesUtil.sourceSetToSources(sourceSetParent, "testParent");
        Code codeParent = c.compile(sourcesParent);
        ClassLoader parent = new BytecodeClassLoader(Thread.currentThread().getContextClassLoader(), 
                loadMode, codeParent);
        
        TestUtil.setFileText(f1, "class Class1 { def marker22() { return 22 } }");
        Source s2 = f.fromText("package ch.grengine.test\nclass Class2 {}");
        Source s3 = f.fromText("class Class3 {}");
        Source s4 = f.fromText("package org.junit\nclass Assume { def marker12345() { return 1 } }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2, s4); // not s3
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        
        // -- findBytecodeClassLoaderBySource(source) --
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(s1);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertThat(loader, is(loaderFound));
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertThat(loaderFound, is(nullValue()));
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertThat(loader, is(loaderFound));

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadMainClass(s2);
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        try {
            loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        clazz = loader.loadMainClass(s4);
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertThat(clazz.getName(), is("Class1"));
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Class 'org.junit.Assume' not found for source. Source: " + s1.toString()));
        }
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s3.toString()));
        }
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader.loadClass(s4, "org.junit.Assume");
        assertThat(clazz.getName(), is("org.junit.Assume"));
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertThat(clazz.getName(), is("Class1"));
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertThat(clazz.getName(), is("ch.grengine.test.Class2"));
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345");
    }
    
    @Test
    public void testLoadClassWithResolveCurrentFirst() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        
        // load class with resolve (protected method)
        loader.loadClass("Class1", true);
    }

    @Test
    public void testStaticLoadMainClassBySource() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");

        // case of loading where parent class loader is not a SourceClassLoader
        
        try {
            BytecodeClassLoader.loadMainClassBySource(parent, s1);
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s1.toString()));
        }
        
        // fabricate inconsistent code
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);
        
        Set<String> classNames1 = new HashSet<>();
        classNames1.add("Class1");
        CompiledSourceInfo info = new CompiledSourceInfo(s1, "Class1", classNames1, 0);
        Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(s1, info);
        Map<String,Bytecode> bytecodeMapEmpty = new HashMap<>();
        Code inconsistentCode = new DefaultCode(code.getSourcesName(), infoMap, bytecodeMapEmpty);
        
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, inconsistentCode);

        try {
            BytecodeClassLoader.loadMainClassBySource(loader, s1);
            fail();
        } catch (LoadException e) {
            //System.out.println(e);
            assertThat(e.getMessage(), is("Inconsistent code: " + inconsistentCode + "." +
                    " Main class 'Class1' not found for source. Source: " + s1.toString()));
        }
    }
    
    @Test
    public void testStaticLoadMainBySourceAndName() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");

        // case of loading where parent class loader is not a SourceClassLoader
        
        try {
            BytecodeClassLoader.loadClassBySourceAndName(parent, s1, "Class1");
            fail();
        } catch (LoadException e) {
            assertThat(e.getMessage(), is("Source not found: " + s1.toString()));
        }
        
        // fabricate inconsistent code
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);
        
        Set<String> classNames1 = new HashSet<>();
        classNames1.add("Class33NoBytecode");
        CompiledSourceInfo info = new CompiledSourceInfo(s1, "Class1", classNames1, 0);
        Map<Source,CompiledSourceInfo> infoMap = new HashMap<>();
        infoMap.put(s1, info);
        Map<String,Bytecode> bytecodeMapEmpty = new HashMap<>();
        Code inconsistentCode = new DefaultCode(code.getSourcesName(), infoMap, bytecodeMapEmpty);
        
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, inconsistentCode);

        try {BytecodeClassLoader.loadClassBySourceAndName(loader, s1, "Class33NoBytecode");
        BytecodeClassLoader.loadMainClassBySource(parent, s1);
            fail();
        } catch (LoadException e) {
            //System.out.println(e);
            assertThat(e.getMessage(), is("Inconsistent code: " + inconsistentCode + "." +
                    " Class 'Class33NoBytecode' not found for source. Source: " + s1.toString()));
        }

    }
    
    @Test
    public void testClone() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        
        BytecodeClassLoader clone = loader.clone();

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
        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 { Class2() { new Class3() }; static class Class3 {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);

        Class<?> clazz1 = loader.loadClass("Class1");
        Class<?> clazz2 = loader.loadClass("Class2");
        clazz2.newInstance();

        RecordingClassReleaser releaser = new RecordingClassReleaser();
        releaser.throwAfterReleasing = throwAfterReleasing;
        loader.releaseClasses(releaser);

        assertThat(releaser.classes.contains(clazz1), is(true));
        assertThat(releaser.classes.contains(clazz2), is(true));
        assertThat(releaser.classes.size(), is(3));
        assertThat(releaser.countClassesWithName("Class1"), is(1));
        assertThat(releaser.countClassesWithName("Class2"), is(1));
        assertThat(releaser.countClassesWithName("Class2$Class3"), is(1));
    }

    private class ThrowingBytecodeClassLoader extends BytecodeClassLoader {
        
        public ThrowingBytecodeClassLoader(BytecodeClassLoader loader) {
            super(loader.getParent(), loader.getLoadMode(), loader.getCode());
        }

        Class<?> defineClass(String name, byte[] bytes) {
            throw new RuntimeException("unit test");
        }
        
    }
    
    @Test
    public void testThrowsInSynchronizedBlock() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final ThrowingBytecodeClassLoader throwingLoader = new ThrowingBytecodeClassLoader(loader);

        try {
            throwingLoader.loadClass("Class1");
            fail();
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("unit test"));
        }
    }

}
