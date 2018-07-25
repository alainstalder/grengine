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

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


class DefaultTopCodeCacheFactoryTest {

    @Test
    void testConstructFromBuilderAndGetters() {

        // given

        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        final DefaultTopCodeCacheFactory.Builder builder = new DefaultTopCodeCacheFactory.Builder();

        // when

        final DefaultTopCodeCacheFactory cf = builder
                .setCompilerFactory(compilerFactory)
                .build();

        // then

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerFactory(), is(compilerFactory));

        // when
        
        final DefaultTopCodeCache c = (DefaultTopCodeCache)cf.newTopCodeCache(parent);

        // then

        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerFactory(), is(compilerFactory));
    }

    @Test
    void testConstructDefault() {

        // when

        final DefaultTopCodeCacheFactory cf = new DefaultTopCodeCacheFactory();

        // then

        assertThat(cf.getCompilerFactory(), is(notNullValue()));
        assertThat(cf.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
    }
    
    @Test
    void testConstructFromCompilerFactory() {

        // given

        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();

        // when

        final DefaultTopCodeCacheFactory cf = new DefaultTopCodeCacheFactory(compilerFactory);

        // then

        assertThat(cf.getCompilerFactory(), is(compilerFactory));
    }
    
    @Test
    void testConstructFromCompilerFactoryNull() {

        // when/then

        assertThrows(NullPointerException.class,
                () -> new DefaultTopCodeCacheFactory((CompilerFactory)null),
                "Compiler factory is null.");
    }
    
    @Test
    void testModifyBuilderAfterUse() {

        // given

        final DefaultTopCodeCacheFactory.Builder builder = new DefaultTopCodeCacheFactory.Builder();
        builder.build();

        // when/then

        assertThrows(IllegalStateException.class,
                () -> builder.setCompilerFactory(new DefaultGroovyCompilerFactory()),
                "Builder already used.");
    }

}
