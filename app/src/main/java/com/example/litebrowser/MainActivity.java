package com.example.litebrowser;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    static final String HOME = "https://m.youtube.com";
    WebView web; FrameLayout root; ProgressBar progress;
    View customView; WebChromeClient.CustomViewCallback customCb;

    static final Set<String> BLOCKED = new HashSet<>(Arrays.asList(
        "doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com",
        "google-analytics.com","googletagmanager.com","googletagservices.com","adnxs.com",
        "taboola.com","outbrain.com","criteo.com","pubmatic.com","rubiconproject.com",
        "moatads.com","amazon-adsystem.com","scorecardresearch.com","imasdk.googleapis.com"));

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        root = findViewById(R.id.root); web = findViewById(R.id.web); progress = findViewById(R.id.progress);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSaveFormData(false);
        if (android.os.Build.VERSION.SDK_INT >= 26) s.setSafeBrowsingEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String host = r.getUrl().getHost();
                String path = r.getUrl().getPath() == null ? "" : r.getUrl().getPath();
                boolean ytAd = host != null && host.endsWith("youtube.com") &&
                    (path.startsWith("/pagead") || path.startsWith("/api/stats/ads") ||
                     path.startsWith("/ptracking") || path.startsWith("/api/stats/atr"));
                if (ytAd || (host != null && isBlocked(host)))
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, String url) {
                Uri u = Uri.parse(url);
                if (!"https".equals(u.getScheme())) return true;      // block http, file, intent, javascript...
                String h = u.getHost() == null ? "" : u.getHost().toLowerCase();
                if (isHost(h, "youtube.com") || h.equals("youtu.be") || isHost(h, "google.com")
                    || isHost(h, "gstatic.com") || isHost(h, "googleusercontent.com")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception e) {}
                return true;
            }
            @Override public void onReceivedSslError(WebView v, android.webkit.SslErrorHandler h, android.net.http.SslError e) {
                h.cancel();                                            // never continue on bad certificates
            }
            @Override public void onPageStarted(WebView v, String url, Bitmap f) { progress.setVisibility(View.VISIBLE); }
            @Override public void onPageFinished(WebView v, String url) {
                progress.setVisibility(View.GONE);
                if (url.contains("youtube.com")) v.evaluateJavascript(readAsset("yt_adblock.js"), null);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) { progress.setProgress(p); }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                if (customView != null) { cb.onCustomViewHidden(); return; }
                customView = v; customCb = cb;
                root.addView(v, new FrameLayout.LayoutParams(-1, -1));
                web.setVisibility(View.GONE);
                setFull(true);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            @Override public void onHideCustomView() { hideFull(); }
        });

        handle(getIntent());
    }

    void handle(Intent i) {
        String d = i == null ? null : i.getDataString();
        web.loadUrl(isYoutube(d) ? d : HOME);
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handle(i); }

    static boolean isHost(String h, String d) { return h.equals(d) || h.endsWith("." + d); }

    static boolean isYoutube(String url) {
        if (url == null) return false;
        Uri u = Uri.parse(url);
        String h = u.getHost();
        return "https".equals(u.getScheme()) && h != null
            && (h.equalsIgnoreCase("youtu.be") || isHost(h.toLowerCase(), "youtube.com"));
    }

    void hideFull() {
        if (customView == null) return;
        root.removeView(customView); customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
        web.setVisibility(View.VISIBLE);
        setFull(false);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    void setFull(boolean on) {
        View d = getWindow().getDecorView();
        if (on) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            d.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    String readAsset(String name) {
        try {
            java.io.InputStream in = getAssets().open(name);
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
            in.close();
            return o.toString("UTF-8");
        } catch (Exception e) { return ""; }
    }

    boolean isBlocked(String host) {
        String h = host.toLowerCase();
        while (h.contains(".")) {
            if (BLOCKED.contains(h)) return true;
            h = h.substring(h.indexOf('.') + 1);
        }
        return false;
    }

    @Override public void onBackPressed() {
        if (customView != null) hideFull();
        else if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
