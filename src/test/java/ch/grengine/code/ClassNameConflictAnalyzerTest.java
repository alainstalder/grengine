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

package ch.grengine.code;

import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class ClassNameConflictAnalyzerTest {

    @Test
    public void testConstruct() {
        new ClassNameConflictAnalyzer();
    }
    
    @Test
    public void testGetAllClassOrigins() {

        // given

        final List<Code> codeLayers = getTestCodeLayers();

        // when

        final Map<String,List<Code>> origins = ClassNameConflictAnalyzer.getAllClassNamesMap(codeLayers);

        // then

        assertThat(origins.size(), is(4));

        // when

        final List<Code> twiceList = origins.get("Twice");

        // then

        assertThat(twiceList.size(), is(2));
        assertThat(codeLayers.get(0), is(twiceList.get(0)));
        assertThat(codeLayers.get(1), is(twiceList.get(1)));

        // when

        final List<Code> twiceInner1List = origins.get("Twice$Inner1");

        // then

        assertThat(twiceInner1List.size(), is(1));
        assertThat(codeLayers.get(0), is(twiceInner1List.get(0)));

        // when

        final List<Code> twiceInner2List = origins.get("Twice$Inner2");

        // then

        assertThat(twiceInner2List.size(), is(1));
        assertThat(codeLayers.get(1), is(twiceInner2List.get(0)));

        // when

        final List<Code> fileList = origins.get("java.io.File");

        // then

        assertThat(fileList.size(), is(1));
        assertThat(codeLayers.get(2), is(fileList.get(0)));
    }
    
    @Test
    public void testGetAllClassOriginsCodeLayersNull() {

        // when/then

        assertThrows(() -> ClassNameConflictAnalyzer.getAllClassNamesMap(null),
                NullPointerException.class,
                "Code layers are null.");
    }
    
    
    @Test
    public void testClassOriginsWithDuplicates() {

        // given

        final List<Code> codeLayers = getTestCodeLayers();

        // when

        final Map<String,List<Code>> origins = ClassNameConflictAnalyzer.getSameClassNamesInMultipleCodeLayersMap(codeLayers);

        // then

        assertThat(origins.size(), is(1));

        // when

        final List<Code> twiceList = origins.get("Twice");

        // then

        assertThat(twiceList.size(), is(2));
        assertThat(codeLayers.get(0), is(twiceList.get(0)));
        assertThat(codeLayers.get(1), is(twiceList.get(1)));
    }
    
    @Test
    public void testClassOriginsWithDuplicatesCodeLayersNull() {

        // when/then

        assertThrows(() -> ClassNameConflictAnalyzer.getSameClassNamesInMultipleCodeLayersMap(null),
                NullPointerException.class,
                "Code layers are null.");
    }
    
    
    @Test
    public void testDetermineClassOriginsWithDuplicateInParent() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final List<Code> codeLayers = getTestCodeLayers();

        // when

        final Map<String,List<Code>> origins = ClassNameConflictAnalyzer.
                getSameClassNamesInParentAndCodeLayersMap(parent, codeLayers);

        // then

        assertThat(origins.size(), is(1));

        // when

        final List<Code> fileList = origins.get("java.io.File");

        // then

        assertThat(fileList.size(), is(1));
        assertThat(codeLayers.get(2), is(fileList.get(0)));
    }
    
    @Test
    public void testDetermineClassOriginsWithDuplicateInParentParentNull() {

        // when/then

        assertThrows(() -> ClassNameConflictAnalyzer.getSameClassNamesInParentAndCodeLayersMap(
                null, new LinkedList<>()),
                NullPointerException.class,
                "Parent class loader is null.");
    }
    
    @Test
    public void testDetermineClassOriginsWithDuplicateInParentCodeLayersNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();


        // when/then

        assertThrows(() -> ClassNameConflictAnalyzer.getSameClassNamesInParentAndCodeLayersMap(
                parent, null),
                NullPointerException.class,
                "Code layers are null.");
    }

    
    public static List<Code> getTestCodeLayers() {
        final SourceFactory f = new DefaultSourceFactory();
        final Source s1 = f.fromText("public class Twice { public def get() { return Inner1.get() }\n" +
                "public class Inner1 { static def get() { return 1 } } }");
        final Source s2 = f.fromText("public class Twice { public def get() { return Inner2.get() }\n" +
                "public class Inner2 { static def get() { return 2 } } }");
        final Source s3 = f.fromText("package java.io\nclass File {}");
        final Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s1);
        final Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s2);
        final Set<Source> sourceSet3 = SourceUtil.sourceArrayToSourceSet(s3);
        final Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "sources1");
        final Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "sources2");
        final Sources sources3 = SourcesUtil.sourceSetToSources(sourceSet3, "sources3");

        final ClassLoader parent = Thread.currentThread().getContextClassLoader();
        final DefaultGroovyCompiler c = new DefaultGroovyCompiler.Builder().setParent(parent).build();
        final Code code1 = c.compile(sources1);
        final Code code2 = c.compile(sources2);
        final Code code3 = c.compile(sources3);

        return Arrays.asList(code1, code2, code3);
    }

}
