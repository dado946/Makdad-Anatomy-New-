
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

import android.content.SharedPreferences;

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

    private static final String PREFS_NAME =
        "makdad_anatomy_preferences";

    private static final String KEY_ATLAS_DOWNLOADED =
        "atlas_download_completed";

    private WebView webView;
    private WebViewAssetLoader assetLoader;

    private FrameLayout root;
    private TextView status;
    private TextView percent;
    private ProgressBar progress;

    private Button downloadButton;
    private Button continueButton;
    private Button retryButton;

    private AtlasDownloader downloader;

    private boolean mainPageOpened = false;
    private boolean downloadInProgress = false;
    private boolean exitPending = false;

    private final Handler handler =
        new Handler(Looper.getMainLooper());

    private final Handler backHandler =
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

        SharedPreferences prefs =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean downloadedBefore =
            prefs.getBoolean(KEY_ATLAS_DOWNLOADED, false);

        if (downloadedBefore && hasAtlasIndex()) {

            // سبق تحميل الأطلس بنجاح:
            // تظهر شاشة الترحيب مع دائرة التحميل فقط.
            showSplash(false);

            handler.postDelayed(
                this::openMainPage,
                900
            );

        } else {

            // لم يكتمل تحميل الأطلس بنجاح:
            // تظهر خيارات البداية.
            if (downloadedBefore) {
                prefs.edit()
                    .putBoolean(KEY_ATLAS_DOWNLOADED, false)
                    .apply();
            }

            showSplash(true);
        }
    }


    private boolean hasAtlasIndex() {

        File installed = new File(
            getFilesDir(),
            "atlas-data/bp3d43/index.json"
        );

        return installed.isFile() && installed.length() > 0;
    }


    private void saveAtlasDownloadCompleted() {

        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ATLAS_DOWNLOADED, true)
            .commit();
    }


    private void showSplash(boolean showChoices) {

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
                    stream,
                    null
                )
            );

            stream.close();

        } catch (Exception e) {

            Log.e(TAG, "Splash image error", e);
        }

        root.addView(
            background,
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );

        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(125, 0, 0, 0));

        root.addView(
            shade,
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        );

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
            "مرحباً بك في عالم التشريح الطبي ثلاثي الأبعاد"
        );
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);

        // دائرة تحميل دائرية.
        progress = new ProgressBar(this);
        progress.setIndeterminate(true);

        percent = new TextView(this);
        percent.setTextColor(Color.rgb(232, 195, 85));
        percent.setTextSize(14);
        percent.setGravity(Gravity.CENTER);
        percent.setText("");

        downloadButton = new Button(this);
        downloadButton.setText("تحميل الأطلس");
        downloadButton.setAllCaps(false);
        downloadButton.setOnClickListener(
            v -> startAtlasDownload()
        );

        continueButton = new Button(this);
        continueButton.setText("المتابعة بدون تحميل");
        continueButton.setAllCaps(false);
        continueButton.setOnClickListener(
            v -> continueWithoutDownload()
        );

        retryButton = new Button(this);
        retryButton.setText("إعادة المحاولة");
        retryButton.setAllCaps(false);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(
            v -> startAtlasDownload()
        );

        content.addView(title);
        content.addView(subtitle);

        content.addView(
            status,
            new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        );

        content.addView(
            progress,
            new android.widget.LinearLayout.LayoutParams(
                dp(65),
                dp(65)
            )
        );

        content.addView(percent);
        content.addView(downloadButton);
        content.addView(continueButton);
        content.addView(retryButton);

        FrameLayout.LayoutParams params =
            new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            );

        params.leftMargin = dp(25);
        params.rightMargin = dp(25);

        root.addView(content, params);

        setContentView(root);

        if (showChoices) {

            downloadButton.setVisibility(View.VISIBLE);
            continueButton.setVisibility(View.VISIBLE);

            progress.setVisibility(View.GONE);

            status.setText(
                hasAtlasIndex()
                    ? "بيانات الأطلس موجودة وجاهزة"
                    : "اختر طريقة الدخول إلى التطبيق"
            );

        } else {

            // بعد نجاح تحميل الأطلس:
            // لا تظهر أزرار البداية مجدداً.
            downloadButton.setVisibility(View.GONE);
            continueButton.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);

            progress.setVisibility(View.VISIBLE);

            status.setText("جارٍ تجهيز التطبيق...");
        }
    }


    private int dp(int value) {

        return (int) (
            value * getResources()
                .getDisplayMetrics().density
        );
    }


    private void continueWithoutDownload() {

        if (mainPageOpened) {
            return;
        }

        // إذا كان التحميل جارياً وأُختيرت المتابعة،
        // نوقف محاولة التحميل ولا نحفظ نجاحها.
        if (downloader != null && downloadInProgress) {

            downloadInProgress = false;

            downloader.cancel();
        }

        openMainPage();
    }


    private void startAtlasDownload() {

        if (mainPageOpened || downloadInProgress) {
            return;
        }

        // إذا كانت بيانات الأطلس موجودة مسبقاً،
        // لا داعي لتنزيلها مرة أخرى.
        if (hasAtlasIndex()) {

            saveAtlasDownloadCompleted();

            status.setText("بيانات الأطلس جاهزة");

            openMainPage();

            return;
        }

        downloadInProgress = true;

        downloadButton.setEnabled(false);

        retryButton.setVisibility(View.GONE);

        progress.setVisibility(View.VISIBLE);
        progress.setIndeterminate(true);

        percent.setText("0%");

        status.setText(
            "جارٍ الاتصال بخادم بيانات التشريح..."
        );

        downloader = new AtlasDownloader();

        downloader.start(this, new AtlasDownloader.Listener() {

            @Override
            public void onProgress(
                long downloaded,
                long total
            ) {

                if (!downloadInProgress || mainPageOpened) {
                    return;
                }

                if (total > 0) {

                    int value = (int) Math.min(
                        100,
                        downloaded * 100 / total
                    );

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

                if (!downloadInProgress || mainPageOpened) {
                    return;
                }

                status.setText(message);
            }


            @Override
            public void onComplete(File atlasDirectory) {

                if (!downloadInProgress || mainPageOpened) {
                    return;
                }

                downloadInProgress = false;

                downloadButton.setEnabled(true);

                // لا نحفظ نجاح التحميل إلا بعد التحقق
                // من وجود ملف index.json فعلياً.
                if (!hasAtlasIndex()) {

                    status.setText(
                        "لم يتم العثور على ملف بيانات الأطلس"
                    );

                    retryButton.setVisibility(View.VISIBLE);

                    return;
                }

                saveAtlasDownloadCompleted();

                status.setText(
                    "اكتمل تحميل الأطلس بنجاح"
                );

                percent.setText("100%");

                progress.setIndeterminate(false);
                progress.setVisibility(View.VISIBLE);

                handler.postDelayed(
                    MainActivity.this::openMainPage,
                    1000
                );
            }


            @Override
            public void onError(String message) {

                if (!downloadInProgress || mainPageOpened) {
                    return;
                }

                downloadInProgress = false;

                downloadButton.setEnabled(true);

                status.setText(
                    "تعذر تحميل الأطلس:\n" + message
                );

                progress.setVisibility(View.GONE);

                percent.setText("");

                retryButton.setVisibility(View.VISIBLE);
            }
        });
    }


    private void openMainPage() {

        if (mainPageOpened) {
            return;
        }

        mainPageOpened = true;

        handler.removeCallbacksAndMessages(null);

        try {

            assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/assets/",
                    new WebViewAssetLoader.AssetsPathHandler(this)
                )
                .build();

            webView = new WebView(this);

            // تفعيل التسريع الرسومي.
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

            // مراقبة أخطاء JavaScript.
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

        } catch (Exception e) {

            Log.e(TAG, "Unable to open main page", e);

            mainPageOpened = false;

            Toast.makeText(
                this,
                "تعذر فتح التطبيق",
                Toast.LENGTH_LONG
            ).show();
        }
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

            File rootFile = atlasRoot.getCanonicalFile();

            File requested = new File(
                rootFile,
                relativePath
            ).getCanonicalFile();

            String rootPath =
                rootFile.getPath() + File.separator;

            if (!requested.getPath().startsWith(rootPath)) {

                return errorResponse(
                    403,
                    "Forbidden"
                );
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

            return;
        }

        if (exitPending) {

            backHandler.removeCallbacksAndMessages(null);

            finish();

            return;
        }

        exitPending = true;

        Toast.makeText(
            this,
            "اضغط مرة أخرى للخروج",
            Toast.LENGTH_SHORT
        ).show();

        backHandler.postDelayed(() -> {

            exitPending = false;

        }, 2000);
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
