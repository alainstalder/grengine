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

package ch.grengine.engine;

import ch.grengine.TestUtil;
import ch.grengine.code.Code;
import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.load.LoadMode;
import ch.grengine.source.MockTextSource;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import groovy.lang.Script;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class LayeredEngineConcurrencyTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private volatile boolean failed;
    private void setFailed(boolean failed) {
        this.failed = failed;
    }
    
    private String mapToString(int nCodeChanges, Map<Integer,Integer> map, int maxDigits) {
        StringBuilder out = new StringBuilder();
        for (int i=0; i<nCodeChanges; i++) {
            Integer value = map.get(i);
            String s = (value == null) ? "" : Integer.toString(value);
            out.append(TestUtil.multiply(" ", maxDigits - s.length()));
            out.append(s);
        }
        return out.toString();
    }

    private void testConcurrent(final LayeredEngine engine, final String info) throws Exception {

        setFailed(false);

        final MockTextSource s1 = new MockTextSource("return 0");
        final Sources sources = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(s1), "concurrent");
        
        Code code = new DefaultGroovyCompiler().compile(sources);
        List<Code> codeLayers = Arrays.asList(code);
        
        engine.setCodeLayers(codeLayers);

        final int nThreads = 4;
        final int nCodeChanges = 5;
        final long delayBetweenCodeChangesMs = 500;
        final long miniDelayMs = 5;
        final Map<Integer,Integer> totalMap = new HashMap<>();
        
        
        System.out.println(info);
        
        long t0 = System.currentTimeMillis();

        Thread[] threads = new Thread[nThreads+1];
        for (int i=0; i<nThreads+1; i++) {
            final int x = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 1; j <= nCodeChanges; j++) {
                        if (x == 0) {
                            Thread.sleep(delayBetweenCodeChangesMs);
                            s1.setText("return " + j);
                            s1.setLastModified(j);
                            Code code1 = new DefaultGroovyCompiler().compile(sources);
                            List<Code> codeLayers1 = Arrays.asList(code1);
                            engine.setCodeLayers(codeLayers1);
                            //System.out.println(j);
                        } else {
                            Loader attachedLoader = engine.newAttachedLoader();
                            Map<Integer, Integer> rcMap = new TreeMap<>();
                            int rc = (Integer) ((Script) engine.loadMainClass(attachedLoader, s1)
                                    .newInstance()).run();
                            int count = 1;
                            do {
                                int rc2 = (Integer) ((Script) engine.loadMainClass(attachedLoader, s1)
                                        .newInstance()).run();
                                if (rc2 == rc) {
                                    count++;
                                } else {
                                    rc = rc2;
                                    rcMap.put(rc - 1, count);
                                    count = 1;
                                    Thread.sleep(x * miniDelayMs);
                                    if (rc == nCodeChanges) {
                                        System.out.printf("Thread %2d %2d: %s%n", x, rcMap.size(),
                                                mapToString(nCodeChanges, rcMap, 8));
                                        synchronized (totalMap) {
                                            for (Entry<Integer, Integer> entry : rcMap.entrySet()) {
                                                int i1 = entry.getKey();
                                                int val = entry.getValue();
                                                Integer valOldInteger = totalMap.get(i1);
                                                int valOld = (valOldInteger == null) ? 0 : valOldInteger;
                                                totalMap.put(i1, valOld + val);
                                            }
                                        }
                                        return;
                                    }
                                    Thread.sleep(x * miniDelayMs);
                                }
                            } while (true);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("x=" + x + ": " + e);
                    e.printStackTrace();
                    setFailed(true);
                }
            });
        }
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }
        
        int n = totalMap.size();
        System.out.printf("TOTAL  %s %2d: %s%n", n == nCodeChanges ? "OK" : "--", totalMap.size(),
                mapToString(nCodeChanges, totalMap, 8));
        int totalRuns = 0;
        for (Entry<Integer, Integer> entry : totalMap.entrySet()) {
            totalRuns += entry.getValue();
        }
        System.out.println("Script runs: " + totalRuns);
        long t1 = System.currentTimeMillis();
        System.out.println("Duration: " + (t1 - t0) + " ms");
        System.out.println("Average time per script run: " + (t1 - t0)*1000000L / totalRuns + " ns");
        assertThat(nCodeChanges, is(n));

        assertThat(failed, is(false));
    }

    @Test
    public void testConcurrentNoTopCodeCache() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        
        final LayeredEngine engine = builder.build();
        
        testConcurrent(engine, "TEST LayeredEngine concurrent - no top code cache");
    }

    @Test
    public void testConcurrentTopCodeCacheParentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(true);
        builder.setTopLoadMode(LoadMode.PARENT_FIRST);
        
        final LayeredEngine engine = builder.build();
        
        testConcurrent(engine, "TEST LayeredEngine concurrent - top code cache - parent first");
    }

    @Test
    public void testConcurrentTopCodeCacheCurrentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(true);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);
        
        final LayeredEngine engine = builder.build();
        
        testConcurrent(engine, "TEST LayeredEngine concurrent - top code cache - current first");
    }

}
