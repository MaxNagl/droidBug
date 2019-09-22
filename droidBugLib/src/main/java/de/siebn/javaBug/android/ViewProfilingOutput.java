package de.siebn.javaBug.android;

import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;

import java.util.List;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.*;
import de.siebn.javaBug.objectOut.BugAnnotatedOutputCategory.OutputMethod;
import de.siebn.javaBug.util.BugByteCodeUtil;
import de.siebn.javaBug.util.BugByteCodeUtil.MethodCall;
import de.siebn.javaBug.util.BugByteCodeUtil.ProfileCallback;

public class ViewProfilingOutput extends BugAnnotatedOutputCategory {
    public ViewProfilingOutput(JavaBugCore javaBug) {
        super(javaBug, "viewProfile", "View Profiling", 200);
    }

    @OutputMethod("profile")
    public BugElement profile(final View v) {
        return getProfileElement(BugByteCodeUtil.profile(
                new Runnable() {
                    @Override
                    public void run() {
                        forceLayoutRec(v);
                        v.measure(MeasureSpec.makeMeasureSpec(v.getWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(v.getHeight(), MeasureSpec.EXACTLY));
                    }
                }, new ProfileCallback() {
                    @Override
                    public void methodCalled(MethodCall methodCall) {
                        View view = (View) methodCall.object;
                        methodCall.returnValue = MeasureSpec.toString(view.getMeasuredWidthAndState()).substring(13) + ", " + MeasureSpec.toString(view.getMeasuredHeightAndState()).substring(13);
                        methodCall.arguments[0] = MeasureSpec.toString((Integer) methodCall.arguments[0]).substring(13);
                        methodCall.arguments[1] = MeasureSpec.toString((Integer) methodCall.arguments[1]).substring(13);
                        super.methodCalled(methodCall);
                    }
                }));
    }

    private void forceLayoutRec(View v) {
        v.forceLayout();
        if (v instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                forceLayoutRec(((ViewGroup) v).getChildAt(i));
            }
        }
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return View.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean opened(List<BugOutputCategory> others, boolean alreadyOpened) {
        return false;
    }
}
