package de.siebn.javaBug.plugins;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD;
import de.siebn.javaBug.NanoHTTPD.Response.Status;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Sieben on 19.03.2015.
 */
public class FileBugPlugin implements RootBugPlugin.MainBugPlugin {
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

    @JavaBug.Serve("^/file/(.*)")
    public NanoHTTPD.Response serverFile(String[] param) throws IOException {
        String mimeType = URLConnection.guessContentTypeFromName(param[1]);
        File file = new File(param[1]);
        if (file.exists()) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, mimeType, new FileInputStream(file));
        file = new File("files/" + param[1]);
        if (file.exists()) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, mimeType, new FileInputStream(file));
        URL url = getClass().getClassLoader().getResource(param[1]);
        if (url != null) return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, mimeType, url.openStream());
        throw new JavaBug.ExceptionResult(Status.NOT_FOUND, "File not found");
    }
}
