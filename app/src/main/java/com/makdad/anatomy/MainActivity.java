
package com.makdad.anatomy;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private WebView webView;
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        showSplash();
    }

    private void showSplash() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(5, 10, 20));

        ImageView background = new ImageView(this);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);

        try {
            InputStream stream = getAssets().open(
                "file_00000000e378820aaf66eca8b0dc691e.png"
            );

            background.setImageDrawable(
                android.graphics.drawable.Drawable.createFromStream(
                    stream, null
                )
            );

            stream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        root.addView(background, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(95, 0, 0, 0));

        root.addView(shade, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayoutHolder content = new LinearLayoutHolder(this);

        TextView title = new TextView(this);
        title.setText("MAKDAD ANATOMY");
        title.setTextColor(Color.rgb(232, 195, 85));
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR JOURNEY INTO HUMAN ANATOMY");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(11);
        subtitle.setGravity(Gravity.CENTER);

        ProgressBar loader = new ProgressBar(this);
        loader.setIndeterminate(true);

        content.addView(title);
        content.addView(subtitle);
        content.addView(loader);

        FrameLayout.LayoutParams params =
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            );

        params.leftMargin = 20;
        params.rightMargin = 20;

        root.addView(content, params);

        setContentView(root);

        new android.os.Handler(
            android.os.Looper.getMainLooper()
        ).postDelayed(this::openMainPage, 3000);
    }

    private void openMainPage() {

        assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                new WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build();

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(true);

        webView.setWebViewClient(new WebViewClientCompat() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                WebView view,
                WebResourceRequest request
            ) {

                String url = request.getUrl().toString();

                WebResourceResponse atlasResponse =
                    serveAtlasData(url);

                if (atlasResponse != null) {
                    return atlasResponse;
                }

                return assetLoader.shouldInterceptRequest(
                    request.getUrl()
                );
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                WebView view,
                String url
            ) {

                WebResourceResponse atlasResponse =
                    serveAtlasData(url);

                if (atlasResponse != null) {
                    return atlasResponse;
                }

                return assetLoader.shouldInterceptRequest(url);
            }
        });

        setContentView(webView);

        webView.loadUrl(
            "https://appassets.androidplatform.net/assets/index.html"
        );
    }

    private WebResourceResponse serveAtlasData(String url) {

        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath();

            String prefix = "/assets/anatomy-atlas/data/";

            if (path == null || !path.startsWith(prefix)) {
                return null;
            }

            String relativePath = path.substring(prefix.length());

            File atlasRoot = new File(
                getFilesDir(),
                "atlas-data/bp3d43"
            );

            File root = atlasRoot.getCanonicalFile();
            File requested = new File(
                root,
                relativePath
            ).getCanonicalFile();

            String rootPath = root.getPath() + File.separator;

            if (!requested.getPath().startsWith(rootPath)) {
                return errorResponse(403, "Forbidden");
            }

            if (!requested.isFile()) {
                return errorResponse(404, "Atlas file not found");
            }

            String name = requested.getName().toLowerCase();
            String mime = "application/octet-stream";

            if (name.endsWith(".json")) {
                mime = "application/json";
            } else if (name.endsWith(".txt")) {
                mime = "text/plain";
            }

            InputStream input = new FileInputStream(requested);

            Map<String, String> headers = new HashMap<>();
            headers.put("Cache-Control", "no-cache");

            return new WebResourceResponse(
                mime,
                "UTF-8",
                200,
                "OK",
                headers,
                input
            );

        } catch (IOException e) {
            e.printStackTrace();
            return errorResponse(500, "Atlas data error");
        }
    }

    private WebResourceResponse errorResponse(
        int code,
        String message
    ) {
        Map<String, String> headers = new HashMap<>();

        return new WebResourceResponse(
            "text/plain",
            "UTF-8",
            code,
            message,
            headers,
            null
        );
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private static class LinearLayoutHolder
            extends android.widget.LinearLayout {

        public LinearLayoutHolder(Activity activity) {
            super(activity);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);
            setPadding(20, 20, 20, 20);
        }
    }
                }
