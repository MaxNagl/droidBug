package de.siebn.javaBug.android;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.plugins.RootBugPlugin;

/**
 * Created by Sieben on 04.03.2015.
 */
public class AndroidBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBugCore javaBug;
    private Application app;

    private ActivityLifecycleCallbacks lifecycleCallback = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            javaBug.getPlugin(ViewBugPlugin.class).setActivity(activity);
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    };

    public AndroidBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    public void setApplication(Application app) {
        this.app = app;
        app.registerActivityLifecycleCallbacks(lifecycleCallback);
    }

    @JavaBugCore.Serve("^/application")
    public BugElement serveViews() {
        BugSplit horizontal = new BugSplit(BugSplit.ORIENTATION_HORIZONTAL);
        return horizontal;
    }

    @Override
    public String getTabName() {
        return "Application";
    }

    @Override
    public BugElement getContent() {
        return new BugInclude("/application");
    }

    @Override
    public int getOrder() {
        return 3000;
    }
}
