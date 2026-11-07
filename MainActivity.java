package com.makdad.anatomy;
import android.app.Activity; import android.os.Bundle; import android.webkit.WebView; import android.webkit.WebSettings; import android.webkit.WebViewClient;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){ super.onCreate(b); WebView w=new WebView(this); WebSettings s=w.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); w.setWebViewClient(new WebViewClient()); setContentView(w); w.loadUrl("file:///android_asset/index.html"); }
}