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

package ch.grengine.code;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.groovy.DefaultGroovyCompiler;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;
import ch.grengine.source.SourceUtil;
import ch.grengine.sources.Sources;
import ch.grengine.sources.SourcesUtil;


public class ClassNameConflictAnalyzerTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstruct() throws Exception {
        new ClassNameConflictAnalyzer();
    }
    
    @Test
    public void testGetAllClassOrigins() throws Exception {
        List<Code> codeLayers = getTestCodeLayers();
        Map<String,List<Code>> origins = ClassNameConflictAnalyzer.getAllClassNamesMap(codeLayers);
        
        assertEquals(4, origins.size());
        
        List<Code> twiceList = origins.get("Twice");
        assertEquals(2, twiceList.size());
        assertEquals(twiceList.get(0), codeLayers.get(0));
        assertEquals(twiceList.get(1), codeLayers.get(1));
        
        List<Code> twiceInner1List = origins.get("Twice$Inner1");
        assertEquals(1, twiceInner1List.size());
        assertEquals(twiceInner1List.get(0), codeLayers.get(0));
        
        List<Code> twiceInner2List = origins.get("Twice$Inner2");
        assertEquals(1, twiceInner2List.size());
        assertEquals(twiceInner2List.get(0), codeLayers.get(1));
        
        List<Code> fileList = origins.get("java.io.File");
        assertEquals(1, fileList.size());
        assertEquals(fileList.get(0), codeLayers.get(2));
    }
    
    @Test
    public void testGetAllClassOriginsCodeLayersNull() throws Exception {
        try {
            ClassNameConflictAnalyzer.getAllClassNamesMap(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Code layers are null.", e.getMessage());
        }
    }
    
    
    @Test
    public void testClassOriginsWithDuplicates() throws Exception {
        List<Code> codeLayers = getTestCodeLayers();
        Map<String,List<Code>> origins = ClassNameConflictAnalyzer.getSameClassNamesInMultipleCodeLayersMap(codeLayers);
        
        assertEquals(1, origins.size());
        
        List<Code> twiceList = origins.get("Twice");
        assertEquals(2, twiceList.size());
        assertEquals(twiceList.get(0), codeLayers.get(0));
        assertEquals(twiceList.get(1), codeLayers.get(1));
    }
    
    @Test
    public void testClassOriginsWithDuplicatesCodeLayersNull() throws Exception {
        try {
            ClassNameConflictAnalyzer.getSameClassNamesInMultipleCodeLayersMap(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Code layers are null.", e.getMessage());
        }
    }
    
    
    @Test
    public void testDetermineClassOriginsWithDuplicateInParent() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        List<Code> codeLayers = getTestCodeLayers();
        Map<String,List<Code>> origins = ClassNameConflictAnalyzer.
                getSameClassNamesInParentAndCodeLayersMap(parent, codeLayers);
        
        assertEquals(1, origins.size());

        List<Code> fileList = origins.get("java.io.File");
        assertEquals(1, fileList.size());
        assertEquals(fileList.get(0), codeLayers.get(2));
    }
    
    @Test
    public void testDetermineClassOriginsWithDuplicateInParentParentNull() throws Exception {
        try {
            ClassNameConflictAnalyzer.getSameClassNamesInParentAndCodeLayersMap(null, new LinkedList<Code>());
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Parent class loader is null.", e.getMessage());
        }
    }
    
    @Test
    public void testDetermineClassOriginsWithDuplicateInParentCodeLayersNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        try {
            ClassNameConflictAnalyzer.getSameClassNamesInParentAndCodeLayersMap(parent, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Code layers are null.", e.getMessage());
        }
    }

    
    public static List<Code> getTestCodeLayers() throws Exception {
        SourceFactory f = new DefaultSourceFactory();
        Source s1 = f.fromText("public class Twice { public def get() { return Inner1.get() }\n" +
                "public class Inner1 { static def get() { return 1 } } }");
        Source s2 = f.fromText("public class Twice { public def get() { return Inner2.get() }\n" +
                "public class Inner2 { static def get() { return 2 } } }");
        Source s3 = f.fromText("package java.io\nclass File {}");
        Set<Source> sourceSet1 = SourceUtil.sourceArrayToSourceSet(s1);
        Set<Source> sourceSet2 = SourceUtil.sourceArrayToSourceSet(s2);
        Set<Source> sourceSet3 = SourceUtil.sourceArrayToSourceSet(s3);
        Sources sources1 = SourcesUtil.sourceSetToSources(sourceSet1, "sources1");
        Sources sources2 = SourcesUtil.sourceSetToSources(sourceSet2, "sources2");
        Sources sources3 = SourcesUtil.sourceSetToSources(sourceSet3, "sources3");
        
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        DefaultGroovyCompiler c = new DefaultGroovyCompiler.Builder().setParent(parent).build();
        Code code1 = c.compile(sources1);
        Code code2 = c.compile(sources2);
        Code code3 = c.compile(sources3);
        
        List<Code> codeLayers = CodeUtil.codeArrayToList(code1, code2, code3);
        return codeLayers;
    }

}
