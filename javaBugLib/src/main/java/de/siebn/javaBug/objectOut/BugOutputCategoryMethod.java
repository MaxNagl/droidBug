package de.siebn.javaBug.objectOut;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BugOutputCategoryMethod {
    String value();
    int order() default 0;
}
