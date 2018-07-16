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

import ch.grengine.code.Code;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.SingleSourceCode;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.except.CompileException;
import ch.grengine.except.LoadException;
import ch.grengine.source.Source;
import ch.grengine.sources.Sources;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * {@link SourceClassLoader} based on layers of {@link BytecodeClassLoader}
 * plus optionally a {@link TopCodeCache}.
 * <p>
 * Depending on load modes, classes are first searched in the parent class
 * loader, the individual layers or optionally created on-the-fly from source
 * as part of the top code cache.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class LayeredClassLoader extends SourceClassLoader {
    
    private final Builder builder;
    
    private List<Code> codeLayers;
    // the class loader on top, without top code cache
    private ClassLoader staticTopLoader;

    private boolean isWithTopCodeCache;
    private LoadMode topLoadMode;
    private TopCodeCache topCodeCache;
    private Map<Source,BytecodeClassLoader> topLoaders;

    private final Queue<WeakReference<BytecodeClassLoader>> classLoaderQueue =
            new ConcurrentLinkedQueue<>();

    /**
     * constructor from builder, based on already compiled code layers.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected LayeredClassLoader(final Builder builder) {
        super(builder.getParent());
        this.builder = builder.commit();
        fromCodeLayers();
    }
    
    /**
     * constructor from builder, based on sources to compile to code layers
     * or on already compiled code layers.
     *
     * @param builder builder
     * @param fromSourcesLayers if based on sources to compile to code layers
     *                          or on already compiled code layers
     *
     * @throws CompileException if based on sources and compiling the sources layers failed
     * 
     * @since 1.0
     */
    protected LayeredClassLoader(final Builder builder, final boolean fromSourcesLayers) {
        super(builder.getParent());
        this.builder = builder.commit();
        if (fromSourcesLayers) {
            fromSourcesLayers();
        } else {
            fromCodeLayers();
        }
    }
    
    private void fromCodeLayers() {
        createLoadersFromCodeLayers();
        initTopCodeCache();
    }
    
    private void fromSourcesLayers() {
        createLoadersFromSourcesLayers();
        initTopCodeCache();
    }
    
    private void createLoadersFromCodeLayers() {
        staticTopLoader = builder.getParent();
        codeLayers = builder.getCodeLayers();
        for (Code code : codeLayers) {
            staticTopLoader = new BytecodeClassLoader(staticTopLoader, builder.getLoadMode(), code);
            classLoaderQueue.add(new WeakReference<>((BytecodeClassLoader) staticTopLoader));
        }
    }
    
    private void createLoadersFromSourcesLayers() {
        staticTopLoader = builder.getParent();
        List<Sources> sourcesLayers = builder.getSourcesLayers();
        codeLayers = new LinkedList<>();
        for (Sources sources : sourcesLayers) {
            CompilerFactory compilerFactory = sources.getCompilerFactory();
            Code code = compilerFactory.newCompiler(staticTopLoader).compile(sources);
            codeLayers.add(code);
            staticTopLoader = new BytecodeClassLoader(staticTopLoader, builder.getLoadMode(), code);
            classLoaderQueue.add(new WeakReference<>((BytecodeClassLoader) staticTopLoader));
        }
        // set code layers in builder so that the builder
        // can be reused without recompiling (e.g. for clone())
        builder.setCodeLayersAfterCreating(codeLayers);
    }
    
    private void initTopCodeCache() {
        isWithTopCodeCache = builder.isWithTopCodeCache();
        if (isWithTopCodeCache) {
            topLoadMode = builder.getTopLoadMode();
            topCodeCache = builder.getTopCodeCache();
            topLoaders = new ConcurrentHashMap<>();
        } else {
            topLoadMode = null;
            topCodeCache = null;
            topLoaders = null;
        }
    }

        
    @Override
    protected Class<?> loadClass(final String name, boolean resolve) throws ClassNotFoundException {
        // can only be done statically...
        Class<?> clazz = staticTopLoader.loadClass(name);
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }
    
    @Override
    public Class<?> loadMainClass(final Source source) {
        
        // loading from code layers only?
        if (!isWithTopCodeCache) {
            return BytecodeClassLoader.loadMainClassBySource(staticTopLoader, source);
        }
        
        // code layers version available and is up-to-date or parent first?
        BytecodeClassLoader staticLoader = findBytecodeClassLoaderBySource(source);
        if (staticLoader != null) {
            long lastModifiedAtCompileTime = staticLoader.getCode().getLastModifiedAtCompileTime(source);
            if (topLoadMode == LoadMode.PARENT_FIRST || lastModifiedAtCompileTime == source.getLastModified()) {
                return BytecodeClassLoader.loadMainClassBySource(staticTopLoader, source);
            }
        }

        // load from top code cache
        SingleSourceCode code = topCodeCache.getUpToDateCode(source);
        BytecodeClassLoader topLoader = topLoaders.get(source);
        if (topLoader == null || ((SingleSourceCode)topLoader.getCode()).getLastModifiedAtCompileTime() 
                != code.getLastModifiedAtCompileTime()) {
            topLoader = new BytecodeClassLoader(this, topLoadMode, code);
            topLoaders.put(source, topLoader);
            classLoaderQueue.add(new WeakReference<>(topLoader));
        }
        return topLoader.loadMainClass(source);
    }

    @Override
    public Class<?> loadClass(final Source source, final String name) {
        
        // loading from static layers only?
        if (!isWithTopCodeCache) {
            return BytecodeClassLoader.loadClassBySourceAndName(staticTopLoader, source, name);
        }

        // code layers version available and is up-to-date or parent first?
        BytecodeClassLoader staticLoader = findBytecodeClassLoaderBySource(source);
        if (staticLoader != null) {
            long lastModifiedAtCompileTime = staticLoader.getCode().getLastModifiedAtCompileTime(source);
            if (topLoadMode == LoadMode.PARENT_FIRST || lastModifiedAtCompileTime == source.getLastModified()) {
                return BytecodeClassLoader.loadClassBySourceAndName(staticTopLoader, source, name);
            }
        }

        // load from top code cache
        SingleSourceCode code = topCodeCache.getUpToDateCode(source);
        BytecodeClassLoader topLoader = topLoaders.get(source);
        if (topLoader == null || ((SingleSourceCode)topLoader.getCode()).getLastModifiedAtCompileTime()
                != code.getLastModifiedAtCompileTime()) {
            topLoader = new BytecodeClassLoader(this, topLoadMode, code);
            topLoaders.put(source, topLoader);
            classLoaderQueue.add(new WeakReference<>(topLoader));
        }
        return topLoader.loadClass(source, name);
    }
    
    @Override
    public BytecodeClassLoader findBytecodeClassLoaderBySource(final Source source) {
        BytecodeClassLoader loader = null;
        if (getLoadMode() == LoadMode.PARENT_FIRST) {
            if (getParent() instanceof SourceClassLoader) {
                loader = ((SourceClassLoader)getParent()).findBytecodeClassLoaderBySource(source);
            }
            if (loader == null && staticTopLoader instanceof SourceClassLoader) {
                loader = ((SourceClassLoader)staticTopLoader).findBytecodeClassLoaderBySource(source);
            }
        } else {
            if (staticTopLoader instanceof SourceClassLoader) {
                loader = ((SourceClassLoader)staticTopLoader).findBytecodeClassLoaderBySource(source);
            }
            if (loader == null && (getParent() instanceof SourceClassLoader)) {
                loader = ((SourceClassLoader)getParent()).findBytecodeClassLoaderBySource(source);
            }
        }
        return loader;
    }
    
    @Override
    public LoadMode getLoadMode() {
        return builder.getLoadMode();
    }
    
    /**
     * creates a clone with the same code layers
     * and with shared top code cache.
     *
     * @return clone
     * 
     * @since 1.0
     */
    @Override
    public LayeredClassLoader clone() {
        return builder.buildFromCodeLayers();
    }

    @Override
    public void releaseClasses(final ClassReleaser releaser) {
        WeakReference<BytecodeClassLoader> ref;
        do {
            ref = classLoaderQueue.poll();
            if (ref != null) {
                BytecodeClassLoader loader = ref.get();
                if (loader != null) {
                    loader.releaseClasses(releaser);
                }
            }
        } while (ref != null);
    }
    
    /**
     * creates a clone with the same code layers
     * and a separate top code cache
     * (initially with the same cached bytecode).
     *
     * @return clone
     * 
     * @since 1.0
     */
    public LayeredClassLoader cloneWithSeparateTopCodeCache() {
        if (!isWithTopCodeCache) {
            return clone();
        }
        LayeredClassLoader detachedClone = builder.buildFromCodeLayers();
        detachedClone.topCodeCache = topCodeCache.clone();
        detachedClone.builder.setTopCodeCacheAfterCreating(detachedClone.topCodeCache);
        return detachedClone;
    }
    
    /**
     * gets the code layers.
     *
     * @return code layers
     * 
     * @since 1.0
     */
    public List<Code> getCodeLayers() {
        return codeLayers;
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
     * gets the top code cache.
     *
     * @return top code cache
     * 
     * @since 1.0
     */
    public TopCodeCache getTopCodeCache() {
        return topCodeCache;
    }
    
    
    /**
     * Builder for instances of {@link LayeredClassLoader}.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        private boolean isCommitted;
        
        private ClassLoader parent;
        private LoadMode loadMode;
        
        private List<Sources> sourcesLayers;
        private List<Code> codeLayers;
        
        private boolean isWithTopCodeCache;
        private LoadMode topLoadMode;
        private TopCodeCache topCodeCache;
        
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
         * sets the load mode for the (static) code layers, default is "current first".
         *
         * @param loadMode load mode for the (static) code layers
         *
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setLoadMode(final LoadMode loadMode) {
            check();
            this.loadMode = loadMode;
            return this;
        }

        /**
         * sets the sources layers, default is no layers.
         *
         * @param sourcesLayers sources layers
         * 
         * @return this, for chaining calls
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
         * 
         * @since 1.0
         */
        public Builder setSourcesLayers(final Sources... sourcesLayers) {
            return setSourcesLayers(Arrays.asList(sourcesLayers));
        }

        /**
         * sets the code layers, default is no layers.
         *
         * @param codeLayers code layers
         * 
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setCodeLayers(final List<Code> codeLayers) {
            check();
            this.codeLayers = codeLayers;
            return this;
        }
        
        private void setCodeLayersAfterCreating(final List<Code> codeLayers) {
            this.codeLayers = codeLayers;
        }
        
        /**
         * sets the code layers, default is no layers.
         *
         * @param codeLayers code layers
         *
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setCodeLayers(final Code... codeLayers) {
            return setCodeLayers(Arrays.asList(codeLayers));
        }

        /**
         * sets whether to use the a top code cache or not,
         * along with the top code cache (OK to pass null if setting to false),
         * default is not to use a top code cache.
         * <p>
         * Note that the default is the opposite in the {@link LayeredEngine}.
         *
         * @param isWithTopCodeCache whether to use the a top code cache or not
         * @param topCodeCache top code cache (OK to pass null if setting to false)
         *
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setWithTopCodeCache(final boolean isWithTopCodeCache, TopCodeCache topCodeCache) {
            check();
            this.isWithTopCodeCache = isWithTopCodeCache;
            this.topCodeCache = topCodeCache;
            return this;
        }
        
        private void setTopCodeCacheAfterCreating(TopCodeCache topCodeCache) {
            this.topCodeCache = topCodeCache;
        }

        /**
         * sets the load mode for the top code cache, default is "parent first".
         *
         * @param topLoadMode load mode for the top code cache
         * 
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setTopLoadMode(final LoadMode topLoadMode) {
            check();
            this.topLoadMode = topLoadMode;
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
         * gets the load mode for the (static) code layers.
         *
         * @return load mode for the (static) code layers
         * 
         * @since 1.0
         */
        public LoadMode getLoadMode() {
            return loadMode;
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
         * gets the code layers.
         *
         * @return code layers
         * 
         * @since 1.0
         */
        public List<Code> getCodeLayers() {
            return codeLayers;
        }
        
        /**
         * gets whether to use the a top code cache or not.
         *
         * @return whether to use the a top code cache or not
         * 
         * @since 1.0
         */
        public boolean isWithTopCodeCache() {
            return isWithTopCodeCache;
        }
        
        /**
         * gets the top code cache.
         *
         * @return top code cache
         * 
         * @since 1.0
         */
        public TopCodeCache getTopCodeCache() {
            return topCodeCache;
        }
        
        /**
         * gets the top load mode.
         *
         * @return top load mode
         * 
         * @since 1.0
         */
        public LoadMode getTopLoadMode() {
            return topLoadMode;
        }
        
        private Builder commit() {
            if (!isCommitted) {
                if (parent == null) {
                    parent = Thread.currentThread().getContextClassLoader();
                }
                if (loadMode == null) {
                    loadMode = LoadMode.CURRENT_FIRST;
                }
                if (sourcesLayers == null) {
                    sourcesLayers = new LinkedList<>();
                }
                if (codeLayers == null) {
                    codeLayers = new LinkedList<>();
                }
                if (topLoadMode == null) {
                    topLoadMode = LoadMode.PARENT_FIRST;
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds a new instance of {@link LayeredClassLoader}
         * based on already compiled code layers.
         *
         * @return new instance
         * 
         * @since 1.0
         */
        public LayeredClassLoader buildFromCodeLayers() {
            commit();
            return new LayeredClassLoader(this);
       }
        
        /**
         * builds a new instance of {@link LayeredClassLoader}
         * based on sources to compile to code layers.
         *
         * @return new instance
         *
         * @throws CompileException if compiling the sources layers failed
         * 
         * @since 1.0
         */
        public LayeredClassLoader buildFromSourcesLayers() {
            commit();
            return new LayeredClassLoader(this, true);
        }
        
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

}
