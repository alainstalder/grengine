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

package ch.grengine.source;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SourceSetStateTest {

    @Test
    public void testConstructAndUpdate() throws Exception {

        // given
        
        MockSource m1 = new MockSource("id1");
        MockSource m2 = new MockSource("id2");
        Set<Source> set = SourceUtil.sourceArrayToSourceSet(m1, m2);

        // when

        SourceSetState s1 = new SourceSetState(set);

        // then

        Thread.sleep(30);
        assertThat(s1.getSourceSet(), is(set));
        assertThat(s1.getLastChecked(), is(s1.getLastModified()));
        assertThat(s1.getLastModified() < System.currentTimeMillis(), is(true));

        // when (same set with last modified unchanged)

        SourceSetState s2 = s1.update(set);

        // then

        Thread.sleep(30);
        assertThat(s2.getSourceSet(), is(set));
        assertThat(s2.getLastModified(), is(s1.getLastModified()));
        assertThat(s1.getLastChecked() < s2.getLastChecked(), is(true));
        assertThat(s2.getLastModified() < System.currentTimeMillis(), is(true));
        
        // when (same set with second source different last modified)

        m2.setLastModified(1);
        SourceSetState s3 = s2.update(set);

        // then

        Thread.sleep(30);
        assertThat(s3.getSourceSet(), is(set));
        assertThat(s2.getLastModified() < s3.getLastModified(), is(true));
        assertThat(s3.getLastModified(), is(s3.getLastChecked()));
        assertThat(s3.getLastModified() < System.currentTimeMillis(), is(true));
        
        // when (additional source)

        MockSource m3 = new MockSource("id3");
        Set<Source> setNew = SourceUtil.sourceArrayToSourceSet(m1, m2, m3);
        SourceSetState s4 = s3.update(setNew);

        // then

        Thread.sleep(30);
        assertThat(s4.getSourceSet(), is(setNew));
        assertThat(s3.getLastModified() < s4.getLastModified(), is(true));
        assertThat(s4.getLastModified(), is(s4.getLastChecked()));
        assertThat(s4.getLastModified() < System.currentTimeMillis(), is(true));

        // when (empty source set)

        Set<Source> setEmpty = new HashSet<>();
        SourceSetState s5 = s4.update(setEmpty);

        // then

        Thread.sleep(30);
        assertThat(s5.getSourceSet(), is(setEmpty));
        assertThat(s4.getLastModified() < s5.getLastModified(), is(true));
        assertThat(s5.getLastModified(), is(s5.getLastChecked()));
        assertThat(s5.getLastModified() < System.currentTimeMillis(), is(true));

        // when (different source set of same size)

        Set<Source> setDifferent = SourceUtil.sourceArrayToSourceSet(m1, m3);
        SourceSetState s6 = s3.update(setDifferent);

        // then

        Thread.sleep(30);
        assertThat(s6.getSourceSet(), is(setDifferent));
        assertThat(s3.getLastModified() < s6.getLastModified(), is(true));
        assertThat(s6.getLastModified(), is(s6.getLastChecked()));
        assertThat(s6.getLastModified() < System.currentTimeMillis(), is(true));
    }
    
    @Test
    public void testConstructWithSourceSetNull() {

        // when/then

        assertThrows(() -> new SourceSetState(null),
                NullPointerException.class,
                "Source set is null.");
    }
    
    @Test
    public void testUpdateWithSourceSetNull() {

        // when/then

        assertThrows(() -> new SourceSetState(new HashSet<>()).update(null),
                NullPointerException.class,
                "New source set is null.");
    }


}
