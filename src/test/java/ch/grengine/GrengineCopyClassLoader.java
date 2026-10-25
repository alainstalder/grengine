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

package ch.grengine;

import ch.grengine.code.groovy.DefaultGroovyCompiler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;


/**
 * Child-first classloader that loads Grengine classes separately
 * for testing paths in the code that use reflection if same class
 * but not same classloader, e.g. {@link DefaultGroovyCompiler}.
 */
public class GrengineCopyClassLoader extends ClassLoader {

    GrengineCopyClassLoader(final ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            if (!name.startsWith("ch.grengine.")) {
                return super.loadClass(name, resolve);
            }
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                    if (in == null) {
                        throw new ClassNotFoundException(name);
                    }
                    final byte[] bytes = IOUtils.toByteArray(in);
                    c = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

}
