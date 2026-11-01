// Minimal reproduction: Grape (Ivy) hangs forever on a stalled download, and
// other threads grabbing the same artifact wait forever for its lock.
//
// Usage: groovy GrapeStall.groovy [timeoutSecs]
//   without argument: Ivy settings like Groovy's defaultGrapeConfig.xml (no timeouts)
//   with argument:    same plus an Ivy <timeout-constraint> on the resolver

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import groovy.grape.Grape
import java.nio.file.Files
import java.util.concurrent.Executors

int timeoutSecs = args ? args[0] as int : 0

def pom = '''<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>repro</groupId><artifactId>stall</artifactId><version>1.0</version>
</project>'''.bytes

// Local Maven repo over HTTP: the POM is served normally, the jar sends half
// of its body and then stalls (never completes, never closes the connection).
def server = HttpServer.create(new InetSocketAddress(InetAddress.loopbackAddress, 0), 0)
server.executor = Executors.newCachedThreadPool { r -> def t = new Thread(r); t.daemon = true; t }
server.createContext('/') { HttpExchange ex ->
    def path = ex.requestURI.path
    boolean head = ex.requestMethod == 'HEAD'
    if (path == '/repro/stall/1.0/stall-1.0.pom') {
        ex.sendResponseHeaders(200, head ? -1 : pom.length)
        if (!head) ex.responseBody.write(pom)
    } else if (path == '/repro/stall/1.0/stall-1.0.jar') {
        ex.responseHeaders.add('Content-Length', '100000')
        ex.sendResponseHeaders(200, head ? -1 : 100000)
        if (!head) {
            println "server: stalling GET $path"
            ex.responseBody.write(new byte[50000])
            ex.responseBody.flush()
            Thread.sleep(Long.MAX_VALUE)
        }
    } else {
        ex.sendResponseHeaders(404, -1)
    }
    ex.close()
}
server.start()
def repoUrl = "http://127.0.0.1:${server.address.port}/"

def dir = Files.createTempDirectory('grape-stall').toFile()
def tc = timeoutSecs > 0
def config = new File(dir, 'grapeConfig.xml')
config.text = """<ivysettings>
  <settings defaultResolver="downloadGrapes"/>
  <caches lockStrategy="artifact-lock-nio"/>
  ${tc ? "<timeout-constraints><timeout-constraint name=\"grape\" connectionTimeout=\"${timeoutSecs * 1000}\" readTimeout=\"${timeoutSecs * 1000}\"/></timeout-constraints>" : ''}
  <resolvers>
    <chain name="downloadGrapes" returnFirst="true">
      <ibiblio name="ibiblio" root="$repoUrl" m2compatible="true" ${tc ? 'timeoutConstraint="grape"' : ''}/>
    </chain>
  </resolvers>
</ivysettings>
"""
System.setProperty('grape.config', config.path)
System.setProperty('grape.root', new File(dir, 'grapes').path)
println "Groovy ${GroovySystem.version}, Java ${System.getProperty('java.version')}, " +
        "timeout: ${tc ? "$timeoutSecs s" : 'none'}"

def threads = (1..2).collect { n ->
    Thread.start("grab-$n") {
        try {
            Grape.grab([classLoader: new GroovyClassLoader()], [group: 'repro', module: 'stall', version: '1.0'])
            println "${Thread.currentThread().name}: grabbed"
        } catch (Throwable t) {
            println "${Thread.currentThread().name}: failed: ${t.message?.readLines()?.first()}"
        }
    }
}

int waitSecs = tc ? timeoutSecs * 4 + 10 : 30
long deadline = System.currentTimeMillis() + waitSecs * 1000L
threads.each { it.join(Math.max(1L, deadline - System.currentTimeMillis())) }
def hung = threads.findAll { it.alive }
hung.each { t ->
    println "\n${t.name} still running after ${waitSecs}s, at:"
    t.stackTrace.take(12).each { println "    at $it" }
}
println "\nRESULT: ${hung ? "${hung.size()} of ${threads.size()} grab threads hung" : 'no hangs'}"
Runtime.runtime.halt(hung ? 1 : 0)
