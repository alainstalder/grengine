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

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.sources.DirMode;


public class DirModeTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testValueOf() {
        assertEquals(DirMode.NO_SUBDIRS, DirMode.valueOf(DirMode.NO_SUBDIRS.toString()));
        assertEquals(DirMode.WITH_SUBDIRS_RECURSIVE, DirMode.valueOf(DirMode.WITH_SUBDIRS_RECURSIVE.toString()));
    }
    
    @Test
    public void testValues() {
        assertEquals(2, DirMode.values().length);
    }

}
