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

package ch.grengine.code.groovy;

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

import java.io.File;
import java.lang.reflect.Method;
import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultGroovyCompilerTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructDefaults() throws Exception {
        DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        DefaultGroovyCompiler c = builder.build();

        assertThat(c.getBuilder(), is(builder));
        assertThat(c.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(c.getParent(), is(c.getBuilder().getParent()));
        assertThat(c.getCompilerConfiguration(), is(notNullValue()));
        assertThat(c.getCompilerConfiguration(), is(c.getBuilder().getCompilerConfiguration()));
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        CompilerConfiguration config = new CompilerConfiguration();
        builder.setParent(parent);
        builder.setCompilerConfiguration(config);
        DefaultGroovyCompiler c = builder.build();

        assertThat(c.getBuilder(), is(builder));
        assertThat(c.getParent(), is(parent));
        assertThat(c.getParent(), is(c.getBuilder().getParent()));
        assertThat(c.getCompilerConfiguration(), is(config));
        assertThat(c.getCompilerConfiguration(), is(c.getBuilder().getCompilerConfiguration()));
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        builder.build();
        try {
            builder.setParent(Thread.currentThread().getContextClassLoader());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }

    @Test
    public void testConstructorNoArgs() {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        assertThat(c.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(c.getCompilerConfiguration(), is(notNullValue()));
    }
    
    @Test
    public void testConstructorFromParent() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);
        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerConfiguration(), is(notNullValue()));
    }
    
    @Test
    public void testConstructorFromParentAndConfig() {
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        CompilerConfiguration config = new CompilerConfiguration();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent, config);
        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerConfiguration(), is(config));
    }
    
    @Test
    public void testConstructFromParentNull() throws Exception {
        try {
            new DefaultGroovyCompiler((ClassLoader)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }
    
    @Test
    public void testConstructFromParentAndConfigParentNull() throws Exception {
        try {
            new DefaultGroovyCompiler(null, new CompilerConfiguration());
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }
    
    @Test
    public void testConstructFromParentAndConfigConfigNull() throws Exception {
        try {
            new DefaultGroovyCompiler(Thread.currentThread().getContextClassLoader(), null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
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

        assertThat(code.toString(), is("DefaultCode[sourcesName='basic', sources:4, classes:4]"));

        assertThat(code.getSourcesName(), is("basic"));

        assertThat(code.getSourceSet().size(), is(4));
        assertThat(code.isForSource(textSource), is(true));
        assertThat(code.isForSource(textSourceWithName), is(true));
        assertThat(code.isForSource(fileSource), is(true));
        assertThat(code.isForSource(urlSource), is(true));

        assertThat(code.getMainClassName(textSource), is(expectedTextSourceMainClassName));
        assertThat(code.getMainClassName(textSourceWithName), is("MyTextScript"));
        assertThat(code.getMainClassName(fileSource), is("MyFileScript"));
        assertThat(code.getMainClassName(urlSource), is("MyUrlScript"));

        assertThat(code.getLastModifiedAtCompileTime(fileSource), is(scriptFile.lastModified()));

        assertThat(code.getClassNameSet().size(), is(4));
        assertThat(code.getBytecode(expectedTextSourceMainClassName), is(notNullValue()));
        assertThat(code.getBytecode("MyTextScript"), is(notNullValue()));
        assertThat(code.getBytecode("MyFileScript"), is(notNullValue()));
        assertThat(code.getBytecode("MyUrlScript"), is(notNullValue()));
    }

    @Test
    public void testCompileBasicWithTargetDir() throws Exception {
        
        File targetDir = new File(tempFolder.getRoot(), "target");
        targetDir.mkdir();
        assertThat(targetDir.exists(), is(true));
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

        assertThat(code.toString(), is("DefaultCode[sourcesName='basic', sources:4, classes:4]"));

        assertThat(code.getSourcesName(), is("basic"));

        assertThat(code.getSourceSet().size(), is(4));
        assertThat(code.isForSource(textSource), is(true));
        assertThat(code.isForSource(textSourceWithName), is(true));
        assertThat(code.isForSource(fileSource), is(true));
        assertThat(code.isForSource(urlSource), is(true));

        assertThat(code.getMainClassName(textSource), is(expectedTextSourceMainClassName));
        assertThat(code.getMainClassName(textSourceWithName), is("MyTextScript"));
        assertThat(code.getMainClassName(fileSource), is("MyFileScript"));
        assertThat(code.getMainClassName(urlSource), is("MyUrlScript"));

        assertThat(code.getLastModifiedAtCompileTime(fileSource), is(scriptFile.lastModified()));

        assertThat(code.getClassNameSet().size(), is(4));
        assertThat(code.getBytecode(expectedTextSourceMainClassName), is(notNullValue()));
        assertThat(code.getBytecode("MyTextScript"), is(notNullValue()));
        assertThat(code.getBytecode("MyFileScript"), is(notNullValue()));
        assertThat(code.getBytecode("MyUrlScript"), is(notNullValue()));

        assertThat(new File(targetDir, expectedTextSourceMainClassName + ".class").exists(), is(true));
        assertThat(new File(targetDir, "MyTextScript.class").exists(), is(true));
        assertThat(new File(targetDir, "MyFileScript.class").exists(), is(true));
        assertThat(new File(targetDir, "MyUrlScript.class").exists(), is(true));
    }

    @Test
    public void testCompileBasicSingleSource() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        SourceFactory f = new DefaultSourceFactory();
        Source textSource = f.fromText("println 'text source'");
        String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(textSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basicSingle");
        
        DefaultSingleSourceCode code = (DefaultSingleSourceCode)c.compile(sources);

        assertThat(code.toString(), is("DefaultSingleSourceCode[sourcesName='basicSingle', " +
                "mainClassName=" + expectedTextSourceMainClassName + ", classes:[" + expectedTextSourceMainClassName +
                "]]"));

        assertThat(code.getSourcesName(), is("basicSingle"));

        assertThat(code.getSourceSet().size(), is(1));
        assertThat(code.isForSource(textSource), is(true));

        assertThat(code.getMainClassName(textSource), is(expectedTextSourceMainClassName));

        assertThat(code.getClassNameSet().size(), is(1));
        assertThat(code.getBytecode(expectedTextSourceMainClassName), is(notNullValue()));

        assertThat(code.getSource(), is(textSource));
        assertThat(code.getMainClassName(), is(expectedTextSourceMainClassName));
    }
    
    @Test
    public void testCompileSourcesNull() throws Exception {
        try {
            new DefaultGroovyCompiler().compile(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Sources are null."));
        }
    }
    
    @Test
    public void testCompileFailsSyntaxWrong() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        SourceFactory f = new DefaultSourceFactory();
        Source textSource = f.fromText("%%)(");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(textSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "syntaxWrong");
        
        try {
            c.compile(sources);
            fail();
        } catch (CompileException e) {
            //System.out.println(e);
            assertThat(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='syntaxWrong']. " +
                    "Cause: org.codehaus.groovy.control.MultipleCompilationErrorsException:"), is(true));
            assertThat(e.getSources(), is(sources));
            Thread.sleep(30);
            assertThat(e.getDateThrown().getTime() < System.currentTimeMillis(), is(true));
            assertThat(e.getDateThrown().getTime() > System.currentTimeMillis() - 5000, is(true));
            //System.out.println(e.getCause());
            assertThat(e.getCause().toString().startsWith(
                    "org.codehaus.groovy.control.MultipleCompilationErrorsException:"), is(true));
            
        }
    }
    
    @Test
    public void testCompileFailsUnknownSource() throws Exception {
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        
        Source mockSource = new MockSource("id1");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(mockSource);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "unknownSource");
        
        try {
            c.compile(sources);
            fail();
        } catch (CompileException e) {
            //System.out.println(e);
            assertThat(e.getMessage(), is("Don't know how to compile source MockSource[ID='id1', lastModified=0]."));
            assertThat(e.getSources(), is(sources));
            Thread.sleep(30);
            assertThat(e.getDateThrown().getTime() < System.currentTimeMillis(), is(true));
            assertThat(e.getDateThrown().getTime() > System.currentTimeMillis() - 5000, is(true));
            assertThat(e.getCause(), is(nullValue()));
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
            assertThat(e.getMessage().startsWith("Compile failed for sources FixedSetSources[name='twice']. Cause: "), is(true));
            assertThat(e.getMessage().contains("Invalid duplicate class definition of class Twice"), is(true));
            assertThat(e.getSources(), is(sources));
            Thread.sleep(30);
            assertThat(e.getDateThrown().getTime() < System.currentTimeMillis(), is(true));
            assertThat(e.getDateThrown().getTime() > System.currentTimeMillis() - 5000, is(true));
            //System.out.println(e.getCause());
            assertThat(e.getCause().getMessage().contains("Invalid duplicate class definition of class Twice"), is(true));
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
        Method method1 = clazz1.getDeclaredMethod("get");
        assertThat((Integer)method1.invoke(obj1), is(1));
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
        Method method2 = clazz2.getDeclaredMethod("get");
        assertThat((Integer)method2.invoke(obj2), is(2));
        loader2.loadClass("Twice$Inner1");
        loader2.loadClass("Twice$Inner2");
        
        // source 2 (parent first)
        ClassLoader loader22 = new BytecodeClassLoader(loader1, LoadMode.PARENT_FIRST, code2);
        Class<?> clazz22 = loader22.loadClass("Twice");
        Object obj22 = clazz22.newInstance();
        Method method22 = clazz22.getDeclaredMethod("get");
        assertThat((Integer)method22.invoke(obj22), is(1));
        loader22.loadClass("Twice$Inner1");
        loader22.loadClass("Twice$Inner2");
    }

}
