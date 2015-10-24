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

package ch.grengine.code.groovy;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


public class DefaultGroovyCompilerFactoryTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructDefaults() throws Exception {
        DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        DefaultGroovyCompilerFactory cf = builder.build();

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerConfiguration(), is(notNullValue()));
        assertThat(cf.getCompilerConfiguration(), is(cf.getBuilder().getCompilerConfiguration()));
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        CompilerConfiguration config = new CompilerConfiguration();
        builder.setCompilerConfiguration(config);
        DefaultGroovyCompilerFactory cf = builder.build();

        assertThat(cf.getBuilder(), is(builder));
        assertThat(cf.getCompilerConfiguration(), is(config));
        assertThat(cf.getCompilerConfiguration(), is(cf.getBuilder().getCompilerConfiguration()));
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        builder.build();
        try {
            builder.setCompilerConfiguration(new CompilerConfiguration());
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("Builder already used."));
        }
    }
    
    @Test
    public void testConstructorDefaultCompilerConfiguration() {
        DefaultGroovyCompilerFactory cf = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        DefaultGroovyCompiler dc = (DefaultGroovyCompiler)cf.newCompiler(parent);
        assertThat(dc.getCompilerConfiguration(), is(cf.getCompilerConfiguration()));
        assertThat(dc.getParent(), is(parent));
    }
    
    @Test
    public void testConstructorSpecificCompilerConfiguration() {
        CompilerConfiguration config = new CompilerConfiguration();
        DefaultGroovyCompilerFactory cf = new DefaultGroovyCompilerFactory(config);
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        DefaultGroovyCompiler dc = (DefaultGroovyCompiler)cf.newCompiler(parent);
        assertThat(cf.getCompilerConfiguration(), is(config));
        assertThat(dc.getCompilerConfiguration(), is(cf.getCompilerConfiguration()));
        assertThat(dc.getParent(), is(parent));
    }

    @Test
    public void testConstructFromConfigNull() throws Exception {
        try {
            new DefaultGroovyCompilerFactory((CompilerConfiguration)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Compiler configuration is null."));
        }
    }

}
