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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static ch.grengine.TestUtil.toRuntimeException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class EngineConcurrencyTestFrame {

    private static final int N_THREADS = 4;
    private static final int N_SOURCE_CHANGES = 5;
    private static final long DELAY_BETWEEN_SOURCE_CHANGES_MS = 500;
    private static final long MINI_DELAY_MS = 5;
    private static final int MAX_DIGITS = 8;

    private volatile boolean failed;
    private void setFailed(final boolean failed) {
        this.failed = failed;
    }
    private final TreeMap<Integer,Integer> totalMap = new TreeMap<>();

    // about a single source that is part of an engine
    public interface ConcurrencyTestContext {

        // init source with given script text
        void initSource(String scriptText);

        // change source script text
        void updateSource(String scriptText);

        // run script
        Object runScript(int iThread) throws Exception;
    }

    // thread that periodically changes the script source text
    private static class SourceChangerThread extends Thread {

        private final ConcurrencyTestContext ctx;

        SourceChangerThread(final ConcurrencyTestContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            IntStream.rangeClosed(1, N_SOURCE_CHANGES).forEach(iSourceChanges -> {
                toRuntimeException(() -> Thread.sleep(DELAY_BETWEEN_SOURCE_CHANGES_MS));
                ctx.updateSource("return " + iSourceChanges);
                //System.out.println("Source changed: " + iSourceChanges);
            });
        }
    }

    // thread that runs the script in a loop
    private class ScriptRunnerThread extends Thread {

        private final ConcurrencyTestContext ctx;
        private final int iThread;

        ScriptRunnerThread(final ConcurrencyTestContext ctx, final int iThread) {
            this.ctx = ctx;
            this.iThread = iThread;
        }

        @Override
        public void run() {
            try {
                final TreeMap<Integer, Integer> rcMap = new TreeMap<>();
                int rc = (Integer) ctx.runScript(iThread);
                int count = 1;
                do {
                    int rc2 = (Integer) ctx.runScript(iThread);
                    if (rc2 == rc) {
                        count++;
                    } else if (rc2 != rc + 1) {
                        System.out.println("iThread: " + iThread +
                                " : skipped from source change " + rc + " to " + rc2);
                        setFailed(true);
                        return;
                    } else {
                        rc = rc2;
                        rcMap.put(rc - 1, count);
                        count = 1;
                        Thread.sleep(iThread * MINI_DELAY_MS);
                        if (rc == N_SOURCE_CHANGES) {
                            System.out.printf("Thread %2d %2d: %s%n", iThread, rcMap.size(),
                                    treeMapToString(rcMap));
                            synchronized (totalMap) {
                                rcMap.forEach((iSourceChange, val) -> {
                                    Integer valOldInteger = totalMap.get(iSourceChange);
                                    int valOld = (valOldInteger == null) ? 0 : valOldInteger;
                                    totalMap.put(iSourceChange, valOld + val);
                                });
                            }
                            return;
                        }
                        Thread.sleep(iThread * MINI_DELAY_MS);
                    }
                } while (true);
            } catch (Exception e) {
                System.out.println("iThread = " + iThread + " : " + e);
                e.printStackTrace();
                setFailed(true);
            }
        }
    }

    private static String treeMapToString(final TreeMap<Integer,Integer> treeMap) {
        final StringBuilder out = new StringBuilder();
        treeMap.forEach((i, value) -> {
            final String s = (value == null) ? "" : Integer.toString(value);
            out.append(TestUtil.repeatString(" ", MAX_DIGITS - s.length()));
            out.append(s);
        });
        return out.toString();
    }

    public void testConcurrent(final String info, final ConcurrencyTestContext ctx) {

        // given

        setFailed(false);
        System.out.println(info);

        totalMap.clear();

        ctx.initSource("return 0");

        List<Thread> threads = new ArrayList<>(1 + N_THREADS);
        threads.add(new SourceChangerThread(ctx));
        IntStream.rangeClosed(1, N_THREADS).forEach(iThread ->
                threads.add(new ScriptRunnerThread(ctx, iThread)));

        final long t0 = System.currentTimeMillis();

        // when

        threads.forEach(Thread::start);
        threads.forEach(t -> toRuntimeException(t::join));

        // then

        final int n = totalMap.size();
        System.out.printf("TOTAL  %s %2d: %s%n", n == N_SOURCE_CHANGES ? "OK" : "--", totalMap.size(),
                treeMapToString(totalMap));
        final int totalRuns = totalMap.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        System.out.println("Script runs: " + totalRuns);
        final long t1 = System.currentTimeMillis();
        System.out.println("Duration: " + (t1 - t0) + " ms");
        System.out.println("Average time per script run: " + (t1 - t0)*1000000L / totalRuns + " ns");
        assertThat(N_SOURCE_CHANGES, is(n));

        assertThat(failed, is(false));
    }

}
