package com.uid2.dev;

import static com.uid2.dev.utils.BundleExKt.isEnvironmentEUID;
import static com.uid2.dev.utils.ContextExKt.getMetadata;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.uid2.EUIDManager;
import com.uid2.UID2Manager;
import com.uid2.data.UID2Identity;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/**
 * Port of <a href="https://github.com/googleads/googleads-mobile-android-examples/tree/main/java/admob/BannerExample">BannerExample</a>
 */
public class BannerActivity extends AppCompatActivity {

    private AdView adView;
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.banner_activity);

        // Load UID2Identity to test with
        loadUID2Identity();

        // Log the Mobile Ads SDK version.
        Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersion());

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        // Set your test devices. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
        // to get test ads on this device."
        MobileAds.setRequestConfiguration(
            new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
                .build());

        // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
        // values/strings.xml.
        adView = findViewById(R.id.ad_view);

        // Create an ad request.
        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        adView.loadAd(adRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
    }

    private void loadUID2Identity() {

        InputStream is = getResources().openRawResource(R.raw.uid2identity);
        StringBuilder text = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }

            String jsonString = text.toString();
            JSONObject jsonObject = new JSONObject(jsonString);
            UID2Identity fromJsonIdentity = UID2Identity.Companion.fromJson(jsonObject);

            // Emulate A UID2Identity With Valid Times
            long now = System.currentTimeMillis();
            long identityExpires = now * 60 * 60;
            long refreshFrom = now * 60 * 40;
            long refreshExpires = now * 60 * 80;

            UID2Identity identity = new UID2Identity(fromJsonIdentity.getAdvertisingToken(),
                fromJsonIdentity.getRefreshToken(),
                identityExpires,
                refreshFrom,
                refreshExpires,
                fromJsonIdentity.getRefreshResponseKey());
            if (isEnvironmentEUID(getMetadata(this))) {
                EUIDManager.getInstance().setIdentity(identity);
            } else {
                UID2Manager.getInstance().setIdentity(identity);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading Identity: " + e);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Called when leaving the activity */
    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
