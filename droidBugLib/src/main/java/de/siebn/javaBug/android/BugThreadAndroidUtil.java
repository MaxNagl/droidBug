package de.siebn.javaBug.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.*;

import de.siebn.javaBug.util.BugThreadUtil;

public class BugThreadAndroidUtil extends BugThreadUtil {
    public final static RunOn ui = new RunOnHandler(new Handler(Looper.getMainLooper()));

    private static class RunOnHandler extends RunOn {
        public final Handler handler;

        public RunOnHandler(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void async(Runnable runnable) {
            handler.post(runnable);
        }

        @Override
        public <T> T sync(Callable<T> callable) {
            if (Thread.currentThread() == handler.getLooper().getThread()) {
                return get(callable);
            } else {
                FutureTask<T> task = new FutureTask<>(callable);
                handler.post(task);
                return get(task);
            }
        }

        @Override
        public void delayed(int delayMs, Runnable runnable) {
            handler.postDelayed(runnable, delayMs);
        }

        @Override
        public <T> T delayedSync(int delayMs, Callable<T> callable) {
            if (Thread.currentThread() == handler.getLooper().getThread()) {
                sleep(delayMs);
                return get(callable);
            } else {
                FutureTask<T> task = new FutureTask<>(callable);
                handler.post(task);
                return get(task);
            }
        }
    }

}
