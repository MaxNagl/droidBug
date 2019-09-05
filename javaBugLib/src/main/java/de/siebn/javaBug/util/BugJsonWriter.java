package de.siebn.javaBug.util;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import de.siebn.javaBug.BugElement;

public class BugJsonWriter {
    private final ByteArrayOutputStream bout = new ByteArrayOutputStream();
    private final GZIPOutputStream zout;
    private final Writer out;
    private boolean skipComma = false;

    public BugJsonWriter(boolean gzip) {
        try {
            if (gzip) {
                zout = new GZIPOutputStream(bout);
                out = new BufferedWriter(new OutputStreamWriter(zout));
            } else {
                zout = null;
                out = new BufferedWriter(new OutputStreamWriter(bout));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public interface BugJsonElement {
        void writeJsonFields(BugJsonWriter writer);
    }

    public BugJsonWriter wrtieField(String key, Object value) {
        if (value == null) return this;
        try {
            if (skipComma) {
                skipComma = false;
            } else {
                out.write(',');
            }
            out.write('"');
            out.write(key);
            out.write('"');
            out.write(':');
            writeObject(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public BugJsonWriter writeObject(Object object) {
        try {
            if (object instanceof BugElement) {
                out.write("{\"type\":\"");
                out.write(((BugElement) object).type);
                out.write('"');
                ((BugElement) object).writeJsonFields(this);
                out.write('}');
            } else if (object instanceof Number || object instanceof Boolean) {
                out.write(String.valueOf(object));
            } else if (object instanceof List) {
                boolean first = true;
                out.write('[');
                for (Object o : (List) object) {
                    if (!first) out.write(',');
                    writeObject(o);
                    first = false;
                }
                out.write(']');
            } else if (object instanceof BugJsonElement) {
                out.write('{');
                skipComma = true;
                ((BugJsonElement) object).writeJsonFields(this);
                skipComma = false;
                out.write('}');
            } else if (object instanceof Map) {
                boolean first = true;
                out.write('{');
                for (Map.Entry e : (Set<Entry>) ((Map) object).entrySet()) {
                    if (!first) out.write(',');
                    out.write('"');
                    out.write(e.getKey().toString());
                    out.write('"');
                    out.write(':');
                    writeObject(e.getValue());
                    first = false;
                }
                out.write('}');
            } else {
                out.write('"');
                writeEscaped(String.valueOf(object));
                out.write('"');
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public String getString() {
        try {
            out.flush();
            if (zout != null) zout.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bout.toString();
    }

    public byte[] getBytes() {
        try {
            out.flush();
            if (zout != null) zout.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bout.toByteArray();
    }

    private void writeEscaped(String s) throws Exception {
        final int len = s.length();
        for (int i = 0; i < len; i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    out.write('\\');
                    out.write('"');
                    break;
                case '\\':
                    out.write('\\');
                    out.write('\\');
                    break;
                case '\b':
                    out.write('\\');
                    out.write('b');
                    break;
                case '\f':
                    out.write('\\');
                    out.write('f');
                    break;
                case '\n':
                    out.write('\\');
                    out.write('n');
                    break;
                case '\r':
                    out.write('\\');
                    out.write('r');
                    break;
                case '\t':
                    out.write('\\');
                    out.write('t');
                    break;
                case '/':
                    out.write('\\');
                    out.write('/');
                    break;
                default:
                    if (ch <= 0x1F || ch >= 0x7F) {
                        String ss = Integer.toHexString(ch);
                        out.write('\\');
                        out.write('u');
                        for (int k = 0; k < 4 - ss.length(); k++) out.write('0');
                        out.write(ss.toUpperCase());
                    } else {
                        out.write(ch);
                    }
            }
        }
    }
}
