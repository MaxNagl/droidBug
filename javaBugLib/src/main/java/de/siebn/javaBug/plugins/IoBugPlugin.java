package de.siebn.javaBug.plugins;

import java.util.ArrayList;
import java.util.HashMap;
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

    public synchronized MonitoredIo getMonitoredIo(Object id) {
        MonitoredIo mio = monitoredIosMap.get(id);
        if (mio == null) {
            mio = new MonitoredIo("io-" + BugObjectCache.getReference(id));
            monitoredIos.add(mio);
            monitoredIosMap.put(id, mio);
            mio.setTitle(id.toString());
        }
        return mio;
    }

    public class MonitoredIo {
        private final String id;
        private BugStream stream;
        private String title;
        private int opened = 0;

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

        private long getInSize() {
            long size = 0;
            for (Object entry : stream.entries) if (entry instanceof IoEntry && !((IoEntry) entry).out) size += ((IoEntry) entry).data.length;
            return size;
        }

        private long getOutSize() {
            long size = 0;
            for (Object entry : stream.entries) if (entry instanceof IoEntry && ((IoEntry) entry).out) size += ((IoEntry) entry).data.length;
            return size;
        }

        private BugElement getFormatted(Boolean outFilter) {
            BugList list = new BugList();
            for (Object entry : stream.entries) {
                if (entry instanceof IoEntry) {
                    if (outFilter != null && ((IoEntry) entry).out != outFilter) continue;
                    list.add(((IoEntry) entry).toBugElement());
                }
            }
            return list.format(BugFormat.preWrap);
        }

        public void opened(Object object) {
            opened++;
        }

        public void closed(Object object) {
            if (--opened == 0) {
                stream.close();
            }
        }

        public BugStream getStream() {
            return stream;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public static class IoEntry implements Callable<BugElement> {
        final byte[] data;
        final boolean out;
        final long time;

        public IoEntry(byte[] data, boolean out, long time) {
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
}
