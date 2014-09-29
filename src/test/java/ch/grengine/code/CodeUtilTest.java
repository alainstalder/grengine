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

import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.source.Source;


public class CodeUtilTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructor() {
        new CodeUtil();
    }

    @Test
    public void testCodeArrayToList() {
        Code c1 = new DefaultCode("code1", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        Code c2 = new DefaultCode("code2", new HashMap<Source,CompiledSourceInfo>(), 
                new HashMap<String,Bytecode>());
        Code[] sArrayEmpty = new Code[0];
        Code[] sArrayAll = new Code[] { c1, c2 };
        
        assertEquals(0, CodeUtil.codeArrayToList(sArrayEmpty).size());
        assertEquals(2, CodeUtil.codeArrayToList(sArrayAll).size());
        assertEquals(c1, CodeUtil.codeArrayToList(sArrayAll).get(0));
        assertEquals(c2, CodeUtil.codeArrayToList(sArrayAll).get(1));
        assertEquals(1, CodeUtil.codeArrayToList(c1).size());
        assertEquals(c1, CodeUtil.codeArrayToList(c1).get(0));
        assertEquals(2, CodeUtil.codeArrayToList(c2, c1).size());
        assertEquals(c2, CodeUtil.codeArrayToList(c2, c1).get(0));
        assertEquals(c1, CodeUtil.codeArrayToList(c2, c1).get(1));
    }

    @Test
    public void testSourcesArrayToListCodesNull() {
        try {
           CodeUtil.codeArrayToList((Code[])null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Code array is null.", e.getMessage());
        }
    }

}
