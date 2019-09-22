package de.siebn.javaBug.plugins;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.objectOut.OutputMethod;

/**
 * Created by Sieben on 16.11.2016.
 */

public class IoBugPlugin implements RootBugPlugin.MainBugPlugin {
    private final JavaBugCore javaBug;

    private ArrayList<MonitoredIo> monitoredIos = new ArrayList<>();
    private HashMap<Object, MonitoredIo> monitoredIosMap = new HashMap<>();

    public IoBugPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    @JavaBugCore.Serve("^/io/")
    public synchronized BugElement serveIos() {
        BugList list = new BugList();
        for (MonitoredIo mio : monitoredIos) {
            BugEntry entry = new BugEntry();
            entry.add(new BugText(mio.title).setOnClick(BugElement.ON_CLICK_EXPAND).format(BugFormat.title));
            entry.addText(" In: ");
            entry.add(BugText.getForByteSize(mio.in.bout.size()));
            entry.addText(" Out: ");
            entry.add(BugText.getForByteSize(mio.out.bout.size()));
            entry.setExpandInclude(ObjectBugPlugin.getObjectDetailsLink(mio));
            list.add(entry);
        }
        return list;
    }

    @Override
    public String getTabName() {
        return "I/O";
    }

    @Override
    public BugElement getContent() {
        return new BugDiv().add(new BugInclude("/io/")).format(BugFormat.tabContent);
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
        public BugElement overview() {
            BugList list = new BugList();
            list.add(new BugEntry().add(new BugText("Input: ")).add(BugText.getForByteSize(in.bout.size())));
            list.add(new BugEntry().add(new BugText("Output: ")).add(BugText.getForByteSize(out.bout.size())));
            return list;
        }

        @OutputMethod(value = "Input", order = 100)
        public BugElement input() {
            return new BugPre(new String(in.bout.toByteArray()));
        }

        @OutputMethod(value = "Output", order = 200)
        public BugElement output() {
            return new BugPre(new String(out.bout.toByteArray()));
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
