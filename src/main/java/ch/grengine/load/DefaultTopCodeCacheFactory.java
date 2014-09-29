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

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;


/**
 * Factory for instances of {@link DefaultTopCodeCache}
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultTopCodeCacheFactory implements TopCodeCacheFactory {
    
    private final Builder builder;
    private final CompilerFactory compilerFactory;
    
    /**
     * constructor from builder.
     * 
     * @since 1.0
     */
    protected DefaultTopCodeCacheFactory(final Builder builder) {
        this.builder = builder.commit();
        compilerFactory = builder.getCompilerFactory();
    }
    
    /**
     * constructor from defaults for all settings.
     * 
     * @since 1.0
     */
    public DefaultTopCodeCacheFactory() {
        this(new Builder());
    }
    
    /**
     * constructor from given compiler factory and defaults for all other settings.
     * 
     * @throws IllegalArgumentException if the compiler factory is null
     * 
     * @since 1.0
     */
    public DefaultTopCodeCacheFactory(CompilerFactory compilerFactory) {
        this(new Builder().setCompilerFactory(compilerFactory));
        if (compilerFactory == null) {
            throw new IllegalArgumentException("Compiler factory is null.");
        }
    }
    
    @Override
    public TopCodeCache newTopCodeCache(final ClassLoader parent) {
        return new DefaultTopCodeCache.Builder(parent)
                .setCompilerFactory(compilerFactory)
                .build();
    }

    /**
     * gets the builder.
     * 
     * @since 1.0
     */
    public Builder getBuilder() {
        return builder;
    }

    /**
     * gets the compiler factory.
     * 
     * @since 1.0
     */
    public CompilerFactory getCompilerFactory() {
        return compilerFactory;
    }
    
    
    /**
     * Builder for instances of {@link DefaultTopCodeCacheFactory}.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        private boolean isCommitted;
        
        private CompilerFactory compilerFactory;
        
        /**
         * constructor.
         * 
         * @since 1.0
         */
        public Builder() {
            isCommitted = false;
        }
        
        /**
         * sets the compiler factory,
         * default is a new instance of {@link DefaultGroovyCompilerFactory} with default settings.
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
         * gets the compiler factory.
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
         * builds a new instance of {@link DefaultTopCodeCacheFactory}.
         * 
         * @since 1.0
         */
        public DefaultTopCodeCacheFactory build() {
            commit();
            return new DefaultTopCodeCacheFactory(this);
       }
        
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

    
}
