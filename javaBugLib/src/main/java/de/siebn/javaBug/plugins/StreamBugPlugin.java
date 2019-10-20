package de.siebn.javaBug.plugins;


import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.BugText;
import de.siebn.javaBug.JavaBugCore.BugPlugin;
import de.siebn.javaBug.NanoHTTPD.Response;
import de.siebn.javaBug.NanoHTTPD.Response.Status;
import de.siebn.javaBug.util.BugJsonWriter;

/**
 * Created by Sieben on 05.03.2015.
 */
public class StreamBugPlugin implements BugPlugin {
    private final static Object CLOSED = new Object();
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
            ArrayList<BugElement> values = new ArrayList<>();
            while (o != null) {
                o = stream.get(index, 0);
                Object v = (o instanceof Callable) ? ((Callable) o).call() : o;
                if (v instanceof BugElement) {
                    values.add((BugElement) v);
                    index++;
                } else if (values.isEmpty()) {
                    if (o == CLOSED) return new Response(Status.GONE, NanoHTTPD.MIME_PLAINTEXT, "");
                    Response response = javaBug.createResponse(o);
                    response.addHeader("next", String.valueOf(index + 1));
                    response.addHeader("uid", stream.uid);
                    return response;
                } else {
                    break;
                }
            }
            Response response;
            if (values.size() == 0) {
                response = javaBug.createResponse(null);
            } else if (values.size() == 1) {
                response = javaBug.createResponse(values.get(0));
            } else {
                response = new Response(Status.OK, "application/json", new ByteArrayInputStream(new BugJsonWriter(true).writeObject(values).getBytes()));
                response.addHeader("Content-Encoding", "gzip");
                response.setChunkedTransfer(true);
            }
            response.addHeader("next", String.valueOf(index));
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
            if (timeout > 0 && entries.size() <= index - removed) {
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

        public void close() {
            send(CLOSED);
        }
    }

    public BugStream getConsoleStream() {
        return console;
    }
}
