package de.siebn.javaBug.testApplication;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.objectOut.AbstractOutputCategory;
import de.siebn.javaBug.util.BugByteCodeUtil;

/**
 * Created by Sieben on 20.03.2015.
 */
public class RecursiveOutputCatergory extends AbstractOutputCategory {

    public RecursiveOutputCatergory(JavaBugCore javaBug) {
        super(javaBug, "recursion", "Recursion", 0);
    }

    @OutputMethod("profile")
    public BugElement profile(final RecursiveTestClass o) {
        return getProfileElement(BugByteCodeUtil.profile(
                new Runnable() {
                    @Override
                    public void run() {
                        o.getTotalValue();
                    }
                }, null)
        );
    }

    @Override
    public boolean canOutputClass(Class<?> clazz) {
        return RecursiveTestClass.class.isAssignableFrom(clazz);
    }
}
