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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static ch.grengine.TestUtil.createTestDir;
import static ch.grengine.TestUtil.toRuntimeException;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


class DefaultGroovyCompilerTest {

    @Test
    void testConstructDefaults() {

        // given

        final DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();

        // when

        final DefaultGroovyCompiler c = builder.build();

        // then

        assertThat(c.getBuilder(), is(builder));
        assertThat(c.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(c.getParent(), is(c.getBuilder().getParent()));
        assertThat(c.getCompilerConfiguration(), is(notNullValue()));
        assertThat(c.getCompilerConfiguration(), is(c.getBuilder().getCompilerConfiguration()));
    }

    @Test
    void testConstructAllDefined() {

        // given

        final DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final CompilerConfiguration config = new CompilerConfiguration();
        builder.setParent(parent);
        builder.setCompilerConfiguration(config);

        // when

        final DefaultGroovyCompiler c = builder.build();

        // then

        assertThat(c.getBuilder(), is(builder));
        assertThat(c.getParent(), is(parent));
        assertThat(c.getParent(), is(c.getBuilder().getParent()));
        assertThat(c.getCompilerConfiguration(), is(config));
        assertThat(c.getCompilerConfiguration(), is(c.getBuilder().getCompilerConfiguration()));
    }

    @Test
    void testModifyBuilderAfterUse() {

        // given

        final DefaultGroovyCompiler.Builder builder = new DefaultGroovyCompiler.Builder();
        builder.build();

        // when/then

        assertThrowsMessageIs(IllegalStateException.class,
                () -> builder.setParent(Thread.currentThread().getContextClassLoader()),
                "Builder already used.");
    }

    @Test
    void testConstructorNoArgs() {

        // when

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();

        // then

        assertThat(c.getParent(), is(Thread.currentThread().getContextClassLoader()));
        assertThat(c.getCompilerConfiguration(), is(notNullValue()));
    }

    @Test
    void testConstructorFromParent() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();

        // when

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent);

        // then

        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerConfiguration(), is(notNullValue()));
    }

    @Test
    void testConstructorFromParentAndConfig() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler(parent, config);

        // then

        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerConfiguration(), is(config));
    }

    @Test
    void testConstructFromParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultGroovyCompiler((ClassLoader) null),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromParentAndConfigParentNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultGroovyCompiler(null, new CompilerConfiguration()),
                "Parent class loader is null.");
    }

    @Test
    void testConstructFromParentAndConfigConfigNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultGroovyCompiler(Thread.currentThread().getContextClassLoader(), null),
                "Compiler configuration is null.");
    }


    @Test
    void testCompileBasic() throws Exception {

        // given

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();

        final SourceFactory f = new DefaultSourceFactory();
        final Source textSource = f.fromText("println 'text source'");
        final String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        final Source textSourceWithName = f.fromText("println 'text source'", "MyTextScript");
        final File dir = createTestDir();
        final File scriptFile = new File(dir, "MyFileScript.groovy");
        TestUtil.setFileText(scriptFile, "println 'file source'");
        final Source fileSource = f.fromFile(scriptFile);
        final File urlScriptFile = new File(dir, "MyUrlScript.groovy");
        TestUtil.setFileText(urlScriptFile, "println 'url source'");
        final Source urlSource = f.fromUrl(urlScriptFile.toURI().toURL());
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(
                textSource, textSourceWithName, fileSource, urlSource);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basic");

        // when

        final DefaultCode code = (DefaultCode) c.compile(sources);


        // then

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
    void testCompileBasicWithTargetDir() throws Exception {

        // given

        final File dir = createTestDir();
        final File targetDir = new File(dir, "target");
        assertThat(targetDir.mkdir(), is(true));
        assertThat(targetDir.exists(), is(true));
        final CompilerConfiguration config = new CompilerConfiguration();
        config.setTargetDirectory(targetDir);

        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory(config);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final DefaultGroovyCompiler c = (DefaultGroovyCompiler) compilerFactory.newCompiler(parent);

        final SourceFactory f = new DefaultSourceFactory();
        final Source textSource = f.fromText("println 'text source'");
        final String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        final Source textSourceWithName = f.fromText("println 'text source'", "MyTextScript");
        final File scriptFile = new File(dir, "MyFileScript.groovy");
        TestUtil.setFileText(scriptFile, "println 'file source'");
        final Source fileSource = f.fromFile(scriptFile);
        final File urlScriptFile = new File(dir, "MyUrlScript.groovy");
        TestUtil.setFileText(urlScriptFile, "println 'url source'");
        final Source urlSource = f.fromUrl(urlScriptFile.toURI().toURL());
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(
                textSource, textSourceWithName, fileSource, urlSource);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basic", compilerFactory);

        // when

        final DefaultCode code = (DefaultCode) c.compile(sources);

        // then

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
    void testCompileBasicSingleSource() {

        // given

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();

        final SourceFactory f = new DefaultSourceFactory();
        final Source textSource = f.fromText("println 'text source'");
        final String expectedTextSourceMainClassName = "Script" + SourceUtil.md5("println 'text source'");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(textSource);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "basicSingle");

        // when

        final DefaultSingleSourceCode code = (DefaultSingleSourceCode) c.compile(sources);

        // then

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
    void testCompileSourcesNull() {

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> new DefaultGroovyCompiler().compile(null),
                "Sources are null.");
    }

    @Test
    void testCompileFailsSyntaxWrong() throws Exception {

        // given

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();

        final SourceFactory f = new DefaultSourceFactory();
        final Source textSource = f.fromText("%%)(");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(textSource);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "syntaxWrong");

        // when/then

        try {
            c.compile(sources);
            fail("did not throw");
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
    void testCompileFailsUnknownSource() throws Exception {

        // given

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();

        final Source mockSource = new MockSource("id1");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(mockSource);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "unknownSource");

        // when/then

        try {
            c.compile(sources);
            fail("did not throw");
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
    void testCompileFailsSameClassNameTwice() throws Exception {

        // given

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("public class Twice { public def x() {} }");
        final Source s2 = f.fromText("public class Twice { public def y() {} }");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "twice");

        // when/then

        try {
            c.compile(sources);
            fail("did not throw");
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
    void testCompileSameClassNameTwiceDifferentLoaders() throws Exception {

        // given

        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("public class Twice { public def get() { return Inner1.get() }\n" +
                "public class Inner1 { static def get() { return 1 } } }");
        final Source s2 = f.fromText("public class Twice { public def get() { return Inner2.get() }\n" +
                "public class Inner2 { static def get() { return 2 } } }");
        final Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s1);
        final Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s2);
        final Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "sources1");
        final Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "sources2");

        // when/then (source 1)

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final DefaultGroovyCompiler c1 = new DefaultGroovyCompiler(parent);
        final Code code1 = c1.compile(sources1);
        final ClassLoader loader1 = new BytecodeClassLoader(parent, LoadMode.PARENT_FIRST, code1);
        final Class<?> clazz1 = loader1.loadClass("Twice");
        final Object obj1 = clazz1.getConstructor().newInstance();
        final Method method1 = clazz1.getDeclaredMethod("get");
        assertThat(method1.invoke(obj1), is(1));
        loader1.loadClass("Twice$Inner1");
        assertThrowsMessageIs(ClassNotFoundException.class,
                () -> loader1.loadClass("Twice$Inner2"),
                "Twice$Inner2");

        // when/then (source 2 - current first)

        final DefaultGroovyCompiler c2 = new DefaultGroovyCompiler(loader1);
        final Code code2 = c2.compile(sources2);
        final ClassLoader loader2 = new BytecodeClassLoader(loader1, LoadMode.CURRENT_FIRST, code2);
        final Class<?> clazz2 = loader2.loadClass("Twice");
        final Object obj2 = clazz2.getConstructor().newInstance();
        final Method method2 = clazz2.getDeclaredMethod("get");
        assertThat(method2.invoke(obj2), is(2));
        loader2.loadClass("Twice$Inner1");
        loader2.loadClass("Twice$Inner2");

        // when/then (source 2 - parent first)

        final ClassLoader loader22 = new BytecodeClassLoader(loader1, LoadMode.PARENT_FIRST, code2);
        final Class<?> clazz22 = loader22.loadClass("Twice");
        final Object obj22 = clazz22.getConstructor().newInstance();
        final Method method22 = clazz22.getDeclaredMethod("get");
        assertThat(method22.invoke(obj22), is(1));
        loader22.loadClass("Twice$Inner1");
        loader22.loadClass("Twice$Inner2");
    }

    @Test
    void testWithGrape() {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();
        final GroovyClassLoader loader = new GroovyClassLoader();

        // when

        final CompilerConfiguration config2 = DefaultGroovyCompiler.withGrape(config, loader);

        // then

        assertThat(config2, sameInstance(config));
        assertThat(config.getCompilationCustomizers().size(), is(1));
        assertThat(config.getCompilationCustomizers().get(0),
                instanceOf(DefaultGroovyCompiler.GrapeCompilationCustomizer.class));

        // when

        final DefaultGroovyCompiler.GrapeCompilationCustomizer customizer =
                (DefaultGroovyCompiler.GrapeCompilationCustomizer) config.getCompilationCustomizers().get(0);

        // then

        assertThat(customizer.runtimeLoader, sameInstance(loader));

        customizer.call(null, null, null);
    }

    @Test
    void testWithGrape_configNull() {

        // given

        final GroovyClassLoader loader = new GroovyClassLoader();

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> DefaultGroovyCompiler.withGrape(null, loader),
                "Compiler configuration is null.");
    }

    @Test
    void testEnableDisableGrapeSupportDefault() {
        try {

            // when (enable)

            DefaultGroovyCompiler.enableGrapeSupport();
            GrapeEngine engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is((Object) Grape.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // when (enable again, must be idempotent)

            DefaultGroovyCompiler.enableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is((Object) Grape.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // when (disable)

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // when (disable again, must be idempotent, too)

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // when (enable once more)

            DefaultGroovyCompiler.enableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is((Object) Grape.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // when (disable)

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    void testEnableDisableGrapeSupportSpecificLock() {
        try {

            // given

            final Object lock = String.class;

            // when (enable)

            DefaultGroovyCompiler.enableGrapeSupport(lock);
            GrapeEngine engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(lock));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // when (enable again, must be idempotent)

            DefaultGroovyCompiler.enableGrapeSupport(lock);
            engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(lock));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // when (disable)

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // when (disable again, must be idempotent, too)

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

            // when (enable once more)

            DefaultGroovyCompiler.enableGrapeSupport(lock);
            engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(lock));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(4));
            assertThat(((DefaultGroovyCompiler.GrengineGrapeEngine) engine).innerEngine.getClass().getName(),
                    is("groovy.grape.GrapeIvy"));

            // when (disable)

            DefaultGroovyCompiler.disableGrapeSupport();
            engine = Grape.getInstance();

            // then

            assertThat(engine.getClass().getName(), is("groovy.grape.GrapeIvy"));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.lock, is(nullValue()));
            assertThat(DefaultGroovyCompiler.GrengineGrapeEngine.defaultDepth, is(0));

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    void testEnableGrapeSupport_differentLock() {
        try {

            // given

            DefaultGroovyCompiler.enableGrapeSupport();

            // when/then

            assertThrowsMessageIs(IllegalStateException.class,
                    () -> DefaultGroovyCompiler.enableGrapeSupport(new Object()),
                    "Attempt to change lock for wrapped Grape class (unwrap first).");
        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    void testEnableGrapeSupport_lockNull() {
        try {

            // when/then

            assertThrowsMessageIs(NullPointerException.class,
                    () -> DefaultGroovyCompiler.enableGrapeSupport(null),
                    "Lock is null.");
        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    void testGetLoaderIfConfigured() {

        // given

        final GroovyClassLoader parent = new GroovyClassLoader();
        final CompilerConfiguration config = new CompilerConfiguration();
        config.addCompilationCustomizers(new ImportCustomizer());

        // when

        GroovyClassLoader loader = DefaultGroovyCompiler.GrapeCompilationCustomizer
                .getLoaderIfConfigured(parent, config);

        // then

        assertThat(loader, is(nullValue()));

        // when

        DefaultGroovyCompiler.withGrape(config, parent);
        loader = DefaultGroovyCompiler.GrapeCompilationCustomizer
                .getLoaderIfConfigured(parent, config);

        // then

        assertThat(loader, instanceOf(DefaultGroovyCompiler.CompileTimeGroovyClassLoader.class));

        // when

        DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoader =
                (DefaultGroovyCompiler.CompileTimeGroovyClassLoader) loader;

        // then

        assertThat(compileTimeLoader.runtimeLoader, sameInstance(parent));
    }


    private volatile boolean failed;

    private static Map<String, Object> getDefaultArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("disableChecksums", false);
        args.put("autoDownload", true);
        return args;
    }

    private static Map<String, Object> getGuavaDependency() {
        Map<String, Object> dependency = new HashMap<>();
        dependency.put("module", "guava");
        dependency.put("version", "18.0");
        dependency.put("group", "com.google.guava");
        return dependency;
    }

    private static Map<String, Object> getDefaultMerged() {
        Map<String, Object> merged = new HashMap<>();
        merged.putAll(getDefaultArgs());
        merged.putAll(getGuavaDependency());
        return merged;
    }

    @Test
    void testGrabs() {
        try {

            // given

            DefaultGroovyCompiler.enableGrapeSupport();

            final GroovyClassLoader runtimeLoader = new GroovyClassLoader();
            final CompilerConfiguration config1 = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config1, runtimeLoader);
            final DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoader =
                    (DefaultGroovyCompiler.CompileTimeGroovyClassLoader)
                            DefaultGroovyCompiler.GrapeCompilationCustomizer
                                    .getLoaderIfConfigured(runtimeLoader, config1);

            final CompilerConfiguration config2 = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config2, null);

            // when

            DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoaderWithNullRuntimeLoaderInside =
                    (DefaultGroovyCompiler.CompileTimeGroovyClassLoader)
                            DefaultGroovyCompiler.GrapeCompilationCustomizer
                                    .getLoaderIfConfigured(null, config2);

            // then

            assertThat(compileTimeLoaderWithNullRuntimeLoaderInside.runtimeLoader, is(nullValue()));

            // when

            final GrapeEngine engine = Grape.getInstance();

            // then

            assertThat(engine, instanceOf(DefaultGroovyCompiler.GrengineGrapeEngine.class));

            Map<String, Object> args;
            Map<String, Object> dependency;
            Map<String, Object> merged;

            // when/then (grab(args, dependency))

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

            // when/then (grab(args))

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

            // when/then (grab(endorsed) - no idea what could passed and would not throw)

            assertThrowsMessageIs(RuntimeException.class,
                    () -> engine.grab("endorsed"),
                    "No suitable ClassLoader found for grab");


            // when/then (enumerateGrapes())

            engine.enumerateGrapes();

            // when/then (resolve(args, dependency))

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

            // when/then (resolve(args, list, dependency))

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

            // when/then (list dependencies)

            engine.listDependencies(runtimeLoader);


            // when/then (addResolver(args))

            args = getDefaultArgs();
            args.put("root", "dummy");
            engine.addResolver(args);

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }
    }

    @Test
    void testConcurrentGrabs() throws Exception {
        try {

            // given

            failed = false;

            DefaultGroovyCompiler.enableGrapeSupport();

            final GroovyClassLoader runtimeLoader = new GroovyClassLoader();
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, runtimeLoader);
            final DefaultGroovyCompiler.CompileTimeGroovyClassLoader compileTimeLoader =
                    (DefaultGroovyCompiler.CompileTimeGroovyClassLoader)
                            DefaultGroovyCompiler.GrapeCompilationCustomizer
                                    .getLoaderIfConfigured(runtimeLoader, config);

            final GrapeEngine engine = Grape.getInstance();

            final int n = 50;
            final List<Thread> threads = IntStream.range(0, n)
                    .boxed()
                    .map(i -> new Thread(() -> {
                        try {
                            final Map<String, Object> args = getDefaultArgs();
                            final Map<String, Object> dependency = getGuavaDependency();
                            args.put("classLoader", compileTimeLoader);
                            engine.grab(args, dependency);
                        } catch (Throwable t) {
                            System.out.println("Thread failed: " + t);
                            failed = true;
                        }
                    }))
                    .collect(toList());

            // when

            threads.forEach(Thread::start);
            threads.forEach(t -> toRuntimeException(t::join));

            // then

            assertThat(failed, is(false));

        } finally {
            DefaultGroovyCompiler.disableGrapeSupport();
        }

    }

    @Test
    void testWrappingFails_instanceNotGrapeIvy() {

        // given

        final GrapeEngine innerEngine = Grape.getInstance();
        try {

            // set GrapeEngine instance in Grape class directly
            new Grape() {
                void set() {
                    Grape.instance = new DefaultGroovyCompiler.GrengineGrapeEngine(Grape.getInstance());
                }
            }.set();

            // when/then

            assertThrowsMessageIs(IllegalStateException.class,
                    DefaultGroovyCompiler::enableGrapeSupport,
                    "Unable to wrap GrapeEngine in Grape.class " +
                            "(current GrapeEngine is ch.grengine.code.groovy.DefaultGroovyCompiler$GrengineGrapeEngine, " +
                            "supported is groovy.grape.GrapeIvy).");
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