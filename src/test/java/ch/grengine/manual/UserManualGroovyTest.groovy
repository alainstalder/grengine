package ch.grengine.manual

import ch.grengine.Grengine
import ch.grengine.sources.DirMode
import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

// Tests code fragments used in the user manual.

class UserManualGroovyTest {

    @TempDir
    public File tempDir
    
    @Test
    void testRunningIndividualScripts_BindingWithGroovy() throws Exception {
        Grengine gren = new Grengine();
        
        //--------------------------------------------
        gren.run('println x+y', [ 'x' : 5, 'y' : 2 ])
        //--------------------------------------------
    }
    
    @Test
    void testInterdependentScripts_DirBasedGrengine_DirChanges() throws Exception {

        String utilScriptText = 'class Util { def concat(String a, String b) { return "$a:$b" } }';
        String scriptText = "println new Util().concat('xx', 'yy')";
        File scriptDir = tempFolder.getRoot();
        new File(scriptDir, "Util.groovy").setText(utilScriptText);
        new File(scriptDir, "Test.groovy").setText(scriptText);
        Grengine gren = new Grengine(scriptDir);
        
        //--------------------------------------------
        def utilFile = new File(scriptDir, 'Util.groovy')
        def newUtilFile = new File(scriptDir, 'NewUtil.groovy')
        def testFile = new File(scriptDir, 'Test.groovy')
        gren.run(testFile)
        utilFile.delete()
        newUtilFile.setText('class Util { def concat(def a, def b) { return "$a--$b" } }')
        testFile.setText('println new Util().concat("aa", "bb")')
        gren.run(testFile)
        Thread.sleep(6000)
        gren.run(testFile)
        //--------------------------------------------
    }
    
    @Test
    void testInterdependentScripts_DirBasedGrengine_Options() throws Exception {

        String utilScriptText = 'class Util { def concat(String a, String b) { return "$a:$b" } }';
        String scriptText = "println new Util().concat('xx', 'yy')";
        File scriptDir = tempFolder.getRoot();
        new File(scriptDir, "Util.groovy").setText(utilScriptText);
        new File(scriptDir, "Test.groovy").setText(scriptText);
        
        //--------------------------------------------
        def config = new CompilerConfiguration()
        config.setScriptExtensions([ "groovy", "funky" ] as Set)
        def gren = new Grengine(config, scriptDir, DirMode.WITH_SUBDIRS_RECURSIVE)
        //--------------------------------------------
    }
    
    
}
