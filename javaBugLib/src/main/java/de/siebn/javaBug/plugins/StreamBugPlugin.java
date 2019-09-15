package de.siebn.javaBug.plugins;


import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;

import de.siebn.javaBug.BugElement.BugText;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.JavaBugCore.BugPlugin;
import de.siebn.javaBug.NanoHTTPD;

/**
 * Created by Sieben on 05.03.2015.
 */
public class StreamBugPlugin implements BugPlugin {
    private final JavaBugCore javaBug;

    private BugStream console = new BugStream();

    public StreamBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBugCore.Serve("^/stream/")
    @JavaBugCore.ServeAsync
    public Object getStream(NanoHTTPD.IHTTPSession session) {
        Object token = new Object();
        try {
            console.token = token;
            console.waitIfEmpty(30000);
            return console.get(token);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public static class BugStream {
        Object token = new LinkedList<>();
        Queue<Object> queue = new LinkedList<>();

        public synchronized void setToken(Object token) {
            this.token = token;
        }

        public synchronized void send(Object o) {
            queue.add(o);
            notifyAll();
        }

        public synchronized void waitIfEmpty(long timeout) {
            if (queue.isEmpty()) {
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                }
            }
        }

        public synchronized Object get(Object token) {
            if (this.token == token) return queue.poll();
            return null;
        }

        public OutputStream createOutputStream() {
            return createOutputStream(new OutputStreamSender());
        }

        public OutputStream createOutputStreamWithClazz(String clazz) {
            return createOutputStream(new OutputStreamFormatedSender(clazz));
        }

        public OutputStream createOutputStream(final OutputStreamSender adapter) {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    write(new byte[]{(byte) b});
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    adapter.send(BugStream.this, b, off, len);
                }
            };
        }

        public static class OutputStreamSender {
            public void send(BugStream bugStream, byte[] b, int off, int len) {
                bugStream.send(new String(b, off, len));
            }
        }

        public static class OutputStreamFormatedSender extends OutputStreamSender {
            public String clazz;

            public OutputStreamFormatedSender(String clazz) {
                this.clazz = clazz;
            }

            public void send(BugStream bugStream, byte[] b, int off, int len) {
                bugStream.send(new BugText(new String(b, off, len)).addClazz(clazz));
            }
        }
    }

    public BugStream getConsoleStream() {
        return console;
    }
}
