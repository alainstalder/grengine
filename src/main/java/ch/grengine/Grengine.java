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

package ch.grengine;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.engine.Engine;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.engine.Loader;
import ch.grengine.except.CompileException;
import ch.grengine.except.GrengineException;
import ch.grengine.except.LoadException;
import ch.grengine.load.DefaultTopCodeCacheFactory;
import ch.grengine.load.TopCodeCacheFactory;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.DirBasedSources;
import ch.grengine.sources.DirMode;
import ch.grengine.sources.FixedSetSources;
import ch.grengine.sources.Sources;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilerConfiguration;


/**
 * Grengine.
 * <p>
 * See {@link BaseGrengine} for most convenience methods for using Grengine.
 * <p>
 * The matrix for running scripts is as follows:
 * <ul>
 * <li>{@code run(<loader>,<source>,<binding>)}
 * <li>{@code <loader>}: can be a specific loader or be omitted for the default loader
 * <li>{@code <source>}: can be an instance of source or indicated by script text, file or URL
 * <li>{@code <binding>}: can be an instance of {@link Binding}, a map or be omitted for an empty binding
 * </ul>
 * <p>
 * Examples:
 * <ul>
 * <li>{@code run("println 'hello world!'")}: default loader, text-based source, empty binding
 * <li>{@code run(myLoader, new URL("http://acme.org/myScript.groovy", myBinding)}:
 *     given loader, URL-based source, given binding
 * </ul>
 * <p>
 * The matrices for loading classes, creating script instances and for creating sources are similar:
 * <ul>
 * <li>{@code load(<loader>,<source>)}
 * <li>{@code create(<loader>,<source>)}
 * <li>{@code source(<source>)}
 * </ul>
 * <p>
 * Grengine performance:
 * <ul>
 * <li>In my experience it is crucial to measure performance under circumstances
 *     that come as close as possible to the actual application.
 * <li>What can be said independently of actual circumstances is:
 *     <ul>
 *     <li>Compiling a very simple script is of the order of <em>milliseconds.</em>
 *     <li>Running it with Grengine by script text or script file is of the order of <em>microseconds</em>
 *         (except for the first run or other circumstances where (re-)compilation is mandated).
 *     <li>Running it from an already created instance of {@link Script} is of the order of <em>nanoseconds</em>.
 *     </ul>
 * <li>Run the GrengineVisualPerformanceTest and see the options of {@link DefaultSourceFactory}.
 * </ul>
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class Grengine extends BaseGrengine {

    /**
     * constant for an infinite latency (static).
     *
     * @since 1.0
     */
    public static final long LATENCY_MS_INFINITE_STATIC = Long.MAX_VALUE;

    private final Builder builder;
    private final List<Sources> sourcesLayers;
    private final long latencyMs;

    private volatile List<Long> lastModifiedList;
    private volatile long lastChecked;
    private volatile GrengineException lastUpdateException;
    private final UpdateExceptionNotifier updateExceptionNotifier;

    /**
     * constructor from builder.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need
     * to make sure all sources could be compiled without errors.
     *
     * @param builder builder
     *
     * @since 1.0
     */
    protected Grengine(Builder builder) {
        this.builder = builder.commit();
        engine = builder.getEngine();
        sourceFactory = builder.getSourceFactory();
        sourcesLayers = builder.getSourcesLayers();
        latencyMs = builder.getLatencyMs();

        // initialize such that sources layers will be loaded at first update
        // further below, even if sources are immutable or latency is infinite
        int n = sourcesLayers.size();
        lastModifiedList = new ArrayList<>(n);
        for (int i=0; i<n; i++) {
            lastModifiedList.add(-1L);
        }
        lastChecked = 0;
        lastUpdateException = null;
        updateExceptionNotifier = builder.getUpdateExceptionNotifier();
        updateEngineIfSourcesLayersModified();

        loader = engine.getLoader();
    }

    /**
     * constructor for a Grengine without any (static) code layers,
     * but with a top code cache.
     * <p>
     * Constructed from a builder with default settings.
     *
     * @since 1.0
     */
    public Grengine() {
        this(new Builder());
    }

    /**
     * constructor for a Grengine without any (static) code layers,
     * but with a top code cache.
     * <p>
     * Constructed from a builder with default settings.
     *
     * @param config compiler configuration to use for compiling all sources
     *
     * @throws IllegalArgumentException if the compiler configuration is null
     *
     * @since 1.0
     */
    public Grengine(final CompilerConfiguration config) {
        this(builderEmpty((ClassLoader)null, true, config, false));
    }

    /**
     * constructor for a Grengine without any (static) code layers,
     * but with a top code cache.
     * <p>
     * Constructed from a builder with default settings.
     *
     * @param parent parent class loader for the engine
     *
     * @throws IllegalArgumentException if the parent class loader is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent) {
        this(builderEmpty(parent, false, (CompilerConfiguration)null, true));
    }

    /**
     * constructor for a Grengine without any (static) code layers,
     * but with a top code cache.
     * <p>
     * Constructed from a builder with default settings.
     *
     * @param parent parent class loader for the engine
     * @param config compiler configuration to use for compiling all sources
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent, final CompilerConfiguration config) {
        this(builderEmpty(parent, false, config, false));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, without subdirectories, and with a top code cache.
     * <p>
     * Script extensions are the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param dir script directory
     *
     * @throws IllegalArgumentException if the directory is null
     *
     * @since 1.0
     */
    public Grengine(final File dir) {
        this(builderFromDir((ClassLoader)null, true, (CompilerConfiguration)null, true, dir, DirMode.NO_SUBDIRS));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, without subdirectories, and with a top code cache.
     * <p>
     * Script extensions are the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param parent parent class loader for the engine
     * @param dir script directory
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent, final File dir) {
        this(builderFromDir(parent, false, (CompilerConfiguration)null, true, dir, DirMode.NO_SUBDIRS));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, without subdirectories, and with a top code cache.
     * <p>
     * Script extensions are taken from the given compiler configuration,
     * which defaults to the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param config compiler configuration to use for compiling all sources
     * @param dir script directory
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0
     */
    public Grengine(final CompilerConfiguration config, final File dir) {
        this(builderFromDir((ClassLoader)null, true, config, false, dir, DirMode.NO_SUBDIRS));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, without subdirectories, and with a top code cache.
     * <p>
     * Script extensions are taken from the given compiler configuration,
     * which defaults to the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param parent parent class loader for the engine
     * @param config compiler configuration to use for compiling all sources
     * @param dir script directory
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent, final CompilerConfiguration config, final File dir) {
        this(builderFromDir(parent, false, config, false, dir, DirMode.NO_SUBDIRS));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, optionally including subdirectories (recursively),
     * and with a top code cache.
     * <p>
     * Script extensions are the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param dir script directory
     * @param dirMode dir mode
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0
     */
    public Grengine(final File dir, final DirMode dirMode) {
        this(builderFromDir((ClassLoader)null, true, (CompilerConfiguration)null, true, dir, dirMode));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, optionally including subdirectories (recursively),
     * and with a top code cache.
     * <p>
     * Script extensions are the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param parent parent class loader for the engine
     * @param dir script directory
     * @param dirMode dir mode
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0.3
     */
    public Grengine(ClassLoader parent, final File dir, final DirMode dirMode) {
        this(builderFromDir(parent, false, (CompilerConfiguration)null, true, dir, dirMode));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, optionally including subdirectories (recursively),
     * and with a top code cache.
     * <p>
     * Script extensions are taken from the given compiler configuration,
     * which defaults to the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param config compiler configuration to use for compiling all sources
     * @param dir script directory
     * @param dirMode dir mode
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0
     */
    public Grengine(final CompilerConfiguration config, final File dir, final DirMode dirMode) {
        this(builderFromDir((ClassLoader)null, true, config, false, dir, dirMode));
    }

    /**
     * constructor for a Grengine based on scripts in a given script
     * directory, optionally including subdirectories (recursively),
     * and with a top code cache.
     * <p>
     * Script extensions are taken from the given compiler configuration,
     * which defaults to the single extension "groovy".
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param parent parent class loader for the engine
     * @param config compiler configuration to use for compiling all sources
     * @param dir script directory
     * @param dirMode dir mode
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent, final CompilerConfiguration config, final File dir, final DirMode dirMode) {
        this(builderFromDir(parent, false, config, false, dir, dirMode));
    }

    /**
     * constructor for a Grengine based on a collection of URL-based scripts,
     * with a top code cache, and with tracking URLs based on content.
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Uses a {@link DefaultSourceFactory} with tracking URL content set to true.
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param urls collection of URL-based scripts
     *
     * @throws IllegalArgumentException if the URL collection is null
     *
     * @since 1.0
     */
    public Grengine(final Collection<URL> urls) {
        this(builderFromUrls((ClassLoader)null, true, (CompilerConfiguration)null, true, urls));
    }

    /**
     * constructor for a Grengine based on a collection of URL-based scripts,
     * with a top code cache, and with tracking URLs based on content.
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Uses a {@link DefaultSourceFactory} with tracking URL content set to true.
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param parent parent class loader for the engine
     * @param urls collection of URL-based scripts
     *
     * @throws IllegalArgumentException if the URL collection is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent, final Collection<URL> urls) {
        this(builderFromUrls(parent, false, (CompilerConfiguration)null, true, urls));
    }

    /**
     * constructor for a Grengine based on a collection of URL-based scripts,
     * with a top code cache, and with tracking URLs based on content.
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Uses a {@link DefaultSourceFactory} with tracking URL content set to true.
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param config compiler configuration to use for compiling all sources
     * @param urls collection of URL-based scripts
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0
     */
    public Grengine(final CompilerConfiguration config, final Collection<URL> urls) {
        this(builderFromUrls((ClassLoader)null, true, config, false, urls));
    }

    /**
     * constructor for a Grengine based on a collection of URL-based scripts,
     * with a top code cache, and with tracking URLs based on content.
     * <p>
     * Load modes are "current first" for the script directory code layer
     * and "parent first" for the top code cache.
     * <p>
     * Uses a {@link DefaultSourceFactory} with tracking URL content set to true.
     * Constructed from builders with otherwise default settings.
     * <p>
     * Call {@link #getLastUpdateException()} after constructing if you need to make sure
     * all sources can be compiled without errors.
     *
     * @param parent parent class loader for the engine
     * @param config compiler configuration to use for compiling all sources
     * @param urls collection of URL-based scripts
     *
     * @throws IllegalArgumentException if any argument is null
     *
     * @since 1.0.3
     */
    public Grengine(final ClassLoader parent, final CompilerConfiguration config, final Collection<URL> urls) {
        this(builderFromUrls(parent, false, config, false, urls));
    }

    private static Builder builderEmpty(final ClassLoader parent, final boolean allowParentNull,
                                        final CompilerConfiguration config, final boolean allowConfigNull) {
        if (parent == null && !allowParentNull) {
            throw new IllegalArgumentException("Parent class loader is null.");
        }
        if (config == null && !allowConfigNull) {
            throw new IllegalArgumentException("Compiler configuration is null.");
        }
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory.Builder()
                .setCompilerConfiguration(config)
                .build();
        TopCodeCacheFactory topCodeCacheFactory = new DefaultTopCodeCacheFactory(compilerFactory);
        Engine engine = new LayeredEngine.Builder()
                .setParent(parent)
                .setTopCodeCacheFactory(topCodeCacheFactory)
                .build();
        return new Builder().setEngine(engine);
    }

    private static Builder builderFromDir(final ClassLoader parent, final boolean allowParentNull,
                                          final CompilerConfiguration config, final boolean allowConfigNull,
                                          final File dir, final DirMode dirMode) {
        if (parent == null && !allowParentNull) {
            throw new IllegalArgumentException("Parent class loader is null.");
        }
        if (config == null && !allowConfigNull) {
            throw new IllegalArgumentException("Compiler configuration is null.");
        }
        if (dir == null) {
            throw new IllegalArgumentException("Directory is null.");
        }
        if (dirMode == null) {
            throw new IllegalArgumentException("Dir mode is null.");
        }
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory.Builder()
                .setCompilerConfiguration(config)
                .build();
        TopCodeCacheFactory topCodeCacheFactory = new DefaultTopCodeCacheFactory(compilerFactory);
        Engine engine = new LayeredEngine.Builder()
                .setParent(parent)
                .setTopCodeCacheFactory(topCodeCacheFactory)
                .build();
        Sources sources = new DirBasedSources.Builder(dir)
                .setScriptExtensions(config == null ? null : config.getScriptExtensions())
                .setCompilerFactory(compilerFactory)
                .setDirMode(dirMode)
                .build();
        return new Builder()
                .setEngine(engine)
                .setSourcesLayers(sources);
    }

    private static Builder builderFromUrls(final ClassLoader parent, final boolean allowParentNull,
                                           final CompilerConfiguration config, final boolean allowConfigNull,
                                           final Collection<URL> urls) {
        if (parent == null && !allowParentNull) {
            throw new IllegalArgumentException("Parent class loader is null.");
        }
        if (config == null && !allowConfigNull) {
            throw new IllegalArgumentException("Compiler configuration is null.");
        }
        if (urls == null) {
            throw new IllegalArgumentException("URL collection is null.");
        }
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory.Builder()
                .setCompilerConfiguration(config)
                .build();
        TopCodeCacheFactory topCodeCacheFactory = new DefaultTopCodeCacheFactory(compilerFactory);
        Engine engine = new LayeredEngine.Builder()
                .setParent(parent)
                .setTopCodeCacheFactory(topCodeCacheFactory)
                .build();
        SourceFactory sourceFactory = new DefaultSourceFactory.Builder().setTrackUrlContent(true).build();
        Set<Source> sourceSet = SourceUtil.urlsToSourceSet(sourceFactory, urls);
        Sources sources = new FixedSetSources.Builder(sourceSet)
                .setName("URL Sources")
                .setCompilerFactory(compilerFactory)
                .build();
        return new Builder()
                .setEngine(engine)
                .setSourceFactory(sourceFactory)
                .setSourcesLayers(sources);
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
     * updates code layers if necessary and gets the exception that occurred at the last update.
     * 
     * @return last exception or null if none
     * 
     * @since 1.0
     */
    public GrengineException getLastUpdateException() {
       updateEngineIfSourcesLayersModified();
       return lastUpdateException;
    }

    /**
     * updates the engine if sources layers have been modified.
     * 
     * @since 1.0
     */
    protected void updateEngineIfSourcesLayersModified() {
        
        // check if lastChecked is 0 to make sure layers are set once if latency is infinite
        // check both boundaries of the interval to exclude problems with leap seconds etc.
        long diff = System.currentTimeMillis() - lastChecked;
        if (lastChecked != 0 && diff >= 0 && diff < latencyMs) {
            return;
        }

        // check layers for changes
        
        int n = sourcesLayers.size();
        List<Long> lastModifiedListNew = new ArrayList<>(n);
        for (Sources sources : sourcesLayers) {
            lastModifiedListNew.add(sources.getLastModified());
        }
        
        boolean hasChanged = false;
        for (int i=0; i<n; i++) {
            if ((long)lastModifiedList.get(i) != (long)lastModifiedListNew.get(i)) {
                hasChanged = true;
                break;
            }
        }
        
        if (!hasChanged) {
            lastChecked = System.currentTimeMillis();
            return;
        }
        
        // layers have changed, update engine...
        
        synchronized(this) {
            
            // prevent multiple updates
            diff = System.currentTimeMillis() - lastChecked;
            if (lastChecked != 0 && diff >= 0 && diff < latencyMs) {
                return;
            }
            
            lastModifiedList = lastModifiedListNew;
            try {
                engine.setCodeLayersBySource(sourcesLayers);
                lastUpdateException = null;
            } catch (CompileException e) {
                lastUpdateException = e;
            } catch (Exception e) {
                lastUpdateException = new GrengineException("Failed to update Grengine.", e);
            }
            lastChecked = System.currentTimeMillis();
            if (updateExceptionNotifier != null) {
                updateExceptionNotifier.notify(lastUpdateException);
            }
        }
    }

    @Override
    public Class<?> loadMainClass(final Loader loader, final Source source)
            throws CompileException, LoadException {
        updateEngineIfSourcesLayersModified();
        return engine.loadMainClass(loader, source);
    }

    @Override
    public Class<?> loadMainClass(final Source source)
            throws CompileException, LoadException {
        updateEngineIfSourcesLayersModified();
        return engine.loadMainClass(loader, source);
    }

    @Override
    public Class<?> loadClass(final Loader loader, final Source source, final String name)
            throws CompileException, LoadException {
        updateEngineIfSourcesLayersModified();
        return engine.loadClass(loader, source, name);
    }

    @Override
    public Class<?> loadClass(final Source source, final String name)
            throws CompileException, LoadException {
        updateEngineIfSourcesLayersModified();
        return engine.loadClass(loader, source, name);
    }

    @Override
    public Class<?> loadClass(final Loader loader, final String name) throws LoadException {
        updateEngineIfSourcesLayersModified();
        return engine.loadClass(loader, name);
    }

    @Override
    public Class<?> loadClass(final String name) throws LoadException {
        updateEngineIfSourcesLayersModified();
        return engine.loadClass(loader, name);
    }

    
    /**
     * Builder for instances of {@link Grengine}.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        /** 
         * the default latency (5000ms = five seconds).
         * 
         * @since 1.0
         */
        public static final long DEFAULT_LATENCY_MS = 5000L;
        
        private boolean isCommitted;
        
        private Engine engine;
        private SourceFactory sourceFactory;
        private List<Sources> sourcesLayers;
        private UpdateExceptionNotifier updateExceptionNotifier;
        private long latencyMs = -1;
        
        /**
         * constructor.
         * 
         * @since 1.0
         */
        public Builder() {
            isCommitted = false;
        }
        
        /**
         * sets the engine, default is a new instance of {@link LayeredEngine}
         * with default settings.
         *
         * @param engine engine
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setEngine(final Engine engine) {
            check();
            this.engine = engine;
            return this;
        }

        /**
         * sets the source factory for creating sources, default is a new instance
         * of {@link DefaultSourceFactory} with default settings.
         *
         * @param sourceFactory source factory
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setSourceFactory(final SourceFactory sourceFactory) {
            check();
            this.sourceFactory = sourceFactory;
            return this;
        }
        
        /**
         * sets the sources layers, default is no layers.
         *
         * @param sourcesLayers sources layers
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setSourcesLayers(final List<Sources> sourcesLayers) {
            check();
            this.sourcesLayers = sourcesLayers;
            return this;
        }
        
        /**
         * sets the sources layers, default is no layers.
         *
         * @param sourcesLayers sources layers
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setSourcesLayers(final Sources... sourcesLayers) {
            return setSourcesLayers(Arrays.asList(sourcesLayers));
        }

        /**
         * sets the update notification notifier, default none (null).
         *
         * @param updateExceptionNotifier update notification notifier
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setUpdateExceptionNotifier(final UpdateExceptionNotifier updateExceptionNotifier) {
            check();
            this.updateExceptionNotifier = updateExceptionNotifier;
            return this;
        }
        
        /**
         * sets the latency in milliseconds for checking if need to
         * recompile sources layers, default is {@link #DEFAULT_LATENCY_MS}.
         *
         * @param latencyMs latency in milliseconds
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setLatencyMs(final long latencyMs) {
            check();
            this.latencyMs = latencyMs;
            return this;
        }

        /**
         * gets the engine.
         *
         * @return engine
         * 
         * @since 1.0
         */
        public Engine getEngine() {
            return engine;
        }
        
        /**
         * gets the source factory for creating sources.
         *
         * @return source factory
         * 
         * @since 1.0
         */
        public SourceFactory getSourceFactory() {
            return sourceFactory;
        }
        
        /**
         * gets the sources layers.
         *
         * @return sources layers
         * 
         * @since 1.0
         */
        public List<Sources> getSourcesLayers() {
            return sourcesLayers;
        }
        
        /**
         * gets the update notification notifier.
         *
         * @return update notification notifier
         * 
         * @since 1.0
         */
        public UpdateExceptionNotifier getUpdateExceptionNotifier() {
            return updateExceptionNotifier;
        }
        
        /**
         * gets the latency in milliseconds.
         *
         * @return latency in milliseconds
         * 
         * @since 1.0
         */
        public long getLatencyMs() {
            return latencyMs;
        }
        
        private Builder commit() {
            if (!isCommitted) {
                if (engine == null) {
                    engine = new LayeredEngine.Builder().build();
                }
                if (sourceFactory == null) {
                    sourceFactory = new DefaultSourceFactory();
                }
                if (sourcesLayers == null) {
                    sourcesLayers = new LinkedList<>();
                }
                if (latencyMs < 0) {
                    latencyMs = DEFAULT_LATENCY_MS;
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds a new instance of {@link Grengine}.
         *
         * @return new instance
         * 
         * @since 1.0
         */
        public Grengine build() {
            commit();
            return new Grengine(this);
        }
                
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

    /**
     * Convenience class for easily getting a Grengine that works with Grape,
     * eliminating also a Groovy issue related to Grape (GROOVY-7407).
     * <p>
     * Call {@link #activate()} once before using the <code>newGrengine()</code>
     * methods to create a Grengine instance that works with Grape.
     * <p>
     * See the user manual at "Grengine and Grape" and the {@link DefaultGroovyCompiler}
     * class for more information.
     *
     * @since 1.2
     *
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Grape {

        /**
         * activate Grape.
         * <p>
         * See {@link DefaultGroovyCompiler#enableGrapeSupport()} and
         * the user manual for more information.
         *
         * @since 1.2
         */
        public static void activate() {
            DefaultGroovyCompiler.enableGrapeSupport();
        }

        /**
         * activate Grape.
         * <p>
         * See {@link DefaultGroovyCompiler#enableGrapeSupport(Object)} and
         * the user manual for more information.
         *
         * @param lock lock to use
         *
         * @since 1.2
         */
        public static void activate(final Object lock) {
            DefaultGroovyCompiler.enableGrapeSupport(lock);
        }

        /**
         * deactivate Grape.
         * <p>
         * See {@link DefaultGroovyCompiler#disableGrapeSupport()} and the user
         * manual for more information.
         *
         * @since 1.2
         */
        public static void deactivate() {
            DefaultGroovyCompiler.disableGrapeSupport();
        }


        /**
         * equivalent to {@link Grengine#Grengine()},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @return new instance
         *
         * @since 1.2
         */
        public static Grengine newGrengine() {
            final GroovyClassLoader parent = new GroovyClassLoader();
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderEmpty(parent, false, config, false));
        }

        /**
         * equivalent to {@link Grengine#Grengine(CompilerConfiguration)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         *
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         *
         * @return new instance
         * @throws IllegalArgumentException if the compiler configuration is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final CompilerConfiguration config) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderEmpty(parent, false, config, false));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader)},
         * with the following differences.
         * <p>
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param parent parent class loader for the engine
         *
         * @return new instance
         * @throws IllegalArgumentException if the parent class loader is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent) {
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderEmpty(parent, false, config, false));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, CompilerConfiguration)}.
         *
         * @param parent parent class loader for the engine
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final CompilerConfiguration config) {
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderEmpty(parent, false, config, false));
        }


        /**
         * equivalent to {@link Grengine#Grengine(File)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param dir script directory
         *
         * @return new instance
         * @throws IllegalArgumentException if the directory is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final File dir) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, DirMode.NO_SUBDIRS));
        }

        /**
         * equivalent to {@link Grengine#Grengine(CompilerConfiguration, File)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         *
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         * @param dir script directory
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final CompilerConfiguration config, final File dir) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, DirMode.NO_SUBDIRS));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, File)},
         * with the following differences.
         * <p>
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param parent parent class loader for the engine
         * @param dir script directory
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final File dir) {
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, DirMode.NO_SUBDIRS));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, CompilerConfiguration, File)}.
         *
         * @param parent parent class loader for the engine
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         * @param dir script directory
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final CompilerConfiguration config, final File dir) {
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, DirMode.NO_SUBDIRS));
        }


        /**
         * equivalent to {@link Grengine#Grengine(File, DirMode)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param dir script directory
         * @param dirMode dir mode
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         * 
         * @since 1.2
         */
        public static Grengine newGrengine(final File dir, final DirMode dirMode) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, dirMode));
        }

        /**
         * equivalent to {@link Grengine#Grengine(CompilerConfiguration, File, DirMode)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         *
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         * @param dir script directory
         * @param dirMode dir mode
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final CompilerConfiguration config, final File dir, final DirMode dirMode) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, dirMode));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, File, DirMode)},
         * with the following differences.
         * <p>
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param parent parent class loader for the engine
         * @param dir script directory
         * @param dirMode dir mode
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final File dir, final DirMode dirMode) {
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, dirMode));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, CompilerConfiguration, File, DirMode)}.
         *
         * @param parent parent class loader for the engine
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         * @param dir script directory
         * @param dirMode dir mode
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final CompilerConfiguration config,
                final File dir, final DirMode dirMode) {
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromDir(parent, false, config, false,
                    dir, dirMode));
        }


        /**
         * equivalent to {@link Grengine#Grengine(Collection)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param urls collection of URL-based scripts
         *
         * @return new instance
         * @throws IllegalArgumentException if the URL collection is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final Collection<URL> urls) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromUrls(parent, false, config, false, urls));
        }

        /**
         * equivalent to {@link Grengine#Grengine(CompilerConfiguration, Collection)},
         * with the following differences.
         * <p>
         * Parent class loader is a GroovyClassLoader with the context
         * class loader of the current thread as its parent.
         *
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         * @param urls collection of URL-based scripts
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final CompilerConfiguration config, final Collection<URL> urls) {
            final GroovyClassLoader parent = new GroovyClassLoader();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromUrls(parent, false, config, false, urls));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, Collection)},
         * with the following differences.
         * <p>
         * Compiler configuration is the default compiler configuration,
         * with added Grape support.
         *
         * @param parent parent class loader for the engine
         * @param urls collection of URL-based scripts
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final Collection<URL> urls) {
            final CompilerConfiguration config = new CompilerConfiguration();
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromUrls(parent, false, config, false, urls));
        }

        /**
         * equivalent to {@link Grengine#Grengine(ClassLoader, CompilerConfiguration, Collection)}.
         *
         * @param parent parent class loader for the engine
         * @param config compiler configuration to use for compiling all sources,
         *               Grape support is added to it
         * @param urls collection of URL-based scripts
         *
         * @return new instance
         * @throws IllegalArgumentException if any argument is null
         *
         * @since 1.2
         */
        public static Grengine newGrengine(final GroovyClassLoader parent, final CompilerConfiguration config,
                final Collection<URL> urls) {
            DefaultGroovyCompiler.withGrape(config, parent);
            return new Grengine(builderFromUrls(parent, false, config, false, urls));
        }

    }

}
