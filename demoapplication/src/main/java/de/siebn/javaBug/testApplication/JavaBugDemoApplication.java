package de.siebn.javaBug.testApplication;


import java.util.*;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.BugByteCodeUtil;

/**
 * Created by Sieben on 16.03.2015.
 */
public class JavaBugDemoApplication {

    public static void main(String[] args) {
        JavaBug.addPlugin(new RecursiveOutputCatergory(JavaBug.getCore()));
        JavaBug.addPlugin(new TestPropertyCategory(JavaBug.getCore()));
        JavaBug.addPlugin(new TestOutputCatergory(JavaBug.getCore()));

        JavaBug.addRootObject("Test", new TestClass());
        JavaBug.addRootObject("TestWrapped", BugByteCodeUtil.getBuggedInstance(TestClass.class));
        JavaBug.addRootObject("Recursion", RecursiveTestClass.getBuggedTestHierarchy());
        JavaBug.addRootObject("JavaBugCore", JavaBug.getCore());
        JavaBug.addRootObject("Formats", new BugFormatTest());
        JavaBug.addRootObject("Array", new String[]{"Eins", "Zwei", "Drei"});
        JavaBug.addRootObject("List", Arrays.asList("One", "Two", "Three"));
        HashMap<String, String> map = new LinkedHashMap<>();
        map.put("One", "Eins");
        map.put("Two", "Zwei");
        map.put("Three", "Drei");
        JavaBug.addRootObject("Map", map);
        for (int i = 0; i < 100; i++)
            JavaBug.addRootObject("Integer " + i, i);
        JavaBug.start();
        System.out.println("javaBug startet. Open your browser at: " + JavaBug.getIPAddresses(true));


        while (true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
            }
        }
    }
}
