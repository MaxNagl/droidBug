package de.desiebn.droidbug;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.NanoHTTPD.AsyncRunner;
import de.siebn.javaBug.android.LayoutParameterOutput;
import de.siebn.javaBug.android.ViewBugPlugin;
import de.siebn.javaBug.android.ViewShotOutput;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JavaBug jb = new JavaBug(7778);
        jb.addDefaultPlugins();

        jb.getFileBug().addRoot("files", getFilesDir());
        jb.getFileBug().addRoot("external files", getExternalFilesDir(null));

        jb.addPlugin(new ViewBugPlugin(jb, this));
        jb.addPlugin(new ViewShotOutput(jb));
        jb.addPlugin(new LayoutParameterOutput(jb));

        jb.getObjectBug().addRootObject("DroidBug", jb);

        jb.tryToStart();

        jb.setInvocationRunner(new AsyncRunner() {
            @Override
            public void exec(Runnable code) {
                runOnUiThread(code);
            }
        });

        LinearLayout lin = (LinearLayout) findViewById(R.id.browserAdresses);
        for (String ipAdress : getIPAddresses()) {
            TextView tv = new TextView(this);
            String httpAdress = "http://";
            httpAdress += isIPv4Address(ipAdress) ? ipAdress : ("[" + ipAdress + "]");
            httpAdress += ":" + jb.getListeningPort();
            tv.setText(httpAdress);
            tv.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Medium);
            final String finalHttpAdress = httpAdress;
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(Intent.parseUri(finalHttpAdress, 0));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            });
            lin.addView(tv);
        }
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @return address or empty string
     */
    public static ArrayList<String> getIPAddresses() {
        ArrayList<String> adresses = new ArrayList<>();
        try {
            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        adresses.add(sAddr.split("%")[0]);
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        Collections.sort(adresses, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                boolean l = isIPv4Address(lhs);
                boolean r = isIPv4Address(lhs);
                if (l && !r) return -1;
                if (!l && r) return 1;
                return lhs.compareTo(rhs);
            }
        });
        return adresses;
    }

    private static boolean isIPv4Address(String addr) {
        try {
            return InetAddress.getByName(addr) instanceof Inet4Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
