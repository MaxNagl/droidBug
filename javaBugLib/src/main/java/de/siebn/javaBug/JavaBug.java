package de.siebn.javaBug;

import java.io.File;
import java.util.ArrayList;

import de.siebn.javaBug.JavaBugCore.BugPlugin;

public class JavaBug {
    protected static JavaBugCore CORE;

    public static void init(int port) {
        if (CORE != null) throw new IllegalStateException("JavaBug was already started");
        CORE = new JavaBugCore(port);
        CORE.addDefaultPlugins();
    }

    public static JavaBugCore getCore() {
        if (CORE == null) init(7777);
        return CORE;
    }

    public static boolean isStarted() {
        return getCore().isAlive();
    }

    public static void start() {
        getCore().tryToStart();
    }

    public static void stop() {
        getCore().stop();
    }

    public static void addRootObject(String name, Object object) {
        getCore().getObjectBug().addRootObject(name, object);
    }

    public static void addPlugin(BugPlugin plugin) {
        getCore().addPlugin(plugin);
    }

    public static ArrayList<String> getIPAddresses(boolean includeLoopback) {
        return getCore().getIPAddresses(includeLoopback);
    }

    public static void addFileRoot(String name, File root) {
        getCore().getFileBug().addRoot(name, root);
    }

    public <T> T bug(T object) {
        return getCore().bug(object);
    }

    public <T> T bug(T object, String title, Object key) {
        return getCore().bug(object, title, key);
    }
}