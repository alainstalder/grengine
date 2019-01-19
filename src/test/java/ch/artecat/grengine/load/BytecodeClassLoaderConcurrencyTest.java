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

package ch.artecat.grengine.load;

import ch.artecat.grengine.code.Code;
import ch.artecat.grengine.code.groovy.DefaultGroovyCompiler;
import ch.artecat.grengine.source.DefaultSourceFactory;
import ch.artecat.grengine.source.Source;
import ch.artecat.grengine.source.SourceFactory;
import ch.artecat.grengine.sources.Sources;
import ch.artecat.grengine.sources.SourcesUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static ch.artecat.grengine.TestUtil.toRuntimeException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class BytecodeClassLoaderConcurrencyTest {
    
    private class SlowBytecodeClassLoader extends BytecodeClassLoader {
        private final long delayDefinePackageMs;
        private final long delayDefineClassMs;

        SlowBytecodeClassLoader(final BytecodeClassLoader loader, final long delayDefinePackageMs,
                                final long delayDefineClassMs) {
            super(loader.getParent(), loader.getLoadMode(), loader.getCode());
            this.delayDefinePackageMs = delayDefinePackageMs;
            this.delayDefineClassMs = delayDefineClassMs;
        }
        
        // slow down within the synchronized block where the class is defined
        void definePackage(final String packageName) {
            if (delayDefinePackageMs != 0) {
                final String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " --- definePackage: about to sleep " + delayDefinePackageMs + "ms");
                try {
                    Thread.sleep(delayDefinePackageMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    setFailed(true);
                }
                System.out.println(threadName + " --- definePackage: done sleeping");
            }
            try {
                super.definePackage(packageName);
            } catch (Throwable t) {
                t.printStackTrace();
                setFailed(true);
            }
        }

        Class<?> defineClass(final String name, final byte[] bytes) {
            if (delayDefineClassMs != 0) {
                final String threadName = Thread.currentThread().getName();
                System.out.println(threadName + " --- defineClass: about to sleep " + delayDefineClassMs + "ms");
                try {
                    Thread.sleep(delayDefineClassMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    setFailed(true);
                    return null;
                }
                System.out.println(threadName + " --- defineClass: done sleeping");
            }
            try {
                return super.defineClass(name, bytes);
            } catch (Throwable t) {
                t.printStackTrace();
                setFailed(true);
                return null;
            }
        }

    }

    private volatile boolean failed;
    private void setFailed(boolean failed) {
        this.failed = failed;
    }

    private void testGeneric(final String packageName, final String... classNames) {

        // given

        setFailed(false);

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        final LoadMode loadMode = LoadMode.CURRENT_FIRST;

        final DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        final SourceFactory f = new DefaultSourceFactory();

        final String packageDeclarePrefix = packageName.isEmpty() ? "" : "package " + packageName + "; ";
        Set<Source> sourceSet = Arrays.stream(classNames)
                .map(className -> packageDeclarePrefix + "class " + className + " {}")
                .map(f::fromText)
                .collect(Collectors.toSet());

        final Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        final Code code = c.compile(sources);

        final BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);

        final SlowBytecodeClassLoader slowLoader;
        if (packageName.isEmpty() || classNames.length > 1) {
            slowLoader = new SlowBytecodeClassLoader(loader, 0, 100);
        } else {
            slowLoader = new SlowBytecodeClassLoader(loader, 100, 0);
        }

        final String packageLoadPrefix = packageName.isEmpty() ? "" : packageName + ".";
        final List<String> classNamesToLoad = Arrays.stream(classNames)
                .map(className -> packageLoadPrefix + className)
                .collect(Collectors.toList());
        final int nClassNamesToLoad = classNamesToLoad.size();

        final int nThreads = 10;
        List<Thread> threads = IntStream.range(0, nThreads)
                .boxed()
                .map(i -> new Thread(() -> {
                    try {
                        final String threadName = "Thread-" + i;
                        Thread.currentThread().setName(threadName);
                        System.out.println(threadName + " about to load...");
                        int index = (i % nClassNamesToLoad);
                        slowLoader.loadClass(classNamesToLoad.get(index));
                        System.out.println(threadName + " loaded.");
                    } catch (Throwable t) {
                        setFailed(true);
                    }
                }))
                .collect(Collectors.toList());

        // when

        threads.forEach(Thread::start);
        threads.forEach(t -> toRuntimeException(t::join));

        // then

        assertThat(failed, is(false));
    }


    @Test
    void testConcurrentSingleClassNoPackage() {
        testGeneric("", "Class1");
    }
    @Test
    void testConcurrentSingleClassWithPackage() {
        testGeneric("a.b.c", "Class1");
    }
    @Test
    void testConcurrentMultiClassNoPackage() {
        testGeneric("", "Class1", "Class2");
    }
    @Test
    void testConcurrentMultiClassWithPackage() {
        testGeneric("a.b.c", "Class1", "Class2");
    }

}
