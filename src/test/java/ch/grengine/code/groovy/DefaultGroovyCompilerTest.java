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
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
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
            new DefaultGroovyCompiler((ClassLoader) null);
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

        DefaultCode code = (DefaultCode) c.compile(sources);

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

        DefaultGroovyCompiler c = (DefaultGroovyCompiler) compilerFactory.newCompiler(parent);

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

        DefaultCode code = (DefaultCode) c.compile(sources);

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

        DefaultSingleSourceCode code = (DefaultSingleSourceCode) c.compile(sources);

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
        assertThat((Integer) method1.invoke(obj1), is(1));
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
        assertThat((Integer) method2.invoke(obj2), is(2));
        loader2.loadClass("Twice$Inner1");
        loader2.loadClass("Twice$Inner2");

        // source 2 (parent first)
        ClassLoader loader22 = new BytecodeClassLoader(loader1, LoadMode.PARENT_FIRST, code2);
        Class<?> clazz22 = loader22.loadClass("Twice");
        Object obj22 = clazz22.newInstance();
        Method method22 = clazz22.getDeclaredMethod("get");
        assertThat((Integer) method22.invoke(obj22), is(1));
        loader22.loadClass("Twice$Inner1");
        loader22.loadClass("Twice$Inner2");
    }

    @Test
    public void testWithGrape() throws Exception {

        CompilerConfiguration config = new CompilerConfiguration();
        GroovyClassLoader loader = new GroovyClassLoader();
        CompilerConfiguration config2 = DefaultGroovyCompiler.withGrape(config, loader);

        assertThat(config2, sameInstance(config));
        assertThat(config.getCompilationCustomizers().size(), is(1));
        assertThat(config.getCompilationCustomizers().get(0),
                instanceOf(DefaultGroovyCompiler.GrapeCompilationCustomizer.class));
        DefaultGroovyCompiler.GrapeCompilationCustomizer customizer =
                (DefaultGroovyCompiler.GrapeCompilationCustomizer) config.getCompilationCustomizers().get(0);
        assertThat(customizer.runtimeLoader, sameInstance(loader));

        customizer.call(null, null, null);
    }

    @Test
    public void testWithGrape_configNull() throws Exception {

        GroovyClassLoader loader = new GroovyClassLoader();
        try {
            DefaultGroovyCompiler.withGrape((CompilerConfiguration) null, loader);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

    @Test
    public void testEnableDisableGrapeSupportDefault() throws Exception {
        try {
            DefaultGroovyCompiler.enableGrapeSupport();
            GrapeEngine engine = Grape.getInstance();

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is((Object) Grape.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // must be idempotent
            DefaultGroovyCompiler.enableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is((Object) Grape.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // must be idempotent, too
            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // once more

            DefaultGroovyCompiler.enableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is((Object) Grape.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    public void testEnableDisableGrapeSupportSpecificLock() throws Exception {
        try {
            Object lock = String.class;

            DefaultGroovyCompiler.enableGrapeSupport(lock);
            GrapeEngine engine = Grape.getInstance();

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(lock));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // must be idempotent
            DefaultGroovyCompiler.enableGrapeSupport(lock);
            engine = Grape.getInstance();

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(lock));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // must be idempotent, too
            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // once more

            DefaultGroovyCompiler.enableGrapeSupport(lock);
            engine = Grape.getInstance();

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(lock));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    public void testEnableGrapeSupport_differentLock() throws Exception {
        try {
            DefaultGroovyCompiler.enableGrapeSupport();
            try {
                DefaultGroovyCompiler.enableGrapeSupport(new Object());
                fail();
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(),
                        is("Attempt to change lock for wrapped Grape class (unwrap first)."));
            }
        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    public void testEnableGrapeSupport_lockNull() throws Exception {
        try {
            try {
                DefaultGroovyCompiler.enableGrapeSupport(null);
                fail();
            } catch (IllegalArgumentException e) {
                assertThat(e.getMessage(),
                        is("Lock is null."));
            }
        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    public void testGetLoaderIfConfigured() throws Exception {

        GroovyClassLoader parent = new GroovyClassLoader();
        CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new ImportCustomizer());
        GroovyClassLoader loader = DefaultGroovyCompiler.GrapeCompilationCustomizer
                .getLoaderIfConfigured(parent, config);

        assertThat(loader, is(nullValue()));

        DefaultGroovyCompiler.withGrape(config, parent);
        loader = DefaultGroovyCompiler.GrapeCompilationCustomizer
                .getLoaderIfConfigured(parent, config);
        assertThat(loader, instanceOf(DefaultGroovyCompiler.CompileTimeGroovyClassLoader.class));
        DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoader =
                (DefaultGroovyCompiler.CompileTimeGroovyClassLoader) loader;
        assertThat(compileTimeLoader.runtimeLoader, sameInstance(parent));
    }


    public volatile boolean failed;

    private static Map<String, Object> getDefaultArgs() {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("disableChecksums", false);
        args.put("autoDownload", true);
        return args;
    }

    private static Map<String, Object> getGuavaDependency() {
        Map<String, Object> dependency = new HashMap<String, Object>();
        dependency.put("module", "guava");
        dependency.put("version", "18.0");
        dependency.put("group", "com.google.guava");
        return dependency;
    }

    private static Map<String, Object> getDefaultMerged() {
        Map<String, Object> merged = new HashMap<String, Object>();
        merged.putAll(getDefaultArgs());
        merged.putAll(getGuavaDependency());
        return merged;
    }

    @Test
    public void testGrabs() throws Exception {
        try {
            DefaultGroovyCompiler.enableGrapeSupport();

            GroovyClassLoader runtimeLoader = new GroovyClassLoader();
            CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, runtimeLoader);
            DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoader =
                    (DefaultGroovyCompiler.CompileTimeGroovyClassLoader)
                            DefaultGroovyCompiler.GrapeCompilationCustomizer
                                    .getLoaderIfConfigured(runtimeLoader, config);

            config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, null);
            DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoaderWithNullRuntimeLoaderInside =
                    (DefaultGroovyCompiler.CompileTimeGroovyClassLoader)
                            DefaultGroovyCompiler.GrapeCompilationCustomizer
                                    .getLoaderIfConfigured(null, config);
            assertThat(compileTimeLoaderWithNullRuntimeLoaderInside.runtimeLoader, is(nullValue()));


            GrapeEngine engine = Grape.getInstance();
            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));

            Map<String, Object> args;
            Map<String, Object> dependency;
            Map<String, Object> merged;

            // grab(args, dependency)

            args = getDefaultArgs();
            dependency = getGuavaDependency();
            args.put("classLoader", compileTimeLoader);
            engine.grab(args, dependency);

            args = getDefaultArgs();
            dependency = getGuavaDependency();
            args.put("classLoader", runtimeLoader);
            engine.grab(args, dependency);

            args = getDefaultArgs();
            dependency = getGuavaDependency();
            args.put("classLoader", compileTimeLoaderWithNullRuntimeLoaderInside);
            engine.grab(args, dependency);

            args = getDefaultArgs();
            dependency = getGuavaDependency();
            args.put("classLoader", compileTimeLoader);
            args.put("calleeDepth", 4);
            engine.grab(args, dependency);

            // grab(args)

            merged = getDefaultMerged();
            merged.put("classLoader", compileTimeLoader);
            engine.grab(merged);

            merged = getDefaultMerged();
            merged.put("classLoader", runtimeLoader);
            engine.grab(merged);

            merged = getDefaultMerged();
            merged.put("classLoader", compileTimeLoaderWithNullRuntimeLoaderInside);
            engine.grab(merged);

            merged = getDefaultMerged();
            merged.put("classLoader", compileTimeLoader);
            merged.put("calleeDepth", 5);
            engine.grab(merged);

            // grab(endorsed) - no idea what could passed and would not throw

            try {
                engine.grab("endorsed");
                fail();
            } catch (RuntimeException e) {
                assertThat(e.getMessage(), is("No suitable ClassLoader found for grab"));
            }


            // enumerateGrapes()

            engine.enumerateGrapes();

            // resolve(args, dependency)

            args = getDefaultArgs();
            args.put("classLoader", compileTimeLoader);
            URI[] uris = engine.resolve(args, dependency);
            assertThat(uris.length, is(1));
            assertThat(uris[0].toString(), containsString("guava-18.0.jar"));

            args = getDefaultArgs();
            args.put("classLoader", compileTimeLoader);
            args.put("calleeDepth", 4);
            uris = engine.resolve(args, dependency);
            assertThat(uris.length, is(1));
            assertThat(uris[0].toString(), containsString("guava-18.0.jar"));

            // resolve(args, list, dependency)

            args = getDefaultArgs();
            args.put("classLoader", compileTimeLoader);
            uris = engine.resolve(args, (List) null, dependency);
            assertThat(uris.length, is(1));
            assertThat(uris[0].toString(), containsString("guava-18.0.jar"));

            args = getDefaultArgs();
            args.put("classLoader", compileTimeLoader);
            args.put("calleeDepth", 4);
            uris = engine.resolve(args, (List) null, dependency);
            assertThat(uris.length, is(1));
            assertThat(uris[0].toString(), containsString("guava-18.0.jar"));

            // list dependencies

            engine.listDependencies(runtimeLoader);


            // addResolver(args);

            args = getDefaultArgs();
            args.put("root", "dummy");
            engine.addResolver(args);

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    public void testConcurrentGrabs() throws Exception {
        try {
            DefaultGroovyCompiler.enableGrapeSupport();

            GroovyClassLoader runtimeLoader = new GroovyClassLoader();
            CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, runtimeLoader);
            final DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoader =
                    (DefaultGroovyCompiler.CompileTimeGroovyClassLoader)
                            DefaultGroovyCompiler.GrapeCompilationCustomizer
                                    .getLoaderIfConfigured(runtimeLoader, config);

            final GrapeEngine engine = Grape.getInstance();

            final int n = 50;
            failed = false;
            List<Thread> threads = new LinkedList<Thread>();
            for (int i = 0; i < n; i++) {
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            final Map<String, Object> args = getDefaultArgs();
                            final Map<String, Object> dependency = getGuavaDependency();
                            args.put("classLoader", compileTimeLoader);
                            engine.grab(args, dependency);
                        } catch (Throwable t) {
                            System.out.println("Thread failed: " + t);
                            failed = true;
                        }
                    }
                };
                threads.add(thread);
            }

            for (Thread thread : threads) {
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(failed, is(false));

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }

    }

    @Test
    public void testWrappingFails_instanceNotGrapeIvy() throws Exception {
        final GrapeEngine innerEngine = Grape.getInstance();
        try {
            // set GrapeEngine instance in Grape class directly
            new Grape() {
                void set() {
                    Grape.instance = new DefaultGroovyCompiler.GrengineGrapeEngine(Grape.getInstance());
                }
            }.set();

            try {
                DefaultGroovyCompiler.enableGrapeSupport();
                fail();
            } catch (IllegalStateException e) {
                assertThat(e.getMessage(), is("Unable to wrap GrapeEngine in Grape.class " +
                        "(current GrapeEngine is ch.grengine.code.groovy.DefaultGroovyCompiler$GrengineGrapeEngine, " +
                        "supported is groovy.grape.GrapeIvy)."));
            }

        } finally {
            // set back to GrapeIvy
            new Grape() {
                void set() {
                    Grape.instance = innerEngine;
                }
            }.set();

        }
    }

}