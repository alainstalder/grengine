package ch.grengine.manual;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import ch.grengine.Grengine;
import ch.grengine.source.DefaultSourceFactory;
import ch.grengine.source.Source;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Tests code fragments used in the user manual.

public class UserManualJavaTest {

    @TempDir
    public File tempDir;

    @Disabled
    @Test
    public void testRunningIndividualScripts_Basic() throws Exception {
        //--------------------------------------------
        Grengine gren = new Grengine();
        gren.run("println 'Hello World!'");
        gren.run(new File("MyScript.groovy"));
        gren.run(new URL("http://somewhere.org/MyScript.groovy"));
        //--------------------------------------------
    }
    
    @Test
    public void testRunningIndividualScripts_Binding() throws Exception {
        Grengine gren = new Grengine();
        File scriptFile= new File(tempDir, "MyScript.groovy");
        setFileText(scriptFile, "println x+y");
        
        //--------------------------------------------
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("x", 5);
        map.put("y", 2);
        Binding binding = new Binding(map);
        gren.run("println x+y", binding);
        gren.run(scriptFile, map);
        //--------------------------------------------
    }
    
    @Test
    public void testRunningIndividualScripts_BindingWithJava() throws Exception {
        Grengine gren = new Grengine();
        
        //--------------------------------------------
        gren.run("println x+y", gren.binding("x", 5, "y", 2));
        //--------------------------------------------
    }
    
    
    @Test
    public void testRunningIndividualScripts_BehindTheScenes_Compile() throws Exception {
        System.out.println(new SomeGeneratedClassName().run());
    }
    
    @Test
    public void testRunningIndividualScripts_BehindTheScenes_CreateAndRun() throws Exception {
        Grengine gren = new Grengine();
        Class<?> clazz = gren.load("println x+y");
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("x", 5);
        map.put("y", 2);
        Binding binding = new Binding(map);
        
        
        //--------------------------------------------
        Script script = (Script)clazz.newInstance();
        script.setBinding(binding);
        Object obj = script.run();
        //--------------------------------------------
        
        System.out.println(obj);
    }
    
    @Test
    public void testRunningIndividualScripts_BehindTheScenes_TextBasedScripts_ClassNames() throws Exception {

        //--------------------------------------------
        String scriptText = "println this.class.name";
        GroovyShell shell = new GroovyShell();
        shell.evaluate(scriptText);
        shell.evaluate(scriptText);
        GroovyClassLoader gcl = new GroovyClassLoader();
        ((Script)gcl.parseClass(scriptText).newInstance()).run();
        ((Script)gcl.parseClass(scriptText).newInstance()).run();
        Grengine gren = new Grengine();
        gren.run(scriptText);
        gren.run(scriptText);
        //--------------------------------------------
        
        gcl.close();
    }

    @Test
    public void testRunningIndividualScripts_BehindTheScenes_TextBasedScripts_Performance() throws Exception {
        GroovyShell shell = new GroovyShell();
        Grengine gren = new Grengine();
        Script script = shell.parse("return 2");
        
        //--------------------------------------------
        long t0 = System.nanoTime();
        for (int i=0; i<1000; i++) shell.evaluate("return 2");
        long t1 = System.nanoTime();
        for (int i=0; i<1000; i++) gren.run("return 2");
        long t2 = System.nanoTime();
        for (int i=0; i<1000; i++) script.run();
        long t3 = System.nanoTime();
        System.out.printf("GroovyShell: %8.3f ms%n", (t1-t0)/1000000.0);
        System.out.printf("Grengine:    %8.3f ms%n", (t2-t1)/1000000.0);
        System.out.printf("Script:      %8.3f ms%n", (t3-t2)/1000000.0);
        //--------------------------------------------
    }

    @Test
    public void testRunningIndividualScripts_BehindTheScenes_TextBasedScripts_DesiredClassName() throws Exception {
        GroovyShell shell = new GroovyShell();
        Grengine gren = new Grengine();
        
        //--------------------------------------------
        gren.run("println this.class.name", "MyScript");
        shell.evaluate("println this.class.name", "MyScript");
        //--------------------------------------------
    }

    @Test
    public void testRunningIndividualScripts_BehindTheScenes_FileBasedScripts_Performance() throws Exception {
        GroovyShell shell = new GroovyShell();
        Grengine gren = new Grengine();
        File scriptFile = new File(tempDir, "MyScript.groovy");
        setFileText(scriptFile, "return 2");
        
        long t0 = System.nanoTime();
        for (int i=0; i<1000; i++) shell.evaluate(scriptFile);
        long t1 = System.nanoTime();
        for (int i=0; i<1000; i++) gren.run(scriptFile);
        long t2 = System.nanoTime();
        System.out.printf("GroovyShell: %8.3f ms%n", (t1-t0)/1000000.0);
        System.out.printf("Grengine:    %8.3f ms%n", (t2-t1)/1000000.0);
    }

    @Test
    public void testRunningIndividualScripts_SeparatingLoadingCreatingRunning() throws Exception {
        Grengine gren = new Grengine();
        File scriptFile = new File(tempDir, "MyScript.groovy");
        setFileText(scriptFile, "return 2");
        URL scriptUrl = scriptFile.toURI().toURL();
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("x", 5);
        map.put("y", 2);
        Binding binding = new Binding(map);

        //--------------------------------------------
        Class<?> clazz;
        clazz = gren.load("return 2");
        clazz = gren.load("return 2", "MyDesiredClassName");
        clazz = gren.load(scriptFile);
        clazz = gren.load(scriptUrl);
        Script script;
        script = gren.create(clazz);
        script = gren.create("return 2");
        script = gren.create("return 2", "MyDesiredClassName");
        script = gren.create(scriptFile);
        script = gren.create(scriptUrl);
        Object obj;
        obj = gren.run(script);
        obj = gren.run(script, binding);
        obj = gren.run(script, map);
        obj = gren.run("return 2");
        obj = gren.run("return x", binding);
        obj = gren.run("return x", map);
        obj = gren.run("return x", gren.binding("x", 5));
        obj = gren.run("return 2", "MyDesiredClassName");
        obj = gren.run("return x", "MyDesiredClassName", binding);
        // ...
        //--------------------------------------------
        
        System.out.println(obj);
    }

    @Test
    public void testRunningIndividualScripts_TheSourceInterface() throws Exception {
        Grengine gren = new Grengine();
        File scriptFile = new File(tempDir, "MyScript.groovy");
        setFileText(scriptFile, "return 2");
        URL scriptUrl = scriptFile.toURI().toURL();
        Class<?> clazz;
        Script script;
        Object obj;
        
        //--------------------------------------------
        Source textSource = gren.source("return 2");
        Source textSourceWithName = gren.source("return 2", "MyScript");
        Source fileSource = gren.source(scriptFile);
        Source urlSource = gren.source(scriptUrl);
        System.out.println(textSource.getId() + " - " + textSource.getLastModified());
        System.out.println(textSourceWithName.getId() + " - " + textSourceWithName.getLastModified());
        System.out.println(fileSource.getId() + " - " + fileSource.getLastModified());
        System.out.println(urlSource.getId() + " - " + urlSource.getLastModified());
        clazz = gren.load(textSource);
        script = gren.create(fileSource);
        obj = gren.run(urlSource, gren.binding("x", 5));
        //--------------------------------------------
        
        System.out.println(clazz);
        System.out.println(script);
        System.out.println(obj);
    }

    @Test
    public void testRunningIndividualScripts_TweakingPerformance() throws Exception {
        Grengine gren = new Grengine();
        File scriptFile = new File(tempDir, "MyScript.groovy");
        setFileText(scriptFile, "return 2");
        
        for (int i=0; i<1000; i++) gren.run("return 55");
        
        //--------------------------------------------
        Grengine grenDefault = new Grengine();
        Grengine grenTweaked= new Grengine.Builder()
                .setSourceFactory(new DefaultSourceFactory.Builder()
                        .setTrackTextSourceIds(true)
                        .setTrackFileSourceLastModified(true)
                        .setTrackUrlContent(true)
                        .build())
                .build();
        grenDefault.run("return 2");
        grenTweaked.run("return 2");
        grenDefault.run(scriptFile);
        grenTweaked.run(scriptFile);
        long t0 = System.nanoTime();
        for (int i=0; i<1000; i++) grenDefault.run("return 2");
        long t1 = System.nanoTime();
        for (int i=0; i<1000; i++) grenTweaked.run("return 2");
        long t2 = System.nanoTime();
        for (int i=0; i<1000; i++) grenDefault.run(scriptFile);
        long t3 = System.nanoTime();
        for (int i=0; i<1000; i++) grenTweaked.run(scriptFile);
        long t4 = System.nanoTime();
        System.out.printf("Script Text - Default:  %8.3f ms%n", (t1-t0)/1000000.0);
        System.out.printf("Script Text - Tweaked:  %8.3f ms%n", (t2-t1)/1000000.0);
        System.out.printf("Script File - Default:  %8.3f ms%n", (t3-t2)/1000000.0);
        System.out.printf("Script File - Tweaked:  %8.3f ms%n", (t4-t3)/1000000.0);
        //--------------------------------------------
    }    
    
    @Test
    public void testInterdependentScripts_DirBasedGrengine_Basic() throws Exception {

        String utilScriptText = "class Util {\n"
                + "  def concat(String a, String b) { return \"$a:$b\" }\n"
                + "}";
        String scriptText = "println new Util().concat('xx', 'yy')";
        File scriptDir = tempDir;
        setFileText(new File(scriptDir, "Util.groovy"), utilScriptText);
        setFileText(new File(scriptDir, "Test.groovy"), scriptText);

        
        //--------------------------------------------
        //File scriptDir = new File(".");
        Grengine gren = new Grengine(scriptDir);
        gren.run(new File(scriptDir, "Test.groovy"));
        gren.run("println new Util().concat('xx', 'yy')");
        //--------------------------------------------

        
        /*File utilScriptFile = new File(scriptDir, "Util.groovy");
        File scriptFile = new File(scriptDir, "Test.groovy");
        GroovyShell shell = new GroovyShell();
        Grengine gren = new Grengine();
        
        System.out.println(new File(".").getAbsolutePath());
        shell.parse(utilScriptFile);
        shell.evaluate(scriptFile);
        gren.load(utilScriptFile);
        gren.run(scriptFile);
        
        setFileText(utilScriptFile, util2ScriptText);
        shell.parse(utilScriptFile);
        shell.evaluate(scriptFile);*/
    }
    
    public static void setFileText(File file, String text)
            throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(file, "UTF-8");
        writer.write(text);
        writer.close();
    }
    
    public static String getFileText(File file) throws FileNotFoundException {
        Scanner scan = new Scanner(file);
        try {
            scan.useDelimiter("\\A");
            return scan.hasNext() ? scan.next() : "";
        } finally {
            scan.close();
        }
    }

}
