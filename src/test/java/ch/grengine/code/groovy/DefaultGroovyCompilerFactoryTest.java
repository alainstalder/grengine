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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.code.Compiler;


public class DefaultGroovyCompilerFactoryTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void testConstructDefaults() throws Exception {
        DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        DefaultGroovyCompilerFactory cf = builder.build();
        
        assertEquals(builder, cf.getBuilder());
        assertNotNull(cf.getCompilerConfiguration());
        assertEquals(cf.getBuilder().getCompilerConfiguration(), cf.getCompilerConfiguration());
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        CompilerConfiguration config = new CompilerConfiguration();
        builder.setCompilerConfiguration(config);
        DefaultGroovyCompilerFactory cf = builder.build();
        
        assertEquals(builder, cf.getBuilder());
        assertEquals(config, cf.getCompilerConfiguration());
        assertEquals(cf.getBuilder().getCompilerConfiguration(), cf.getCompilerConfiguration());
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        DefaultGroovyCompilerFactory.Builder builder = new DefaultGroovyCompilerFactory.Builder();
        builder.build();
        try {
            builder.setCompilerConfiguration(new CompilerConfiguration());
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }
    
    @Test
    public void testConstructorDefaultCompilerConfiguration() {
        DefaultGroovyCompilerFactory cf = new DefaultGroovyCompilerFactory();
        ClassLoader parent = Thread.currentThread().getContextClassLoader();
        Compiler c = cf.newCompiler(parent);
        assertTrue(c instanceof DefaultGroovyCompiler);
        DefaultGroovyCompiler dc = (DefaultGroovyCompiler)c;
        assertEquals(cf.getCompilerConfiguration(), dc.getCompilerConfiguration());
        assertEquals(parent, dc.getParent());
    }
    
    @Test
    public void testConstructorSpecificCompilerConfiguration() {
        CompilerConfiguration config = new CompilerConfiguration();
        DefaultGroovyCompilerFactory cf = new DefaultGroovyCompilerFactory(config);
        ClassLoader parent = Thread.currentThread().getContextClassLoader().getParent();
        Compiler c = cf.newCompiler(parent);
        assertTrue(c instanceof DefaultGroovyCompiler);
        DefaultGroovyCompiler dc = (DefaultGroovyCompiler)c;
        assertEquals(config, cf.getCompilerConfiguration());
        assertEquals(cf.getCompilerConfiguration(), dc.getCompilerConfiguration());
        assertEquals(parent, dc.getParent());
    }

    @Test
    public void testConstructFromConfigNull() throws Exception {
        try {
            new DefaultGroovyCompilerFactory((CompilerConfiguration)null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Compiler configuration is null.", e.getMessage());
        }
    }

}
