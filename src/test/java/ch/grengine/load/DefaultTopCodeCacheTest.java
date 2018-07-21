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

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;

import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class DefaultTopCodeCacheTest {

    @Test
    public void testConstructAndGettersAndMore() {

        // given
        
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultTopCodeCache.Builder builder =
                new DefaultTopCodeCache.Builder(parent).setCompilerFactory(compilerFactory);

        // when

        DefaultTopCodeCache c = builder.build();

        // then

        assertThat(c.getBuilder(), is(builder));
        assertThat(c.getCompilerFactory(), is(compilerFactory));
        assertThat(c.getParent(), is(parent));

        // when
        
        ClassLoader parentNew = Thread.currentThread().getContextClassLoader();
        c.setParent(parentNew);

        // then

        assertThat(c.getParent(), is(parentNew));

        // when
        
        DefaultTopCodeCache c2 = c.clone();

        // then

        assertThat(c2.getCompilerFactory(), is(compilerFactory));
        assertThat(c2.getParent(), is(parentNew));

        // when

        c = new DefaultTopCodeCache.Builder(null).build();

        // then

        assertThat(c.getParent(), is(nullValue()));
        assertThat(c.getCompilerFactory(), is(notNullValue()));
        assertThat(c.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));

        // when

        c.setParent(parentNew);

        // then

        assertThat(c.getParent(), is(parentNew));
    }
    
    @Test
    public void testModifyBuilderAfterUse() {

        // given

        DefaultTopCodeCache.Builder builder = new DefaultTopCodeCache.Builder(null);
        builder.build();

        // when/then

        assertThrows(() -> builder.setCompilerFactory(new DefaultGroovyCompilerFactory()),
                IllegalStateException.class,
                "Builder already used.");
    }

    @Test
    public void testSetParentNull() {

        // given

        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        TopCodeCache c = new DefaultTopCodeCache.Builder(parent).build();

        // when/then

        assertThrows(() -> c.setParent(null),
                NullPointerException.class,
                "Parent class loader is null.");
    }
    
    // most functionality is tested in LayeredClassLoaderTest

}
