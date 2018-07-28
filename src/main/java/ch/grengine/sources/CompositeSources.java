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

package ch.grengine.sources;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.Source;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;


/**
 * Sources based on a collection of {@link Sources}.
 * <p>
 * A typical example might be several directories plus maybe
 * a fixed set of sources.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class CompositeSources extends BaseSources {
    
    private final Builder builder;
    
    private final List<Sources> sourcesList;

    /**
     * constructor from builder.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected CompositeSources(final Builder builder) {
        this.builder = builder.commit();
        sourcesList = new ArrayList<>();
        sourcesList.addAll(builder.getSourcesCollection());
        super.init(builder.getName(), builder.getCompilerFactory(), builder.getLatencyMs());
    }
    
    @Override
    protected Set<Source> getSourceSetNew() {
        return sourcesList.stream()
                .map(Sources::getSourceSet)
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(HashSet::new));
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
     * Builder for instances of {@link CompositeSources}.
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
        
        private final Collection<Sources> sourcesCollection;
        private String name;
        private CompilerFactory compilerFactory;
        private long latencyMs = -1;
        
        /**
         * constructor from a collection of sources.
         *
         * @param sourcesCollection collection of sources
         * 
         * @throws NullPointerException if the sources collection is null
         * 
         * @since 1.0
         */
        public Builder(final Collection<Sources> sourcesCollection) {
            requireNonNull(sourcesCollection, "Sources collection is null.");
            this.sourcesCollection = sourcesCollection;
            isCommitted = false;
        }

        /**
         * sets the sources name, default is a generated random ID.
         *
         * @param name sources name
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setName(final String name) {
            check();
            this.name = name;
            return this;
        }

        /**
         * sets the compiler factory for compiling sources, default
         * is a new instance of {@link DefaultGroovyCompilerFactory}.
         *
         * @param compilerFactory compiler factory
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setCompilerFactory(final CompilerFactory compilerFactory) {
            check();
            this.compilerFactory = compilerFactory;
            return this;
        }

        /**
         * sets the latency in milliseconds for checking if script files
         * in the directory have changed, default is {@link #DEFAULT_LATENCY_MS}.
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
         * gets the collection of sources.
         *
         * @return collection of sources
         * 
         * @since 1.0
         */
        public Collection<Sources> getSourcesCollection() {
            return sourcesCollection;
        }
        
        /**
         * gets the sources name.
         *
         * @return sources name
         * 
         * @since 1.0
         */
        public String getName() {
            return name;
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
                if (name == null) {
                    name = UUID.randomUUID().toString();
                }
                if (compilerFactory == null) {
                    compilerFactory = new DefaultGroovyCompilerFactory();
                }
                if (latencyMs < 0) {
                    latencyMs = DEFAULT_LATENCY_MS;
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds a new instance of {@link CompositeSources}.
         *
         * @return new instance
         *
         * @since 1.0
         */
        public CompositeSources build() {
            commit();
            return new CompositeSources(this);
        }
                
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

}
