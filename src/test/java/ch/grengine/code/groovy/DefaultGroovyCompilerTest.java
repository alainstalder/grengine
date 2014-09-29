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

package ch.grengine.code.groovy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.DefaultCode;
import ch.grengine.code.DefaultSingleSourceCode;
import ch.grengine.except.CompileException;
import ch.grengine.load.BytecodeClassLoader;
import ch.grengine.load.LoadMode;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.MockSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;


public class DefaultGroovyCompilerTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructDefaults() throws Exception {
        DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        DefaultGroovyCompiler c = builder.build();
        
        assertEquals(builder, c.getBuilder());
        assertEquals(Thread.currentThread().getContextClassLoader(), c.getParent());
        assertEquals(c.getBuilder().getParent(), c.getParent());
        assertNotNull(c.getCompilerConfiguration());
        assertEquals(c.getBuilder().getCompilerConfiguration(), c.getCompilerConfiguration());
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        CompilerConfiguration config = new CompilerConfiguration();
        builder.setParent(parent);
        builder.setCompilerConfiguration(config);
        DefaultGroovyCompiler c = builder.build();
        
        assertEquals(builder, c.getBuilder());
        assertEquals(parent, c.getParent());
        assertEquals(c.getBuilder().getParent(), c.getParent());
        assertEquals(config, c.getCompilerConfiguration());
        assertEquals(c.getBuilder().getCompilerConfiguration(), c.getCompilerConfiguration());
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        builder.build();
        try {
            builder.setParent(Thread.currentThread().getContextClassLoader());
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }

    @Test
    public void testConstructorNoArgs() {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        assertEquals(Thread.currentThread().getContextClassLoader(), c.getParent());
        assertNotNull(c.getCompilerConfiguration());
    }
    
    @Test
    public void testConstructorFromParent() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);
        assertEquals(parent, c.getParent());
        assertNotNull(c.getCompilerConfiguration());
    }
    
    @Test
    public void testConstructorFromParentAndConfig() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        CompilerConfiguration config = new CompilerConfiguration();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent, config);
        assertEquals(parent, c.getParent());
        assertEquals(config, c.getCompilerConfiguration());
    }
    
    @Test
    public void testConstructFromParentNull() throws Exception {
        try {
            new DefaultGroovyCompiler((ClassLoader)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructFromParentAndConfigParentNull() throws Exception {
        try {
            new DefaultGroovyCompiler(null, new CompilerConfiguration());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructFromParentAndConfigConfigNull() throws Exception {
        try {
            new DefaultGroovyCompiler(Thread.currentThread().getContextClassLoader(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }
    
    
    @Test
    public void testCompileBasic() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        SourceFactory f = new DefaultSourceFactory();
        Source textSource = f.fromText("println 'text source'");
        String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        Source textSourceWithName = f.fromText("println 'text source'", "MyTextScript");
        File scriptFile = new File(tempFolder.getRoot(), "MyFileScript.groovy");
        TestUtil.setFileText(scriptFile, "println 'file source'");
        Source fileSource = f.fromFile(scriptFile);
        File urlScriptFile = new File(tempFolder.getRoot(), "MyUrlScript.groovy");
        TestUtil.setFileText(urlScriptFile, "println 'url source'");
        Source urlSource = f.fromUrl(urlScriptFile.toURI().toURL());
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(
                textSource, textSourceWithName, fileSource, urlSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basic");
        
        DefaultCode code = (DefaultCode)c.compile(sources);
        
        assertEquals("DefaultCode[sourcesName='basic', sources:4, classes:4]", code.toString());
        
        assertEquals("basic", code.getSourcesName());
        
        assertEquals(4, code.getSourceSet().size());
        assertTrue(code.isForSource(textSource));
        assertTrue(code.isForSource(textSourceWithName));
        assertTrue(code.isForSource(fileSource));
        assertTrue(code.isForSource(urlSource));
        
        assertEquals(expectedTextSourceMainClassName, code.getMainClassName(textSource));
        assertEquals("MyTextScript", code.getMainClassName(textSourceWithName));
        assertEquals("MyFileScript", code.getMainClassName(fileSource));
        assertEquals("MyUrlScript", code.getMainClassName(urlSource));
        
        assertEquals(scriptFile.lastModified(), code.getLastModifiedAtCompileTime(fileSource));
        
        assertEquals(4, code.getClassNameSet().size());
        assertNotNull(code.getBytecode(expectedTextSourceMainClassName));
        assertNotNull(code.getBytecode("MyTextScript"));
        assertNotNull(code.getBytecode("MyFileScript"));
        assertNotNull(code.getBytecode("MyUrlScript"));
    }

    @Test
    public void testCompileBasicWithTargetDir() throws Exception {
        
        File targetDir = new File(tempFolder.getRoot(), "target");
        targetDir.mkdir();
        assertTrue(targetDir.exists());
        CompilerConfiguration config = new CompilerConfiguration();
        config.setTargetDirectory(targetDir);

        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory(config);
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        DefaultGroovyCompiler c = (DefaultGroovyCompiler)compilerFactory.newCompiler(parent);
        
        SourceFactory f = new DefaultSourceFactory();
        Source textSource = f.fromText("println 'text source'");
        String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        Source textSourceWithName = f.fromText("println 'text source'", "MyTextScript");
        File scriptFile = new File(tempFolder.getRoot(), "MyFileScript.groovy");
        TestUtil.setFileText(scriptFile, "println 'file source'");
        Source fileSource = f.fromFile(scriptFile);
        File urlScriptFile = new File(tempFolder.getRoot(), "MyUrlScript.groovy");
        TestUtil.setFileText(urlScriptFile, "println 'url source'");
        Source urlSource = f.fromUrl(urlScriptFile.toURI().toURL());
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(
                textSource, textSourceWithName, fileSource, urlSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basic", compilerFactory);
        
        DefaultCode code = (DefaultCode)c.compile(sources);
        
        assertEquals("DefaultCode[sourcesName='basic', sources:4, classes:4]", code.toString());
        
        assertEquals("basic", code.getSourcesName());
        
        assertEquals(4, code.getSourceSet().size());
        assertTrue(code.isForSource(textSource));
        assertTrue(code.isForSource(textSourceWithName));
        assertTrue(code.isForSource(fileSource));
        assertTrue(code.isForSource(urlSource));
        
        assertEquals(expectedTextSourceMainClassName, code.getMainClassName(textSource));
        assertEquals("MyTextScript", code.getMainClassName(textSourceWithName));
        assertEquals("MyFileScript", code.getMainClassName(fileSource));
        assertEquals("MyUrlScript", code.getMainClassName(urlSource));
        
        assertEquals(scriptFile.lastModified(), code.getLastModifiedAtCompileTime(fileSource));
        
        assertEquals(4, code.getClassNameSet().size());
        assertNotNull(code.getBytecode(expectedTextSourceMainClassName));
        assertNotNull(code.getBytecode("MyTextScript"));
        assertNotNull(code.getBytecode("MyFileScript"));
        assertNotNull(code.getBytecode("MyUrlScript"));
        
        assertTrue(new File(targetDir, expectedTextSourceMainClassName + ".class").exists());
        assertTrue(new File(targetDir, "MyTextScript.class").exists());
        assertTrue(new File(targetDir, "MyFileScript.class").exists());
        assertTrue(new File(targetDir, "MyUrlScript.class").exists());
    }

    @Test
    public void testCompileBasicSingleSource() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        SourceFactory f = new DefaultSourceFactory();
        Source textSource = f.fromText("println 'text source'");
        String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(textSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basicsingle");
        
        DefaultSingleSourceCode code = (DefaultSingleSourceCode)c.compile(sources);
        
        assertEquals("DefaultSingleSourceCode[sourcesName='basicsingle', " +
                "mainClassName=" + expectedTextSourceMainClassName + ", classes:[" + expectedTextSourceMainClassName +
                "]]", code.toString());
        
        assertEquals("basicsingle", code.getSourcesName());
        
        assertEquals(1, code.getSourceSet().size());
        assertTrue(code.isForSource(textSource));
        
        assertEquals(expectedTextSourceMainClassName, code.getMainClassName(textSource));
        
        assertEquals(1, code.getClassNameSet().size());
        assertNotNull(code.getBytecode(expectedTextSourceMainClassName));
        
        assertEquals(textSource, code.getSource());
        assertEquals(expectedTextSourceMainClassName, code.getMainClassName());
    }
    
    @Test
    public void testCompileSourcesNull() throws Exception {
        try {
            new DefaultGroovyCompiler().compile(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Sources are null.", e.getMessage());
        }
    }
    
    @Test
    public void testCompileFailsSyntaxWrong() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        SourceFactory f = new DefaultSourceFactory();
        Source textSource = f.fromText("%%)(");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(textSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "syntaxwrong");
        
        try {
            c.compile(sources);
            fail();
        } catch (CompileException e) {
            //System.out.println(e);
            assertTrue(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='syntaxwrong']. " +
                    "Cause: org.codehaus.groovy.control.MultipleCompilationErrorsException:"));
            assertEquals(sources, e.getSources());
            Thread.sleep(30);
            assertTrue(e.getDateThrown().getTime() < System.currentTimeMillis());
            assertTrue(e.getDateThrown().getTime() > System.currentTimeMillis() - 5000);
            //System.out.println(e.getCause());
            assertTrue(e.getCause().toString().startsWith(
                    "org.codehaus.groovy.control.MultipleCompilationErrorsException:"));
            
        }
    }
    
    @Test
    public void testCompileFailsUnknownSource() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        Source mockSource = new MockSource("id1");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(mockSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "unknownsource");
        
        try {
            c.compile(sources);
            fail();
        } catch (CompileException e) {
            //System.out.println(e);
            assertEquals("Don't know how to compile source MockSource[ID='id1', lastModified=0].", e.getMessage());
            assertEquals(sources, e.getSources());
            Thread.sleep(30);
            assertTrue(e.getDateThrown().getTime() < System.currentTimeMillis());
            assertTrue(e.getDateThrown().getTime() > System.currentTimeMillis() - 5000);
            assertNull(e.getCause());
        }
    }
    
    @Test
    public void testCompileFailsSameClassNameTwice() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("public class Twice { public def x() {} }");
        Source s2 = f.fromText("public class Twice { public def y() {} }");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "twice");
        
        try {
            c.compile(sources);
            fail();
        } catch (CompileException e) {
            //System.out.println(e);
            assertTrue(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='twice']. Cause: "));
            assertTrue(e.getMessage().contains("Invalid duplicate class definition of class Twice"));
            assertEquals(sources, e.getSources());
            Thread.sleep(30);
            assertTrue(e.getDateThrown().getTime() < System.currentTimeMillis());
            assertTrue(e.getDateThrown().getTime() > System.currentTimeMillis() - 5000);
            //System.out.println(e.getCause());
            assertTrue(e.getCause().getMessage().contains("Invalid duplicate class definition of class Twice"));
        }
    }
    
    @Test
    public void testCompileSameClassNameTwiceDifferentLoaders() throws Exception {
        
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("public class Twice { public def get() { return Inner1.get() }\n" +
                "public class Inner1 { static def get() { return 1 } } }");
        Source s2 = f.fromText("public class Twice { public def get() { return Inner2.get() }\n" +
                "public class Inner2 { static def get() { return 2 } } }");
        Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s1);
        Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s2);
        Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "sources1");
        Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "sources2");
        
        // source 1
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        DefaultGroovyCompiler c1 = new DefaultGroovyCompiler(parent);
        Code code1 = c1.compile(sources1);
        ClassLoader loader1 = new BytecodeClassLoader(parent, LoadMode.PARENT_FIRST, code1);
        Class<?> clazz1 = loader1.loadClass("Twice");
        Object obj1 = clazz1.newInstance();
        Method method1 = clazz1.getDeclaredMethod("get", new Class<?>[0]);
        Object out1 = method1.invoke(obj1);
        assertTrue(out1 instanceof Integer);
        assertTrue(1 == (Integer)out1);
        loader1.loadClass("Twice$Inner1");
        try {
            loader1.loadClass("Twice$Inner2");
            fail();
        } catch (ClassNotFoundException e) {
            // expected
        }

        // source 2 (current first)
        DefaultGroovyCompiler c2 = new DefaultGroovyCompiler(loader1);
        Code code2 = c2.compile(sources2);
        ClassLoader loader2 = new BytecodeClassLoader(loader1, LoadMode.CURRENT_FIRST, code2);
        Class<?> clazz2 = loader2.loadClass("Twice");
        Object obj2 = clazz2.newInstance();
        Method method2 = clazz2.getDeclaredMethod("get", new Class<?>[0]);
        Object out2 = method2.invoke(obj2);
        assertTrue(out2 instanceof Integer);
        assertTrue(2 == (Integer)out2);
        loader2.loadClass("Twice$Inner1");
        loader2.loadClass("Twice$Inner2");
        
        // source 2 (parent first)
        ClassLoader loader22 = new BytecodeClassLoader(loader1, LoadMode.PARENT_FIRST, code2);
        Class<?> clazz22 = loader22.loadClass("Twice");
        Object obj22 = clazz22.newInstance();
        Method method22 = clazz22.getDeclaredMethod("get", new Class<?>[0]);
        Object out22 = method22.invoke(obj22);
        assertTrue(out22 instanceof Integer);
        assertTrue(1 == (Integer)out22);
        loader22.loadClass("Twice$Inner1");
        loader22.loadClass("Twice$Inner2");
    }

}
