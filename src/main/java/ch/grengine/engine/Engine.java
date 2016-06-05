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

import ch.grengine.Grengine;
import ch.grengine.code.Code;
import ch.grengine.except.ClassNameConflictException;
import ch.grengine.except.CompileException;
import ch.grengine.except.LoadException;
import ch.grengine.source.Source;
import ch.grengine.sources.Sources;

import java.io.Closeable;
import java.util.List;


/**
 * Engine interface.
 * <p>
 * Provides the base functionality for a {@link Grengine},
 * without all the convenience methods.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public interface Engine extends Closeable {
    
    /**
     * gets the default attached loader.
     *
     * @return default attached loader
     * 
     * @since 1.0
     */
    Loader getLoader();
    
    /**
     * creates and gets a new attached loader, backed by the same bytecode
     * as all other shared loaders created by this engine
     * and automatically updated if code layers are set.
     *
     * @return new attached loader
     *
     * @since 1.0
     */
    Loader newAttachedLoader();
    
    /**
     * creates and gets a new detached loader, backed initially by the same
     * bytecode as all attached loaders created by this engine,
     * but not updated if code layers are set.
     * <p>
     * For example, a web application might create a detached loader for
     * each new HTTP session: A new loader in order to separate static
     * variables of scripts between sessions (security feature); a detached
     * loader in order to keep code layers constant during the lifetime
     * of the session (consistent behavior of Groovy script calls).
     *
     * @return new detached loader
     *
     * @since 1.0
     */
    Loader newDetachedLoader();
    
    /**
     * loads the main class of the given source from the given loader.
     * <p>
     * Note that if a class with the main class name is available for loading,
     * but was not compiled as part of a set of {@link Source} that included
     * the given source, that class will not count for loading.
     *
     * @param loader loader
     * @param source source
     *
     * @return loaded class
     * @throws CompileException if compilation was necessary to load the class and failed
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @since 1.0
     */
    Class<?> loadMainClass(Loader loader, Source source) throws CompileException, LoadException;
    
    /**
     * loads a class with the given name and from the given source from the given loader.
     * <p>
     * Note that if a class with the given class name is available for loading,
     * but was not compiled as part of a set of {@link Source} that included
     * the given source, that class will not count for loading.
     *
     * @param loader loader
     * @param source source
     * @param name class name
     *
     * @return loaded class
     * @throws CompileException if compilation was necessary to load the class and failed
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @since 1.0
     */
    Class<?> loadClass(Loader loader, Source source, String name) throws CompileException, LoadException;
    
    /**
     * loads a class by name from the given loader.
     * <p>
     * Note that a top code cache is not searched in this case,
     * because each set of source has its own top loader.
     *
     * @param loader loader
     * @param name class name
     *
     * @return loaded class
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @since 1.0
     */
    Class<?> loadClass(Loader loader, String name) throws LoadException;
    
    /**
     * sets (replaces) code layers of the engine, based on already compiled code layers.
     * <p>
     * Note that normally it is supported to do this "live", while the engine is used.
     *
     * @param codeLayers code layers
     * 
     * @throws ClassNameConflictException optionally if the same class name occurs in
     *     different code layers or would already be available from a parent class loader
     * @throws IllegalArgumentException if code layers are null
     * 
     * @since 1.0
     */
    void setCodeLayers(List<Code> codeLayers) throws ClassNameConflictException;
    
    /**
     * sets (replaces) code layers of the engine, based on sources to compile to code layers.
     * <p>
     * Note that normally it is supported to do this "live", while the engine is used.
     *
     * @param sourcesLayers sources layers
     *
     * @throws CompileException if compilation failed
     * @throws ClassNameConflictException optionally if the same class name resulted from
     *     compiling different sources layers or would already be available
     *     from a parent class loader
     * @throws IllegalArgumentException if sources layers are null
     * 
     * @since 1.0
     */
    void setCodeLayersBySource(List<Sources> sourcesLayers) throws CompileException, ClassNameConflictException;

    /**
     * release metadata for all classed ever loaded using this engine.
     * <p>
     * Allows to remove metadata associated by Groovy (or Java) with a class,
     * which is often necessary to get on-the-fly garbage collection.
     * <p>
     * Generally call only when really done using this engine and
     * all loaded classes; subsequently trying to use this engine
     * or its classes results generally in undefined behavior.
     *
     * @since 1.1
     */
    @Override
    void close();

}
