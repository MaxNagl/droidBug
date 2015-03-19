package de.siebn.javaBug.testApplication;

/**
 * Created by Sieben on 09.03.2015.
 */
public class TestClass {
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

    public String test(String text) {
        return "Called with: \"" + text + "\"";
    }
}
