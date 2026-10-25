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

import ch.grengine.Grengine;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.engine.Loader;
import ch.grengine.load.ClassReleaser;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.Map;
import java.util.WeakHashMap;

import groovy.lang.GroovySystem;
import groovy.lang.Script;

/**
 * Class garbage collection check for Grengine, used by testbed/matrix.sh --gc.
 * <p>
 * Initially created by Claude Code (Opus 5.5) in Oct 2026,
 * based on the earlier ClassGCTester (2016) by Alain Stalder.
 * <p>
 * Runs a Groovy script in a loop, each time loaded from cached bytecode
 * into a new detached loader, which is closed afterwards, so that the
 * class releaser releases the script classes. Classes are only referenced
 * weakly afterwards. Prints one line per second with the number of loaded
 * and still alive script classes and memory usage, and finally a RESULT line.
 * <p>
 * Usage: java [-XX:MaxMetaspaceSize=64m ...] GrengineClassGc [-secs N] [-noRelease]
 * <p>
 * Result is OK (exit code 0) if there was no OutOfMemoryError and
 * some script classes were garbage collected, else FAILED (exit code 1).
 */
public class GrengineClassGc {

    private static final String SCRIPT_TEXT =
            "def list = [1, 2, 3].collect { it * 2 }\n" +
            "return list.sum()\n";

    public static void main(final String[] args) {
        int secs = 20;
        boolean release = true;
        for (int i = 0; i < args.length; i++) {
            if ("-secs".equals(args[i]) && i + 1 < args.length) {
                secs = Integer.parseInt(args[++i]);
            } else if ("-noRelease".equals(args[i])) {
                release = false;
            } else {
                System.err.println("Usage: java [java-args] GrengineClassGc [-secs N] [-noRelease]");
                System.exit(2);
            }
        }

        System.out.println("Java:    " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Groovy:  " + GroovySystem.getVersion());
        System.out.println("JVM:     " + ManagementFactory.getRuntimeMXBean().getInputArguments());
        System.out.println("Release: " + release);
        System.out.println();

        final Grengine gren;
        if (release) {
            gren = new Grengine();
        } else {
            final ClassReleaser noReleaser = clazz -> { };
            gren = new Grengine.Builder()
                    .setEngine(new LayeredEngine.Builder().setClassReleaser(noReleaser).build())
                    .build();
        }

        final MemoryPoolMXBean metaspace = getMetaspaceBean();
        final Map<Class<?>, Boolean> aliveClasses = new WeakHashMap<>();

        System.out.println("Secs    #loaded     #alive   metaspace       heap");

        final long t0 = System.currentTimeMillis();
        final long tEnd = t0 + secs * 1000L;
        long nLoaded = 0;
        int maxAlive = 0;
        double maxMetaspaceMb = 0;
        boolean collected = false;
        int lastSec = -1;
        try {
            while (System.currentTimeMillis() < tEnd) {
                try (Loader loader = gren.newDetachedLoader()) {
                    final Script script = gren.create(loader, SCRIPT_TEXT);
                    script.run();
                    aliveClasses.put(script.getClass(), Boolean.TRUE);
                }
                nLoaded++;

                final int sec = (int) ((System.currentTimeMillis() - t0) / 1000L);
                if (sec != lastSec) {
                    lastSec = sec;
                    final int alive = aliveClasses.size();
                    final double metaspaceMb = metaspace == null ? 0 : mb(metaspace.getUsage().getUsed());
                    final Runtime rt = Runtime.getRuntime();
                    maxAlive = Math.max(maxAlive, alive);
                    maxMetaspaceMb = Math.max(maxMetaspaceMb, metaspaceMb);
                    collected |= alive < nLoaded;
                    System.out.printf("%4d %10d %10d %10.1fm %9.1fm%n",
                            sec, nLoaded, alive, metaspaceMb, mb(rt.totalMemory() - rt.freeMemory()));
                }
            }
        } catch (OutOfMemoryError e) {
            aliveClasses.clear();
            System.out.println();
            System.out.printf("RESULT FAILED %s after %d loaded%n", e, nLoaded);
            System.exit(1);
        }

        collected |= aliveClasses.size() < nLoaded;
        System.out.println();
        System.out.printf("RESULT %s %d loaded, max %d alive, metaspace max %.1fm%n",
                collected ? "OK" : "FAILED (no class collected)", nLoaded, maxAlive, maxMetaspaceMb);
        System.exit(collected ? 0 : 1);
    }

    private static MemoryPoolMXBean getMetaspaceBean() {
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (bean.getName().equals("Metaspace")) {
                return bean;
            }
        }
        return null;
    }

    private static double mb(final long bytes) {
        return bytes / 1024.0 / 1024.0;
    }

}
