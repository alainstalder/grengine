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

package ch.grengine.sources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.Source;


/**
 * Static utility methods around {@link Sources}.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class SourcesUtil {
    
    /**
     * converts a source to sources with name set to the source ID
     * and with the default Groovy compiler factory.
     *
     * @param source source
     * 
     * @return sources
     * @throws IllegalArgumentException if the source is null
     * 
     * @since 1.0
     */
    public static Sources sourceToSources(final Source source) {
        return sourceToSources(source, new DefaultGroovyCompilerFactory());
    }

    /**
     * converts a source to sources with name set to the source ID
     * and with the given compiler factory.
     *
     * @param source source
     * @param compilerFactory compiler factory
     *
     * @return sources
     * @throws IllegalArgumentException if the source or the compiler factory is null
     * 
     * @since 1.0
     */
    public static Sources sourceToSources(final Source source, final CompilerFactory compilerFactory) {
        if (source == null) {
            throw new IllegalArgumentException("Source is null.");
        }
        if (compilerFactory == null) {
            throw new IllegalArgumentException("Compiler factory is null.");
        }
        Set<Source> sourceSet = new HashSet<Source>();
        sourceSet.add(source);
        return new FixedSetSources.Builder(sourceSet)
                .setName(source.getId())
                .setCompilerFactory(compilerFactory)
                .build();
    }
    
    /**
     * converts a set of source to sources with the given name
     * and with the default Groovy compiler factory.
     *
     * @param sourceSet set of source
     * @param name sources name
     *
     * @return sources
     * @throws IllegalArgumentException if the source set or the name is null
     * 
     * @since 1.0
     */
    public static Sources sourceSetToSources(final Set<Source> sourceSet, final String name) {
        return sourceSetToSources(sourceSet, name, new DefaultGroovyCompilerFactory());
    }
    
    /**
     * converts a set of source to sources with the given name
     * and with the given compiler factory.
     *
     * @param sourceSet set of source
     * @param name sources name
     * @param compilerFactory compiler factory
     *
     * @return sources
     * @throws IllegalArgumentException if the source set, the name or the compiler factory is null
     * 
     * @since 1.0
     */
    public static Sources sourceSetToSources(final Set<Source> sourceSet, final String name,
            final CompilerFactory compilerFactory) {
        if (sourceSet == null) {
            throw new IllegalArgumentException("Source set is null.");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name is null.");
        }
        if (compilerFactory == null) {
            throw new IllegalArgumentException("Compiler factory is null.");
        }
        return new FixedSetSources.Builder(sourceSet)
                .setName(name)
                .setCompilerFactory(compilerFactory)
                .build();
    }

    /**
     * converts the given sources to a list of sources.
     *
     * @param sources sources
     * 
     * @return list of sources
     * @throws IllegalArgumentException if the sources array is null
     * 
     * @since 1.0
     */
    public static List<Sources> sourcesArrayToList(final Sources... sources) {
        if (sources == null) {
            throw new IllegalArgumentException("Sources array is null.");
        }
        return new LinkedList<Sources>(Arrays.asList(sources));
    }

}
