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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.engine.LayeredEngine;
import ch.grengine.engine.Loader;
import ch.grengine.load.LoadMode;
import ch.grengine.source.MockTextSource;
import ch.grengine.source.Source;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.FixedSetSources;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

/**
 * Tests the respective class.
 * 
 * @author Alain Stalder
 *
 */
public class GrengineConcurrencyTest {
    
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

    public void testConcurrent(final LayeredEngine engine, final String info) throws Exception {

        final MockTextSource s1 = new MockTextSource("return 0");
        final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(s1);
        final Sources sources = new FixedSetSources.Builder(sourceSet)
                .setLatencyMs(10)
                .setName("concurrent")
                .build();
        List<Sources> sourcesLayers = SourcesUtil.sourcesArrayToList(sources);
        
        final Grengine gren = new Grengine.Builder()
                .setEngine(engine)
                .setSourcesLayers(sourcesLayers)
                .setLatencyMs(30)
                .build();
        
        final int nThreads = 4;
        final int nCodeChanges = 5;
        final long delayBetweenCodeChangesMs = 500;
        final long miniDelayMs = 5;
        final Map<Integer,Integer> totalMap = new HashMap<Integer,Integer>();
        
        System.out.println(info);
        
        long t0 = System.currentTimeMillis();
        
        Thread[] threads = new Thread[nThreads+1];
        for (int i=0; i<nThreads+1; i++) {
            final int x = i;
            threads[i] = new Thread(
                    new Runnable() {
                        public void run() {
                            try {
                                for (int j=1; j<=nCodeChanges; j++) {
                                    if (x == 0) {
                                        Thread.sleep(delayBetweenCodeChangesMs);
                                        s1.setText("return " + j);
                                        s1.setLastModified(j);
                                        //System.out.println(j);                                        
                                    } else {
                                        Loader attachedLoader = gren.newAttachedLoader();
                                        Map<Integer,Integer> rcMap = new TreeMap<Integer,Integer>();
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
                                                Thread.sleep(x * miniDelayMs);
                                                if (rc == nCodeChanges) {
                                                    System.out.printf("Thread %2d %2d: %s%n", x, rcMap.size(),
                                                            mapToString(nCodeChanges, rcMap, 8));
                                                    synchronized(totalMap) {
                                                        for (Entry<Integer, Integer> entry : rcMap.entrySet()) {
                                                            int i = entry.getKey();
                                                            int val = entry.getValue();
                                                            Integer valOldInteger = totalMap.get(i);
                                                            int valOld = (valOldInteger == null) ? 0 : valOldInteger;
                                                            totalMap.put(i, valOld + val);
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
        System.out.println("Average time per script run: " + (t1 - t0)*1000000L / totalRuns + " ps");
        assertEquals(n, nCodeChanges);
        
        assertFalse(failed);
    }

    @Test
    public void testConcurrentNoTopCodeCache() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(false);
        
        final LayeredEngine engine = builder.build();
        
        testConcurrent(engine, "TEST Grengine concurrent - no top code cache");
    }

    @Test
    public void testConcurrentTopCodeCacheParentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(true);
        builder.setTopLoadMode(LoadMode.PARENT_FIRST);
        
        final LayeredEngine engine = builder.build();
        
        testConcurrent(engine, "TEST Grengine concurrent - top code cache - parent first");
    }

    @Test
    public void testConcurrentTopCodeCacheCurrentFirst() throws Exception {
        
        LayeredEngine.Builder builder = new LayeredEngine.Builder();
        builder.setWithTopCodeCache(true);
        builder.setTopLoadMode(LoadMode.CURRENT_FIRST);
        
        final LayeredEngine engine = builder.build();
        
        testConcurrent(engine, "TEST Grengine concurrent - top code cache - current first");
    }

}

