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

import java.util.Set;

import ch.grengine.source.Source;


/**
 * Interface for code created by compiling a single {@link Source} script.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface SingleSourceCode extends Code {
    
    /**
     * gets the main class name for the single source.
     * 
     * @since 1.0
     */
    String getMainClassName();

    /**
     * gets all class names for the single source.
     * 
     * @since 1.0
     */
    Set<String> getClassNames();

    /**
     * gets the last modified at compile time for the single source.
     * 
     * @since 1.0
     */
    long getLastModifiedAtCompileTime();
    
    /**
     * gets the source.
     * 
     * @since 1.0
     */
    Source getSource();

}
