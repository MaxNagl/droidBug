package de.siebn.javaBug.plugins;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import de.siebn.javaBug.*;
import de.siebn.javaBug.BugElement.*;
import de.siebn.javaBug.NanoHTTPD.Response;
import de.siebn.javaBug.NanoHTTPD.Response.Status;

public class FileBugPlugin implements RootBugPlugin.MainBugPlugin {
    private Map<File, String> roots = new LinkedHashMap<>();

    public void addRoot(String name, File root) {
        roots.put(root, name);
    }

    @Override
    public String getTabName() {
        return "Files";
    }

    @Override
    public String getTabId() {
        return "files";
    }

    @Override
    public BugElement getContent() {
        return new BugDiv().add(new BugInclude("/files/")).format(BugFormat.tabContent);
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @JavaBugCore.Serve("^/files/(.*)")
    public BugElement serverFiles(String[] param) {
        BugList list = new BugList();
        String path = param[1];
        File[] files;
        boolean showRoots = path == null || path.length() == 0;
        if (showRoots) {
            files = roots.keySet().toArray(new File[0]);
        } else {
            File file = new File(param[1]);
            if (!file.exists()) {
                return list;
            }
            files = file.listFiles();
        }
        if (files != null) {
            sortFiles(files);
            for (File file : files) {
                BugEntry f = new BugEntry();
                f.add(new BugText(file.getAbsolutePath()).format(BugFormat.file).setOnClick(BugText.ON_CLICK_EXPAND));
                if (file.isDirectory()) {
                    f.setExpandInclude("/files/" + file.getAbsolutePath());
                } else {
                    f.addSpace().add(new BugLink("[view]").setUrl("/file/" + file.getAbsolutePath()));
                    f.addSpace().add(new BugLink("[download]").setUrl("/file/" + file.getAbsolutePath() + "?download=true"));
                }
                long size = file.length();
                f.addSpace().add(BugText.getForByteSize(size));
                list.add(f);
            }
        }
        return list;
    }

    private void sortFiles(File[] files) {
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1 == null || o2 == null) return 0;
                if (o1.isDirectory() != o2.isDirectory()) return o1.isDirectory() ? -1 : 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    @JavaBugCore.Serve("^/file/(.*)")
    public NanoHTTPD.Response serverFile(String[] param, NanoHTTPD.IHTTPSession session) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(param[1]);
        InputStream in = openStream(param[1]);
        Map<String, String> parms = session.getParms();
        Response response;
        if (parms != null && parms.containsKey("tail")) {
            int tail = Integer.parseInt(parms.get("tail"));
            response = new Response(Status.OK, mimeType, getTail(in, tail));
        } else {
            response = new Response(Status.OK, mimeType, in);
        }
        if (parms != null && parms.containsKey("download")) {
            response.addHeader("Content-Disposition", "attachment");
        }
        return response;
    }

    private String getTail(InputStream in, int count) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        LinkedList<String> lines = new LinkedList<>();
        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            if (lines.size() > count) lines.remove(0);
            line = reader.readLine();
        }
        StringBuilder sb = new StringBuilder();
        for (String l : lines) sb.append(sb.length() == 0 ? "" : "\n").append(l);
        reader.close();
        return sb.toString();
    }

    private InputStream openStream(String fname) throws IOException {
        File file = new File(fname);
        if (file.exists())
            return new BufferedInputStream(new FileInputStream(file));
        file = new File("files/" + fname);
        if (file.exists())
            return new BufferedInputStream(new FileInputStream(file));
        URL url = getClass().getClassLoader().getResource(fname);
        if (url != null)
            return url.openStream();
        throw new JavaBugCore.ExceptionResult(Status.NOT_FOUND, "File not found");
    }
}
