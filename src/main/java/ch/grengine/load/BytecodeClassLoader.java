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

import ch.grengine.code.Bytecode;
import ch.grengine.code.Code;
import ch.grengine.except.CompileException;
import ch.grengine.except.LoadException;
import ch.grengine.source.Source;


/**
 * Bytecode-based class loader that also implements the {@link SourceClassLoader}
 * interface.
 * <p>
 * Loads classes based on given bytecode or from the parent class loader.
 * Depending on the given load mode, the parent class loader is searched
 * first or last.
 * 
 * @since 1.0
 * 
 * @author Alain Stalder
 * @author Made in Switzerland.
 */
public class BytecodeClassLoader extends SourceClassLoader {
    
    private final LoadMode loadMode;
    private final Code code;
    
    /**
     * constructor.
     * 
     * @param parent parent class loader
     * @param loadMode load mode
     * @param code code with bytecode for one or more classes
     * 
     * @throws IllegalArgumentException if any of the arguments is null
     * 
     * @since 1.0
     */
    public BytecodeClassLoader(final ClassLoader parent, final LoadMode loadMode, final Code code) {
        super(parent);
        if (parent == null) {
            throw new IllegalArgumentException("Parent class loader is null.");
        }
        if (loadMode == null) {
            throw new IllegalArgumentException("Load mode is null.");
        }
        if (code == null) {
            throw new IllegalArgumentException("Code is null.");
        }
        this.loadMode = loadMode;
        this.code = code;
    }
    
    /*
     * Default calling sequence of loadClass(..) from Oracle Javadoc:
     * 
     * - Invoke findLoadedClass(String) to check if the class has already been loaded.
     * - Invoke the loadClass method on the parent class loader. If the parent is null
     *   the class loader built-in to the virtual machine is used, instead.
     * - Invoke the findClass(String) method to find the class.
     * 
     * LoadMode.PARENT_FIRST: Follows this convention; findClass(name) tries to load
     *   the class from bytecode and, if not present, throws a ClassNotFoundException.
     * 
     * LoadMode.CURRENT_FIRST: Instead in loadClass(..) tries first to load the class
     *   from bytecode, then tries the other two options.
     */
    
    private Class<?> loadClassFromBytecode(final String name) {
        Bytecode bc = code.getBytecode(name);
        if (bc == null) {
            return null;
        }
        
        byte[] bytes = bc.getBytes();
        // define class if not already defined by another thread
        Class<?> clazz;
        synchronized(this) {
            if ((clazz = findLoadedClass(name)) == null) {
                definePackage(name);
                clazz = defineClass(name, bytes, 0, bytes.length);
            }
        }
        return clazz;        
    }
    
    // package scope for unit tests
    void definePackage(String className) {
        int i = className.lastIndexOf('.');
        if (i >= 0) {
            String packageName = className.substring(0, i);
            // define package if not already defined
            if (getPackage(packageName) == null) {
                definePackage(packageName, null, null, null, null, null, null, null);
            }
        }
    }
        
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        
        Class<?> clazz;
        if ((clazz = loadClassFromBytecode(name)) != null) {
            return clazz;
        }
        
        // not found
        throw new ClassNotFoundException(name);
    }

    
    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        
        if (loadMode != LoadMode.CURRENT_FIRST) {
            return super.loadClass(name, resolve);
        }
        
        Class<?> clazz;
        if ((clazz = loadClassFromBytecode(name)) == null) {
            if ((clazz = findLoadedClass(name)) == null) {
                clazz = getParent().loadClass(name);
            }
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }
    
    @Override
    public Class<?> loadMainClass(final Source source) throws CompileException, LoadException {
        return BytecodeClassLoader.loadMainClassBySource(this, source);
    }
    
    @Override
    public Class<?> loadClass(final Source source, final String name) throws CompileException, LoadException {
        return BytecodeClassLoader.loadClassBySourceAndName(this, source, name);
    }

    @Override
    public BytecodeClassLoader findBytecodeClassLoaderBySource(final Source source) {
        BytecodeClassLoader loader = null;
        if (getLoadMode() == LoadMode.PARENT_FIRST) {
            if (getParent() instanceof SourceClassLoader) {
                loader = ((SourceClassLoader)getParent()).findBytecodeClassLoaderBySource(source);
            }
            if (loader == null && code.isForSource(source)) {
                loader = this;
            }
        } else {
            if (code.isForSource(source)) {
                loader = this;
            }
            if (loader == null && (getParent() instanceof SourceClassLoader)) {
                loader = ((SourceClassLoader)getParent()).findBytecodeClassLoaderBySource(source);
            }
        }
        return loader;
    }
    
    @Override
    public LoadMode getLoadMode() {
        return loadMode;
    }
    
    /**
     * Helper method for loading the main class of the given source from the given class loader.
     * 
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @since 1.0
     */
    public static Class<?> loadMainClassBySource(final ClassLoader classLoader, final Source source)
            throws LoadException {
        BytecodeClassLoader loader = null;
        if (classLoader instanceof SourceClassLoader) {
            loader = ((SourceClassLoader)classLoader).findBytecodeClassLoaderBySource(source);
        }
        if (loader == null) {
            throw new LoadException("Source not found: " + source);
        }
        String name = loader.code.getMainClassName(source);
        Class<?> clazz = loader.loadClassFromBytecode(name);
        if (clazz == null) {
            throw new LoadException("Inconsistent code: " + loader.code +
                    ". Main class '" + name + "' not found for source. Source: " + source);
        }
        return clazz;
    }
    
    /**
     * Helper method for loading a class with the given name and from the given source
     * from the given class loader.
     * 
     * @throws LoadException if loading failed, including if the class was not found
     * 
     * @since 1.0
     */
    public static Class<?> loadClassBySourceAndName(ClassLoader classLoader, final Source source, final String name)
            throws LoadException {
        BytecodeClassLoader loader = null;
        if (classLoader instanceof SourceClassLoader) {
            loader = ((SourceClassLoader)classLoader).findBytecodeClassLoaderBySource(source);
        }
        if (loader == null) {
            throw new LoadException("Source not found: " + source);
        }
        if (!loader.code.getClassNames(source).contains(name)) {
            throw new LoadException("Class '" + name + "' not found for source. Source: " + source);
        }
        Class<?> clazz = loader.loadClassFromBytecode(name);
        if (clazz == null) {
            throw new LoadException("Inconsistent code: " + loader.code +
                    ". Class '" + name + "' not found for source. Source: " + source);
        }
        return clazz;
    }
    
    @Override
    public BytecodeClassLoader clone() {
        return new BytecodeClassLoader(getParent(), loadMode, code);
    }
    
    /**
     * gets the code.
     * 
     * @since 1.0
     */
    public Code getCode() {
        return code;
    }

}
