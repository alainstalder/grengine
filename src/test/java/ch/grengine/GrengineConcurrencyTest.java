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

import ch.grengine.engine.LayeredEngine;
import ch.grengine.engine.Loader;
import ch.grengine.load.LoadMode;
import ch.grengine.source.MockTextSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.FixedSetSources;
import ch.grengine.sources.Sources;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class GrengineConcurrencyTest {

    private static final int N_THREADS = 4;
    private static final int N_CODE_CHANGES = 5;
    private static final long DELAY_BETWEEN_CODE_CHANGES_MS = 500;
    private static final long MINI_DELAY_MS = 5;
    private static final int MAX_DIGITS = 8;

    private volatile boolean failed;
    private void setFailed(final boolean failed) {
        this.failed = failed;
    }
    
    private static String mapToString(final Map<Integer,Integer> map) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i< N_CODE_CHANGES; i++) {
            final Integer value = map.get(i);
            final String s = (value == null) ? "" : Integer.toString(value);
            out.append(TestUtil.multiply(" ", MAX_DIGITS - s.length()));
            out.append(s);
        }
        return out.toString();
    }

    private void testConcurrent(final LayeredEngine engine, final String info) throws Exception {

        // given

        setFailed(false);

        final MockTextSource s1 = new MockTextSource("return 0");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(10)
                .setName("concurrent")
                .build();
        final List<Sources> sourcesLayers = Collections.singletonList(sources);
        
        final Grengine gren = new Grengine.Builder()
                .setEngine(engine)
                .setSourcesLayers(sourcesLayers)
                .setLatencyMs(30)
                .build();
        
        final Map<Integer,Integer> totalMap = new HashMap<>();
        
        System.out.println(info);

        final long t0 = System.currentTimeMillis();

        Thread[] threads = new Thread[N_THREADS +1];
        for (int i = 0; i< N_THREADS +1; i++) {
            final int x = i;
            threads[i] = new Thread(
                    () -> {
                        try {
                            for (int j = 1; j<= N_CODE_CHANGES; j++) {
                                if (x == 0) {
                                    Thread.sleep(DELAY_BETWEEN_CODE_CHANGES_MS);
                                    s1.setText("return " + j);
                                    s1.setLastModified(j);
                                    //System.out.println(j);
                                } else {
                                    Loader attachedLoader = gren.newAttachedLoader();
                                    Map<Integer,Integer> rcMap = new TreeMap<>();
                                    int rc = (Integer)(gren.run(attachedLoader, s1));
                                    int count = 1;
                                    do {
                                        int rc2 = (Integer)(gren.run(attachedLoader, s1));
                                        if (rc2 == rc) {
                                            count++;
                                        } else {
                                            rc = rc2;
                                            rcMap.put(rc-1, count);
                                            count = 1;
                                            Thread.sleep(x * MINI_DELAY_MS);
                                            if (rc == N_CODE_CHANGES) {
                                                System.out.printf("Thread %2d %2d: %s%n", x, rcMap.size(),
                                                        mapToString(rcMap));
                                                synchronized(totalMap) {
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
                                            Thread.sleep(x * MINI_DELAY_MS);
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

        // when
        
        for (Thread t : threads) {
            t.start();
        }
        
        for (Thread t : threads) {
            t.join();
        }

        // then

        final int n = totalMap.size();
        System.out.printf("TOTAL  %s %2d: %s%n", n == N_CODE_CHANGES ? "OK" : "--", totalMap.size(),
                mapToString(totalMap));
        final int totalRuns = totalMap.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        System.out.println("Script runs: " + totalRuns);
        final long t1 = System.currentTimeMillis();
        System.out.println("Duration: " + (t1 - t0) + " ms");
        System.out.println("Average time per script run: " + (t1 - t0)*1000000L / totalRuns + " ns");
        assertThat(N_CODE_CHANGES, is(n));

        assertThat(failed, is(false));
    }

    @Test
    void testConcurrentNoTopCodeCache() throws Exception {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .build();
        testConcurrent(engine, "TEST Grengine concurrent - no top code cache");
    }

    @Test
    void testConcurrentTopCodeCacheParentFirst() throws Exception {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.PARENT_FIRST)
                .build();
        testConcurrent(engine, "TEST Grengine concurrent - top code cache - parent first");
    }

    @Test
    void testConcurrentTopCodeCacheCurrentFirst() throws Exception {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .build();
        testConcurrent(engine, "TEST Grengine concurrent - top code cache - current first");
    }

}

