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

import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;
import ch.grengine.code.CompilerFactory;

import java.util.Set;

import static java.util.Objects.requireNonNull;


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
     * @throws NullPointerException if the source is null
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
     * @throws NullPointerException if the source or the compiler factory is null
     * 
     * @since 1.0
     */
    public static Sources sourceToSources(final Source source, final CompilerFactory compilerFactory) {
        requireNonNull(source, "Source is null.");
        return sourceSetToSources(SourceUtil.sourceToSourceSet(source), source.getId(), compilerFactory);
    }
    
    /**
     * converts a set of source to sources with the given name
     * and with the default Groovy compiler factory.
     *
     * @param sourceSet set of source
     * @param name sources name
     *
     * @return sources
     * @throws NullPointerException if the source set or the name is null
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
     * @throws NullPointerException if the source set, the name or the compiler factory is null
     * 
     * @since 1.0
     */
    public static Sources sourceSetToSources(final Set<Source> sourceSet, final String name,
            final CompilerFactory compilerFactory) {
        requireNonNull(sourceSet, "Source set is null.");
        requireNonNull(name, "Name is null.");
        requireNonNull(compilerFactory, "Compiler factory is null.");
        return new FixedSetSources.Builder(sourceSet)
                .setName(name)
                .setCompilerFactory(compilerFactory)
                .build();
    }

}
