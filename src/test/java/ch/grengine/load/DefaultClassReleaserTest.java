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

import ch.grengine.Grengine;

import groovy.lang.MetaClass;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultClassReleaserTest {
    
    @Test
    public void testGetInstance() {

        // when
        ClassReleaser releaser1 = DefaultClassReleaser.getInstance();
        ClassReleaser releaser2 = DefaultClassReleaser.getInstance();

        // then

        assertThat(releaser1, sameInstance(releaser2));
        assertThat(releaser1, instanceOf(DefaultClassReleaser.class));
    }

    @Test
    public void testReleaseBasic() {

        // hard to test in full detail because depends also on Groovy version,
        // so only this basic test here, testing elsewhere that allows to GC

        // given

        Class<?> clazz = new Grengine().load("class Class {}");

        // when

        MetaClass metaClass1 = InvokerHelper.metaRegistry.getMetaClass(clazz);

        ClassReleaser releaser = DefaultClassReleaser.getInstance();
        releaser.release(clazz);

        MetaClass metaClass2 = InvokerHelper.metaRegistry.getMetaClass(clazz);

        // then

        assertThat(metaClass1, not(sameInstance(metaClass2)));
    }

}
