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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BytecodeTest {

    @Test
    void testConstructPlusGetters() {

        // given

        final String className = "MyScript";
        final byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };

        // when

        final Bytecode bytecode = new Bytecode(className, bytes);

        // then

        assertThat(bytecode.getClassName(), is(className));
        assertThat(bytecode.getBytes(), is(bytes));
    }

    @Test
    void testToString() {

        // given

        final String className = "MyScript";
        final byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };

        // when

        final Bytecode bytecode = new Bytecode(className, bytes);

        // then

        //System.out.println(bytecode);
        assertThat(bytecode.toString().startsWith("Bytecode[className=MyScript, bytes=["), is(true));
        assertThat(bytecode.toString().endsWith("]"), is(true));
    }
    
    @Test
    void testConstructWithNameNull() {

        // given

        final byte[] bytes = new byte[] { 1, 2, 3, 4, 5 };

        // when/then

        assertThrows(NullPointerException.class,
                () -> new Bytecode(null, bytes),
                "Class name is null.");
    }
    
    @Test
    void testConstructWithBytesNull() {

        // given

        final String className = "MyScript";

        // when/then

        assertThrows(NullPointerException.class,
                () -> new Bytecode(className, null),
                "Bytes are null.");
    }

}
