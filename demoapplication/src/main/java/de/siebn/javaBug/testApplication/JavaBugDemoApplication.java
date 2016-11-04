package de.siebn.javaBug.testApplication;

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
