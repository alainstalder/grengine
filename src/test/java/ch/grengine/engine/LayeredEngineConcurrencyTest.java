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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import groovy.lang.Script;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class LayeredEngineConcurrencyTest {

    private static final int N_THREADS = 4;
    private static final int N_CODE_CHANGES = 5;
    private static final long DELAY_BETWEEN_CODE_CHANGES_MS = 500;
    private static final long MINI_DELAY_MS = 5;
    private static final int MAX_DIGITS = 8;

    private volatile boolean failed;
    private void setFailed(final boolean failed) {
        this.failed = failed;
    }
    
    private String mapToString(final Map<Integer,Integer> map) {
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
        final Sources sources = SourcesUtil.sourceSetToSources(SourceUtil.sourceArrayToSourceSet(s1), "concurrent");

        final Code code = new DefaultGroovyCompiler().compile(sources);
        final List<Code> codeLayers = Collections.singletonList(code);
        
        engine.setCodeLayers(codeLayers);

        final Map<Integer,Integer> totalMap = new HashMap<>();

        System.out.println(info);

        final long t0 = System.currentTimeMillis();

        final Thread[] threads = new Thread[N_THREADS +1];
        for (int i = 0; i< N_THREADS +1; i++) {
            final int x = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 1; j <= N_CODE_CHANGES; j++) {
                        if (x == 0) {
                            Thread.sleep(DELAY_BETWEEN_CODE_CHANGES_MS);
                            s1.setText("return " + j);
                            s1.setLastModified(j);
                            final Code code1 = new DefaultGroovyCompiler().compile(sources);
                            final List<Code> codeLayers1 = Collections.singletonList(code1);
                            engine.setCodeLayers(codeLayers1);
                            //System.out.println(j);
                        } else {
                            final Loader attachedLoader = engine.newAttachedLoader();
                            final Map<Integer, Integer> rcMap = new TreeMap<>();
                            int rc = (Integer) ((Script) engine.loadMainClass(attachedLoader, s1)
                                    .getConstructor().newInstance()).run();
                            int count = 1;
                            do {
                                int rc2 = (Integer) ((Script) engine.loadMainClass(attachedLoader, s1)
                                        .getConstructor().newInstance()).run();
                                if (rc2 == rc) {
                                    count++;
                                } else {
                                    rc = rc2;
                                    rcMap.put(rc - 1, count);
                                    count = 1;
                                    Thread.sleep(x * MINI_DELAY_MS);
                                    if (rc == N_CODE_CHANGES) {
                                        System.out.printf("Thread %2d %2d: %s%n", x, rcMap.size(),
                                                mapToString(rcMap));
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
    public void testConcurrentNoTopCodeCache() throws Exception {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .build();
        testConcurrent(engine, "TEST LayeredEngine concurrent - no top code cache");
    }

    @Test
    public void testConcurrentTopCodeCacheParentFirst() throws Exception {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.PARENT_FIRST)
                .build();
        testConcurrent(engine, "TEST LayeredEngine concurrent - top code cache - parent first");
    }

    @Test
    public void testConcurrentTopCodeCacheCurrentFirst() throws Exception {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .build();
        testConcurrent(engine, "TEST LayeredEngine concurrent - top code cache - current first");
    }

}
