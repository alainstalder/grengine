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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultTopCodeCacheTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructAndGettersAndMore() throws Exception {
        
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultTopCodeCache.Builder builder =
                new DefaultTopCodeCache.Builder(parent).setCompilerFactory(compilerFactory);
        DefaultTopCodeCache c = builder.build();

        assertThat(c.getBuilder(), is(builder));
        assertThat(c.getCompilerFactory(), is(compilerFactory));
        assertThat(c.getParent(), is(parent));
        
        ClassLoader parentNew = Thread.currentThread().getContextClassLoader();
        c.setParent(parentNew);
        assertThat(c.getParent(), is(parentNew));
        
        DefaultTopCodeCache c2 = c.clone();
        assertThat(c2.getCompilerFactory(), is(compilerFactory));
        assertThat(c2.getParent(), is(parentNew));

        c = new DefaultTopCodeCache.Builder(null).build();
        assertThat(c.getParent(), is(nullValue()));
        assertThat(c.getCompilerFactory(), is(notNullValue()));
        assertThat(c.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));
        c.setParent(parentNew);
        assertThat(c.getParent(), is(parentNew));
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultTopCodeCache.Builder builder = new DefaultTopCodeCache.Builder(null);
        builder.build();
        try {
            builder.setCompilerFactory(new DefaultGroovyCompilerFactory());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }

    @Test
    public void testSetParentNull() throws Exception {
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        TopCodeCache c = new DefaultTopCodeCache.Builder(parent).build();
        
        try {
            c.setParent(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Parent class loader is null."));
        }
    }
    
    // most functionality is tested in LayeredClassLoaderTest

}
