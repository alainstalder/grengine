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

import ch.grengine.code.Code;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class BytecodeClassLoaderConcurrencyTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private class SlowBytecodeClassLoader extends BytecodeClassLoader {
        private final long delayDefinePackageMs;
        private final long delayDefineClassMs;

        public SlowBytecodeClassLoader(BytecodeClassLoader loader, long delayDefinePackageMs, long delayDefineClassMs) {
            super(loader.getParent(), loader.getLoadMode(), loader.getCode());
            this.delayDefinePackageMs = delayDefinePackageMs;
            this.delayDefineClassMs = delayDefineClassMs;
        }
        
        // slow down within the synchronized block where the class is defined
        void definePackage(String packageName) {
            if (delayDefinePackageMs != 0) {
                String threadName = Thread.currentThread().getName();
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

        Class<?> defineClass(String name, byte[] bytes) {
            if (delayDefineClassMs != 0) {
                String threadName = Thread.currentThread().getName();
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

    @Test
    public void testConcurrentSingleClassNoPackage() throws Exception {

        setFailed(false);

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final SlowBytecodeClassLoader slowLoader = new SlowBytecodeClassLoader(loader, 0, 100);

        final int nThreads = 10;

        Thread[] threads = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final int x = i;
            threads[i] = new Thread() {
                public void run() {
                    try {
                        String threadName = "Thread-" + x;
                        Thread.currentThread().setName(threadName);
                        System.out.println(threadName + " about to load...");
                        slowLoader.loadClass("Class1");
                        System.out.println(threadName + " loaded.");
                    } catch (Throwable t) {
                        setFailed(true);
                    }
                }
            };
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertThat(failed, is(false));
    }

    @Test
    public void testConcurrentSingleClassWithPackage() throws Exception {

        setFailed(false);

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("package a.b.c; class Class1 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final SlowBytecodeClassLoader slowLoader = new SlowBytecodeClassLoader(loader, 100, 0);

        final int nThreads = 10;

        Thread[] threads = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final int x = i;
            threads[i] = new Thread() {
                public void run() {
                    try {
                        String threadName = "Thread-" + x;
                        Thread.currentThread().setName(threadName);
                        System.out.println(threadName + " about to load...");
                        slowLoader.loadClass("a.b.c.Class1");
                        System.out.println(threadName + " loaded.");
                    } catch (Throwable t) {
                        setFailed(true);
                    }
                }
            };
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertThat(failed, is(false));
    }

    @Test
    public void testConcurrentMultiClassNoPackage() throws Exception {

        setFailed(false);

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Source s2 = f.fromText("class Class2 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final SlowBytecodeClassLoader slowLoader = new SlowBytecodeClassLoader(loader, 0, 100);

        final int nThreads = 10;

        Thread[] threads = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final int x = i;
            threads[i] = new Thread() {
                public void run() {
                    try {
                        String threadName = "Thread-" + x;
                        Thread.currentThread().setName(threadName);
                        System.out.println(threadName + " about to load...");
                        slowLoader.loadClass((x % 2 == 0) ? "Class1" : "Class2");
                        System.out.println(threadName + " loaded.");
                    } catch (Throwable t) {
                        setFailed(true);
                    }
                }
            };
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertThat(failed, is(false));
    }

    @Test
    public void testConcurrentMultiClassWithPackage() throws Exception {

        setFailed(false);

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        LoadMode loadMode = LoadMode.CURRENT_FIRST;

        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("package a.b.c; class Class1 {}");
        Source s2 = f.fromText("package a.b.c; class Class2 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1, s2);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final SlowBytecodeClassLoader slowLoader = new SlowBytecodeClassLoader(loader, 0, 100);

        final int nThreads = 10;

        Thread[] threads = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final int x = i;
            threads[i] = new Thread() {
                public void run() {
                    try {
                        String threadName = "Thread-" + x;
                        Thread.currentThread().setName(threadName);
                        System.out.println(threadName + " about to load...");
                        slowLoader.loadClass((x % 2 == 0) ? "a.b.c.Class1" : "a.b.c.Class2");
                        System.out.println(threadName + " loaded.");
                    } catch (Throwable t) {
                        setFailed(true);
                    }
                }
            };
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertThat(failed, is(false));
    }

}
