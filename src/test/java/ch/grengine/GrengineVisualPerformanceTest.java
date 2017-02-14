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

import ch.grengine.source.DefaultSourceFactory;

import java.io.File;
import java.net.URL;
import java.util.UUID;

import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Command line visual performance test.
 * 
 * @author Alain Stalder
 *
 */
public class GrengineVisualPerformanceTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private static final long RUN_DURATION_NS = 1000L * 1000L * 1000L;
    private static final int N_RUNS = 4;
    private static final long N_INNER = 100;
    
    //@Test
    public void runMain() throws Exception {
        main();
    }

    public static void main(String... args) throws Exception {
        
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File scriptDir = new File(tempDir, UUID.randomUUID().toString());
        scriptDir.mkdirs();
        Runnable runner;
        
        System.out.printf("Grengine Visual Performance Test%n");
        System.out.printf("================================%n");
        System.out.println();
        System.out.printf("Essentially measures the performance cost for invoking a script in different ways.%n");
        System.out.printf("In general, the faster ways need some caching, hence cost memory instead.%n");
        System.out.println();        
        System.out.printf("OS name:    %s%n", System.getProperty("os.name"));
        System.out.printf("OS arch:    %s%n", System.getProperty("os.arch"));
        System.out.printf("OS version: %s%n", System.getProperty("os.version"));
        System.out.println();
        System.out.printf("Duration of each run: %d ms%n", RUN_DURATION_NS / (1000L * 1000L));
        System.out.printf("Number of runs: %d%n", N_RUNS);
        
        System.out.println();
        System.out.println("Reference: Compile each time");
        System.out.println("----------------------------");
        System.out.println();
        System.out.println("  Using the Groovy JDK GroovyShell as a reference:");
        System.out.println("    GroovyShell shell = new GroovyShell();");
        System.out.println("    shell.evaluate(\"return 2\");");
        System.out.println("  NOTE: Compiles at each evaluation.");

        final GroovyShell shell = new GroovyShell();
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        shell.evaluate("return 2");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));

        System.out.println();
        System.out.println("Grengine");
        System.out.println("--------");
        System.out.println();
        System.out.println("  Grengine with all settings default:");
        System.out.println("    Grengine gren = new Grengine();");
        System.out.println();
        System.out.println("  Grengine with all settings default, except tracking text source ID and file last modified");
        System.out.println("    Grengine grenOptimized = new Grengine.Builder()");
        System.out.println("       .setSourceFactory(new DefaultSourceFactory.Builder()");
        System.out.println("           .setTrackTextSourceIds(true)");
        System.out.println("           .setTrackFileSourceLastModified(true)");
        System.out.println("           .build())"); 
        System.out.println("       .build())"); 
        System.out.println();
        System.out.println("  Run script by script text:");
        System.out.println("    gren.run(\"return 2\");");
        System.out.println("  NOTE: Compiles only once during the first run.");
        System.out.println("  NOTE: Calculates MD5 hash at each run.");

        // no layers, all code running in dynamic top code cache (load mode "parent first")
        final Grengine gren = new Grengine();
        
        final Grengine grenOptimized = new Grengine.Builder()
            .setSourceFactory(new DefaultSourceFactory.Builder()
                .setTrackTextSourceIds(true)
                .setTrackFileSourceLastModified(true)
                .build())    
            .build();
        
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        gren.run("return 2");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));
        
        System.out.println();
        System.out.println("  Run script by script text, with caching text source ID:");
        System.out.println("    grenOptimized.run(\"return 2\");");
        System.out.println("  NOTE: Compiles only once during the first run.");
        System.out.println("  NOTE: Calculates MD5 hash only once during the first run.");

        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        grenOptimized.run("return 2");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));
        
        System.out.println();
        System.out.println("  Run script by script file:");
        System.out.println("    File scriptFile = <some file which contains \"return 2\">;");
        System.out.println("    gren.run(scriptFile);");
        System.out.println("  NOTE: Compiles only once during the first run.");
        System.out.println("  NOTE: Calls scriptFile.lastModified() at each run.");
        
        final File scriptFile = new File(scriptDir, "Return.groovy");
        TestUtil.setFileText(scriptFile, "return 2");
        
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        gren.run(scriptFile);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));

        System.out.println();
        System.out.println("  Run script by script file, with caching file last modified:");
        System.out.println("    grenOptimized.run(scriptFile);");
        System.out.println("  NOTE: Compiles only once during the first run.");
        System.out.println("  NOTE: Calls scriptFile.lastModified() only during the first run.");
        
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        grenOptimized.run(scriptFile);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));

        System.out.println();
        System.out.println("  Run script by script URL:");
        System.out.println("    File scriptUrl = scriptFile.toURI().toURL();");
        System.out.println("    grenOptimized.run(scriptFile);");
        System.out.println("  NOTE: Compiles only once during the first run.");
        System.out.println("  NOTE: Not tracking script modifications.");
        
        final URL scriptUrl = scriptFile.toURI().toURL();
        
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        gren.run(scriptUrl);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));
        
        System.out.println();
        System.out.println("  Load script class only once, but create a new Script instance at each run:");
        System.out.println("    Class<?> scriptClass = gren.load(\"return 2\");");
        System.out.println("    gren.run(gren.create(scriptClass));");
        System.out.println("  NOTE: Compiles never during runs (compiled once when loaded).");
        System.out.println("  NOTE: Creates a new Script instance at each run.");

        final Class<?> scriptClass = gren.load("return 2");
        
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        gren.run(gren.create(scriptClass));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));

        System.out.println();
        System.out.println("  Load script class only once and create Script instance also only once:");
        System.out.println("    Script script = gren.create(scriptClass);");
        System.out.println("    gren.run(script);");
        System.out.println("  NOTE: Compiles never during runs (compiled once when loaded).");
        System.out.println("  NOTE: Creates no Script instances during runs.");

        final Script script = gren.create(scriptClass);
        
        runner = new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i=0; i<N_INNER; i++) {
                        gren.run(script);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        printRunInfo(runForDuration(runner));
    }

    private static long[] runForDuration(Runnable runner) {
        long[] timesPerRunNs = new long[N_RUNS];
        for (int j=0; j<N_RUNS; j++) {
            long t0 = System.nanoTime();
            long t1;
            int n = 0;
            do {
                runner.run();
                t1 = System.nanoTime();
                n+= N_INNER;
            } while (t1-t0 < RUN_DURATION_NS);
            long timePerRunNs = (t1-t0) / n;
            timesPerRunNs[j] = timePerRunNs;
        }
        return timesPerRunNs;
    }

    private static void printRunInfo(long[] timesPerRunNs) {
        System.out.println();
        System.out.print("  Run: ");
        for (int i=0; i<N_RUNS; i++) {
            System.out.printf("%9d      ", i+1);
        }
        System.out.println();
        System.out.print("       ");
        for (long timeNs : timesPerRunNs) {
            System.out.printf("%9d ns   ", timeNs);
        }
        System.out.println("  (average time per script run)");
    }

}

