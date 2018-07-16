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

import ch.grengine.code.Compiler;
import ch.grengine.code.CompilerFactory;

import java.util.Set;

import org.codehaus.groovy.control.CompilerConfiguration;

import static java.util.Objects.requireNonNull;


/**
 * Factory for instances of {@link DefaultGroovyCompiler}
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultGroovyCompilerFactory implements CompilerFactory {
    
    /**
     * default script extensions, namely just the single extension "groovy".
     * 
     * @since 1.0
     */
    public static final Set<String> DEFAULT_SCRIPT_EXTENSIONS =
            CompilerConfiguration.DEFAULT.getScriptExtensions();
    
    private final Builder builder;
    private final CompilerConfiguration compilerConfiguration;
    
    /**
     * constructor from builder.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected DefaultGroovyCompilerFactory(final Builder builder) {
        this.builder = builder.commit();
        compilerConfiguration = builder.getCompilerConfiguration();
    }

    /**
     * constructor from defaults for all settings.
     * 
     * @since 1.0
     */
    public DefaultGroovyCompilerFactory() {
        this(new Builder());
    }

    /**
     * constructor from given compiler configuration and defaults for all other settings.
     *
     * @param compilerConfiguration compiler configuration
     * 
     * @throws NullPointerException if the compiler configuration is null
     * 
     * @since 1.0
     */
    public DefaultGroovyCompilerFactory(final CompilerConfiguration compilerConfiguration) {
        this(new Builder()
                .setCompilerConfiguration(requireNonNull(compilerConfiguration, "Compiler configuration is null."))
        );
    }
    
    @Override
    public Compiler newCompiler(final ClassLoader parent) {
        return new DefaultGroovyCompiler.Builder()
                .setParent(parent)
                .setCompilerConfiguration(compilerConfiguration)
                .build();
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
     * gets the compiler configuration.
     *
     * @return compiler configuration
     * 
     * @since 1.0
     */
    public CompilerConfiguration getCompilerConfiguration() {
        return compilerConfiguration;
    }
    
    
    /**
     * Builder for instances of {@link DefaultGroovyCompilerFactory}.
     * 
     * @since 1.0
     * 
     * @author Alain Stalder
     * @author Made in Switzerland.
     */
    public static class Builder {
        
        private boolean isCommitted;
        
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
                if (compilerConfiguration == null) {
                    compilerConfiguration = new CompilerConfiguration();
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds a new instance of {@link DefaultGroovyCompilerFactory}.
         *
         * @return new instance
         * 
         * @since 1.0
         */
        public DefaultGroovyCompilerFactory build() {
            commit();
            return new DefaultGroovyCompilerFactory(this);
       }
        
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

    
}
