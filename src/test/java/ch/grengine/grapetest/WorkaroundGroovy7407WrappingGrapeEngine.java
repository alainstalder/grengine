/*
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

package ch.grengine.grapetest;

import groovy.grape.Grape;
import groovy.grape.GrapeEngine;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A GrapeEngine that wraps the current GrapeEngine with a wrapper where all calls
 * of the GrapeEngine API are synchronized with a configurable lock, and allows to
 * set this engine in the Grape class.
 *
 * Works at least in basic situations with Groovy 2.4.3 where the wrapped GrapeEngine
 * is always a GrapeIvy instance (not all public interface methods have been tested).
 *
 * But note that while a synchronized GrapeEngine call is in progress (which may take
 * a long time to complete, if e.g. downloading a JAR file from a maven repo),
 * all other threads that want to pull Grape dependencies must wait...
 *
 * Several things are not so nice about this approach:
 * - This is using a "trick" to set the static protected GrapeEngine instance in Grape;
 *   although nominally protected variables are part of the public API (and in this case
 *   is shown in the online JavaDoc of the Grape class).
 * - The "magic" with "calleeDepth" is based on exact knowledge of what GrapeIvy
 *   does (which, by the way, appears even inconsistent internally(?)), so this
 *   workaround is not guaranteed to be robust if GroovyIvy implementation changes.
 * - I refrained from referring to the GrapeIvy class in the source, because it is
 *   not publicly documented in the online JavaDoc of groovy-core.
 */
public class WorkaroundGroovy7407WrappingGrapeEngine implements GrapeEngine {

    private final Object lock;
    private final GrapeEngine innerEngine;

    // GrapeIvy.DEFAULT_DEPTH + 1, because is additionally wrapped by this class...
    private static final int DEFAULT_DEPTH = 4;

    public WorkaroundGroovy7407WrappingGrapeEngine(Object lock, GrapeEngine innerEngine) {
        this.lock = lock;
        this.innerEngine = innerEngine;
    }

    public static void setEngine(GrapeEngine engine) {
        new Grape() {
            public void setInstance(GrapeEngine engine) {
                synchronized (Grape.class) {
                    Grape.instance = engine;
                }
            }
        }.setInstance(engine);
    }

	// call this somewhere during initialization to apply the workaround
    public static void createAndSet() {
        setEngine(new WorkaroundGroovy7407WrappingGrapeEngine(Grape.class, Grape.getInstance()));
    }

    @Override
    public Object grab(String endorsedModule) {
        synchronized(lock) {
            return innerEngine.grab(endorsedModule);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object grab(Map args) {
        synchronized(lock) {
            if (args.get("calleeDepth") == null) {
                args.put("calleeDepth", DEFAULT_DEPTH + 1);
            }
            return innerEngine.grab(args);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object grab(Map args, Map... dependencies) {
        synchronized(lock) {
            if (args.get("calleeDepth") == null) {
                args.put("calleeDepth", DEFAULT_DEPTH);
            }
            return innerEngine.grab(args, dependencies);
        }
    }

    @Override
    public Map<String, Map<String, List<String>>> enumerateGrapes() {
        synchronized(lock) {
            return innerEngine.enumerateGrapes();
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public URI[] resolve(Map args, Map... dependencies) {
        synchronized(lock) {
            if (args.get("calleeDepth") == null) {
                args.put("calleeDepth", DEFAULT_DEPTH);
            }
            return innerEngine.resolve(args, dependencies);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public URI[] resolve(Map args, List depsInfo, Map... dependencies) {
        synchronized(lock) {
            return innerEngine.resolve(args, depsInfo, dependencies);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Map[] listDependencies(ClassLoader classLoader) {
        synchronized(lock) {
            return innerEngine.listDependencies(classLoader);
        }
    }

    @Override
    public void addResolver(Map<String, Object> args) {
        synchronized(lock) {
            innerEngine.addResolver(args);
        }
    }

}
