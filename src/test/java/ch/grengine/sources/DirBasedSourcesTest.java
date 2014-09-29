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

package ch.grengine.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.grengine.TestUtil;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.DefaultFileSource;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.MockFile;
import ch.grengine.source.MockSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;


public class DirBasedSourcesTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructDefaults() throws Exception {
        File dir = tempFolder.getRoot();
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder.build();
        
        Thread.sleep(30);
        assertEquals(builder, s.getBuilder());
        assertEquals(dir.getCanonicalPath(), s.getDir().getPath());
        assertEquals(DirMode.NO_SUBDIRS, s.getDirMode());
        assertTrue(s.getScriptExtensions().size() == 1);
        assertTrue(s.getScriptExtensions().contains("groovy"));
        assertEquals(dir.getCanonicalPath(), s.getName());
        assertNotNull(s.getCompilerFactory());
        assertTrue(s.getCompilerFactory() instanceof DefaultGroovyCompilerFactory);
        
        assertEquals(s.getDir().getPath(), s.getBuilder().getDir().getPath());
        assertEquals(s.getDirMode(), s.getBuilder().getDirMode());
        assertEquals(s.getScriptExtensions(), s.getBuilder().getScriptExtensions());
        assertEquals(s.getName(), s.getBuilder().getName());
        assertEquals(s.getCompilerFactory(), s.getBuilder().getCompilerFactory());
        assertNotNull(s.getBuilder().getSourceFactory());
        assertTrue(s.getBuilder().getSourceFactory() instanceof DefaultSourceFactory);
        assertEquals(DirBasedSources.Builder.DEFAULT_LATENCY_MS, s.getBuilder().getLatencyMs());
        assertTrue(s.getLastModified() < System.currentTimeMillis());
        
        assertTrue(s.getSourceSet().isEmpty());
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {
        File dir = tempFolder.getRoot();
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        Set<String> scriptExtensions = TestUtil.argsToSet("groovy", "gradle");
        builder.setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE).setScriptExtensions(scriptExtensions);
        builder.setScriptExtensions("groovy", "gradle");
        builder.setName("dirbased");
        CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        builder.setCompilerFactory(compilerFactory);
        SourceFactory sourceFactory = new DefaultSourceFactory();
        builder.setSourceFactory(sourceFactory);
        builder.setLatencyMs(200);
        DirBasedSources s = builder.build();
        
        Thread.sleep(30);
        assertEquals(builder, s.getBuilder());
        assertEquals(dir.getCanonicalPath(), s.getDir().getPath());
        assertEquals(DirMode.WITH_SUBDIRS_RECURSIVE, s.getDirMode());
        assertTrue(s.getScriptExtensions().size() == 2);
        assertTrue(s.getScriptExtensions().contains("groovy"));
        assertTrue(s.getScriptExtensions().contains("gradle"));
        assertEquals("dirbased", s.getName());
        assertEquals(compilerFactory, s.getCompilerFactory());
        
        assertEquals(s.getDir().getPath(), s.getBuilder().getDir().getPath());
        assertEquals(s.getDirMode(), s.getBuilder().getDirMode());
        assertEquals(s.getScriptExtensions(), s.getBuilder().getScriptExtensions());
        assertEquals(s.getName(), s.getBuilder().getName());
        assertEquals(s.getCompilerFactory(), s.getBuilder().getCompilerFactory());
        assertEquals(sourceFactory, s.getBuilder().getSourceFactory());
        assertEquals(200, s.getBuilder().getLatencyMs());
        assertTrue(s.getLastModified() < System.currentTimeMillis());
        
        assertTrue(s.getSourceSet().isEmpty());
    }
    
    @Test
    public void testConstructDirNull() throws Exception {
        try {
            new DirBasedSources.Builder(null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Dir is null.", e.getMessage());
        }
    }
    
    @Test
    public void testModifyBuilderAfterUse() throws Exception {
        File dir = tempFolder.getRoot();
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        builder.build();
        try {
            builder.setName("name");
            fail();
        } catch (IllegalStateException e) {
            assertEquals("Builder already used.", e.getMessage());
        }
    }
    
    @Test
    public void testGetSourcesNoSubdirsDefaultExtensions() throws Exception {
        File dir = tempFolder.getRoot();
        Map<String,File> m = createFiles(dir);
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder.build();
        
        Set<Source> set = s.getSourceSet();
        
        assertTrue(set.contains(new DefaultFileSource(m.get("file"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileGoo"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileNoExt"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSub"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubGoo"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubSub"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))));
        if (!TestUtil.isWindows()) {
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis1"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis2"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis3"))));
        }
    }
    
    @Test
    public void testGetSourcesWithSubdirsDefaultExtensions() throws Exception {
        File dir = tempFolder.getRoot();
        Map<String,File> m = createFiles(dir);
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder.setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE).build();
        
        Set<Source> set = s.getSourceSet();
        
        assertTrue(set.contains(new DefaultFileSource(m.get("file"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileGoo"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileNoExt"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileSub"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubGoo"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileSubSub"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))));
        if (!TestUtil.isWindows()) {
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis1"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis2"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis3"))));
        }
    }
    
    @Test
    public void testGetSourcesNoSubdirsSpecificExtensions() throws Exception {
        File dir = tempFolder.getRoot();
        Map<String,File> m = createFiles(dir);
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder.setScriptExtensions("groovy", "goo").build();
        
        Set<Source> set = s.getSourceSet();
        
        assertTrue(set.contains(new DefaultFileSource(m.get("file"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileGoo"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileNoExt"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSub"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubGoo"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubSub"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))));
        if (!TestUtil.isWindows()) {
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis1"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis2"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis3"))));
        }
    }

    @Test
    public void testGetSourcesWithSubdirsSpecificExtensions() throws Exception {
        File dir = tempFolder.getRoot();
        Map<String,File> m = createFiles(dir);
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder.setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE)
                .setScriptExtensions("groovy", "goo").build();
        
        Set<Source> set = s.getSourceSet();
        
        assertTrue(set.contains(new DefaultFileSource(m.get("file"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileGoo"))));
        assertTrue(!set.contains(new DefaultFileSource(m.get("fileNoExt"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileSub"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileSubGoo"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileSubSub"))));
        assertTrue(set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))));
        if (!TestUtil.isWindows()) {
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis1"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis2"))));
            assertTrue(!set.contains(new DefaultFileSource(m.get("fileUnixInvis3"))));
        }
    }

    @Test
    public void testGetSourcesNonExistentDir() throws Exception {
        File dir = new File(tempFolder.getRoot(), "does/not/exist");
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder.setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE)
                .setScriptExtensions("groovy", "goo").build();
        Set<Source> set = s.getSourceSet();
        assertTrue(set.isEmpty());
    }
    
    @Test
    public void testLastModified() throws Exception {
        File dir = tempFolder.getRoot();
        Map<String,File> m = createFiles(dir);
        DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        DirBasedSources s = builder
                .setSourceFactory(new MockSourceFactory())
                .setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE)
                .setLatencyMs(50)
                .build();
        
        // change file last modified
        File file = m.get("file");
        assertTrue(file instanceof MockFile);
        file.setLastModified(1);
        assertEquals(1, file.lastModified());
        long lastMod = s.getLastModified();
        Thread.sleep(30);
        assertEquals(s.getLastModified(), lastMod);
        Thread.sleep(30);
        long lastMod2 = s.getLastModified();
        assertTrue(lastMod2 > lastMod);
        Thread.sleep(60);
        assertEquals(s.getLastModified(), lastMod2);
        
        // add a file
        File newFile = new MockFile(dir, "MyScript2New.groovy");
        TestUtil.setFileText(newFile, "println 'new'");
        Thread.sleep(60);
        long lastMod3 = s.getLastModified();
        assertTrue(lastMod3 > lastMod2);
        
        // remove a file
        newFile.delete();
        assertTrue(!newFile.exists());
        Thread.sleep(60);
        long lastMod4 = s.getLastModified();
        assertTrue(lastMod4 > lastMod3);

        // add a file that is not part of the set to watch
        File newFile2 = new MockFile(dir, "MyScript2New.off");
        TestUtil.setFileText(newFile2, "println 'new'");
        Thread.sleep(60);
        long lastMod5 = s.getLastModified();
        assertEquals(lastMod5, lastMod4);
    }

    
    public static Map<String,File> createFiles(File dir) throws Exception {
        File subDir = new File(dir, "foo");
        File subSubDir = new File(subDir, "bar");
        subSubDir.mkdirs();
        assertTrue(subSubDir.exists());
        
        Map<String,File> m = new HashMap<String,File>();
        m.put("dir", dir);
        m.put("subDir", subDir);
        m.put("subSubDir", subSubDir);
        m.put("file", new MockFile(dir, "MyScript.groovy"));
        m.put("fileGoo", new MockFile(dir, "MyScriptGoo.goo"));
        m.put("fileNoExt", new MockFile(dir, "groovy"));
        m.put("fileSub", new MockFile(subDir, "MySubScript.groovy"));
        m.put("fileSubGoo", new MockFile(subDir, "MySubScriptGoo.goo"));
        m.put("fileSubSub", new MockFile(subSubDir, "MySubSubScript.groovy"));
        m.put("fileSubSubGoo", new MockFile(subSubDir, "MySubSubScriptGoo.goo"));
        m.put("fileUnixInvis1", new MockFile(dir, ".groovy"));
        m.put("fileUnixInvis2", new MockFile(dir, ".UnixInvis.groovy"));
        m.put("fileUnixInvis3", new MockFile(subDir, ".groovy"));
        
        TestUtil.setFileText(m.get("file"), "println 'file'");
        TestUtil.setFileText(m.get("fileGoo"), "println 'fileGoo'");
        TestUtil.setFileText(m.get("fileNoExt"), "println 'fileNoExt'");
        TestUtil.setFileText(m.get("fileSub"), "println 'fileSub'");
        TestUtil.setFileText(m.get("fileSubGoo"), "println 'fileSubGoo'");
        TestUtil.setFileText(m.get("fileSubSub"), "println 'fileSubSub'");
        TestUtil.setFileText(m.get("fileSubSubGoo"), "println 'fileSubSubGoo'");
        TestUtil.setFileText(m.get("fileUnixInvis1"), "println 'fileUnixInvis1'");
        TestUtil.setFileText(m.get("fileUnixInvis2"), "println 'fileUnixInvis2'");
        TestUtil.setFileText(m.get("fileUnixInvis3"), "println 'fileUnixInvis3'");
        
        return m;
    }
    
}
