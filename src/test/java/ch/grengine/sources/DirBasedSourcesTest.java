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

package ch.grengine.sources;

import ch.grengine.TestUtil;
import ch.grengine.code.CompilerFactory;
import ch.grengine.code.groovy.DefaultGroovyCompilerFactory;
import ch.grengine.source.DefaultFileSource;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.MockFile;
import ch.grengine.source.MockSourceFactory;
import ch.grengine.source.Source;
import ch.grengine.source.SourceFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static ch.grengine.TestUtil.assertThrows;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


public class DirBasedSourcesTest {
    
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructDefaults() throws Exception {

        // given

        final File dir = tempFolder.getRoot();

        // when

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder.build();

        // then
        
        Thread.sleep(30);
        assertThat(s.getBuilder(), is(builder));
        assertThat(s.getDir().getPath(), is(dir.getCanonicalPath()));
        assertThat(s.getDirMode(), is(DirMode.NO_SUBDIRS));
        assertThat(s.getScriptExtensions().size(), is(1));
        assertThat(s.getScriptExtensions().contains("groovy"), is(true));
        assertThat(s.getName(), is(dir.getCanonicalPath()));
        assertThat(s.getCompilerFactory(), is(notNullValue()));
        assertThat(s.getCompilerFactory(), instanceOf(DefaultGroovyCompilerFactory.class));

        assertThat(s.getBuilder().getDir().getPath(), is(s.getDir().getPath()));
        assertThat(s.getBuilder().getDirMode(), is(s.getDirMode()));
        assertThat(s.getBuilder().getScriptExtensions(), is(s.getScriptExtensions()));
        assertThat(s.getBuilder().getName(), is(s.getName()));
        assertThat(s.getBuilder().getCompilerFactory(), is(s.getCompilerFactory()));
        assertThat(s.getBuilder().getSourceFactory(), is(notNullValue()));
        assertThat(s.getBuilder().getSourceFactory(), instanceOf(DefaultSourceFactory.class));
        assertThat(s.getBuilder().getLatencyMs(), is(DirBasedSources.Builder.DEFAULT_LATENCY_MS));
        assertThat(s.getLastModified() < System.currentTimeMillis(), is(true));

        assertThat(s.getSourceSet().isEmpty(), is(true));
    }
    
    @Test
    public void testConstructAllDefined() throws Exception {

        // given

        final File dir = tempFolder.getRoot();
        final Set<String> scriptExtensions = new HashSet<>(Arrays.asList("groovy", "gradle"));
        final CompilerFactory compilerFactory = new DefaultGroovyCompilerFactory();
        final SourceFactory sourceFactory = new DefaultSourceFactory();

        // when

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder
                .setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE).setScriptExtensions(scriptExtensions)
                .setScriptExtensions("groovy", "gradle")
                .setName("dirBased")
                .setCompilerFactory(compilerFactory)
                .setSourceFactory(sourceFactory)
                .setLatencyMs(200)
                .build();

        // then
        
        Thread.sleep(30);
        assertThat(s.getBuilder(), is(builder));
        assertThat(s.getDir().getPath(), is(dir.getCanonicalPath()));
        assertThat(s.getDirMode(), is(DirMode.WITH_SUBDIRS_RECURSIVE));
        assertThat(s.getScriptExtensions().size(), is(2));
        assertThat(s.getScriptExtensions().contains("groovy"), is(true));
        assertThat(s.getScriptExtensions().contains("gradle"), is(true));
        assertThat(s.getName(), is("dirBased"));
        assertThat(s.getCompilerFactory(), is(compilerFactory));

        assertThat(s.getBuilder().getDir().getPath(), is(s.getDir().getPath()));
        assertThat(s.getBuilder().getDirMode(), is(s.getDirMode()));
        assertThat(s.getBuilder().getScriptExtensions(), is(s.getScriptExtensions()));
        assertThat(s.getBuilder().getName(), is(s.getName()));
        assertThat(s.getBuilder().getCompilerFactory(), is(s.getCompilerFactory()));
        assertThat(s.getBuilder().getSourceFactory(), is(sourceFactory));
        assertThat(s.getBuilder().getLatencyMs(), is(200L));
        assertThat(s.getLastModified() < System.currentTimeMillis(), is(true));

        assertThat(s.getSourceSet().isEmpty(), is(true));
    }
    
    @Test
    public void testConstructDirNull() {

        // when/then

        assertThrows(() -> new DirBasedSources.Builder(null),
                NullPointerException.class,
                "Dir is null.");
    }
    
    @Test
    public void testModifyBuilderAfterUse() {

        // given

        final File dir = tempFolder.getRoot();
        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        builder.build();

        // when/then

        assertThrows(() -> builder.setName("name"),
                IllegalStateException.class,
                "Builder already used.");
    }
    
    @Test
    public void testGetSourcesNoSubDirsDefaultExtensions() throws Exception {

        // given

        final File dir = tempFolder.getRoot();
        final Map<String,File> m = createFiles(dir);

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder.build();

        // when

        final Set<Source> set = s.getSourceSet();

        // then

        assertThat(set.contains(new DefaultFileSource(m.get("file"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileGoo"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileNoExt"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSub"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubGoo"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubSub"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))), is(true));
        if (!TestUtil.isWindows()) {
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible1"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible2"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible3"))), is(true));
        }
    }
    
    @Test
    public void testGetSourcesWithSubDirsDefaultExtensions() throws Exception {

        // given

        final File dir = tempFolder.getRoot();
        final Map<String,File> m = createFiles(dir);

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder.setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE).build();

        // when

        final Set<Source> set = s.getSourceSet();

        // then

        assertThat(set.contains(new DefaultFileSource(m.get("file"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileGoo"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileNoExt"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileSub"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubGoo"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileSubSub"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))), is(true));
        if (!TestUtil.isWindows()) {
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible1"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible2"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible3"))), is(true));
        }
    }
    
    @Test
    public void testGetSourcesNoSubDirsSpecificExtensions() throws Exception {

        // given

        final File dir = tempFolder.getRoot();
        final Map<String,File> m = createFiles(dir);

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder.setScriptExtensions("groovy", "goo").build();

        // when

        final Set<Source> set = s.getSourceSet();

        // then

        assertThat(set.contains(new DefaultFileSource(m.get("file"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileGoo"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileNoExt"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSub"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubGoo"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubSub"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))), is(true));
        if (!TestUtil.isWindows()) {
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible1"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible2"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible3"))), is(true));
        }
    }

    @Test
    public void testGetSourcesWithSubDirsSpecificExtensions() throws Exception {

        // given

        final File dir = tempFolder.getRoot();
        final Map<String,File> m = createFiles(dir);

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder
                .setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE)
                .setScriptExtensions("groovy", "goo")
                .build();

        // when

        final Set<Source> set = s.getSourceSet();

        // then

        assertThat(set.contains(new DefaultFileSource(m.get("file"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileGoo"))), is(true));
        assertThat(!set.contains(new DefaultFileSource(m.get("fileNoExt"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileSub"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileSubGoo"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileSubSub"))), is(true));
        assertThat(set.contains(new DefaultFileSource(m.get("fileSubSubGoo"))), is(true));
        if (!TestUtil.isWindows()) {
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible1"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible2"))), is(true));
            assertThat(!set.contains(new DefaultFileSource(m.get("fileUnixInvisible3"))), is(true));
        }
    }

    @Test
    public void testGetSourcesNonExistentDir() {

        // given

        final File dir = new File(tempFolder.getRoot(), "does/not/exist");

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder
                .setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE)
                .setScriptExtensions("groovy", "goo")
                .build();

        // when

        final Set<Source> set = s.getSourceSet();

        // then

        assertThat(set.isEmpty(), is(true));
    }
    
    @Test
    public void testLastModified() throws Exception {

        // given

        final File dir = tempFolder.getRoot();
        final Map<String,File> m = createFiles(dir);

        final DirBasedSources.Builder builder = new DirBasedSources.Builder(dir);
        final DirBasedSources s = builder
                .setSourceFactory(new MockSourceFactory())
                .setDirMode(DirMode.WITH_SUBDIRS_RECURSIVE)
                .setLatencyMs(50)
                .build();

        // when

        final File file = m.get("file");

        // then

        assertThat(file, instanceOf(MockFile.class));

        // when (change file last modified)

        assertThat(file.setLastModified(1), is(true));

        // then

        assertThat(file.lastModified(), is(1L));
        final long lastMod = s.getLastModified();
        Thread.sleep(30);
        assertThat(lastMod, is(s.getLastModified()));
        Thread.sleep(30);
        final long lastMod2 = s.getLastModified();
        assertThat(lastMod2 > lastMod, is(true));
        Thread.sleep(60);
        assertThat(lastMod2, is(s.getLastModified()));

        // when (add a file)

        final File newFile = new MockFile(dir, "MyScript2New.groovy");
        TestUtil.setFileText(newFile, "println 'new'");

        // then

        Thread.sleep(60);
        final long lastMod3 = s.getLastModified();
        assertThat(lastMod3 > lastMod2, is(true));

        // when (remove a file)

        assertThat(newFile.delete(), is(true));

        // then

        assertThat(!newFile.exists(), is(true));
        Thread.sleep(60);
        final long lastMod4 = s.getLastModified();
        assertThat(lastMod4 > lastMod3, is(true));

        // when (add a file that is not part of the set to watch)

        final File newFile2 = new MockFile(dir, "MyScript2New.off");
        TestUtil.setFileText(newFile2, "println 'new'");

        // then

        Thread.sleep(60);
        final long lastMod5 = s.getLastModified();
        assertThat(lastMod4, is(lastMod5));
    }

    
    private static Map<String,File> createFiles(File dir) throws Exception {
        final File subDir = new File(dir, "foo");
        final File subSubDir = new File(subDir, "bar");
        assertThat(subSubDir.mkdirs(), is(true));
        assertThat(subSubDir.exists(), is(true));

        final Map<String,File> m = new HashMap<>();
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
        m.put("fileUnixInvisible1", new MockFile(dir, ".groovy"));
        m.put("fileUnixInvisible2", new MockFile(dir, ".UnixInvisible.groovy"));
        m.put("fileUnixInvisible3", new MockFile(subDir, ".groovy"));
        
        TestUtil.setFileText(m.get("file"), "println 'file'");
        TestUtil.setFileText(m.get("fileGoo"), "println 'fileGoo'");
        TestUtil.setFileText(m.get("fileNoExt"), "println 'fileNoExt'");
        TestUtil.setFileText(m.get("fileSub"), "println 'fileSub'");
        TestUtil.setFileText(m.get("fileSubGoo"), "println 'fileSubGoo'");
        TestUtil.setFileText(m.get("fileSubSub"), "println 'fileSubSub'");
        TestUtil.setFileText(m.get("fileSubSubGoo"), "println 'fileSubSubGoo'");
        TestUtil.setFileText(m.get("fileUnixInvisible1"), "println 'fileUnixInvisible1'");
        TestUtil.setFileText(m.get("fileUnixInvisible2"), "println 'fileUnixInvisible2'");
        TestUtil.setFileText(m.get("fileUnixInvisible3"), "println 'fileUnixInvisible3'");
        
        return m;
    }
    
}
