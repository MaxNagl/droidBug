package de.desiebn.droidbug;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.siebn.javaBug.android.ViewBugPlugin;
import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.siebn.javaBug.plugins.ClassPathBugPlugin;
import de.siebn.javaBug.JavaBug;
import de.siebn.javaBug.plugins.ObjectBugPlugin;
import de.siebn.javaBug.plugins.RootBugPlugin;
import de.siebn.javaBug.plugins.ThreadsBugPlugin;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        JavaBug jb = new JavaBug(7778);
        jb.addDefaultPlugins();

        jb.addPlugin(new ViewBugPlugin(jb, this));

        jb.getObjectBug().addRootObject(jb);

        jb.tryToStart();

        LinearLayout lin = (LinearLayout) findViewById(R.id.browserAdresses);
        for (String ipAdress : getIPAddresses()) {
            TextView tv = new TextView(this);
            String httpAdress = "http://";
            httpAdress += InetAddressUtils.isIPv4Address(ipAdress) ? ipAdress : ("[" + ipAdress + "]");
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
     * @return  address or empty string
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
        } catch (Exception ex) { } // for now eat exceptions
        Collections.sort(adresses, new Comparator<String>() {
            @Override
            public int compare(String lhs, String rhs) {
                boolean l = InetAddressUtils.isIPv4Address(lhs);
                boolean r = InetAddressUtils.isIPv4Address(lhs);
                if (l && !r) return -1;
                if (!l && r) return 1;
                return lhs.compareTo(rhs);
            }
        });
        return adresses;
    }
}
