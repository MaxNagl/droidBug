package de.siebn.javaBug.util;

import java.util.Locale;

public class HumanReadable {
    public final static String[] byteUnitsSi = {"B", "kB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
    public final static String[] byteUnitsBinary = {"B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB", "ZiB", "YiB"};

    public static String formatByteSizeSi(long size) {
        if (size < 1000) return size + byteUnitsSi[0];
        double f = size;
        int i = 0;
        while (f > 1000 && i < byteUnitsSi.length - 1) {
            f /= 1000;
            i++;
        }
        return String.format(Locale.getDefault(), "%.2f ", f) + byteUnitsSi[i];
    }

    public static String formatByteSizeBinary(long size) {
        if (size < 1024) return size + " " + byteUnitsBinary[0];
        double f = size;
        int i = 0;
        while (f > 1024 && i < byteUnitsBinary.length - 1) {
            f /= 1024;
            i++;
        }
        return String.format(Locale.getDefault(), "%.2f ", f) + byteUnitsBinary[i];
    }
}
