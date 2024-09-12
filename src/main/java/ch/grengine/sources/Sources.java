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

import ch.grengine.source.Source;
import ch.grengine.code.CompilerFactory;

import java.util.Set;


/**
 * Interface for a set of {@link Source} that can change with time.
 * <p>
 * A typical example is a directory that contains Groovy scripts
 * which can be modified, deleted and created. (This use case
 * is implemented by {@link DirBasedSources}.)
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface Sources {
    
    /**
     * updates the source set, if necessary, and returns it.
     *
     * @return source set
     * 
     * @since 1.0
     */
    Set<Source> getSourceSet();
    
    /**
     * updates the source set, if necessary, and returns the last modified
     * of when last found changed.
     * <p>
     * You should not rely on this number to increase monotonically or even
     * to represent an actual date and time, but instead consider the sources
     * changed each time this method returns a different value.
     *
     * @return last modified
     * 
     * @since 1.0
     */
    long getLastModified();
    
    /**
     * gets the name of the sources.
     *
     * @return name of the sources
     * 
     * @since 1.0
     */
    String getName();

    /**
     * gets the compiler factory that should be used to compile the sources.
     *
     * @return compiler factory
     * 
     * @since 1.0
     */
    CompilerFactory getCompilerFactory();
    
    /**
     * returns a string suitable for logging.
     *
     * @return a string suitable for logging
     *
     * @since 1.0
     */
    String toString();

}
