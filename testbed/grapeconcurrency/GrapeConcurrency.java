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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import groovy.grape.Grape;
import groovy.grape.GrapeEngine;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovySystem;

/**
 * Grape concurrency check (GROOVY-7407), used by testbed/matrix.sh --grape.
 * <p>
 * Initially created by Claude Code (Opus 5.5) in Oct 2026,
 * based on the earlier GrabConcurrencyTest (Jan 2025).
 * <p>
 * Starts many threads that concurrently grab guava 10.0 to 19.0 into an
 * empty Grape cache, each into its own GroovyClassLoader, and then load a
 * guava class from it. Uses the default Ivy configuration of the Groovy
 * version, except that the Grape cache is under the given root and the
 * local Maven repository (~/.m2) is not used; with -repo, artifacts are
 * grabbed from the given local Maven repository instead of Maven Central,
 * with -http served over HTTP from 127.0.0.1 instead of as file: URLs,
 * with -httpDelay MS (implies -http) delayed by that many milliseconds
 * before each response and after each 16 KB sent, with -httpFaults PERCENT
 * (implies -http) with that percentage of responses failing, equally with
 * status 503 or (if not HEAD) after half of the body with the connection
 * closed or stalled until the end of the run.
 * With -lock, grabs are serialized with Grengine.Grape.activate().
 * With -ivyTimeout, Ivy connection and read timeouts are set for the
 * ibiblio resolvers.
 * <p>
 * Usage: java [java-args] GrapeConcurrency -root DIR
 *        [-repo DIR [-http] [-httpDelay MS] [-httpFaults PERCENT]] [-lock]
 *        [-ivyTimeout SECS] [-threads N] [-timeout SECS]
 * <p>
 * Result is OK (exit code 0) if all threads succeeded within the timeout,
 * else FAILED (exit code 1).
 */
public class GrapeConcurrency {

    private static final int N_VERSIONS = 10;
    private static final String CLASS_NAME = "com.google.common.base.Ascii";
    private static final int HTTP_CHUNK_SIZE = 16 * 1024;
    private static final AtomicInteger HTTP_FAULTS_503 = new AtomicInteger();
    private static final AtomicInteger HTTP_FAULTS_ABORTED = new AtomicInteger();
    private static final AtomicInteger HTTP_FAULTS_STALLED = new AtomicInteger();

    public static void main(final String[] args) throws Exception {
        File root = null;
        File repo = null;
        boolean http = false;
        int httpDelayMs = 0;
        int httpFaultsPercent = 0;
        boolean lock = false;
        int nThreads = 100;
        int timeoutSecs = 60;
        int ivyTimeoutSecs = 0;
        for (int i = 0; i < args.length; i++) {
            if ("-root".equals(args[i]) && i + 1 < args.length) {
                root = new File(args[++i]).getAbsoluteFile();
            } else if ("-repo".equals(args[i]) && i + 1 < args.length) {
                repo = new File(args[++i]).getAbsoluteFile();
            } else if ("-http".equals(args[i])) {
                http = true;
            } else if ("-httpDelay".equals(args[i]) && i + 1 < args.length) {
                http = true;
                httpDelayMs = Integer.parseInt(args[++i]);
            } else if ("-httpFaults".equals(args[i]) && i + 1 < args.length) {
                http = true;
                httpFaultsPercent = Integer.parseInt(args[++i]);
            } else if ("-lock".equals(args[i])) {
                lock = true;
            } else if ("-threads".equals(args[i]) && i + 1 < args.length) {
                nThreads = Integer.parseInt(args[++i]);
            } else if ("-ivyTimeout".equals(args[i]) && i + 1 < args.length) {
                ivyTimeoutSecs = Integer.parseInt(args[++i]);
            } else if ("-timeout".equals(args[i]) && i + 1 < args.length) {
                timeoutSecs = Integer.parseInt(args[++i]);
            } else {
                usage();
            }
        }
        if (root == null || (http && repo == null)) {
            usage();
        }
        final String repoUrl = repo == null ? null : http ? serveRepo(repo, httpDelayMs, httpFaultsPercent) : repo.toURI().toString();

        final AtomicInteger nOk = new AtomicInteger();
        final Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();
        startWatchdog(timeoutSecs + 60, nThreads, nOk, failures);
        // e.g. the HTTP server would keep the JVM alive after an exception in main
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            System.out.flush();
            Runtime.getRuntime().halt(1);
        });

        final File cache = new File(root, "grapes");
        if (!cache.mkdirs()) {
            throw new IOException("Could not create empty Grape cache " + cache);
        }
        System.setProperty("grape.root", root.getPath());
        final String ivyConfig = readDefaultIvyConfig();
        if (ivyConfig != null) {
            final File configFile = new File(root, "grapeConfig.xml");
            final String testConfig = toTestIvyConfig(ivyConfig, cache, repoUrl, ivyTimeoutSecs);
            Files.write(configFile.toPath(), testConfig.getBytes(StandardCharsets.UTF_8));
            System.setProperty("grape.config", configFile.getPath());
        }

        if (Grape.getInstance() == null) {
            throw new IllegalStateException("No GrapeEngine available");
        }
        if (repoUrl != null && ivyConfig == null) {
            useOnlyLocalRepo(Grape.getInstance(), repoUrl);
        }
        if (lock) {
            Grengine.Grape.activate();
        }
        final GrapeEngine engine = Grape.getInstance();

        System.out.println("Java:    " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("Groovy:  " + GroovySystem.getVersion());
        System.out.println("Engine:  " + (lock ? "Grengine lock around " : "")
                + (ivyConfig != null ? "Ivy" : "not Ivy") + " (" + engine.getClass().getName() + ")");
        System.out.println("Repo:    " + (repoUrl != null ? repoUrl : "Maven Central")
                + (httpDelayMs > 0 ? " (delay " + httpDelayMs + " ms)" : "")
                + (httpFaultsPercent > 0 ? " (faults " + httpFaultsPercent + "%)" : ""));
        System.out.println("Threads: " + nThreads + " (guava 10.0 to " + (9 + N_VERSIONS) + ".0)");
        System.out.println();

        final List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < nThreads; i++) {
            final String version = (10 + i % N_VERSIONS) + ".0";
            final Thread thread = new Thread(() -> {
                try {
                    final GroovyClassLoader loader = new GroovyClassLoader();
                    final Map<String, Object> grabArgs = new HashMap<>();
                    grabArgs.put("classLoader", loader);
                    final Map<String, Object> dependency = new HashMap<>();
                    dependency.put("group", "com.google.guava");
                    dependency.put("module", "guava");
                    dependency.put("version", version);
                    Grape.grab(grabArgs, dependency);
                    loader.loadClass(CLASS_NAME);
                    nOk.incrementAndGet();
                } catch (Throwable t) {
                    final String msg = String.valueOf(t).split("\n")[0];
                    failures.computeIfAbsent(msg.length() > 160 ? msg.substring(0, 160) + "..." : msg,
                            m -> new AtomicInteger()).incrementAndGet();
                }
            }, String.format("Grab%03d-guava-%s", i, version));
            thread.setDaemon(true);
            threads.add(thread);
        }

        final long t0 = System.currentTimeMillis();
        for (Thread thread : threads) {
            thread.start();
        }
        final long tEnd = t0 + timeoutSecs * 1000L;
        for (Thread thread : threads) {
            final long remaining = tEnd - System.currentTimeMillis();
            if (remaining > 0) {
                thread.join(remaining);
            }
        }
        final double secs = (System.currentTimeMillis() - t0) / 1000.0;

        // hung threads grouped by stack, e.g. lock holder vs. threads waiting for it
        int nHung = 0;
        final Map<String, List<String>> hungByStack = new LinkedHashMap<>();
        for (Thread thread : threads) {
            if (thread.isAlive()) {
                nHung++;
                final StringBuilder stack = new StringBuilder();
                for (StackTraceElement element : thread.getStackTrace()) {
                    stack.append("    at ").append(element).append('\n');
                    if (element.getClassName().startsWith(GrapeConcurrency.class.getName())) {
                        break;
                    }
                }
                hungByStack.computeIfAbsent(stack.toString(), s -> new ArrayList<>()).add(thread.getName());
            }
        }
        for (Map.Entry<String, List<String>> entry : hungByStack.entrySet()) {
            final List<String> names = entry.getValue();
            System.out.println("Hung: " + names.size() + " x, e.g. " + String.join(", ",
                    names.subList(0, Math.min(names.size(), 3))) + (names.size() > 3 ? ", ..." : ""));
            System.out.print(entry.getKey());
            System.out.println();
        }
        int nFailed = 0;
        for (AtomicInteger count : failures.values()) {
            nFailed += count.get();
        }

        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            for (Map.Entry<String, AtomicInteger> entry : new TreeMap<>(failures).entrySet()) {
                System.out.printf("%5d x %s%n", entry.getValue().get(), entry.getKey());
            }
            System.out.println();
        }
        if (httpFaultsPercent > 0) {
            System.out.println("HTTP faults injected: " + HTTP_FAULTS_503.get() + " x 503, "
                    + HTTP_FAULTS_ABORTED.get() + " x aborted, " + HTTP_FAULTS_STALLED.get() + " x stalled");
            System.out.println();
        }
        final String[] partFiles = findPartFiles(cache);
        if (partFiles.length > 0) {
            System.out.println("Partial downloads left in Grape cache: " + String.join(", ", partFiles));
            System.out.println();
        }

        final boolean ok = nOk.get() == nThreads;
        System.out.printf("RESULT %s %d ok, %d failed, %d hung (%.1fs)%n",
                ok ? "OK" : "FAILED", nOk.get(), nFailed, nHung, secs);
        System.out.flush();
        // halt instead of exit, shutdown hooks might block on hung downloads
        Runtime.getRuntime().halt(ok ? 0 : 1);
    }

    // for hangs not covered by the timeout, e.g. in the main thread
    private static void startWatchdog(final int secs, final int nThreads,
            final AtomicInteger nOk, final Map<String, AtomicInteger> failures) {
        final Thread mainThread = Thread.currentThread();
        final Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(secs * 1000L);
            } catch (InterruptedException e) {
                return;
            }
            System.out.println("Watchdog: main thread:");
            for (StackTraceElement element : mainThread.getStackTrace()) {
                System.out.println("    at " + element);
            }
            System.out.println();
            int nFailed = 0;
            for (AtomicInteger count : failures.values()) {
                nFailed += count.get();
            }
            System.out.printf("RESULT FAILED %d ok, %d failed, %d hung (watchdog after %ds)%n",
                    nOk.get(), nFailed, nThreads - nOk.get() - nFailed, secs);
            System.out.flush();
            Runtime.getRuntime().halt(1);
        }, "Watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private static void usage() {
        System.err.println("Usage: java [java-args] GrapeConcurrency -root DIR"
                + " [-repo DIR [-http] [-httpDelay MS] [-httpFaults PERCENT]] [-lock]"
                + " [-ivyTimeout SECS] [-threads N] [-timeout SECS]");
        System.exit(2);
    }

    // e.g. GrapeMaven: removes Maven Central (else Maven Resolver would still
    // contact it, e.g. for its repository prefixes filter) and adds the local repo
    private static void useOnlyLocalRepo(final GrapeEngine engine, final String repoUrl) throws Exception {
        final Object repos = engine.getClass().getMethod("getRepos").invoke(engine);
        ((List<?>) repos).clear();
        final Map<String, Object> resolverArgs = new HashMap<>();
        resolverArgs.put("name", "local");
        resolverArgs.put("root", repoUrl);
        engine.addResolver(resolverArgs);
    }

    // Groovy up to 5: in groovy jar, Groovy 6: in groovy-grape-ivy jar, null if not Ivy
    private static String readDefaultIvyConfig() throws IOException {
        for (String name : new String[] { "groovy/grape/defaultGrapeConfig.xml",
                "groovy/grape/ivy/defaultGrapeConfig.xml" }) {
            final URL url = GrapeConcurrency.class.getClassLoader().getResource(name);
            if (url != null) {
                try (InputStream in = url.openStream()) {
                    final StringBuilder sb = new StringBuilder();
                    final byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) > 0) {
                        sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                    }
                    return sb.toString();
                }
            }
        }
        return null;
    }

    private static String toTestIvyConfig(final String config, final File cache, final String repoUrl,
            final int ivyTimeoutSecs) {
        String testConfig = config
                .replace("${user.home}/.groovy/grapes", cache.getPath())
                .replaceAll("(?m)^.*name=\"localm2\".*\\R", "");
        if (repoUrl != null) {
            testConfig = testConfig
                    .replaceAll("(?m)^.*name=\"jcenter\".*\\R", "")
                    .replace("<ibiblio name=\"ibiblio\" m2compatible=\"true\"/>",
                            "<ibiblio name=\"ibiblio\" root=\"" + repoUrl + "\" m2compatible=\"true\"/>");
            if (!testConfig.contains(repoUrl)) {
                throw new IllegalStateException("Could not set local repo in Ivy config:\n" + config);
            }
        }
        if (ivyTimeoutSecs > 0) {
            final int ms = ivyTimeoutSecs * 1000;
            testConfig = testConfig
                    .replace("<ivysettings>", "<ivysettings>\n  <timeout-constraints>\n"
                            + "    <timeout-constraint name=\"grape\" connectionTimeout=\"" + ms
                            + "\" readTimeout=\"" + ms + "\"/>\n  </timeout-constraints>")
                    .replace("<ibiblio ", "<ibiblio timeoutConstraint=\"grape\" ");
        }
        return testConfig;
    }

    // HTTP server for the local Maven repo on a free port, with a thread per request,
    // optionally delayed before the response and after each chunk, like a slow network,
    // and with faults injected into the given percentage of responses (503, abort or stall)
    private static String serveRepo(final File repo, final int delayMs, final int faultsPercent)
            throws IOException {
        final Path base = repo.toPath().toAbsolutePath().normalize();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                final Path file = base.resolve(exchange.getRequestURI().getPath().substring(1)).normalize();
                final boolean head = "HEAD".equals(exchange.getRequestMethod());
                final int fault = ThreadLocalRandom.current().nextInt(100) < faultsPercent
                        ? ThreadLocalRandom.current().nextInt(3) : -1;
                if (!file.startsWith(base) || !Files.isRegularFile(file)) {
                    exchange.sendResponseHeaders(404, -1);
                } else if (fault == 0) {
                    HTTP_FAULTS_503.incrementAndGet();
                    exchange.sendResponseHeaders(503, -1);
                } else if (head) {
                    exchange.getResponseHeaders().set("Content-Length", String.valueOf(Files.size(file)));
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    sleep(delayMs);
                    final long size = Files.size(file);
                    exchange.sendResponseHeaders(200, size);
                    final long limit = fault >= 1 ? size / 2 : size;
                    if (fault >= 1) {
                        (fault == 1 ? HTTP_FAULTS_ABORTED : HTTP_FAULTS_STALLED).incrementAndGet();
                    }
                    final OutputStream out = exchange.getResponseBody();
                    try (InputStream in = Files.newInputStream(file)) {
                        final byte[] buf = new byte[HTTP_CHUNK_SIZE];
                        long sent = 0;
                        int n;
                        while (sent < limit && (n = in.read(buf, 0, (int) Math.min(buf.length, limit - sent))) > 0) {
                            out.write(buf, 0, n);
                            out.flush();
                            sent += n;
                            sleep(delayMs);
                        }
                    }
                    if (fault == 2) {
                        sleep(Integer.MAX_VALUE);
                    }
                }
            } finally {
                // closes the connection if the body is incomplete
                exchange.close();
            }
        });
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            final Thread thread = new Thread(r, "HttpRepo");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    private static void sleep(final int ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String[] findPartFiles(final File dir) {
        final List<String> partFiles = new ArrayList<>();
        final File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    for (String name : findPartFiles(file)) {
                        partFiles.add(name);
                    }
                } else if (file.getName().endsWith(".part")) {
                    partFiles.add(file.getName());
                }
            }
        }
        return partFiles.toArray(new String[0]);
    }

}
