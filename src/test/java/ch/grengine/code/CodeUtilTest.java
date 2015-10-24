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

import ch.grengine.source.Source;

import java.util.HashMap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


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

        assertThat(CodeUtil.codeArrayToList(sArrayEmpty).size(), is(0));
        assertThat(CodeUtil.codeArrayToList(sArrayAll).size(), is(2));
        assertThat(CodeUtil.codeArrayToList(sArrayAll).get(0), is(c1));
        assertThat(CodeUtil.codeArrayToList(sArrayAll).get(1), is(c2));
        assertThat(CodeUtil.codeArrayToList(c1).size(), is(1));
        assertThat(CodeUtil.codeArrayToList(c1).get(0), is(c1));
        assertThat(CodeUtil.codeArrayToList(c2, c1).size(), is(2));
        assertThat(CodeUtil.codeArrayToList(c2, c1).get(0), is(c2));
        assertThat(CodeUtil.codeArrayToList(c2, c1).get(1), is(c1));
    }

    @Test
    public void testSourcesArrayToListCodesNull() {
        try {
           CodeUtil.codeArrayToList((Code[])null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Code array is null."));
        }
    }

}
