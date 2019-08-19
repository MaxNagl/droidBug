package de.siebn.javaBug.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import de.siebn.javaBug.BugElement;
import de.siebn.javaBug.BugElement.BugLink;
import de.siebn.javaBug.BugElement.BugList;
import de.siebn.javaBug.BugElement.BugText;
import de.siebn.javaBug.BugExpandableEntry;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.NanoHTTPD.Response;
import de.siebn.javaBug.NanoHTTPD.Response.Status;
import de.siebn.javaBug.objectOut.ListItemBuilder;
import de.siebn.javaBug.util.HumanReadable;
import de.siebn.javaBug.util.JsUtil;
import de.siebn.javaBug.util.XML;

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
    public String getUrl() {
        return "/files";
    }

    @Override
    public String getTagClass() {
        return "files";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @JavaBug.Serve("^/filesJson/(.*)")
    public BugElement serverFilesJson(String[] param) throws IOException {
        BugList list = new BugList();
        String path = param[1];
        File files[];
        boolean showRoots = path == null || path.length() == 0;
        if (showRoots) {
            files = roots.keySet().toArray(new File[0]);
        } else {
            File file = new File(param[1]);
            if (!file.exists()) {
//                xml.appendText("Not Found.");
                return list;
            }
            files = file.listFiles();
        }
        if (files != null) {
            sortFiles(files);
            for (File file : files) {
                BugExpandableEntry f = new BugExpandableEntry();
                f.title = new BugText(file.getAbsolutePath());
                if (file.isDirectory()) {
                    f.expand = "/filesJson/" + file.getAbsolutePath();
                } else {
                    f.elements.add(new BugLink("[view]").setUrl("/file/" + file.getAbsolutePath()).setClazz("link"));
                    f.elements.add(new BugLink("[download]").setUrl("/fileDownload/" + file.getAbsolutePath()).setClazz("link"));
                }
                long size = file.length();
                f.elements.add(new BugText(HumanReadable.formatByteSizeBinary(size)).setTooltip("size").setClazz("size"));
                list.elements.add(f);
            }
        }
        return list;
    }

    //@JavaBug.Serve("^/files(.*)")
    public String serverFiles(String[] param) throws IOException {
        XML xml = new XML();
        String path = param[1];
        File files[];
        boolean showRoots = path == null || path.length() == 0;
        if (showRoots) {
            files = roots.keySet().toArray(new File[0]);
        } else {
            File file = new File(param[1]);
            if (!file.exists()) {
                xml.appendText("Not Found.");
                return xml.getXml();
            }
            files = file.listFiles();
        }
        if (files != null) {
            sortFiles(files);
            for (File file : files) {
                ListItemBuilder builder = new ListItemBuilder();
                String name = file.getAbsolutePath();
                if (showRoots) name = roots.get(file) + " (" + name + ")";
                builder.setName(name);
                if (file.isDirectory()) {
                    builder.setExpandLink("/files" + file.getAbsolutePath());
                } else {
                    long size = file.length();
                    builder.addColumn().setText(HumanReadable.formatByteSizeBinary(size)).setClass("byteSize");
                    builder.addColumn().setText("[download]").setClass("download").setLink("/file/" + file.getAbsolutePath());
                    builder.addColumn().setText("[tail]").setClass("tail").setAppendLink("/fileIframe/" + file.getAbsolutePath() + "?tail=10");
                }
                builder.build(xml);
            }
        }
        return xml.getXml();
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

    @JavaBug.Serve("^/fileIframe/(.*)")
    public String serverFileIframe(String[] param, NanoHTTPD.IHTTPSession session) throws IOException {
        XML span = new XML("span");
        XML iframe = span.add("iframe");
        String id = UUID.randomUUID().toString();
        iframe.setId(id);
        String src = param[0].replace("fileIframe", "file");
        String query = session.getQueryParameterString();
        if (query != null) src += "?" + query;
        iframe.setAttr("src", src);
        XML refresh = span.add("span");
        refresh.appendText(" \u27F3");
        String jQueryIframe = JsUtil.getJQuerySelector(iframe);
        refresh.setAttr("onclick", jQueryIframe + ".attr(\"src\", " + jQueryIframe + ".attr(\"src\"))");
        return span.getXml();
    }

    @JavaBug.Serve("^/file/(.*)")
    public NanoHTTPD.Response serverFile(String[] param, NanoHTTPD.IHTTPSession session) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(param[1]);
        InputStream in = openStream(param[1]);
        Map<String, String> parms = session.getParms();
        if (parms != null && parms.containsKey("tail")) {
            int tail = Integer.parseInt(parms.get("tail"));
            return new Response(Status.OK, mimeType, getTail(in, tail));
        }
        return new Response(Status.OK, mimeType, in);
    }

    @JavaBug.Serve("^/fileDownload/(.*)")
    public NanoHTTPD.Response serverFileDownload(String[] param, NanoHTTPD.IHTTPSession session) throws IOException {
        Response response = serverFile(param, session);
        response.addHeader("Content-Disposition", "attachment");
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
        throw new JavaBug.ExceptionResult(Status.NOT_FOUND, "File not found");
    }
}
