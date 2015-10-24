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

package ch.grengine.load;

import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultTopCodeCacheFactoryTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructFromBuilderAndGetters() throws Exception {
        
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultTopCodeCacheFactory.Builder builder = 
                new DefaultTopCodeCacheFactory.Builder().setCompilerFactory(compilerFactory);
        DefaultTopCodeCacheFactory cf = builder.build();

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerFactory(), is(compilerFactory));
        
        DefaultTopCodeCache c = (DefaultTopCodeCache)cf.newTopCodeCache(parent);

        assertThat(c.getParent(), is(parent));
        assertThat(c.getCompilerFactory(), is(compilerFactory));
    }

    @Test
    public void testConstructDefault() throws Exception {
        DefaultTopCodeCacheFactory cf = new DefaultTopCodeCacheFactory();
        assertThat(cf.getCompilerFactory(), is(notNullValue()));
        assertThat(cf.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
    }
    
    @Test
    public void testConstructFromCompilerFactory() throws Exception {
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        DefaultTopCodeCacheFactory cf = new DefaultTopCodeCacheFactory(compilerFactory);
        assertThat(cf.getCompilerFactory(), is(compilerFactory));
    }
    
    @Test
    public void testConstructFromCompilerFactoryNull() throws Exception {
        try {
            new DefaultTopCodeCacheFactory((CompilerFactory)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler factory is null."));
        }
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultTopCodeCacheFactory.Builder builder = new DefaultTopCodeCacheFactory.Builder();
        builder.build();
        try {
            builder.setCompilerFactory(new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }

}
