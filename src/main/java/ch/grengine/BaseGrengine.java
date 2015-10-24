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

package ch.grengine;

import ch.grengine.engine.Engine;
import ch.grengine.engine.Loader;
import ch.grengine.except.CompileException;
import ch.grengine.except.CreateException;
import ch.grengine.except.LoadException;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.Script;


/**
 * Abstract base {@link Grengine}.
 * <p>
 * Implements most convenience methods for using Grengine.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public abstract class BaseGrengine {
    
    /**
     * the engine that powers this Grengine.
     * 
     * @since 1.0
     */
    protected Engine engine;
    
    /**
     * the source factory used to create instances of {@link Source},
     * often implicitly from script text, file or URL.
     * 
     * @since 1.0
     */
    protected SourceFactory sourceFactory;
    
    /**
     * the default loader instance, which is implicitly used when no specific loader is indicated.
     * 
     * @since 1.0
     */
    protected Loader loader;
    
    /**
     * constructor.
     * 
     * @since 1.0
     */
    protected BaseGrengine() {
    }
        
    // Engine...
    
    /**
     * gets the engine.
     *
     * @return engine
     * 
     * @since 1.0
     */
    public Engine getEngine() {
        return engine;
    }

    /**
     * gets the default loader instance, which is implicitly used when no specific loader is indicated.
     *
     * @return default loader instance
     *
     * @see Engine#getLoader()
     *
     * @since 1.0
     */
    public Loader getLoader() {
        return engine.getLoader();
    }
    
    /**
     * creates and gets a new attached loader, backed by the same bytecode
     * as all other shared loaders created by this engine
     * and automatically updated if code layers are set.
     *
     * @return new attached loader
     * 
     * @since 1.0
     */
    public Loader newAttachedLoader() {
        return engine.newAttachedLoader();
    }
    
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
    public Loader newDetachedLoader() {
        return engine.newDetachedLoader();
    }
    
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
     * @return main class
     * @throws CompileException if compilation was necessary to load the class and failed
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @see Engine#loadMainClass(Loader loader, Source source)
     * 
     * @since 1.0
     */
    public abstract Class<?> loadMainClass(Loader loader, Source source) throws CompileException, LoadException;
    
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
     * @return class
     * @throws CompileException if compilation was necessary to load the class and failed
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @see Engine#loadClass(Loader loader, Source source, String name)
     * 
     * @since 1.0
     */
    public abstract Class<?> loadClass(Loader loader, Source source, String name) throws CompileException, LoadException;
    
    /**
     * loads a class by name from the given loader.
     * <p>
     * Note that a top code cache is not searched in this case,
     * because each set of source has its own top loader.
     *
     * @param loader loader
     * @param name class name
     *
     * @return class
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @see Engine#loadClass(Loader loader, String name)
     * 
     * @since 1.0
     */
    public abstract Class<?> loadClass(Loader loader, String name) throws LoadException;
    
    // SourceFactory...
    
    /**
     * gets the source factory used to create instances of {@link Source},
     * often implicitly from script text or file or URL.
     *
     * @return source factory
     * 
     * @since 1.0
     */
    public SourceFactory getSourceFactory() {
        return sourceFactory;
    }

    /**
     * gets source from script text.
     *
     * @param text script text
     *
     * @return source
     * 
     * @see SourceFactory#fromText(String text)
     */
    public Source source(final String text) {
        return sourceFactory.fromText(text);
    }

    /**
     * gets source from script text and desired class name.
     *
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return source
     *
     * @see SourceFactory#fromText(String text, String desiredClassName)
     * 
     * @since 1.0
     */
    public Source source(final String text, final String desiredClassName) {
        return sourceFactory.fromText(text, desiredClassName);
    }

    /**
     * gets source from script file.
     *
     * @param file script file
     *
     * @return source
     * 
     * @see SourceFactory#fromFile(File file)
     */
    public Source source(final File file) {
        return sourceFactory.fromFile(file);
    }
    
    /**
     * gets source from script URL.
     *
     * @param url script URL
     *
     * @return source
     *
     * @see SourceFactory#fromUrl(URL url)
     * 
     * @since 1.0
     */
    public Source source(final URL url) {
        return sourceFactory.fromUrl(url);
    }
    
    // Grengine...
    
    /**
     * loads a class (default loader, text-based source),
     * compiling first if necessary.
     *
     * @param text script text
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final String text) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromText(text));
    }

    /**
     * loads a class (given loader, text-based source),
     * compiling first if necessary.
     *
     * @param loader loader
     * @param text script text
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final Loader loader, final String text) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromText(text));
    }

    /**
     * loads a class (default loader, text-based source with desired class name),
     * compiling first if necessary.
     *
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final String text, final String desiredClassName) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromText(text, desiredClassName));
    }

    /**
     * loads a class (given loader, text-based source with desired class name),
     * compiling first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final Loader loader, final String text, final String desiredClassName)
            throws CompileException, LoadException {
        return load(loader, sourceFactory.fromText(text, desiredClassName));
    }

    /**
     * loads a class (default loader, file-based source),
     * compiling first if necessary.
     *
     * @param file script file
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final File file) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromFile(file));
    }

    /**
     * loads a class (given loader, file-based source),
     * compiling first if necessary.
     *
     * @param loader loader
     * @param file script file
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final Loader loader, final File file) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromFile(file));
    }
    
    /**
     * loads a class (default loader, URL-based source),
     * compiling first if necessary.
     *
     * @param url script URL
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final URL url) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromUrl(url));
    }

    /**
     * loads a class (given loader, URL-based source),
     * compiling first if necessary.
     *
     * @param loader loader
     * @param url script URL
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final Loader loader, final URL url) throws CompileException, LoadException {
        return load(loader, sourceFactory.fromUrl(url));
    }

    /**
     * loads a class (default loader, given source),
     * compiling first if necessary.
     *
     * @param source source
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final Source source) throws CompileException, LoadException {
        return load(loader, source);
    }

    /**
     * loads a class (given loader, given source),
     * compiling first if necessary.
     *
     * @param loader loader
     * @param source source
     *
     * @return loaded class
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * 
     * @since 1.0
     */
    public Class<?> load(final Loader loader, final Source source) throws CompileException, LoadException {
        return loadMainClass(loader, source);
    }
    
    
    /**
     * creates an instance of {@link Script} from the given class.
     *
     * @param clazz class
     * 
     * @return new instance
     * @throws CreateException if could not create the instance or the given class is not a script
     * 
     * @since 1.0
     */
    public Script create(final Class<?> clazz) throws CreateException {
        try {
            return (Script)clazz.newInstance();
        } catch (Exception e) {
            throw new CreateException("Could not create script for class " + clazz.getCanonicalName() + ".", e);
        }
    }
    
    
    /**
     * creates a script (default loader, text-based source),
     * compiling and loading first if necessary.
     *
     * @param text script text
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final String text)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromText(text));
    }
    
    /**
     * creates a script (given loader, text-based source),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final Loader loader, final String text)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromText(text));
    }
    
    /**
     * creates a script (default loader, text-based source with desired class name),
     * compiling and loading first if necessary.
     *
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final String text, final String desiredClassName)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromText(text, desiredClassName));
    }
    
    /**
     * creates a script (given loader, text-based source with desired class name),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final Loader loader, final String text, final String desiredClassName)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromText(text, desiredClassName));
    }
    
    /**
     * creates a script (default loader, file-based source),
     * compiling and loading first if necessary.
     *
     * @param file script file
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final File file)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromFile(file));
    }
    
    /**
     * creates a script (given loader, file-based source),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param file script file
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final Loader loader, final File file)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromFile(file));
    }
    
    /**
     * creates a script (default loader, URL-based source),
     * compiling and loading first if necessary.
     *
     * @param url script URL
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final URL url)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromUrl(url));
    }
    
    /**
     * creates a script (given loader, URL-based source),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param url script URL
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final Loader loader, final URL url)
            throws CompileException, LoadException, CreateException {
        return create(loader, sourceFactory.fromUrl(url));
    }
    
    /**
     * creates a script (default loader, given source),
     * compiling and loading first if necessary.
     *
     * @param source source
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final Source source)
            throws CompileException, LoadException, CreateException {
        return create(loader, source);
    }
    
    /**
     * creates a script (given loader, given source),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param source source
     *
     * @return new instance
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * 
     * @since 1.0
     */
    public Script create(final Loader loader, final Source source)
            throws CompileException, LoadException, CreateException {
        Class<?> clazz = load(loader, source);
        try {
            return (Script)clazz.newInstance();
        } catch (Throwable t) {
            throw new CreateException("Could not create script for class '" + clazz.getCanonicalName() + 
                    "' from source " + source + ".", t);
        }
    }


    /**
     * creates an empty binding.
     * <p>
     * Same as {@code new Binding()}.
     *
     * @return new binding
     * 
     * @since 1.0
     */
    public Binding binding() {
        return new Binding();
    }

    /**
     * creates a binding using the given key and value.
     *
     * @param key key
     * @param value value
     *
     * @return new binding
     *
     * @since 1.0
     */
    public Binding binding(final String key, final Object value) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(key, value);
        return new Binding(map);
    }

    /**
     * creates a binding using the given keys and values.
     *
     * @param key1 key 1
     * @param value1 value 1
     * @param key2 key 2
     * @param value2 value 2
     *
     * @return new binding
     *
     * @since 1.0
     */
    public Binding binding(final String key1, final Object value1, final String key2, final Object value2) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        return new Binding(map);
    }

    /**
     * creates a binding using the given keys and values.
     *
     * @param key1 key 1
     * @param value1 value 1
     * @param key2 key 2
     * @param value2 value 2
     * @param key3 key 3
     * @param value3 value 3
     *
     * @return new binding
     *
     * @since 1.0
     */
    public Binding binding(final String key1, final Object value1, final String key2, final Object value2,
            final String key3, final Object value3) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return new Binding(map);
    }

    /**
     * creates a binding using the given keys and values.
     *
     * @param key1 key 1
     * @param value1 value 1
     * @param key2 key 2
     * @param value2 value 2
     * @param key3 key 3
     * @param value3 value 3
     * @param key4 key 4
     * @param value4 value 4
     *
     * @return new binding
     *
     * @since 1.0
     */
    public Binding binding(final String key1, final Object value1, final String key2, final Object value2,
            final String key3, final Object value3, final String key4, final Object value4) {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        return new Binding(map);
    }

    /**
     * creates a binding using the given keys and values.
     *
     * @param key1 key 1
     * @param value1 value 1
     * @param key2 key 2
     * @param value2 value 2
     * @param key3 key 3
     * @param value3 value 3
     * @param key4 key 4
     * @param value4 value 4
     * @param key5 key 5
     * @param value5 value 5
     * @param moreKeyValuePairs must be an even number of arguments (key/value) and all keys must be strings
     * 
     * @throws IllegalArgumentException if the number of arguments is odd or if a key is not a string
     *
     * @return new binding
     *
     * @since 1.0
     */
    public Binding binding(final String key1, final Object value1, final String key2, final Object value2,
            final String key3, final Object value3, final String key4, final Object value4,
            final String key5, final Object value5, final Object... moreKeyValuePairs) {
        int nFixed = 10;
        int nMore = moreKeyValuePairs.length;
        int n = nFixed + nMore;
        if (n % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments.");
        }
        Map<String,Object> map = new HashMap<String,Object>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.put(key4, value4);
        map.put(key5, value5);
        for (int i=0; i<nMore; i+=2) {
            Object keyObj = moreKeyValuePairs[i];
            if (!(keyObj instanceof String)) {
                throw new IllegalArgumentException("Argument " + (nFixed + i + 1) + " is not a string.");
            }
            map.put((String)keyObj, moreKeyValuePairs[i+1]);
        }
        return new Binding(map);
    }
        
    
    /**
     * runs the given script (empty binding).
     * <p>
     * Note that the script may throw anything, checked or unchecked.
     *
     * @param script script
     * 
     * @return what the script returned
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Script script) {
        return run(script, new Binding());
    }
    
    /**
     * runs the given script (given binding).
     * <p>
     * Note that the script may throw anything, checked or unchecked.
     *
     * @param script script
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Script script, final Map<String,Object> bindingMap) {
        return run(script, new Binding(bindingMap));
    }
    
    /**
     * runs the given script (given binding).
     * <p>
     * Note that the script may throw anything, checked or unchecked.
     *
     * @param script script
     * @param binding binding
     *
     * @return what the script returned
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Script script, final Binding binding) {
        script.setBinding(binding);
        return script.run();
    }
    
    
    /**
     * creates and runs a script (default loader, text-based source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param text script text
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final String text)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text), new Binding());
    }

    /**
     * creates and runs a script (default loader, text-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param text script text
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final String text, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (default loader, text-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param text script text
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final String text, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text), binding);
    }

    /**
     * creates and runs a script (given loader, text-based source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final String text)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text), new Binding());
    }

    /**
     * creates and runs a script (given loader, text-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final String text, Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (given loader, text-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final String text, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text), binding);
    }


    /**
     * creates and runs a script (default loader, text-based source with desired class name, empty binding),
     * compiling and loading first if necessary.
     *
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final String text, final String desiredClassName)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text, desiredClassName), new Binding());
    }

    /**
     * creates and runs a script (default loader, text-based source with desired class name, given binding),
     * compiling and loading first if necessary.
     *
     * @param text script text
     * @param desiredClassName desired class name
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final String text, final String desiredClassName, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text, desiredClassName), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (default loader, text-based source with desired class name, given binding),
     * compiling and loading first if necessary.
     *
     * @param text script text
     * @param desiredClassName desired class name
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final String text, final String desiredClassName, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text, desiredClassName), binding);
    }

    /**
     * creates and runs a script (given loader, text-based source with desired class name, empty binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param desiredClassName desired class name
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final String text, final String desiredClassName)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text, desiredClassName), new Binding());
    }

    /**
     * creates and runs a script (given loader, text-based source with desired class name, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param desiredClassName desired class name
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - nything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final String text, final String desiredClassName,
            final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text, desiredClassName), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (given loader, text-based source with desired class name, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param text script text
     * @param desiredClassName desired class name
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final String text, final String desiredClassName,
            final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromText(text, desiredClassName), binding);
    }

    
    /**
     * creates and runs a script (default loader, file-based source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param file script file
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final File file)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromFile(file), new Binding());
    }

    /**
     * creates and runs a script (default loader, file-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param file script file
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final File file, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromFile(file), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (default loader, file-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param file script file
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final File file, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromFile(file), binding);
    }

    /**
     * creates and runs a script (given loader, file-based source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param file script file
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final File file)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromFile(file), new Binding());
    }

    /**
     * creates and runs a script (given loader, file-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param file script file
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final File file, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromFile(file), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (given loader, file-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param file script file
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final File file, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromFile(file), binding);
    }

    
    /**
     * creates and runs a script (default loader, URL-based source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param url script URL
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final URL url)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromUrl(url), new Binding());
    }

    /**
     * creates and runs a script (default loader, URL-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param url script URL
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final URL url, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromUrl(url), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (default loader, URL-based source, new binding),
     * compiling and loading first if necessary.
     *
     * @param url script URL
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final URL url, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromUrl(url), binding);
    }

    /**
     * creates and runs a script (given loader, URL-based source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param url script URL
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final URL url)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromUrl(url), new Binding());
    }

    /**
     * creates and runs a script (given loader, URL-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param url script URL
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final URL url, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromUrl(url), new Binding(bindingMap));
    }

    /**
     * creates and runs a script (given loader, URL-based source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param url script URL
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final URL url, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, sourceFactory.fromUrl(url), binding);
    }

    
    /**
     * creates and runs a script (default loader, given source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param source source
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Source source)
            throws CompileException, LoadException, CreateException {
        return run(loader, source, new Binding());
    }

    /**
     * creates and runs a script (default loader, given source, given binding),
     * compiling and loading first if necessary.
     *
     * @param source source
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Source source, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, source, new Binding(bindingMap));
    }

    /**
     * creates and runs a script (default loader, given source, given binding),
     * compiling and loading first if necessary.
     *
     * @param source source
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Source source, final Binding binding)
            throws CompileException, LoadException, CreateException {
        return run(loader, source, binding);
    }

    /**
     * creates and runs a script (given loader, given source, empty binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param source source
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final Source source)
            throws CompileException, LoadException, CreateException {
        return run(loader, source, new Binding());
    }

    /**
     * creates and runs a script (given loader, given source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param source source
     * @param bindingMap binding map
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final Source source, final Map<String,Object> bindingMap)
            throws CompileException, LoadException, CreateException {
        return run(loader, source, new Binding(bindingMap));
    }

    /**
     * creates and runs a script (given loader, given source, given binding),
     * compiling and loading first if necessary.
     *
     * @param loader loader
     * @param source source
     * @param binding binding
     *
     * @return what the script returned
     * @throws CompileException if compiling failed
     * @throws LoadException if loading failed
     * @throws CreateException if could not create the instance or is not a script
     * @grengine.scriptthrows {@link Throwable} - anything (checked or unchecked) that {@link Script#run()} may throw
     * 
     * @since 1.0
     */
    public Object run(final Loader loader, final Source source, final Binding binding)
            throws CompileException, LoadException, CreateException {
        Script script = create(loader, source);
        script.setBinding(binding);
        return script.run();
    }

}
