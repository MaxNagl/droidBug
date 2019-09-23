package de.siebn.droidbug;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.net.URISyntaxException;

import de.siebn.javaBug.DroidBug;
import de.siebn.javaBug.android.BugLayoutInflaterFactory;
import de.siebn.javaBug.util.BugByteCodeUtil;


public class MainActivity extends Activity {
    private Button startBtn;
    private LinearLayout browserAddresses;
    private View started;
    private CheckBox autoStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DroidBug.setApplication(getApplication());
        DroidBug.addRootObject("DroidBug", DroidBug.getCore());
        DroidBug.addRootObject("BuggedPoint", BugByteCodeUtil.getBuggedInstance(Point.class));

        setContentView(BugLayoutInflaterFactory.wrapInflater(getLayoutInflater()).inflate(R.layout.activity_main, null));

        browserAddresses = findViewById(R.id.browserAddresses);
        startBtn = findViewById(R.id.start);
        started = findViewById(R.id.started);
        autoStart = findViewById(R.id.autoStart);

        startBtn.setOnClickListener(v -> {
            if (DroidBug.isStarted()) {
                stopDroidBug();
            } else {
                startDroidBug();
            }
        });

        if (getPreferences(MODE_PRIVATE).getBoolean("autoStart", false)) {
            autoStart.setChecked(true);
            startDroidBug();
        }

        autoStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            getPreferences(MODE_PRIVATE).edit().putBoolean("autoStart", isChecked).apply();
        });
    }

    public void startDroidBug() {
        DroidBug.start();
        startBtn.setText(R.string.stop);

        for (String ipAddress : DroidBug.getIPAddresses(false)) {
            TextView tv = BugByteCodeUtil.getBuggedInstance(TextView.class, this);
            tv.setText(ipAddress);
            tv.setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Medium);
            tv.setTextColor(Color.BLACK);
            tv.setPadding(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.margin_half));
            final String finalHttpAdress = ipAddress;
            tv.setOnClickListener(v -> {
                try {
                    startActivity(Intent.parseUri(finalHttpAdress, 0));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            });
            browserAddresses.addView(tv);
            started.setVisibility(View.VISIBLE);
        }
    }

    public void stopDroidBug() {
        DroidBug.stop();
        startBtn.setText(R.string.start);
        browserAddresses.removeAllViews();
        started.setVisibility(View.GONE);
    }
}
