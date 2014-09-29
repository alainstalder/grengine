/*
   Copyright 2014-now by Alain Stalder. Made in Switzerland.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package ch.grengine.load;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

public class BytecodeClassLoaderTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructAndGetters() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, code);
        
        assertEquals(parent, loader.getParent());
        assertEquals(LoadMode.CURRENT_FIRST, loader.getLoadMode());
        assertEquals(code, loader.getCode());
    }
    
    @Test
    public void testConstructParentNull() throws Exception {
        Code code = new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        try {
            new BytecodeClassLoader(null, LoadMode.CURRENT_FIRST, code);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructLoadModeNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Code code = new DefaultCode("name", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        try {
            new BytecodeClassLoader(parent, null, code);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Load mode is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructCodeNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try {
            new BytecodeClassLoader(parent, LoadMode.CURRENT_FIRST, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Code is null.", e.getMessage());
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
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertNull(loaderFound);
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertEquals(loaderFound, loader);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadMainClass(s2);
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        try {
            clazz = loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        clazz = loader.loadMainClass(s4);
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadClass(s1, "Class1$Sub");
        assertEquals("Class1$Sub", clazz.getName());
        clazz = loader.loadClass(s1, "Side");
        assertEquals("Side", clazz.getName());
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + s1.toString(), e.getMessage());
        }
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            clazz = loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader.loadClass(s4, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadClass("Class1$Sub");
        assertEquals("Class1$Sub", clazz.getName());
        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the Java version of the class was loaded
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
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
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertNull(loaderFound);
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertEquals(loaderFound, loader);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadMainClass(s2);
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        try {
            clazz = loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        clazz = loader.loadMainClass(s4);
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadClass(s1, "Class1$Sub");
        assertEquals("Class1$Sub", clazz.getName());
        clazz = loader.loadClass(s1, "Side");
        assertEquals("Side", clazz.getName());
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + s1.toString(), e.getMessage());
        }
        clazz = loader.loadClass(s4, "org.junit.Assume");
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            clazz = loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadClass("Class1$Sub");
        assertEquals("Class1$Sub", clazz.getName());
        clazz = loader.loadClass("Side");
        assertEquals("Side", clazz.getName());
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
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
        
        BytecodeClassLoader loaderFound = loader.findBytecodeClassLoaderBySource(s1);
        assertEquals(loaderFound, parent);
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertNull(loaderFound);
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertEquals(loaderFound, loader);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadMainClass(s2);
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        try {
            clazz = loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        clazz = loader.loadMainClass(s4);
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertEquals("Class1", clazz.getName());
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + s1.toString(), e.getMessage());
        }
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            clazz = loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader.loadClass(s4, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the Java version of the class was loaded
        clazz.getDeclaredMethod("assumeNoException", new Class<?>[] { Throwable.class });
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
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s2);
        assertEquals(loaderFound, loader);
        loaderFound = loader.findBytecodeClassLoaderBySource(s3);
        assertNull(loaderFound);
        loaderFound = loader.findBytecodeClassLoaderBySource(s4);
        assertEquals(loaderFound, loader);

        // -- loadMainClass(source) --
        
        Class<?> clazz = loader.loadMainClass(s1);
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadMainClass(s2);
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        try {
            clazz = loader.loadMainClass(s3);
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        clazz = loader.loadMainClass(s4);
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(source, name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass(s1, "Class1");
        assertEquals("Class1", clazz.getName());
        // wrong source, not found
        try {
            loader.loadClass(s1, "org.junit.Assume");
        } catch (LoadException e) {
            assertEquals("Class 'org.junit.Assume' not found for source. Source: " + s1.toString(), e.getMessage());
        }
        
        clazz = loader.loadClass(s2, "ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());

        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        try {
            clazz = loader.loadClass(s3, "Class1");
            fail();
        } catch (LoadException e) {
            assertEquals("Source not found: " + s3.toString(), e.getMessage());
        }
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);

        clazz = loader.loadClass(s4, "org.junit.Assume");
        assertEquals("org.junit.Assume", clazz.getName());
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
        
        // -- loadClass(name) --
        
        // new loader instance, else already loaded classes cannot be loaded differently
        loader = new BytecodeClassLoader(parent, loadMode, code);
        
        clazz = loader.loadClass("Class1");
        assertEquals("Class1", clazz.getName());
        clazz = loader.loadClass("ch.grengine.test.Class2");
        assertEquals("ch.grengine.test.Class2", clazz.getName());
        clazz = loader.loadClass("org.junit.Assume");
        // make sure the groovy version of the class was loaded
        clazz.getDeclaredMethod("marker12345", new Class<?>[0]);
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
            assertEquals("Source not found: " + s1.toString(), e.getMessage());
        }
        
        // fabricate inconsistent code
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);
        
        Set<String> classNames1 = new HashSet<String>();
        classNames1.add("Class1");
        CompiledSourceInfo info = new CompiledSourceInfo(s1, "Class1", classNames1, 0);
        Map<Source,CompiledSourceInfo> infos = new HashMap<Source,CompiledSourceInfo>();
        infos.put(s1, info);
        Map<String,Bytecode> bytecodesEmpty = new HashMap<String,Bytecode>();
        Code inconsistentCode = new DefaultCode(code.getSourcesName(), infos, bytecodesEmpty);
        
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, inconsistentCode);

        try {
            BytecodeClassLoader.loadMainClassBySource(loader, s1);
            fail();
        } catch (LoadException e) {
            //System.out.println(e);
            assertEquals("Inconsistent code: " + inconsistentCode + "." +
                    " Main class 'Class1' not found for source. Source: " + s1.toString(), e.getMessage());
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
            assertEquals("Source not found: " + s1.toString(), e.getMessage());
        }
        
        // fabricate inconsistent code
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);
        
        Set<String> classNames1 = new HashSet<String>();
        classNames1.add("Class33NoBytecode");
        CompiledSourceInfo info = new CompiledSourceInfo(s1, "Class1", classNames1, 0);
        Map<Source,CompiledSourceInfo> infos = new HashMap<Source,CompiledSourceInfo>();
        infos.put(s1, info);
        Map<String,Bytecode> bytecodesEmpty = new HashMap<String,Bytecode>();
        Code inconsistentCode = new DefaultCode(code.getSourcesName(), infos, bytecodesEmpty);
        
        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, inconsistentCode);

        try {BytecodeClassLoader.loadClassBySourceAndName(loader, s1, "Class33NoBytecode");
        BytecodeClassLoader.loadMainClassBySource(parent, s1);
            fail();
        } catch (LoadException e) {
            //System.out.println(e);
            assertEquals("Inconsistent code: " + inconsistentCode + "." +
                    " Class 'Class33NoBytecode' not found for source. Source: " + s1.toString(), e.getMessage());
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
        
        assertTrue(clone != loader);
        assertEquals(loader.getParent(), clone.getParent());
        assertEquals(loader.getLoadMode(), clone.getLoadMode());
        assertEquals(loader.getCode(), clone.getCode());
    }
    
    
    private class ThrowingBytecodeClassLoader extends BytecodeClassLoader {
        
        public ThrowingBytecodeClassLoader(BytecodeClassLoader loader) {
            super(loader.getParent(), loader.getLoadMode(), loader.getCode());
        }
        
        void definePackage(String name) {
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
            assertEquals("unit test", e.getMessage());
        }
    }

}
