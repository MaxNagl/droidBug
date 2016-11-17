package de.siebn.javaBug.objectOut;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.util.XML;

@Retention(RetentionPolicy.RUNTIME)
public @interface OutputMethod {
    String value();
    int order() default 0;
}
