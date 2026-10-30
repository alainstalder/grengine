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
import org.codehaus.groovy.reflection.ClassInfo;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

class DefaultClassReleaserTest {

    @Test
    void testGetInstance() {

        // when
        final ClassReleaser releaser1 = DefaultClassReleaser.getInstance();
        final ClassReleaser releaser2 = DefaultClassReleaser.getInstance();

        // then

        assertThat(releaser1, sameInstance(releaser2));
        assertThat(releaser1, instanceOf(DefaultClassReleaser.class));
    }

    @Test
    void testReleaseBasic() {

        // cannot test here if actually GCs, that depends
        // also on Groovy version and JDK version+make

        // given

        final Class<?> clazz = new Grengine().load("class Class {}");
        final ClassInfo classInfo1 = ClassInfo.getClassInfo(clazz);
        final MetaClass metaClass1 = InvokerHelper.metaRegistry.getMetaClass(clazz);

        final ClassInfo classInfo2 = ClassInfo.getClassInfo(clazz);
        final MetaClass metaClass2 = InvokerHelper.metaRegistry.getMetaClass(clazz);
        assertThat(classInfo1, sameInstance(classInfo2));
        assertThat(metaClass1, sameInstance(metaClass2));

        // when

        DefaultClassReleaser.getInstance().release(clazz);

        // then

        final ClassInfo classInfo3 = ClassInfo.getClassInfo(clazz);
        final MetaClass metaClass3 = InvokerHelper.metaRegistry.getMetaClass(clazz);
        assertThat(classInfo1, not(sameInstance(classInfo3)));
        assertThat(metaClass1, not(sameInstance(metaClass3)));
    }

}
