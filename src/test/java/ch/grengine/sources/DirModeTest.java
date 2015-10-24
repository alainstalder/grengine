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

package ch.grengine.sources;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class DirModeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testValueOf() {
        assertThat(DirMode.valueOf(DirMode.NO_SUBDIRS.toString()), is(DirMode.NO_SUBDIRS));
        assertThat(DirMode.valueOf(DirMode.WITH_SUBDIRS_RECURSIVE.toString()), is(DirMode.WITH_SUBDIRS_RECURSIVE));
    }
    
    @Test
    public void testValues() {
        assertThat(DirMode.values().length, is(2));
    }

}
