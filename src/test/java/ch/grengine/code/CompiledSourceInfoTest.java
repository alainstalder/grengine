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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.source.MockSource;


public class CompiledSourceInfoTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructPlusGetters() {
        MockSource m1 = new MockSource("id1");
        String name = "MainClassName";
        Set<String> names = new HashSet<String>();
        names.add("Side");
        names.add("MainClassName");
        CompiledSourceInfo info = new CompiledSourceInfo(m1, name, names, 55);
        assertEquals(m1, info.getSource());
        assertEquals(name, info.getMainClassName());
        assertEquals(names, info.getClassNames());
        assertEquals(55, info.getLastModifiedAtCompileTime());
        assertTrue(info.toString().startsWith("CompiledSourceInfo[source=MockSource[ID='id1', lastModified=0], " +
                "mainClassName=MainClassName, classNames=" + names + ", lastModifiedAtCompileTime="));
    }
    
    @Test
    public void testConstructSourceNull() {
        try {
            new CompiledSourceInfo(null, "", new HashSet<String>(), 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Source is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructMainClassNameNull() {
        try {
            new CompiledSourceInfo(new MockSource("id1"), null, new HashSet<String>(), 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Main class name is null.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructClassNamesNull() {
        try {
            new CompiledSourceInfo(new MockSource("id1"), "", null, 0);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Class names are null.", e.getMessage());
        }
    }

}
