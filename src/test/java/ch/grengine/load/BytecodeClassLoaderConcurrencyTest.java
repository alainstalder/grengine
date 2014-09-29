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

import static org.junit.Assert.assertFalse;

import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.Code;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;


public class BytecodeClassLoaderConcurrencyTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private class SlowBytecodeClassLoader extends BytecodeClassLoader {
        private final long delayMs;
        
        public SlowBytecodeClassLoader(BytecodeClassLoader loader, long delayMs) {
            super(loader.getParent(), loader.getLoadMode(), loader.getCode());
            this.delayMs = delayMs;
        }
        
        // slow down within the synchronized block where the class is defined
        void definePackage(String name) {
            String threadName = Thread.currentThread().getName();
            System.out.println(threadName + " --- about to sleep " + delayMs + "ms");
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                setFailed(true);
            }
            System.out.println(threadName + " --- done sleeping");
        }
        
    }
    
    private volatile boolean failed;
    public void setFailed(boolean failed) {
        this.failed = failed;
    }
    
    @Test
    public void testConcurrent() throws Exception {
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        
        LoadMode loadMode = LoadMode.CURRENT_FIRST;
        
        DefaultGroovyCompiler c = new DefaultGroovyCompiler();
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("class Class1 {}");
        Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        Sources sources = SourcesUtil.sourceSetToSources(sourceSet, "test");
        Code code = c.compile(sources);

        BytecodeClassLoader loader = new BytecodeClassLoader(parent, loadMode, code);
        final SlowBytecodeClassLoader slowLoader = new SlowBytecodeClassLoader(loader, 100);

        final int nThreads = 5;
        
        Thread[] threads = new Thread[nThreads];
        for (int i=0; i<nThreads; i++) {
            final int x = i;
            threads[i] = new Thread(
                    new Runnable() {
                        public void run() {
                            try {
                                String threadName = "Thread-" + x;
                                Thread.currentThread().setName(threadName);
                                System.out.println(threadName + " about to load...");
                                slowLoader.loadClass("Class1");
                                System.out.println(threadName + " loaded.");
                            } catch (Exception e) {
                                setFailed(true);
                            }
                        }
                    });
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        assertFalse(failed);
    }

}
