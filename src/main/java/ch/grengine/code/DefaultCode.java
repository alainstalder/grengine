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

package ch.grengine.code;

import ch.grengine.source.Source;
import ch.grengine.sources.Sources;

import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;


/**
 * Default implementation of {@link Code} with all bytecode in memory.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class DefaultCode implements Code {
    
    private final String sourcesName;
    private final Map<Source,CompiledSourceInfo> compiledSourceInfoMap;
    private final Map<String,Bytecode> bytecodeMap;
    
    /**
     * constructor (typically called by the compiler).
     * 
     * @param sourcesName the name of the originating {@link Sources} instance
     * @param compiledSourceInfoMap the map of originating {@link Source} to {@link CompiledSourceInfo}
     * @param bytecodeMap the map of class name to {@link Bytecode}
     * 
     * @throws NullPointerException if any argument is null
     * 
     * @since 1.0
     */
    public DefaultCode(final String sourcesName, final Map<Source,CompiledSourceInfo> compiledSourceInfoMap,
            final Map<String,Bytecode> bytecodeMap) {
        requireNonNull(sourcesName, "Sources name is null.");
        requireNonNull(compiledSourceInfoMap, "Compiled source info map is null.");
        requireNonNull(bytecodeMap, "Bytecode map is null.");
        this.sourcesName = sourcesName;
        this.compiledSourceInfoMap = compiledSourceInfoMap;
        this.bytecodeMap = bytecodeMap;
    }
    
    @Override
    public String getSourcesName() {
        return sourcesName;
    }
    
    @Override
    public boolean isForSource(final Source source) {
        return compiledSourceInfoMap.containsKey(source);
    }
    
    @Override
    public String getMainClassName(final Source source) {
        final CompiledSourceInfo info = compiledSourceInfoMap.get(source);
        if (info == null) {
            throw new IllegalArgumentException("Source is not for this code. Source: " + source);
        }
        return info.getMainClassName();
    }
    
    @Override
    public Set<String> getClassNames(final Source source) {
        final CompiledSourceInfo info = compiledSourceInfoMap.get(source);
        if (info == null) {
            throw new IllegalArgumentException("Source is not for this code. Source: " + source);
        }
        return info.getClassNames();
    }

    @Override
    public long getLastModifiedAtCompileTime(final Source source) {
        final CompiledSourceInfo info = compiledSourceInfoMap.get(source);
        if (info == null) {
            throw new IllegalArgumentException("Source is not for this code. Source: " + source);
        }
       return info.getLastModifiedAtCompileTime();
    }

    @Override
    public Set<Source> getSourceSet() {
        return compiledSourceInfoMap.keySet();
    }

    @Override
    public Bytecode getBytecode(final String className) {
        return bytecodeMap.get(className);
    }
    
    @Override
    public Set<String> getClassNameSet() {
        return bytecodeMap.keySet();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "[sourcesName='" + sourcesName + "', sources:" + compiledSourceInfoMap.size() +
                ", classes:" + bytecodeMap.size() + "]";
    }

}
