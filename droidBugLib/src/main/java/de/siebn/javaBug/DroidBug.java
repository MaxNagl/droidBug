package de.siebn.javaBug;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import net.bytebuddy.android.AndroidClassLoadingStrategy;

import java.io.File;
import java.util.regex.Pattern;

import de.siebn.javaBug.NanoHTTPD.AsyncRunner;
import de.siebn.javaBug.android.*;
import de.siebn.javaBug.android.TypeAdapters.AndroidTypeAdapterProvider;
import de.siebn.javaBug.typeAdapter.TypeAdapters;
import de.siebn.javaBug.util.*;

public class DroidBug extends JavaBug {
    private static boolean isAppSet = false;

    public static void setApplication(final Application app) {
        if (!isAppSet) {
            JavaBugCore core = getCore();
            final Handler handler = new Handler(Looper.getMainLooper());
            core.setInvocationRunner(new AsyncRunner() {
                @Override
                public void exec(Runnable code) {
                    handler.post(code);
                }
            });
            BugThreadUtil.runOn = new BugThreadAndroidUtil();
            BugResourcesUtil.setResources(app.getResources());
            BugResourcesUtil.addPackage(app.getPackageName());

            TypeAdapters.addAdapterProvider(new AndroidTypeAdapterProvider());

            core.addPlugin(new AndroidBugPlugin(core, app));
            core.addPlugin(new ViewBugPlugin(core, null));
            core.addPlugin(new ViewShotOutput(core, false));
            core.addPlugin(new ViewShotOutput(core, true));
            core.addPlugin(new ViewProfilingOutput(core));
            core.addPlugin(new LayoutParameterOutput(core));
            core.addPlugin(new ExportedPropertyParameterOutput(core));


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
