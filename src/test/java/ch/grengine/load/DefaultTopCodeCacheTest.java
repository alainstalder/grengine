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

import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.code.CompilerFactory;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static ch.grengine.TestUtil.assertThrowsMessageIs;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


class DefaultTopCodeCacheTest {

    @Test
    void testConstructAndGettersAndMore() {

        // given
        
        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final DefaultTopCodeCache.Builder builder = new DefaultTopCodeCache.Builder(parent);

        // when

        final DefaultTopCodeCache c1 = builder
                .setCompilerFactory(compilerFactory)
                .build();

        // then

        assertThat(c1.getBuilder(), CoreMatchers.is(builder));
        assertThat(c1.getCompilerFactory(), is(compilerFactory));
        assertThat(c1.getParent(), is(parent));

        // when
        
        final ClassLoader parentNew = Thread.currentThread().getContextClassLoader();
        c1.setParent(parentNew);

        // then

        assertThat(c1.getParent(), is(parentNew));

        // when

        final DefaultTopCodeCache c2 = c1.clone();

        // then

        assertThat(c2.getCompilerFactory(), is(compilerFactory));
        assertThat(c2.getParent(), is(parentNew));

        // when

        final DefaultTopCodeCache c3 = new DefaultTopCodeCache.Builder(null).build();

        // then

        assertThat(c3.getParent(), is(nullValue()));
        assertThat(c3.getCompilerFactory(), is(notNullValue()));
        assertThat(c3.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));

        // when

        c3.setParent(parentNew);

        // then

        assertThat(c3.getParent(), is(parentNew));
    }
    
    @Test
    void testModifyBuilderAfterUse() {

        // given

        final DefaultTopCodeCache.Builder builder = new DefaultTopCodeCache.Builder(null);
        builder.build();

        // when/then

        assertThrowsMessageIs(IllegalStateException.class,
                () -> builder.setCompilerFactory(new DefaultGroovyCompilerFactory()),
                "Builder already used.");
    }

    @Test
    void testSetParentNull() {

        // given

        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final TopCodeCache c = new DefaultTopCodeCache.Builder(parent).build();

        // when/then

        assertThrowsMessageIs(NullPointerException.class,
                () -> c.setParent(null),
                "Parent class loader is null.");
    }
    
    // most functionality is tested in LayeredClassLoaderTest

}
