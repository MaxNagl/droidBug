package de.siebn.javaBug.plugins;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD;

import java.io.*;
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
        File file = new File(param[1]);
        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, Files.probeContentType(file.toPath()), new FileInputStream(file));
    }
}
