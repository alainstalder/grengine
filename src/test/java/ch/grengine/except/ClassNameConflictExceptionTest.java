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

package ch.grengine.except;

import ch.grengine.code.ClassNameConflictAnalyzer;
import ch.grengine.code.ClassNameConflictAnalyzerTest;
import ch.grengine.code.Code;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class ClassNameConflictExceptionTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstruct() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        List<Code> codeLayers = ClassNameConflictAnalyzerTest.getTestCodeLayers();
        Map<String,List<Code>> map1 =
                ClassNameConflictAnalyzer.getSameClassNamesInMultipleCodeLayersMap(codeLayers);
        Map<String,List<Code>> map2 =
                ClassNameConflictAnalyzer.getSameClassNamesInParentAndCodeLayersMap(parent, codeLayers);
        String msg = "Got " + (map1.size() + map2.size()) + " conflict(s).";
        ClassNameConflictException e = new ClassNameConflictException(msg, map1, map2);

        assertThat(e, instanceOf(GrengineException.class));
        assertThat(e.getMessage(), is("Got 2 conflict(s). " +
                "Duplicate classes in code layers: [Twice], classes in code layers and parent: [java.io.File]"));
        assertThat(e.getSameClassNamesInMultipleCodeLayersMap(), is(map1));
        assertThat(e.getSameClassNamesInParentAndCodeLayersMap(), is(map2));
        assertThat(e.getCause(), is(nullValue()));
        assertThat(e.getDateThrown().getTime() <= System.currentTimeMillis(), is(true));
        assertThat(e.getDateThrown().getTime() + 60000 > System.currentTimeMillis(), is(true));
    }

}
