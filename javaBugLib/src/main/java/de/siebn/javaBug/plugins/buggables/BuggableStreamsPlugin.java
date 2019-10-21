package de.siebn.javaBug.plugins.buggables;

import java.io.*;
import java.util.Arrays;

import de.siebn.javaBug.JavaBugCore;
import de.siebn.javaBug.plugins.IoBugPlugin.IoEntry;
import de.siebn.javaBug.plugins.IoBugPlugin.MonitoredIo;

public class BuggableStreamsPlugin implements BuggablePlugin {
    private final JavaBugCore javaBug;

    public BuggableStreamsPlugin(JavaBugCore javaBug) {
        this.javaBug = javaBug;
    }

    @Override
    public boolean canBug(Object object) {
        return object instanceof InputStream || object instanceof OutputStream;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T bug(T object, String title, Object key) {
        if (key == null) key = object;
        MonitoredIo mio = javaBug.getIoBugPlugin().getMonitoredIo(key);
        if (object instanceof OutputStream) {
            object = (T) new MonitoredOutputStream((OutputStream) object, mio);
        } else if (object instanceof InputStream) {
            object = (T) new MonitoredInputStream((InputStream) object, mio);
        }
        mio.opened(object);
        if (title != null) mio.setTitle(title);
        return object;
    }

    @Override
    public int getOrder() {
        return 0;
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
            mio.getStream().send(new IoEntry(new byte[]{(byte) read}, false, System.nanoTime()));
            return read;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (off == 0 && len == b.length) {
                mio.getStream().send(new IoEntry(b, false, System.nanoTime()));
            } else {
                mio.getStream().send(new IoEntry(Arrays.copyOfRange(b, off, off + len), false, System.nanoTime()));
            }
            return read;
        }

        @Override
        public void close() throws IOException {
            super.close();
            mio.closed(this);
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
            mio.getStream().send(new IoEntry(new byte[]{(byte) b}, true, System.nanoTime()));
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (off == 0 && len == b.length) {
                mio.getStream().send(new IoEntry(b, true, System.nanoTime()));
            } else {
                mio.getStream().send(new IoEntry(Arrays.copyOfRange(b, off, off + len), true, System.nanoTime()));
            }
            out.write(b, off, len);
        }

        @Override
        public void write(byte[] b) throws IOException {
            mio.getStream().send(new IoEntry(b, true, System.nanoTime()));
            out.write(b);
        }

        @Override
        public void close() throws IOException {
            super.close();
            mio.closed(this);
        }
    }
}
