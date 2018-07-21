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

package ch.grengine.code.groovy;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Test;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class DefaultGroovyCompilerFactoryTest {

    @Test
    public void testConstructDefaults() {

        // given

        final DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();

        // when

        final DefaultGroovyCompilerFactory cf = builder.build();

        // then

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerConfiguration(), is(notNullValue()));
        assertThat(cf.getCompilerConfiguration(), is(cf.getBuilder().getCompilerConfiguration()));
    }
    
    @Test
    public void testConstructAllDefined() {

        // given

        final DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        final CompilerConfiguration config = new CompilerConfiguration();

        // when

        final DefaultGroovyCompilerFactory cf = builder
                .setCompilerConfiguration(config)
                .build();

        // then

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerConfiguration(), is(config));
        assertThat(cf.getCompilerConfiguration(), is(cf.getBuilder().getCompilerConfiguration()));
    }
    
    @Test
    public void testModifyBuilderAfterUse() {

        // given

        final DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        builder.build();

        // when/then

        assertThrows(() -> builder.setCompilerConfiguration(new CompilerConfiguration()),
                IllegalStateException.class,
                "Builder already used.");
    }
    
    @Test
    public void testConstructorDefaultCompilerConfiguration() {

        // given

        final DefaultGroovyCompilerFactory cf = new DefaultGroovyCompilerFactory();
        final ClassLoader parent = Thread.currentThread().getContextClassLoader();

        // when

        final DefaultGroovyCompiler dc = (DefaultGroovyCompiler)cf.newCompiler(parent);

        // then

        assertThat(dc.getCompilerConfiguration(), is(cf.getCompilerConfiguration()));
        assertThat(dc.getParent(), is(parent));
    }
    
    @Test
    public void testConstructorSpecificCompilerConfiguration() {

        // given

        final CompilerConfiguration config = new CompilerConfiguration();
        final DefaultGroovyCompilerFactory cf = new DefaultGroovyCompilerFactory(config);
        final ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();

        // when

        final DefaultGroovyCompiler dc = (DefaultGroovyCompiler)cf.newCompiler(parent);

        // then

        assertThat(cf.getCompilerConfiguration(), is(config));
        assertThat(dc.getCompilerConfiguration(), is(cf.getCompilerConfiguration()));
        assertThat(dc.getParent(), is(parent));
    }

    @Test
    public void testConstructFromConfigNull() {

        // when/then

        assertThrows(() -> new DefaultGroovyCompilerFactory((CompilerConfiguration)null),
                NullPointerException.class,
                "Compiler configuration is null.");
    }

}
