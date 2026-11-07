
package com.makdad.anatomy;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.os.Handler;
import android.os.Looper;

import java.io.InputStream;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(9, 11, 14));
        getWindow().setNavigationBarColor(Color.rgb(9, 11, 14));

        showSplash();
    }

    private void showSplash() {

        ImageView image = new ImageView(this);
        image.setBackgroundColor(Color.rgb(9, 11, 14));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);

        try {
            InputStream stream = getAssets().open(
                "file_00000000e378820aaf66eca8b0dc691e.png"
            );

            Drawable picture = Drawable.createFromStream(stream, null);
            image.setImageDrawable(picture);
            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(image);

        new Handler(Looper.getMainLooper()).postDelayed(
            () -> openMainPage(), 3000
        );
    }

    private void openMainPage() {

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
