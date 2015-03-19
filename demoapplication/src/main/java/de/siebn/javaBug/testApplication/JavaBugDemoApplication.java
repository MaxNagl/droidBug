package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.*;
import de.siebn.javaBug.objectOut.*;
import de.siebn.javaBug.plugins.ClassPathBugPlugin;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.RootBugPlugin;
import de.siebn.javaBug.plugins.ThreadsBugPlugin;

/**
 * Created by Sieben on 16.03.2015.
 */
public class JavaBugDemoApplication {

    public static void main(String[] args) {
        JavaBug jb = new JavaBug(7777);
        jb.addDefaultPlugins();

        jb.getObjectBug().addRootObject(new TestClass());
        jb.getObjectBug().addRootObject(jb);

        jb.tryToStart();

        System.out.println("javaBug startet. Open your browser at: " + jb.getIPAddresses(true));

        while(true) {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
            }
        }
    }
}
