package de.siebn.javaBug.plugins;


import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.Callable;

import de.siebn.javaBug.BugElement.BugText;
import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.JavaBugCore.BugPlugin;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.NanoHTTPD.Response;
import de.siebn.javaBug.NanoHTTPD.Response.Status;

/**
 * Created by Sieben on 05.03.2015.
 */
public class StreamBugPlugin implements BugPlugin {
    private final JavaBugCore javaBug;

    private HashMap<String, BugStream> streams = new HashMap<>();
    private BugStream console = createStream("console").setLimits(1000, 10);

    public StreamBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBugCore.Serve("^/stream/(.+)")
    @JavaBugCore.ServeAsync
    public Object getStream(NanoHTTPD.IHTTPSession session, String[] param) {
        BugStream stream = streams.get(param[1]);
        if (stream == null) return new Response(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "");
        Map<String, String> sessionParms = session.getParms();
        String indexString = sessionParms == null ? null : sessionParms.get("index");
        String uid = sessionParms == null ? null : sessionParms.get("uid");
        long index = stream.removed;
        if (indexString != null && (uid == null || uid.equals(stream.uid))) index = Math.max(index, Long.valueOf(indexString));

        try {
            Object o = stream.get(index, 30000);
            long next = o == null ? index : index + 1;
            if (o instanceof Callable) o = ((Callable) o).call();
            Response response = javaBug.createResponse(o);
            response.addHeader("index", String.valueOf(index));
            response.addHeader("next", String.valueOf(next));
            response.addHeader("uid", stream.uid);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public BugStream createStream(String name) {
        BugStream stream = new BugStream();
        streams.put(name, stream);
        return stream;
    }

    public BugStream getStream(String name) {
        return streams.get(name);
    }

    public static class BugStream {
        private Object token = new LinkedList<>();
        private long removed = 0;
        public final List<Object> entries = new LinkedList<>();
        public final String uid = String.valueOf(System.currentTimeMillis());

        private int limitWrite = Integer.MAX_VALUE - 100;
        private int limitRead = Integer.MAX_VALUE - 100;

        public synchronized void send(Object o) {
            entries.add(o);
            reduceToSize(limitWrite);
            notifyAll();
        }

        public synchronized Object get(long index, long timeout) {
            Object token = this.token = new Object();
            if (entries.size() <= index - removed) {
                try {
                    wait(timeout);
                } catch (InterruptedException ignored) {
                }
            }
            if (this.token == token && entries.size() > index - removed) {
                reduceToSize(limitRead);
                return entries.get((int) (index - removed));
            }
            return null;
        }

        public OutputStream createOutputStream(final String clazz) {
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    write(new byte[]{(byte) b});
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    if (clazz == null) {
                        send(new String(b, off, len));
                    } else {
                        send(new BugText(new String(b, off, len)).addClazz(clazz));
                    }
                }
            };
        }

        public BugStream setLimits(int limitWrite, int limitRead) {
            this.limitWrite = limitWrite;
            this.limitRead = limitRead;
            return this;
        }

        private void reduceToSize(int size) {
            while (entries.size() > size) {
                entries.remove(0);
                removed++;
            }
        }
    }

    public BugStream getConsoleStream() {
        return console;
    }
}
