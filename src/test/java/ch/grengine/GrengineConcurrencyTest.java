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

import ch.grengine.source.SourceUtil;
import ch.grengine.engine.EngineConcurrencyTestFrame;
import ch.grengine.engine.EngineConcurrencyTestFrame.ConcurrencyTestContext;
import ch.grengine.engine.LayeredEngine;
import ch.grengine.engine.Loader;
import ch.grengine.load.LoadMode;
import ch.grengine.source.MockTextSource;
import ch.grengine.source.Source;
import ch.grengine.sources.FixedSetSources;
import ch.grengine.sources.Sources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;


class GrengineConcurrencyTest {

    private final EngineConcurrencyTestFrame frame = new EngineConcurrencyTestFrame();

    private static class GrengineConcurrencyTestContext implements ConcurrencyTestContext {

        private final LayeredEngine engine;
        private final Map<Integer, Loader> loaderMap = new ConcurrentHashMap<>();
        private MockTextSource source;
        private int lastModified;
        private Grengine gren;

        GrengineConcurrencyTestContext(final LayeredEngine engine) {
            this.engine = engine;
        }

        @Override
        public void initSource(String scriptText) {
            source = new MockTextSource(scriptText);
            lastModified = 0;
            final Set<Source> sourceSet = SourceUtil.sourceArrayToSourceSet(source);
            final Sources sources = new FixedSetSources.Builder(sourceSet)
                    .setLatencyMs(10)
                    .setName("concurrent")
                    .build();
            final List<Sources> sourcesLayers = Collections.singletonList(sources);
            gren = new Grengine.Builder()
                    .setEngine(engine)
                    .setSourcesLayers(sourcesLayers)
                    .setLatencyMs(30)
                    .build();
        }

        @Override
        public void updateSource(String scriptText) {
            lastModified++;
            source.setText(scriptText);
            source.setLastModified(lastModified);
        }

        @Override
        public Object runScript(int iThread) {
            Loader loader = loaderMap.computeIfAbsent(iThread, i -> gren.newAttachedLoader());
            return gren.run(loader, source);
        }
    }

    @Test
    void testConcurrentNoTopCodeCache() {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .build();
        frame.testConcurrent("TEST Grengine concurrent - no top code cache",
                new GrengineConcurrencyTestContext(engine));
    }

    @Test
    void testConcurrentTopCodeCacheParentFirst() {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.PARENT_FIRST)
                .build();
        frame.testConcurrent("TEST Grengine concurrent - top code cache - parent first",
                new GrengineConcurrencyTestContext(engine));
    }

    @Test
    void testConcurrentTopCodeCacheCurrentFirst() {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .build();
        frame.testConcurrent("TEST Grengine concurrent - top code cache - current first",
                new GrengineConcurrencyTestContext(engine));
    }

}
