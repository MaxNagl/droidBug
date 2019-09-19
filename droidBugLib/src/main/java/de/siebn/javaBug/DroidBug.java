package de.siebn.javaBug;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import net.bytebuddy.android.AndroidClassLoadingStrategy;

import java.io.File;
import java.util.regex.Pattern;

import de.siebn.javaBug.NanoHTTPD.AsyncRunner;
import de.siebn.javaBug.android.*;
import de.siebn.javaBug.util.BugByteCodeUtil;

public class DroidBug extends JavaBug {
    private static boolean inited = false;
    private static boolean isAppSet = false;

    private static void ensureInit() {
        if (!inited) {
            JavaBugCore core = getCore();
            final Handler handler = new Handler(Looper.getMainLooper());
            core.setInvocationRunner(new AsyncRunner() {
                @Override
                public void exec(Runnable code) {
                    handler.post(code);
                }
            });
            addAndroidPlugins();
            inited = true;
        }
    }

    public static void addAndroidPlugins() {
        JavaBugCore jb = getCore();
        jb.addPlugin(new AndroidBugPlugin(jb));
        jb.addPlugin(new ViewBugPlugin(jb, null));
        jb.addPlugin(new ViewShotOutput(jb, false));
        jb.addPlugin(new ViewShotOutput(jb, true));
        jb.addPlugin(new ViewProfilingOutput(jb));
        jb.addPlugin(new LayoutParameterOutput(jb));
    }

    public static void setApplication(final Application app) {
        if (!isAppSet) {
            ensureInit();
            getCore().getPlugin(AndroidBugPlugin.class).setApplication(app);
            addFileRoot("filesDir", app.getFilesDir());
            addFileRoot("externalCacheDir", app.getExternalCacheDir());
            addFileRoot("externalFilesDir", app.getExternalFilesDir(null));
            addFileRoot("cacheDir", app.getCacheDir());
            try {
                BugByteCodeUtil.CLASS_LOADING_STRATEGY = new AndroidClassLoadingStrategy.Wrapping(app.getCacheDir());
                BugByteCodeUtil.CACHE_FILE = new File(app.getCacheDir(), "BugByteCodeUtil.cache");
                BugByteCodeUtil.buggedMethods.add(Pattern.compile("onMeasure"));
                BugByteCodeUtil.buggedMethods.add(Pattern.compile("setMeasuredDimension"));
                BugByteCodeUtil.buggedMethods.add(Pattern.compile("getTotalValue"));
            } catch (Throwable t) {
                // Ignore. Bytebuddy probably not available.
            }
        }
    }
}
