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

package ch.grengine.load;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LoadModeTest {

    @Test
    public void testValueOf() {

        // when/then

        assertThat(LoadMode.valueOf(LoadMode.CURRENT_FIRST.toString()), is(LoadMode.CURRENT_FIRST));
        assertThat(LoadMode.valueOf(LoadMode.PARENT_FIRST.toString()), is(LoadMode.PARENT_FIRST));
    }
    
    @Test
    public void testValues() {

        // when/then

        assertThat(LoadMode.values().length, is(2));
    }

}
