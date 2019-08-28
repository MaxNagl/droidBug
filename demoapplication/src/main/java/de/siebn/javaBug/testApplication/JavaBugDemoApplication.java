package de.siebn.javaBug.testApplication;


import java.util.*;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.BugObjectCache;

/**
 * Created by Sieben on 16.03.2015.
 */
public class JavaBugDemoApplication {

    public static void main(String[] args) {
        JavaBug jb = new JavaBug(7777);
        jb.addDefaultPlugins();
        jb.addPlugin(new TestPropertyCategory(jb));
        jb.addPlugin(new TestOutputCatergory(jb));

        TestClass test = new TestClass();
        jb.getObjectBug().addRootObject("Test", test);
        jb.getObjectBug().addRootObject("JavaBug", jb);
        jb.getObjectBug().addRootObject("Formats", new BugFormatTest());
        jb.getObjectBug().addRootObject("Array", new String[]{"Eins", "Zwei", "Drei"});
        jb.getObjectBug().addRootObject("List", Arrays.asList("One", "Two", "Three"));
        HashMap<String, String> map = new LinkedHashMap<>();
        map.put("One", "Eins");
        map.put("Two", "Zwei");
        map.put("Three", "Drei");
        jb.getObjectBug().addRootObject("Map", map);
        for (int i = 0; i < 100; i++)
            jb.getObjectBug().addRootObject("Integer " + i, i);

        jb.tryToStart();

        System.out.println("javaBug startet. Open your browser at: " + jb.getIPAddresses(true));
        System.out.println("Test Object " + BugObjectCache.getReference(test));

        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
            }
        }
    }
}
