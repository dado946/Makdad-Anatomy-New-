
package com.makdad.anatomy;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.Button;
import android.widget.Toast;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.ConsoleMessage;
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

    private static final String TAG = "MAKDAD_ANATOMY";

    private WebView webView;
    private WebViewAssetLoader assetLoader;

    private FrameLayout root;
    private TextView status;
    private TextView percent;
    private ProgressBar progress;
    private Button retryButton;

    private AtlasDownloader downloader;

    private final Handler handler =
        new Handler(Looper.getMainLooper());

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

        root = new FrameLayout(this);
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
            Log.e(TAG, "Splash image error", e);
        }

        root.addView(background, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(110, 0, 0, 0));

        root.addView(shade, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayoutHolder content =
            new LinearLayoutHolder(this);

        TextView title = new TextView(this);
        title.setText("MAKDAD ANATOMY");
        title.setTextColor(Color.rgb(232, 195, 85));
        title.setTextSize(27);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText(
            "تجهيز الأطلس التشريحي ثلاثي الأبعاد"
        );
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("جارٍ التحقق من بيانات التشريح...");
        status.setTextColor(Color.WHITE);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);

        progress = new ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        );

        progress.setMax(100);
        progress.setProgress(0);

        percent = new TextView(this);
        percent.setText("0%");
        percent.setTextColor(Color.rgb(232, 195, 85));
        percent.setTextSize(14);
        percent.setGravity(Gravity.CENTER);

        retryButton = new Button(this);
        retryButton.setText("إعادة المحاولة");
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(v -> startAtlasDownload());

        Button continueButton = new Button(this);
        continueButton.setText("متابعة بدون تنزيل");
        continueButton.setOnClickListener(v -> openMainPage());

        content.addView(title);
        content.addView(subtitle);
        content.addView(status);
        content.addView(progress);
        content.addView(percent);
        content.addView(retryButton);
        content.addView(continueButton);

        FrameLayout.LayoutParams params =
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            );

        params.leftMargin = 25;
        params.rightMargin = 25;

        root.addView(content, params);

        setContentView(root);

        checkAtlasData();
    }

    private void checkAtlasData() {

        File installed = new File(
            getFilesDir(),
            "atlas-data/bp3d43/index.json"
        );

        if (installed.isFile() && installed.length() > 0) {

            status.setText("بيانات الأطلس جاهزة");
            progress.setProgress(100);
            percent.setText("100%");

            handler.postDelayed(
                this::openMainPage,
                700
            );

        } else {
            startAtlasDownload();
        }
    }

    private void startAtlasDownload() {

        retryButton.setVisibility(View.GONE);
        progress.setProgress(0);
        percent.setText("0%");
        status.setText("جارٍ الاتصال بخادم بيانات التشريح...");

        downloader = new AtlasDownloader();

        downloader.start(this, new AtlasDownloader.Listener() {

            @Override
            public void onProgress(
                long downloaded,
                long total
            ) {

                if (total > 0) {

                    int value = (int) Math.min(
                        100,
                        downloaded * 100 / total
                    );

                    progress.setProgress(value);
                    percent.setText(value + "%");

                } else {

                    percent.setText(
                        android.text.format.Formatter.formatFileSize(
                            MainActivity.this,
                            downloaded
                        )
                    );
                }
            }

            @Override
            public void onStage(String message) {
                status.setText(message);
            }

            @Override
            public void onComplete(File atlasDirectory) {

                status.setText(
                    "اكتمل تجهيز الأطلس بنجاح"
                );

                progress.setProgress(100);
                percent.setText("100%");

                handler.postDelayed(
                    MainActivity.this::openMainPage,
                    1000
                );
            }

            @Override
            public void onError(String message) {

                status.setText(
                    "تعذر تجهيز الأطلس:\n" + message
                );

                retryButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openMainPage() {

        assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                new WebViewAssetLoader.AssetsPathHandler(this)
            )
            .build();

        webView = new WebView(this);

        // تفعيل التسريع الرسومي
        webView.setLayerType(
            View.LAYER_TYPE_HARDWARE,
            null
        );

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportZoom(true);

        // مراقبة أخطاء JavaScript
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onConsoleMessage(
                ConsoleMessage consoleMessage
            ) {

                Log.e(
                    TAG,
                    "JavaScript: "
                    + consoleMessage.message()
                    + " | Line: "
                    + consoleMessage.lineNumber()
                    + " | Source: "
                    + consoleMessage.sourceId()
                );

                return true;
            }
        });

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

                return assetLoader.shouldInterceptRequest(
                    Uri.parse(url)
                );
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

            String prefix =
                "/assets/anatomy-atlas/data/";

            if (path == null || !path.startsWith(prefix)) {
                return null;
            }

            String relativePath =
                path.substring(prefix.length());

            File atlasRoot = new File(
                getFilesDir(),
                "atlas-data/bp3d43"
            );

            File root = atlasRoot.getCanonicalFile();

            File requested = new File(
                root,
                relativePath
            ).getCanonicalFile();

            String rootPath =
                root.getPath() + File.separator;

            if (!requested.getPath().startsWith(rootPath)) {
                return errorResponse(403, "Forbidden");
            }

            if (!requested.isFile()) {
                return errorResponse(
                    404,
                    "Atlas file not found"
                );
            }

            String name =
                requested.getName().toLowerCase();

            String mime =
                "application/octet-stream";

            if (name.endsWith(".json")) {
                mime = "application/json";
            } else if (name.endsWith(".txt")) {
                mime = "text/plain";
            }

            InputStream input =
                new FileInputStream(requested);

            Map<String, String> headers =
                new HashMap<>();

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

            Log.e(TAG, "Atlas data error", e);

            return errorResponse(
                500,
                "Atlas data error"
            );
        }
    }

    private WebResourceResponse errorResponse(
        int code,
        String message
    ) {

        Map<String, String> headers =
            new HashMap<>();

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
