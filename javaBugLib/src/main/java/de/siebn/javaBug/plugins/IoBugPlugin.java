package de.siebn.javaBug.plugins;

import com.sun.corba.se.impl.orbutil.ObjectUtility;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.objectOut.BugOutputCategoryMethod;
import de.siebn.javaBug.plugins.StreamBugPlugin.BugStream;
import de.siebn.javaBug.util.BugObjectCache;

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
            entry.add(BugText.getForByteSize(mio.getInSize()));
            entry.addText(" Out: ");
            entry.add(BugText.getForByteSize(mio.getOutSize()));
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
            mio = new MonitoredIo("io-" + BugObjectCache.getReference(id));
            monitoredIos.add(mio);
            monitoredIosMap.put(id, mio);
        }
        return mio;
    }

    public InputStream wrapInputStream(Object id, InputStream in) {
        return new MonitoredInputStream(in, getMonitoredIo(id));
    }

    public OutputStream wrapOutputStream(Object id, OutputStream out) {
        return new MonitoredOutputStream(out, getMonitoredIo(id));
    }

    public void setTitle(Object id, String title) {
        getMonitoredIo(id).title = title;
    }

    private class MonitoredIo {
        private final String id;
        private BugStream stream;
        private String title;

        public MonitoredIo(String id) {
            this.id = id;
            stream = javaBug.getStreamBugPlugin().createStream(id);
        }

        @BugOutputCategoryMethod("Overview")
        public BugElement overview() {
            BugList list = new BugList();
            list.add(new BugEntry().add(new BugText("Input: ")).add(BugText.getForByteSize(getInSize())));
            list.add(new BugEntry().add(new BugText("Output: ")).add(BugText.getForByteSize(getOutSize())));
            return list;
        }

        @BugOutputCategoryMethod(value = "Input", order = 100)
        public BugElement input() {
            return getFormatted(false);
        }

        @BugOutputCategoryMethod(value = "Output", order = 200)
        public BugElement output() {
            return getFormatted(true);
        }

        @BugOutputCategoryMethod(value = "In-/Output", order = 300)
        public BugElement inStream() {
            return new BugList().setStream("stream/" + id).format(BugFormat.inlineStream);
        }

        @SuppressWarnings("unchecked")
        private List<IoEntry> getEntries() {
            return (List<IoEntry>) ((List) stream.entries);
        }

        private long getInSize() {
            long size = 0;
            for (IoEntry entry : getEntries()) if (!entry.out) size += entry.data.length;
            return size;
        }

        private long getOutSize() {
            long size = 0;
            for (IoEntry entry : getEntries()) if (entry.out) size += entry.data.length;
            return size;
        }

        private BugElement getFormatted(Boolean outFilter) {
            BugList list = new BugList();
            for (IoEntry entry : getEntries()) {
                if (outFilter != null && entry.out != outFilter) continue;
                list.add(entry.toBugElement());
            }
            return list.format(BugFormat.preWrap);
        }
    }

    private class IoEntry implements Callable<BugElement> {
        final byte[] data;
        final boolean out;
        final long time;

        private IoEntry(byte[] data, boolean out, long time) {
            this.data = data;
            this.out = out;
            this.time = time;
        }

        private BugElement toBugElement() {
            return new BugText(new String(data)).format(out ? BugFormat.colorSecondaryLight : BugFormat.colorTernary);
        }

        @Override
        public BugElement call() {
            return toBugElement();
        }
    }

    private class MonitoredInputStream extends FilterInputStream {
        private final MonitoredIo mio;

        MonitoredInputStream(InputStream in, MonitoredIo mio) {
            super(in);
            this.mio = mio;
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            mio.stream.send(new IoEntry(new byte[]{(byte) read}, false, System.nanoTime()));
            return read;
        }

        public int read(byte[] data, int offset, int length) throws IOException {
            int read = super.read(data, offset, length);
            mio.stream.send(new IoEntry(data, false, System.nanoTime()));
            return read;
        }
    }

    private class MonitoredOutputStream extends OutputStream {
        private final OutputStream out;
        private final MonitoredIo mio;

        MonitoredOutputStream(OutputStream out, MonitoredIo mio) {
            this.out = out;
            this.mio = mio;
        }

        @Override
        public void write(int b) throws IOException {
            mio.stream.send(new IoEntry(new byte[]{(byte) b}, true, System.nanoTime()));
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (off == 0 && len == b.length) {
                mio.stream.send(new IoEntry(b, true, System.nanoTime()));
            } else {
                mio.stream.send(new IoEntry(Arrays.copyOfRange(b, off, off + len), true, System.nanoTime()));
            }
            out.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            mio.stream.send(new IoEntry(b, true, System.nanoTime()));
            out.write(b);
        }
    }
}
