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

package ch.grengine.engine;

import ch.grengine.load.ClassCloser;
import ch.grengine.load.SourceClassLoader;


/**
 * Wrapper for a {@link SourceClassLoader} that can only be used
 * by the {@link Engine} that created it.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class Loader {

    private final EngineId engineId;
    private final long number;
    private final boolean isAttached;
    private SourceClassLoader sourceClassLoader;
    
    /**
     * constructor.
     * 
     * @param engineId the engine ID, used to authenticate the caller in some instance methods
     * @param number the loader number
     * @param isAttached whether the loader is attached to the engine or not
     * @param sourceClassLoader the {@link SourceClassLoader} to use for loading classes
     * 
     * @throws IllegalArgumentException if any of the arguments is null
     * 
     * @since 1.0
     */
    public Loader(final EngineId engineId, final long number, final boolean isAttached,
            final SourceClassLoader sourceClassLoader) {
        if (engineId == null) {
            throw new IllegalArgumentException("Engine ID is null.");
        }
        if (sourceClassLoader == null) {
            throw new IllegalArgumentException("Source class loader is null.");
        }
        this.engineId = engineId;
        this.number = number;
        this.isAttached = isAttached;
        setSourceClassLoader(engineId, sourceClassLoader);
    }

    /**
     * gets the source class loader (if the engine ID matches).
     *
     * @param engineId engine ID
     *
     * @return source class loader
     * @throws IllegalArgumentException if the engine ID does not match
     * 
     * @since 1.0
     */
    public SourceClassLoader getSourceClassLoader(final EngineId engineId) {
        if (engineId != this.engineId) {
            throw new IllegalArgumentException("Engine ID does not match (loader created by a different engine).");
        }
        return sourceClassLoader;
    }

    /**
     * sets the source class loader (if the engine ID matches).
     *
     * @param engineId engine ID
     * @param sourceClassLoader source class loader
     *
     * @throws IllegalArgumentException if the engine ID does not match
     * 
     * @since 1.0
     */
    public void setSourceClassLoader(final EngineId engineId, final SourceClassLoader sourceClassLoader) {
        if (engineId != this.engineId) {
            throw new IllegalArgumentException("Engine ID does not match (loader created by a different engine).");
        }
        this.sourceClassLoader = sourceClassLoader;
    }
    
    /**
     * gets the loader number.
     *
     * @return loader number
     * 
     * @since 1.0
     */
    public long getNumber() {
        return number;
    }
    
    /**
     * gets whether the loader is attached to the engine or not.
     *
     * @return whether the loader is attached to the engine or not
     * 
     * @since 1.0
     */
    public boolean isAttached() {
        return isAttached;
    }
   
    /**
     * two loaders are equal if and only if their loader number and their engine ID are equal.
     *
     * @since 1.0
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || !(obj instanceof Loader)) return false;
        Loader loader = (Loader)obj;
        return this.number == loader.number && this.engineId == loader.engineId;
    }
    
    /**
     * implementation based on loader number and engine ID.
     * 
     * @since 1.0
     */
    @Override
    public int hashCode() {
        return (int)number * 17  + engineId.hashCode();
    }

    /**
     * returns a string suitable for logging.
     *
     * @return a string suitable for logging
     * 
     * @since 1.0
     */
    public String toString() {
        return this.getClass().getSimpleName() + "[engineId=" + engineId +
        ", number=" + number + ", isAttached=" + isAttached + "]";
    }

    // TODO
    public void closeClasses(ClassCloser closer) {
        sourceClassLoader.closeClasses(closer);
    }
    
}
