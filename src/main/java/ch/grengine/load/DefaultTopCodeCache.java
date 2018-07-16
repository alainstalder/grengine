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

import ch.grengine.code.Compiler;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.SingleSourceCode;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.sources.SourcesUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;


/**
 * Default implementation of the {@link SourceFactory} interface.
 * <p>
 * No automatic eviction of cached code.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultTopCodeCache implements TopCodeCache {
    
    private final Builder builder;
    private volatile State state;
    private final CompilerFactory compilerFactory;
        
    /**
     * constructor from builder.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected DefaultTopCodeCache(Builder builder) {
        this.builder = builder.commit();
        state = new State(builder.getParent());
        compilerFactory = builder.getCompilerFactory();
    }
    
    @Override
    public SingleSourceCode getUpToDateCode(final Source source) {

        State stateNow = state;
        
        SingleSourceCode code = stateNow.cache.get(source);
        if (code != null && code.getLastModifiedAtCompileTime() == source.getLastModified()) {
            return code;
        }
        
        synchronized(this) {
            // prevent multiple compilations
            code = stateNow.cache.get(source);
            if (code != null && code.getLastModifiedAtCompileTime() == source.getLastModified()) {
                return code;
            }
            Compiler compiler = compilerFactory.newCompiler(stateNow.parent);
            code = (SingleSourceCode)compiler.compile(SourcesUtil.sourceToSources(source, compilerFactory));
            stateNow.cache.put(source, code);
            return code;
        }
    }
    
    @Override
    public void setParent(final ClassLoader parent) {
        requireNonNull(parent, "Parent class loader is null.");
        state = new State(parent);
    }
    
    @Override
    public ClassLoader getParent() {
        return state.parent;
    }

    @Override
    public void clear() {
        state.cache.clear();
    }

    @Override
    public DefaultTopCodeCache clone() {
        State stateNow = state;
        DefaultTopCodeCache topCodeCache =
                new DefaultTopCodeCache.Builder(stateNow.parent).setCompilerFactory(compilerFactory).build();
        topCodeCache.state.cache.putAll(stateNow.cache);
        return topCodeCache;
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
     * gets the compiler factory.
     *
     * @return compiler factory
     * 
     * @since 1.0
     */
    public CompilerFactory getCompilerFactory() {
        return compilerFactory;
    }
    
    
    private static class State {
        private final ClassLoader parent;
        private final Map<Source,SingleSourceCode> cache;
        private State(ClassLoader parent) {
            this.parent = parent;
            cache = new ConcurrentHashMap<>();
        }
    }
    
    
    /**
     * Builder for instances of {@link DefaultTopCodeCache}.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        private boolean isCommitted;
        
        private final ClassLoader parent;
        private CompilerFactory compilerFactory;
        
        /**
         * constructor from parent class loader.
         *
         * @param parent parent class loader
         * 
         * @since 1.0
         */
        public Builder(ClassLoader parent) {
            this.parent = parent;
            isCommitted = false;
        }

        /**
         * sets the compiler factory,
         * default is a new instance {@link DefaultGroovyCompilerFactory} with default settings
         *
         * @param compilerFactory compiler factory
         *
         * @return this, for chaining calls
         * 
         * @since 1.0
         */
        public Builder setCompilerFactory(final CompilerFactory compilerFactory) {
            check();
            this.compilerFactory = compilerFactory;
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
         * gets the compiler factory.
         *
         * @return compiler factory
         * 
         * @since 1.0
         */
        public CompilerFactory getCompilerFactory() {
            return compilerFactory;
        }
        
        private Builder commit() {
            if (!isCommitted) {
                if (compilerFactory == null) {
                    compilerFactory = new DefaultGroovyCompilerFactory();
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
        public DefaultTopCodeCache build() {
            commit();
            return new DefaultTopCodeCache(this);
       }
        
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

}
