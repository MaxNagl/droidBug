package de.siebn.javaBug.testApplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;

import de.siebn.javaBug.JavaBug;

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
        jb.getObjectBug().addRootObject(test);
        jb.getObjectBug().addRootObject(jb);
        jb.getObjectBug().addRootObject(new BugFormatTest());
        jb.getObjectBug().addRootObject(new String[]{"Eins", "Zwei", "Drei"});
        jb.getObjectBug().addRootObject(Arrays.asList("One", "Two", "Three"));
        HashMap<String, String> map = new LinkedHashMap<>();
        map.put("One", "Eins");
        map.put("Two", "Zwei");
        map.put("Three", "Drei");
        jb.getObjectBug().addRootObject(map);
        for (int i = 0; i < 100; i++)
            jb.getObjectBug().addRootObject(i);

        jb.tryToStart();

        System.out.println("javaBug startet. Open your browser at: " + jb.getIPAddresses(true));
        System.out.println("Test Object " + jb.getObjectBug().getObjectReference(test));

        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
            }
        }
    }
}
