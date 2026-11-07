
package com.makdad.anatomy;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebView;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private WebView webView;
    private LinearLayout splash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.rgb(9, 11, 14));
        getWindow().setNavigationBarColor(Color.rgb(9, 11, 14));

        showSplash();
    }

    private void showSplash() {

        splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.rgb(9, 11, 14));

        TextView symbol = new TextView(this);
        symbol.setText("⚕");
        symbol.setTextSize(76);
        symbol.setTextColor(Color.rgb(212, 175, 55));
        symbol.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("مقداد أناتومي");
        title.setTextSize(32);
        title.setTextColor(Color.rgb(212, 175, 55));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);

        TextView subtitle = new TextView(this);
        subtitle.setText("MAKDAD ANATOMY");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.WHITE);
        subtitle.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText("رحلتك إلى فهم جسم الإنسان");
        description.setTextSize(15);
        description.setTextColor(Color.LTGRAY);
        description.setGravity(Gravity.CENTER);

        splash.addView(symbol);
        splash.addView(title);
        splash.addView(subtitle);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.topMargin = 24;
        splash.addView(description, params);

        setContentView(splash);

        splash.postDelayed(() -> openMainPage(), 2500);
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
