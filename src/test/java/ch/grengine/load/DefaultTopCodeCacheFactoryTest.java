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
import static org.hamcrest.MatcherAssert.assertThat;


public class DefaultTopCodeCacheFactoryTest {

    @Test
    public void testConstructFromBuilderAndGetters() {

        // given
        
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultTopCodeCacheFactory.Builder builder = 
                new DefaultTopCodeCacheFactory.Builder().setCompilerFactory(compilerFactory);

        // when

        DefaultTopCodeCacheFactory cf = builder.build();

        // then

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerFactory(), is(compilerFactory));

        // when
        
        DefaultTopCodeCache c = (DefaultTopCodeCache)cf.newTopCodeCache(parent);

        // then

        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerFactory(), is(compilerFactory));
    }

    @Test
    public void testConstructDefault() {

        // when

        DefaultTopCodeCacheFactory cf = new DefaultTopCodeCacheFactory();

        // then

        assertThat(cf.getCompilerFactory(), is(notNullValue()));
        assertThat(cf.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
    }
    
    @Test
    public void testConstructFromCompilerFactory() {

        // given

        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();

        // when

        DefaultTopCodeCacheFactory cf = new DefaultTopCodeCacheFactory(compilerFactory);

        // then

        assertThat(cf.getCompilerFactory(), is(compilerFactory));
    }
    
    @Test
    public void testConstructFromCompilerFactoryNull() {

        // when/then

        assertThrows(() -> new DefaultTopCodeCacheFactory((CompilerFactory)null),
                NullPointerException.class,
                "Compiler factory is null.");
    }
    
    @Test
    public void testModifyBuilderAfterUse() {

        // given

        DefaultTopCodeCacheFactory.Builder builder = new DefaultTopCodeCacheFactory.Builder();
        builder.build();

        // when/then

        assertThrows(() -> builder.setCompilerFactory(new DefaultGroovyCompilerFactory()),
                IllegalStateException.class,
                "Builder already used.");
    }

}
