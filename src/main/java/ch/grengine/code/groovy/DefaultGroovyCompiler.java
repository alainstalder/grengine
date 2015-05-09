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

package ch.grengine.code.groovy;

import groovy.lang.GroovyClassLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.tools.GroovyClass;

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
        groovyClassLoader = new GroovyClassLoader(parent, config);
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
     * @throws IllegalArgumentException if the parent class loader is null
     * 
     * @since 1.0
     */
    public DefaultGroovyCompiler(final ClassLoader parent) {
        this(new Builder().setParent(parent));
        if (parent == null) {
            throw new IllegalArgumentException("Parent class loader is null.");
        }
    }
    
    /**
     * constructor from given parent class loader and compiler configuration.
     *
     * @param parent parent class loader
     * @param config compiler configuration
     *
     * @throws IllegalArgumentException if the parent class loader or the compiler configuration is null
     * 
     * @since 1.0
     */
    public DefaultGroovyCompiler(final ClassLoader parent, final CompilerConfiguration config) {
        this(new Builder().setParent(parent).setCompilerConfiguration(config));
        if (parent == null) {
            throw new IllegalArgumentException("Parent class loader is null.");
        }
        if (config == null) {
            throw new IllegalArgumentException("Compiler configuration is null.");
        }
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
     * @throws IllegalArgumentException if sources are null
     * 
     * @since 1.0
     */
    @Override
    public Code compile(final Sources sources) throws CompileException {
        if (sources == null) {
            throw new IllegalArgumentException("Sources are null.");
        }
        try {
            CompilationUnit cu = new CompilationUnit(config, null, groovyClassLoader);
            Map<Source,SourceUnit> sourceUnitMap = new HashMap<Source,SourceUnit>();
            Set<Source> sourceSet = sources.getSourceSet();
            for (Source source : sourceSet) {
                SourceUnit su = addToCompilationUnit(cu, source, sources);
                //System.out.println("SU Name: " + su.getName());
                sourceUnitMap.put(source, su);
            }

            int phase = (config.getTargetDirectory() == null) ? Phases.CLASS_GENERATION : Phases.OUTPUT;
            cu.compile(phase);

            Map<Source,CompiledSourceInfo> compiledSourceInfos = new HashMap<Source,CompiledSourceInfo>();
            for (Entry<Source, SourceUnit> entry : sourceUnitMap.entrySet()) {
                Source source = entry.getKey();
                SourceUnit su = entry.getValue();
                Set<String> classNames = new HashSet<String>();
                List<ClassNode> nodes = su.getAST().getClasses();
                for (ClassNode node : nodes) {
                    classNames.add(node.getName());
                }
                CompiledSourceInfo compiledSourceInfo = new CompiledSourceInfo(source,
                        su.getAST().getMainClassName(), classNames, source.getLastModified());
                //System.out.println("SU MainClassName: " + su.getAST().getMainClassName());
                compiledSourceInfos.put(source, compiledSourceInfo);
            }

            @SuppressWarnings("unchecked")
            List<GroovyClass> groovyClasses = cu.getClasses();
            Map<String, Bytecode> bytecodes = new HashMap<String,Bytecode>();
            for (GroovyClass groovyClass : groovyClasses) {
                String name = groovyClass.getName();
                bytecodes.put(name, new Bytecode(name, groovyClass.getBytes()));
            }

            Code code;
            if (sourceSet.size() == 1) {
                code = new DefaultSingleSourceCode(sources.getName(), compiledSourceInfos, bytecodes);
            } else {
                code = new DefaultCode(sources.getName(), compiledSourceInfos, bytecodes);
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
    protected SourceUnit addToCompilationUnit(final CompilationUnit cu, final Source source, final Sources sources)
            throws CompileException {
        if (source instanceof TextSource) {
            TextSource textSource = (TextSource)source;
            return cu.addSource(textSource.getId(), textSource.getText());
        } else if (source instanceof FileSource) {
            FileSource fileSource = (FileSource)source;
            return cu.addSource(fileSource.getFile());
        } else if (source instanceof UrlSource) {
            UrlSource urlSource = (UrlSource)source;
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

}
