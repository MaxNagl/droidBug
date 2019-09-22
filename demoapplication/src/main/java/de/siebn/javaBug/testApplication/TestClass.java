package de.siebn.javaBug.testApplication;

import java.time.DayOfWeek;

/**
 * Created by Sieben on 09.03.2015.
 */
public class TestClass extends TestBaseClass {
    public String publicString = "public";
    private String privateString = "private";
    public final String publicFinalString = "FINAL";
    private final String privateFinalString = "FINAL";
    public final static String publicFinalStaticString = "FINAL";
    private final static String privateFinalStaticString = "FINAL";
    public static String publicStaticString = "FINAL";
    private static String privateStaticString = "FINAL";
    transient String transientString;
    volatile public String volatileString;

    public String[] arrayString = {"A", "B", "C"};

    public int primitiveInt;
    public Integer objectInteger;

    private String getter = "G";
    private String setter = "S";
    private String getterAndSetter = "SG";

    private DayOfWeek day = DayOfWeek.MONDAY;

    public String override = "";

    public String test(String text) {
        return "Called with: \"" + text + "\"";
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public int nonParsable(TestClass nonParsable) {
        return 7;
    }

    public String getGetter() {
        return getter;
    }

    public void setSetter(String setter) {
        this.setter = setter;
    }

    public DayOfWeek getDay() {
        return day;
    }

    public void setDay(DayOfWeek day) {
        this.day = day;
    }

    public String getGetterAndSetter() {
        return getterAndSetter;
    }

    public void setGetterAndSetter(String getterAndSetter) {
        this.getterAndSetter = getterAndSetter;
    }

    public static String getStatic() {
        return "static";
    }
}
