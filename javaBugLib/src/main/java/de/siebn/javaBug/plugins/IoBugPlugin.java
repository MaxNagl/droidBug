package de.siebn.javaBug.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale.Builder;
import java.util.Set;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.objectOut.ListItemBuilder;
import de.siebn.javaBug.objectOut.OutputMethod;
import de.siebn.javaBug.util.HumanReadable;
import de.siebn.javaBug.util.XML;

/**
 * Created by Sieben on 16.11.2016.
 */

public class IoBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBug javaBug;

    private ArrayList<MonitoredIo> monitoredIos = new ArrayList<>();
    private HashMap<Object, MonitoredIo> monitoredIosMap = new HashMap<>();

    public IoBugPlugin(JavaBug javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBug.Serve("^/io/")
    public synchronized String serveIos() {
        XML xhtml = new XML();
        XML ul = xhtml.add("ul");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        for (MonitoredIo mio : monitoredIos) {
            ListItemBuilder builder = new ListItemBuilder();
            //builder.createValue().setValue(mio);
            builder.setName(mio.title);
            builder.addColumn().setText("In: " + HumanReadable.formatByteSizeBinary(mio.in.bout.size()));
            builder.addColumn().setText("Out: " + HumanReadable.formatByteSizeBinary(mio.out.bout.size()));
            builder.setExpandObject(javaBug.getObjectBug(), mio, mio.getClass());
            builder.build(ul);
        }
        return xhtml.getHtml();
    }

    @Override
    public String getTabName() {
        return "I/O";
    }

    @Override
    public String getUrl() {
        return "/io/";
    }

    @Override
    public String getTagClass() {
        return "io";
    }

    @Override
    public int getOrder() {
        return 2000;
    }

    private synchronized MonitoredIo getMonitoredIo(Object id) {
        MonitoredIo mio = monitoredIosMap.get(id);
        if (mio == null) {
            mio = new MonitoredIo();
            monitoredIos.add(mio);
            monitoredIosMap.put(id, mio);
        }
        return mio;
    }

    public InputStream wrapInputStream(Object id, InputStream in) {
        MonitoredInputStream mIn = new MonitoredInputStream(in);
        getMonitoredIo(id).in = mIn;
        return mIn;
    }

    public OutputStream wrapOutputStream(Object id, OutputStream out) {
        MonitoredOutputStream mOut = new MonitoredOutputStream(out);
        getMonitoredIo(id).out = mOut;
        return mOut;
    }

    public void setTitle(Object id, String title) {
        getMonitoredIo(id).title = title;
    }

    private class MonitoredIo {
        private MonitoredInputStream in;
        private MonitoredOutputStream out;
        private String title;

        @OutputMethod("Overview")
        public void overview(XML xml) {
            ListItemBuilder builder = new ListItemBuilder();
            builder.setName("Input");
            builder.createValue().setValue(HumanReadable.formatByteSizeBinary(in.bout.size()));
            builder.build(xml);

            builder = new ListItemBuilder();
            builder.setName("Output");
            builder.createValue().setValue(HumanReadable.formatByteSizeBinary(out.bout.size()));
            builder.build(xml);
        }

        @OutputMethod(value = "Input", order = 100)
        public void input(XML xml) {
            xml.add("li").add("pre").appendText(new String(in.bout.toByteArray()));
        }

        @OutputMethod(value = "Output", order = 200)
        public void output(XML xml) {
            xml.add("li").add("pre").appendText(new String(out.bout.toByteArray()));
        }
    }

    private class MonitoredInputStream extends FilterInputStream {
        private ByteArrayOutputStream bout = new ByteArrayOutputStream();

        protected MonitoredInputStream(InputStream in) {
            super(in);
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            bout.write(read);
            return read;
        }

        public int read(byte[] data, int offset, int length) throws IOException {
            int read = super.read(data, offset, length);
            bout.write(data, offset, read);
            return read;
        }
    }

    private class MonitoredOutputStream extends FilterOutputStream {
        private ByteArrayOutputStream bout = new ByteArrayOutputStream();

        public MonitoredOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            bout.write(b);
            super.write(b);
        }
    }
}
