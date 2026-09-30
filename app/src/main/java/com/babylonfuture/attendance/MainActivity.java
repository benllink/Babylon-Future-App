package com.babylonfuture.attendance;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends android.app.Activity {
    private static final String PREFS = "babylon_future_prefs";
    private static final String KEY_SERVER_URL = "server_url";

    private WebView webView;
    private ProgressBar progress;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        webView = findViewById(R.id.webView);
        progress = findViewById(R.id.progress);
        TextView settingsButton = findViewById(R.id.settingsButton);

        configureWebView();
        settingsButton.setOnClickListener(v -> showServerDialog(false));

        String server = normalizeUrl(prefs.getString(KEY_SERVER_URL, ""));
        if (server.isEmpty()) {
            showServerDialog(true);
        } else {
            loadServer(server);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setUserAgentString(settings.getUserAgentString() + " BabylonFutureAndroid/1.0");

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                return !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
            }
        });

        webView.setDownloadListener(createDownloadListener());
    }

    private DownloadListener createDownloadListener() {
        return (url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) request.addRequestHeader("Cookie", cookies);
                if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
                request.setMimeType(mimetype);
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setTitle(fileName);
                request.setDescription("بابليون المستقبل - تنزيل التقرير");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(this, "بدأ تنزيل التقرير: " + fileName, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "تعذر تنزيل الملف: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };
    }

    private void showServerDialog(boolean firstRun) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(getString(R.string.server_url_hint));
        input.setText(prefs.getString(KEY_SERVER_URL, ""));
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("رابط سيرفر بابليون المستقبل")
                .setMessage("أدخل IP أو رابط السيرفر. مثال:\nhttp://192.168.1.10:5000")
                .setView(input)
                .setCancelable(!firstRun)
                .setPositiveButton("حفظ", null)
                .setNegativeButton(firstRun ? null : "إلغاء", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String url = normalizeUrl(input.getText().toString());
            if (url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) {
                input.setError("اكتب رابط صحيح يبدأ بـ http:// أو https://");
                return;
            }
            prefs.edit().putString(KEY_SERVER_URL, url).apply();
            dialog.dismiss();
            loadServer(url);
        }));
        dialog.show();
    }

    private String normalizeUrl(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network active = cm.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void loadServer(String server) {
        if (!isOnline()) {
            Toast.makeText(this, "لا يوجد اتصال بالشبكة. تحقق من Wi-Fi أو بيانات الهاتف.", Toast.LENGTH_LONG).show();
        }
        webView.loadUrl(server + "/");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
