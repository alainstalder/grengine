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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class BytecodeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructPlusGetters() {
        String className = "MyScript";
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
        Bytecode bytecode = new Bytecode(className, bytes);
        assertThat(bytecode.getClassName(), is(className));
        assertThat(bytecode.getBytes(), is(bytes));
    }

    @Test
    public void testToString() {
        String className = "MyScript";
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
        Bytecode bytecode = new Bytecode(className, bytes);
        //System.out.println(bytecode);
        assertThat(bytecode.toString().startsWith("Bytecode[className=MyScript, bytes=["), is(true));
        assertThat(bytecode.toString().endsWith("]"), is(true));
    }
    
    @Test
    public void testConstructWithNameNull() {
        byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };
        try {
            new Bytecode(null, bytes);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Class name is null."));
        }
    }
    
    @Test
    public void testConstructWithBytesNull() {
        String className = "MyScript";
        try {
            new Bytecode(className, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Bytes are null."));
        }
    }

}
