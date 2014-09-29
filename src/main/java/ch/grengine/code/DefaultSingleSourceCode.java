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

package ch.grengine.code;

import java.util.Map;
import java.util.Set;

import ch.grengine.source.Source;
import ch.grengine.sources.Sources;


/**
 * Default implementation of {@link SingleSourceCode} with all bytecode in memory.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultSingleSourceCode implements SingleSourceCode {
    
    private final String sourcesName;
    private final Set<Source> sourceSet;
    private final Source source;
    private final String mainClassName;
    private final Set<String> classNames;
    private final long lastModifiedAtCompileTime;
    private final Set<String> classNameSet;
    private final Map<String,Bytecode> bytecodes;
    
    /**
     * constructor (typically called by the compiler).
     * 
     * @param sourcesName the name of the originating {@link Sources} instance
     * @param compiledSourceInfos the map of originating {@link Source} to {@link CompiledSourceInfo}
     * @param bytecodes the map of class name to {@link Bytecode}
     * 
     * @throws IllegalArgumentException if any argument is null or not for a single source
     * 
     * @since 1.0
     */
    public DefaultSingleSourceCode(final String sourcesName, final Map<Source,CompiledSourceInfo> compiledSourceInfos,
            final Map<String,Bytecode> bytecodes) {
        if (sourcesName == null) {
            throw new IllegalArgumentException("Sources name is null.");
        }
        if (compiledSourceInfos == null) {
            throw new IllegalArgumentException("Compiled source infos is null.");
        }
        if (bytecodes == null) {
            throw new IllegalArgumentException("Bytecodes is null.");
        }
        if (compiledSourceInfos.size() != 1) {
            throw new IllegalArgumentException("Not a single source.");
        }

        this.sourcesName = sourcesName;
        sourceSet = compiledSourceInfos.keySet();
        source = sourceSet.iterator().next();
        CompiledSourceInfo info = compiledSourceInfos.get(source);
        mainClassName = info.getMainClassName();
        classNames =  info.getClassNames();
        lastModifiedAtCompileTime = info.getLastModifiedAtCompileTime();
        this.bytecodes = bytecodes;
        classNameSet = bytecodes.keySet();
    }

    @Override
    public String getSourcesName() {
        return sourcesName;
    }

    @Override
    public boolean isForSource(final Source source) {
        return this.source.equals(source);
    }

    @Override
    public String getMainClassName(final Source source) {
        if (!isForSource(source)) {
            throw new IllegalArgumentException("Source is not for this code. Source: " + source);
        }
        return mainClassName;
    }

    @Override
    public String getMainClassName() {
        return mainClassName;
    }

    @Override
    public Set<String> getClassNames(final Source source) {
        if (!isForSource(source)) {
            throw new IllegalArgumentException("Source is not for this code. Source: " + source);
        }
        return classNames;
    }

    @Override
    public Set<String> getClassNames() {
        return classNames;
    }

    @Override
    public long getLastModifiedAtCompileTime(final Source source) {
        if (!isForSource(source)) {
            throw new IllegalArgumentException("Source is not for this code. Source: " + source);
        }
        return lastModifiedAtCompileTime;
    }

    @Override
    public long getLastModifiedAtCompileTime() {
        return lastModifiedAtCompileTime;
    }

    @Override
    public Set<Source> getSourceSet() {
        return sourceSet;
    }

    @Override
    public Source getSource() {
        return source;
    }

    @Override
    public Bytecode getBytecode(final String className) {
        return bytecodes.get(className);
    }

    @Override
    public Set<String> getClassNameSet() {
        return classNameSet;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[sourcesName='" + getSourcesName() + 
                "', mainClassName=" + mainClassName + ", classes:" + getClassNameSet() + "]";
    }

}
