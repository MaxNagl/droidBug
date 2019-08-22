package de.siebn.javaBug.plugins;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.BugEntry;
import de.siebn.javaBug.BugElement.BugGroup;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.BugElement.BugPre;
import de.siebn.javaBug.BugElement.BugText;
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

    @JavaBug.Serve("^/ioJson/")
    public synchronized BugElement serveIosJson() {
        BugList list = new BugList();
        for (MonitoredIo mio : monitoredIos) {
            BugEntry entry = new BugEntry();
            entry.add(new BugText(mio.title).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.title));
            entry.addText(" In: ");
            entry.add(BugText.getForByteSize(mio.in.bout.size()));
            entry.addText(" Out: ");
            entry.add(BugText.getForByteSize(mio.out.bout.size()));
            entry.setExpand(javaBug.getObjectBug().getObjectDetailsLinkJson(mio));
            list.add(entry);
        }
        return list;
    }

    @JavaBug.Serve("^/io/")
    public synchronized String serveIos() {
        XML xhtml = new XML();
        XML ul = xhtml.add("ul");
        for (MonitoredIo mio : monitoredIos) {
            ListItemBuilder builder = new ListItemBuilder();
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
    public String getContentUrl() {
        return "/ioJson/";
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
        public void overview(BugGroup parent) {
            BugList list = new BugList();
            list.add(new BugEntry().add(new BugText("Input: ")).add(BugText.getForByteSize(in.bout.size())));
            list.add(new BugEntry().add(new BugText("Output: ")).add(BugText.getForByteSize(out.bout.size())));
            parent.add(list);
        }

        @OutputMethod(value = "Input", order = 100)
        public void input(BugGroup parent) {
            parent.add(new BugPre(new String(in.bout.toByteArray())));
        }

        @OutputMethod(value = "Output", order = 200)
        public void output(BugGroup parent) {
            parent.add(new BugPre(new String(out.bout.toByteArray())));
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

    private class MonitoredOutputStream extends OutputStream {
        private final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        private final OutputStream out;

        public MonitoredOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            bout.write(b);
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            bout.write(b, off, len);
            out.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            bout.write(b);
            out.write(b);
        }
    }
}
