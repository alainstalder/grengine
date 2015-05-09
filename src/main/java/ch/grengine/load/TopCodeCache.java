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

package ch.grengine.load;

import ch.grengine.Grengine;
import ch.grengine.code.SingleSourceCode;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.except.CompileException;
import ch.grengine.source.Source;


/**
 * Code cache used to compile and cache code for single source instances.
 * <p>
 * Used to cache individual code layers on top of the {@link LayeredEngine}.
 * This top code cache is typically what is used if running a script from
 * the {@link Grengine} if the script is not part of static code layers or the
 * top load mode is "current first" and the source in the static code layers
 * has been modified since compilation. Each source gets its own independent
 * bytecode class loader that does not see any code of the other source
 * instances in the top code cache.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface TopCodeCache {
        
    /**
     * gets the code from the cache, recompiling the given source if it had been modified.
     *
     * @param source source
     *
     * @return code
     *
     * @throws CompileException if compilation failed
     * @throws IllegalArgumentException if source is null
     * 
     * @since 1.0
     */
    SingleSourceCode getUpToDateCode(Source source) throws CompileException;
    
    /**
     * sets the parent class loader and clears the cache.
     * 
     * @param parent parent class loader
     *
     * @throws IllegalArgumentException if parent is null
     * 
     * @since 1.0
     */
    void setParent(final ClassLoader parent);
    
    /**
     * gets the parent class loader.
     *
     * @return parent class loader
     * 
     * @since 1.0
     */
    ClassLoader getParent();

    /**
     * clears the cache.
     * 
     * @since 1.0
     */
    void clear();
    
    /**
     * creates a clone with the same cached bytecode.
     *
     * @return clone
     * 
     * @since 1.0
     */
    TopCodeCache clone();

}
