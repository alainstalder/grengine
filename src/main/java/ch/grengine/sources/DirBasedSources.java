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
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * Sources based on a directory with script files.
 * <p>
 * Script file extensions and whether to also scan subdirectories
 * is configurable.
 * <p>
 * The implementation is basic, but may still be good enough for many
 * use cases in practice: Invisible files, as well as invisible or
 * unlistable directories, are ignored; no attempts are made to detect
 * symlinks. Note e.g. that detecting symlinks for sure is not possible
 * before Java 7, unless the native system is queried in some way.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DirBasedSources extends BaseSources {
        
    private final Builder builder;

    private final File dir;
    private final DirMode dirMode;
    private final Set<String> scriptExtensions;
    private final SourceFactory sourceFactory;

    /**
     * constructor from builder.
     *
     * @param builder builder
     * 
     * @since 1.0
     */
    protected DirBasedSources(final Builder builder) {
        this.builder = builder.commit();
        
        dir = builder.getDir();
        dirMode = builder.getDirMode();
        scriptExtensions = builder.getScriptExtensions();
        sourceFactory = builder.getSourceFactory();
        
        super.init(builder.getName(), builder.getCompilerFactory(), builder.getLatencyMs());
    }

    /**
     * gets the updated source set.
     *
     * @return updated source set
     *
     * @since 1.0
     */
    @Override
    protected Set<Source> getSourceSetNew() {
        final Set<Source> sourceSet = new HashSet<>();
        fromDirectoryAddRecursively(sourceSet, dir, true);
        return sourceSet;
    }

    /**
     * gets the script file directory.
     *
     * @return script file directory
     * 
     * @since 1.0
     */
    public File getDir() {
        return dir;
    }
    
    /**
     * gets the dir mode.
     *
     * @return dir mode
     * 
     * @since 1.0
     */
    public DirMode getDirMode() {
        return dirMode;
    }
    
    /**
     * gets the set of script extensions.
     *
     * @return set of script extensions
     * 
     * @since 1.0
     */
    public Set<String> getScriptExtensions() {
        return scriptExtensions;
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

    
    private void fromDirectoryAddRecursively(final Set<Source> sources, final File file, final boolean firstDir) {
        if (!firstDir && file.isHidden()) {
            return;
        }
        if (file.isDirectory()) {
            if (firstDir || dirMode==DirMode.WITH_SUBDIRS_RECURSIVE) {
                final File[] listedFiles = file.listFiles();
                if (listedFiles != null) {
                    for (File listedFile : listedFiles) {
                        fromDirectoryAddRecursively(sources, listedFile, false);
                    }
                }
            }
        } else if (file.isFile()) {
            final String name = file.getName();
            final int i = name.lastIndexOf('.');
            if (i>=0) {
                String ext = name.substring(i+1);
                if (scriptExtensions.contains(ext)) {
                    sources.add(sourceFactory.fromFile(file));
                }
            }
        }
    }
    
    
    /**
     * Builder for instances of {@link DirBasedSources}.
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
        
        private final File dir;
        private DirMode dirMode;
        private Set<String> scriptExtensions;
        private String name;
        private CompilerFactory compilerFactory;
        private SourceFactory sourceFactory;
        private long latencyMs = -1;
        
        /**
         * constructor from script file directory.
         * <p>
         * The given file is immediately converted to the canonical file,
         * with fallback to the absolute file.
         *
         * @param dir script file directory
         *
         * @throws NullPointerException if the directory is null
         * 
         * @since 1.0
         */
        public Builder(final File dir) {
            requireNonNull(dir, "Dir is null.");
            this.dir = SourceUtil.toCanonicalOrAbsoluteFile(dir);
            isCommitted = false;
        }
        
        /**
         * sets the dir mode, default is not to scan subdirectories.
         *
         * @param dirMode dir mode
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setDirMode(final DirMode dirMode) {
            check();
            this.dirMode = dirMode;
            return this;
        }

        /**
         * sets the script extensions, default is only "groovy".
         *
         * @param scriptExtensions script extensions
         * 
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setScriptExtensions(final Set<String> scriptExtensions) {
            check();
            this.scriptExtensions = scriptExtensions;
            return this;
        }

        /**
         * sets the script extensions, default is only "groovy".
         *
         * @param scriptExtensions script extensions
         *
         * @return this, for chaining calls
         * @throws IllegalStateException if the builder had already been used to build an instance
         * 
         * @since 1.0
         */
        public Builder setScriptExtensions(final String... scriptExtensions) {
            Set<String> set = new HashSet<>();
            Collections.addAll(set, scriptExtensions);
            return setScriptExtensions(set);
        }

        /**
         * sets the sources name, default is the canonical file path,
         * with fallback to the absolute file path.
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
        public Builder setCompilerFactory(CompilerFactory compilerFactory) {
            check();
            this.compilerFactory = compilerFactory;
            return this;
        }

        /**
         * sets the source factory for creating sources from files, default
         * is a new instance of {@link DefaultSourceFactory}.
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
         * gets the script file directory.
         *
         * @return script file directory
         * 
         * @since 1.0
         */
        public File getDir() {
            return dir;
        }
        
        /**
         * gets the dir mode.
         *
         * @return dir mode
         * 
         * @since 1.0
         */
        public DirMode getDirMode() {
            return dirMode;
        }

        /**
         * gets the script extensions.
         *
         * @return script extensions
         * 
         * @since 1.0
         */
        public Set<String> getScriptExtensions() {
            return scriptExtensions;
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
         * gets the source factory.
         *
         * @return source factory
         * 
         * @since 1.0
         */
        public SourceFactory getSourceFactory() {
            return sourceFactory;
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
                if (dirMode == null) {
                    dirMode = DirMode.NO_SUBDIRS;
                }
                if (scriptExtensions == null) {
                    scriptExtensions = DefaultGroovyCompilerFactory.DEFAULT_SCRIPT_EXTENSIONS;
                }
                if (name == null) {
                    name = SourceUtil.toCanonicalOrAbsoluteFile(dir).getPath();
                }
                if (compilerFactory == null) {
                    compilerFactory = new DefaultGroovyCompilerFactory();
                }
                if (sourceFactory == null) {
                    sourceFactory = new DefaultSourceFactory();
                }
                if (latencyMs < 0) {
                    latencyMs = DEFAULT_LATENCY_MS;
                }
                isCommitted = true;
            }
            return this;
        }
        
        /**
         * builds a new instance of {@link DirBasedSources}.
         *
         * @return new instance
         *
         * @since 1.0
         */
        public DirBasedSources build() {
            commit();
            return new DirBasedSources(this);
        }
                
        private void check() {
            if (isCommitted) {
                throw new IllegalStateException("Builder already used.");
            }
        }

    }

}
