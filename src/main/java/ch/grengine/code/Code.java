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

import ch.grengine.source.Source;
import ch.grengine.sources.Sources;

import java.util.Set;


/**
 * Interface for code from compiling an instance of {@link Sources}.
 * <p>
 * Contains bytecode and class names, as well as information
 * related to the originating sources.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface Code {
    
    /**
     * gets the name of the originating {@link Sources} instance,
     * or a similar name that can help a human reader to identify
     * the originating sources.
     *
     * @return sources name
     *
     * @since 1.0
     */
    String getSourcesName();

    /**
     * returns true if the given source is for this code,
     * {@literal i.e.} had been part of the sources compiled to this code.
     *
     * @param source source
     *
     * @return true if given source is for this code
     * @throws IllegalArgumentException if the given source is null
     * 
     * @since 1.0
     */
    boolean isForSource(Source source);
    
    /**
     * gets the main class name of the given source.
     *
     * @param source source
     *
     * @return main class name
     * @throws IllegalArgumentException if the given source is null or not for this code
     * 
     * @since 1.0
     */
    String getMainClassName(Source source);
    
    /**
     * gets all class names of the given source.
     *
     * @param source source
     *
     * @return set of all class names
     * @throws IllegalArgumentException if the given source is null or not for this code
     * 
     * @since 1.0
     */
    Set<String> getClassNames(Source source);

    /**
     * gets the last modified at compile time of the given source.
     *
     * @param source source
     *
     * @return last modified at compile time
     * @throws IllegalArgumentException if the given source is null or is not for this code
     * 
     * @since 1.0
     */
    long getLastModifiedAtCompileTime(Source source);

    /**
     * gets the set of all sources which had been compiled to this code.
     * <p>
     * Note that if code is not in memory, this operation may be slow or
     * expensive in terms of resources.
     *
     * @return set of all sources which had been compiled to this code
     *
     * @since 1.0
     */
    Set<Source> getSourceSet();

    /**
     * gets the bytecode for the given class name.
     *
     * @param className class name
     *
     * @return bytecode or null if not found
     * @throws IllegalArgumentException if the given class name is null
     *
     * @since 1.0
     */
    Bytecode getBytecode(String className);
        
    /**
     * gets the set of all class names for which bytecode is in this code.
     * <p>
     * Note that if code is not in memory, this operation may be slow or
     * expensive in terms of resources.
     *
     * @return set of all class names for which bytecode is in this code
     *
     * @since 1.0
     */
    Set<String> getClassNameSet();
    
    /**
     * returns a string suitable for logging.
     *
     * @return a string suitable for logging
     * 
     * @since 1.0
     */
    String toString();

}
