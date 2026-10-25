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

import ch.grengine.code.Bytecode;
import ch.grengine.code.Code;
import ch.grengine.code.CompiledSourceInfo;
import ch.grengine.code.Compiler;
import ch.grengine.code.DefaultCode;
import ch.grengine.code.DefaultSingleSourceCode;
import ch.grengine.except.CompileException;
import ch.grengine.source.FileSource;
import ch.grengine.source.Source;
import ch.grengine.source.TextSource;
import ch.grengine.source.UrlSource;
import ch.grengine.sources.Sources;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.tools.GroovyClass;
import org.codehaus.groovy.tools.RootLoader;

import static java.util.Objects.requireNonNull;


/**
 * Default Groovy compiler.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultGroovyCompiler implements Compiler {
    
    private final Builder builder;
    private final ClassLoader parent;
    private final CompilerConfiguration config;
    private final GroovyClassLoader groovyClassLoader;

    /**
     * constructor from builder.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected DefaultGroovyCompiler(final Builder builder) {
        this.builder = builder.commit();
        parent = builder.getParent();
        config = builder.getCompilerConfiguration();
        final GroovyClassLoader loader = GrapeCompilationCustomizer.getLoaderIfConfigured(parent, config);
        groovyClassLoader = (loader == null) ? new GroovyClassLoader(parent, config) : loader;
    }
    
    /**
     * constructor from parent class loader set to the context class loader of the current thread
     * and from default compiler configuration.
     * 
     * @since 1.0
     */
    public DefaultGroovyCompiler() {
        this(new Builder());
    }

    /**
     * constructor from given parent class loader and default compiler configuration.
     *
     * @param parent parent class loader
     *
     * @throws NullPointerException if the parent class loader is null
     * 
     * @since 1.0
     */
    public DefaultGroovyCompiler(final ClassLoader parent) {
        this(new Builder()
                .setParent(requireNonNull(parent, "Parent class loader is null."))
        );
    }
    
    /**
     * constructor from given parent class loader and compiler configuration.
     *
     * @param parent parent class loader
     * @param config compiler configuration
     *
     * @throws NullPointerException if the parent class loader or the compiler configuration is null
     * 
     * @since 1.0
     */
    public DefaultGroovyCompiler(final ClassLoader parent, final CompilerConfiguration config) {
        this(new Builder()
                .setParent(requireNonNull(parent, "Parent class loader is null."))
                .setCompilerConfiguration(requireNonNull(config, "Compiler configuration is null."))
        );
    }

    /**
     * modifies the given compiler configuration for Grape support in this compiler.
     * <p>
     * Only has an effect during compilation if Grape support had been enabled
     * and only with this compiler class.
     *
     * @param config compiler configuration to enable for Grape
     * @param runtimeLoader the GroovyClassLoader that is intended to be used later
     *                      as a parent loader when loading classes compiled with
     *                      the given compiler configuration
     *
     * @return modified compiler configuration, same instance as argument
     * @throws NullPointerException if the compiler configuration is null
     *
     * @since 1.2
     */
    public static CompilerConfiguration withGrape(final CompilerConfiguration config,
            final GroovyClassLoader runtimeLoader) {
        requireNonNull(config, "Compiler configuration is null.");
        GrapeCompilationCustomizer.enableGrape(config, runtimeLoader);
        return config;
    }

    /**
     * enable Grape support for Grengine with the default lock.
     * <p>
     * Wraps the {@link GrapeEngine} in the {@link Grape} class
     * with a wrapper that handles class loaders as needed by Grengine
     * and locks grabs on the wrapper instance.
     * <p>
     * Call once before using this compiler class in combination with
     * Grengine when Grape grabs are needed (equivalent to calling
     * <code>Grengine.Grape.activate()</code>).
     * <p>
     * See user manual under "Grengine and Grape" for more info.
     *
     * @since 1.2
     */
    public static void enableGrapeSupport() {
        GrengineGrapeEngine.wrap(null);
    }

    /**
     * enable Grape support for Grengine with the given lock.
     * <p>
     * Wraps the {@link GrapeEngine} in the {@link Grape} class with
     * a wrapper that handles class loaders as needed by Grengine
     * and locks grabs with the given lock or, if the given lock is
     * null, with the wrapper instance.
     * <p>
     * Call once before using this compiler class in combination with
     * Grengine when Grape grabs are needed (equivalent to calling
     * <code>Grengine.Grape.activate(lock)</code>).
     * <p>
     * See user manual under "Grengine and Grape" for more info.
     *
     * @param lock the lock to use; uses default lock if null
     *
     * @since 1.2
     */
    public static void enableGrapeSupport(Object lock) {
        GrengineGrapeEngine.wrap(lock);
    }

    /**
     * disable Grape support for Grengine - not recommended in some scenarios
     * where multiple callers can enable in an idempotent way but a single
     * caller can disable for all.
     * <p>
     * Unwraps the {@link GrapeEngine} in the {@link Grape} class
     * (equivalent to calling <code>Grengine.Grape.deactivate()</code>).
     * <p>
     * See user manual under "Grengine and Grape" for more info.
     *
     * @since 1.2
     */
    public static void disableGrapeSupport() {
        GrengineGrapeEngine.unwrap();
    }

    /**
     * compiles the given Groovy script sources to an instance of {@link Code} in memory.
     * <p>
     * If {@link CompilerConfiguration#getTargetDirectory()} is not null,
     * class files are also written to the target directory.
     *
     * @param sources sources
     *
     * @return code
     * @throws CompileException if compilation failed
     * @throws NullPointerException if sources are null
     * 
     * @since 1.0
     */
    @Override
    public Code compile(final Sources sources) {
        requireNonNull(sources, "Sources are null.");
        try {
            final CompilationUnit cu = new CompilationUnit(config, null, groovyClassLoader);
            final Map<Source,SourceUnit> sourceUnitMap = sources.getSourceSet().stream()
                    .collect(Collectors.toMap(source -> source, source -> addToCompilationUnit(cu, source, sources)));

            final int phase = (config.getTargetDirectory() == null) ? Phases.CLASS_GENERATION : Phases.OUTPUT;
            cu.compile(phase);

            final Map<Source,CompiledSourceInfo> compiledSourceInfoMap = new HashMap<>();
            sourceUnitMap.forEach((source, su) -> {
                final Set<String> classNames = su.getAST().getClasses().stream()
                        .map(ClassNode::getName)
                        .collect(Collectors.toSet());
                final CompiledSourceInfo compiledSourceInfo = new CompiledSourceInfo(source,
                        su.getAST().getMainClassName(), classNames, source.getLastModified());
                //System.out.println("SU MainClassName: " + su.getAST().getMainClassName());
                compiledSourceInfoMap.put(source, compiledSourceInfo);
            });

            final List<GroovyClass> cuClassesObject = cu.getClasses();
            final Map<String, Bytecode> bytecodeMap = cuClassesObject.stream()
                    .collect(Collectors.toMap(GroovyClass::getName, c -> new Bytecode(c.getName(), c.getBytes())));

            final Code code;
            if (sources.getSourceSet().size() == 1) {
                code = new DefaultSingleSourceCode(sources.getName(), compiledSourceInfoMap, bytecodeMap);
            } else {
                code = new DefaultCode(sources.getName(), compiledSourceInfoMap, bytecodeMap);
            }
            //System.out.println("--- compile ---");
            return code;
            
        } catch (CompileException e) {
            throw e;
        } catch (Throwable t) {
            throw new CompileException("Compile failed for sources " + sources + ".", t, sources);
        }
    }
    
    /**
     * adds the given source to the given compilation unit and returns the resulting source unit.
     *
     * @param cu compilation unit
     * @param source source
     * @param sources all sources, needed only if the type of source is unsupported,
     *                for the resulting {@link CompileException}
     *
     * @return source unit
     * @throws CompileException if the type of source is unsupported
     * 
     * @since 1.0
     */
    protected SourceUnit addToCompilationUnit(final CompilationUnit cu, final Source source, final Sources sources) {
        if (source instanceof TextSource textSource) {
            return cu.addSource(textSource.getId(), textSource.getText());
        } else if (source instanceof FileSource fileSource) {
            return cu.addSource(fileSource.getFile());
        } else if (source instanceof UrlSource urlSource) {
            return cu.addSource(urlSource.getUrl());
        } else {
            throw new CompileException("Don't know how to compile source " + source + ".", sources);
        }
    }

    /**
     * gets the builder.
     *
     * @return builder
     *
     * @since 1.0
     */
    public Builder getBuilder() {
        return builder;
    }

    /**
     * gets the parent class loader.
     *
     * @return parent class loader
     * 
     * @since 1.0
     */
    public ClassLoader getParent() {
        return parent;
    }
    
    /**
     * gets the compiler configuration.
     *
     * @return compiler configuration
     * 
     * @since 1.0
     */
    public CompilerConfiguration getCompilerConfiguration() {
        return config;
    }
    
    
    /**
     * Builder for instances of {@link DefaultGroovyCompiler}.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        private boolean isCommitted;
        
        private ClassLoader parent;
        private CompilerConfiguration compilerConfiguration;

        /**
         * constructor.
         * 
         * @since 1.0
         */
        public Builder() {
            isCommitted = false;
        }

        /**
         * sets the parent class loader, default is the context class loader
         * of the current thread.
         *
         * @param parent parent class loader
         * 
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setParent(final ClassLoader parent) {
            check();
            this.parent = parent;
            return this;
        }
        
        /**
         * sets the compiler configuration,
         * default is a new instance of {@link CompilerConfiguration} with default settings.
         *
         * @param compilerConfiguration compiler configuration
         *
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setCompilerConfiguration(final CompilerConfiguration compilerConfiguration) {
            check();
            this.compilerConfiguration = compilerConfiguration;
            return this;
        }

        /**
         * gets the parent class loader.
         *
         * @return parent class loader
         * 
         * @since 1.0
         */
        public ClassLoader getParent() {
            return parent;
        }
        
        /**
         * gets the compiler configuration.
         *
         * @return compiler configuration
         * 
         * @since 1.0
         */
        public CompilerConfiguration getCompilerConfiguration() {
            return compilerConfiguration;
        }

        private Builder commit() {
            if (!isCommitted) {
                if (parent == null) {
                    parent = Thread.currentThread().getContextClassLoader();
                }
                if (compilerConfiguration == null) {
                    compilerConfiguration = new CompilerConfiguration();
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds a new instance of {@link DefaultGroovyCompiler}.
         *
         * @return new instance
         *
         * @since 1.0
         */
        public DefaultGroovyCompiler build() {
            commit();
            return new DefaultGroovyCompiler(this);
        }
        
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }


    // dummy compilation customizer as holder for GroovyClassloader,
    // wraps GrapeEngine the first time an instance is created
    static class GrapeCompilationCustomizer extends CompilationCustomizer {

        final GroovyClassLoader runtimeLoader;

        GrapeCompilationCustomizer(final GroovyClassLoader runtimeLoader) {
            super(CompilePhase.INITIALIZATION);
            this.runtimeLoader = runtimeLoader;
            //GrengineGrapeEngine.wrap();
        }

        static void enableGrape(final CompilerConfiguration config,
                final GroovyClassLoader runtimeLoader) {
            config.addCompilationCustomizers(new GrapeCompilationCustomizer(runtimeLoader));
        }

        @Override
        public void call(SourceUnit source, GeneratorContext context, ClassNode classNode)
                throws CompilationFailedException {
        }

        // looks for a GrapeCompilationCustomizer in the given compiler config and,
        // if found, returns a new instance of CompileTimeGroovyClassLoader
        static GroovyClassLoader getLoaderIfConfigured(ClassLoader parent, CompilerConfiguration config) {
            return config.getCompilationCustomizers().stream()
                    .filter(c -> c instanceof GrapeCompilationCustomizer)
                    .findFirst()
                    .map(c -> new CompileTimeGroovyClassLoader(
                            ((GrapeCompilationCustomizer)c).runtimeLoader, parent, config))
                    .orElse(null);
        }

    }

    // wraps the runtime GroovyClassLoader
    static class CompileTimeGroovyClassLoader extends GroovyClassLoader {

        final GroovyClassLoader runtimeLoader;

        CompileTimeGroovyClassLoader(GroovyClassLoader runtimeLoader, ClassLoader parent,
                                     CompilerConfiguration config) {
            super(parent, config);
            this.runtimeLoader = runtimeLoader;
        }

    }

    // wrapper for GrapeEngine, based on inner details of Groovy sources
    static class GrengineGrapeEngine implements GrapeEngine {

        // arg keys
        private static final String CALLEE_DEPTH_KEY = "calleeDepth";
        private static final String CLASS_LOADER_KEY = "classLoader";

        // the lock for calls to GrapeEngine methods
        private final Object lock;

        // the wrapped GrapeEngine
        private final GrapeEngine innerEngine;

        // constructor from engine to wrap
        private GrengineGrapeEngine(final GrapeEngine innerEngine, final Object lock) {
            this.innerEngine = innerEngine;
            this.lock = (lock == null) ? this : lock;
        }

        // sets the engine instance in the Grape class (only once, idempotent if compatible lock)
        static void wrap(final Object lock) {
            synchronized (Grape.class) {

                // any GrapeEngine in the classpath?
                final GrapeEngine engine = Grape.getInstance();
                if (engine == null) {
                    throw new IllegalStateException("Unable to wrap GrapeEngine in Grape.class, " +
                            "no GrapeEngine available (since Groovy 6 that requires a separate " +
                            "Maven dependency like groovy-grape-ivy or groovy-grape-maven).");
                }

                // if already wrapped by a GrengineGrapeEngine, only allow if compatible lock
                if (GrengineGrapeEngine.class.getName().equals(engine.getClass().getName())) {
                    final Object otherLock;
                    if (engine instanceof GrengineGrapeEngine) {
                        otherLock = ((GrengineGrapeEngine)engine).lock;
                    } else {
                        // get lock with reflection
                        final Field lockField;
                        try {
                            lockField = engine.getClass().getDeclaredField("lock");
                        } catch (NoSuchFieldException e) {
                            throw new IllegalStateException("Unable to wrap GrapeEngine in Grape.class because " +
                                    "it is already wrapped by a Grengine using a different mechanism.");
                        }
                        lockField.setAccessible(true);
                        try {
                            otherLock = lockField.get(engine);
                        } catch (IllegalAccessException e) {
                            throw new IllegalStateException("Unable to wrap GrapeEngine in Grape.class because " +
                                    "it is already wrapped by a Grengine using an unobtainable lock.");
                        }
                    }
                    if (lock == null && engine == otherLock) {
                        // already wrapped with the default lock of the engine (compatible)
                        return;
                    } else if (otherLock == lock) {
                        // already wrapped with the same lock
                        return;
                    } else {
                        throw new IllegalStateException("Unable to wrap GrapeEngine in Grape.class because " +
                                "it is already wrapped by a Grengine using an incompatible lock.");
                    }
                }

                // set in Grape class
                final GrengineGrapeEngine grengineGrapeEngine = new GrengineGrapeEngine(engine, lock);
                new Grape() {
                    void wrap() {
                        Grape.instance = grengineGrapeEngine;
                    }
                }.wrap();
            }
        }

        // sets the engine instance in the Grape class back to the wrapped instance
        static void unwrap() {
            synchronized (Grape.class) {

                // wrapped by Grengine?
                final GrapeEngine engine = Grape.getInstance();
                if (engine == null || !GrengineGrapeEngine.class.getName().equals(engine.getClass().getName())) {
                    return;
                }

                // try to obtain the inner engine
                final GrapeEngine innerEngine;
                if (engine instanceof GrengineGrapeEngine) {
                    innerEngine = ((GrengineGrapeEngine)engine).innerEngine;
                } else {
                    // get inner engine with reflection
                    final Field innerEngineField;
                    try {
                        innerEngineField = engine.getClass().getDeclaredField("innerEngine");
                    } catch (NoSuchFieldException e) {
                        throw new IllegalStateException("Unable to unwrap GrapeEngine in Grape.class because " +
                                "it is already wrapped by a Grengine using a different mechanism.");
                    }
                    innerEngineField.setAccessible(true);
                    try {
                        innerEngine = (GrapeEngine)innerEngineField.get(engine);
                    } catch (IllegalAccessException | ClassCastException e) {
                        throw new IllegalStateException("Unable to unwrap GrapeEngine in Grape.class because " +
                                "it is already wrapped by a Grengine using an unobtainable inner engine.");
                    }
                }

                // unwrap
                new Grape() {
                    void unwrap() {
                        Grape.instance = innerEngine;
                    }
                }.unwrap();
            }
        }

        @Override
        public Object grab(final String endorsedModule) {
            synchronized(lock) {
                return innerEngine.grab(endorsedModule);
            }
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Object grab(final Map args) {
            synchronized(lock) {
                adjustCalleeDepth(args);
                adjustClassLoader(args);
                // apply grab also to runtime loader, if present
                final GroovyClassLoader runtimeLoader = getRuntimeLoader(args);
                if (runtimeLoader != null) {
                    final Map args2 = new HashMap(args);
                    args2.put(CLASS_LOADER_KEY, runtimeLoader);
                    innerEngine.grab(args2);
                }
                return innerEngine.grab(args);
            }
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public Object grab(final Map args, final Map... dependencies) {
            synchronized(lock) {
                adjustCalleeDepth(args);
                adjustClassLoader(args);
                // apply grab also to runtime loader, if present
                final GroovyClassLoader runtimeLoader = getRuntimeLoader(args);
                if (runtimeLoader != null) {
                    final Map args2 = new HashMap(args);
                    args2.put(CLASS_LOADER_KEY, runtimeLoader);
                    innerEngine.grab(args2, dependencies);
                }
                return innerEngine.grab(args, dependencies);
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private GroovyClassLoader getRuntimeLoader(final Map args) {
            final Object obj = args.get(CLASS_LOADER_KEY);
            if (obj instanceof CompileTimeGroovyClassLoader) {
                return ((CompileTimeGroovyClassLoader) obj).runtimeLoader;
            }
            if (obj != null && CompileTimeGroovyClassLoader.class.getName().equals(obj.getClass().getName())) {
                // get runtime loader with reflection
                final Field runtimeLoaderField;
                try {
                    runtimeLoaderField = obj.getClass().getDeclaredField("runtimeLoader");
                } catch (NoSuchFieldException e) {
                    return null;
                }
                runtimeLoaderField.setAccessible(true);
                try {
                    return (GroovyClassLoader)runtimeLoaderField.get(obj);
                } catch (IllegalAccessException | ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public Map<String, Map<String, List<String>>> enumerateGrapes() {
            synchronized(lock) {
                return innerEngine.enumerateGrapes();
            }
        }

        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public URI[] resolve(final Map args, final Map... dependencies) {
            synchronized(lock) {
                adjustCalleeDepth(args);
                adjustClassLoader(args);
                return innerEngine.resolve(args, dependencies);
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public URI[] resolve(final Map args, final List dependenciesInfo, final Map... dependencies) {
            synchronized(lock) {
                return innerEngine.resolve(args, dependenciesInfo, dependencies);
            }
        }

        @Override
        @SuppressWarnings("rawtypes")
        public Map[] listDependencies(final ClassLoader classLoader) {
            synchronized(lock) {
                return innerEngine.listDependencies(classLoader);
            }
        }

        @Override
        public void addResolver(final Map<String, Object> args) {
            synchronized(lock) {
                innerEngine.addResolver(args);
            }
        }

        private void adjustCalleeDepth(Map<String, Object> args) {
            Object calleeDepthObj = args.get(CALLEE_DEPTH_KEY);
            // see unit test that verifies that the default does not change
            int calleeDepth = (calleeDepthObj == null) ? GrapeEngine.DEFAULT_CALLEE_DEPTH : (int)calleeDepthObj;
            args.put(CALLEE_DEPTH_KEY, calleeDepth + 1);
        }

        // see GROOVY-12458 (Maven engine with Groovy 6.0.0); even when fixed
        // could still affect future engines, hence this will probably stay
        // in here, does no harm
        private void adjustClassLoader(Map<String, Object> args) {
            final Object obj = args.get(CLASS_LOADER_KEY);
            if (!(obj instanceof ClassLoader) || isValidTargetClassLoader((ClassLoader) obj)) {
                return;
            }
            ClassLoader loader = ((ClassLoader) obj).getParent();
            while (loader != null && !isValidTargetClassLoader(loader)) {
                loader = loader.getParent();
            }
            if (loader != null) {
                args.put(CLASS_LOADER_KEY, loader);
            }
        }

        private static boolean isValidTargetClassLoader(ClassLoader loader) {
            return isValidTargetClassLoaderClass(loader.getClass());
        }

        private static boolean isValidTargetClassLoaderClass(Class<?> clazz) {
            return clazz != null && (clazz.getName().equals(GroovyClassLoader.class.getName())
                    || clazz.getName().equals(RootLoader.class.getName())
                    || isValidTargetClassLoaderClass(clazz.getSuperclass()));
        }

    }

}
