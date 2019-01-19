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

package ch.artecat.grengine.engine;

import ch.artecat.grengine.code.Code;
import ch.artecat.grengine.code.groovy.DefaultGroovyCompiler;
import ch.artecat.grengine.engine.EngineConcurrencyTestFrame.ConcurrencyTestContext;
import ch.artecat.grengine.load.LoadMode;
import ch.artecat.grengine.source.MockTextSource;
import ch.artecat.grengine.source.SourceUtil;
import ch.artecat.grengine.sources.Sources;
import ch.artecat.grengine.sources.SourcesUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import groovy.lang.Script;
import org.junit.jupiter.api.Test;


class LayeredEngineConcurrencyTest {

    private final EngineConcurrencyTestFrame frame = new EngineConcurrencyTestFrame();

    private static class LayeredEngineConcurrencyTestContext implements ConcurrencyTestContext {

        private final LayeredEngine engine;
        private final Map<Integer,Loader> loaderMap = new ConcurrentHashMap<>();
        private MockTextSource source;
        private int lastModified;
        private Sources sources;

        LayeredEngineConcurrencyTestContext(final LayeredEngine engine) {
            this.engine = engine;
        }

        private void setCodeLayers() {
            final Code code = new DefaultGroovyCompiler().compile(sources);
            final List<Code> codeLayers = Collections.singletonList(code);
            engine.setCodeLayers(codeLayers);
        }

        @Override
        public void initSource(String scriptText) {
            source = new MockTextSource(scriptText);
            lastModified = 0;
            sources = SourcesUtil.sourceSetToSources(
                    SourceUtil.sourceArrayToSourceSet(source), "concurrent");
            setCodeLayers();
        }

        @Override
        public void updateSource(String scriptText) {
            lastModified++;
            source.setText(scriptText);
            source.setLastModified(lastModified);
            setCodeLayers();
        }

        @Override
        public Object runScript(int iThread) throws Exception {
            Loader loader = loaderMap.computeIfAbsent(iThread, i -> engine.newAttachedLoader());
            return ((Script) engine.loadMainClass(loader, source)
                    .getConstructor().newInstance()).run();
        }
    }

    @Test
    void testConcurrentNoTopCodeCache() {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(false)
                .build();
        frame.testConcurrent("TEST LayeredEngine concurrent - no top code cache",
                new LayeredEngineConcurrencyTestContext(engine));
    }

    @Test
    void testConcurrentTopCodeCacheParentFirst() {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.PARENT_FIRST)
                .build();
        frame.testConcurrent("TEST LayeredEngine concurrent - top code cache - parent first",
                new LayeredEngineConcurrencyTestContext(engine));
    }

    @Test
    void testConcurrentTopCodeCacheCurrentFirst() {
        final LayeredEngine engine = new LayeredEngine.Builder()
                .setWithTopCodeCache(true)
                .setTopLoadMode(LoadMode.CURRENT_FIRST)
                .build();
        frame.testConcurrent("TEST LayeredEngine concurrent - top code cache - current first",
                new LayeredEngineConcurrencyTestContext(engine));
    }

}
