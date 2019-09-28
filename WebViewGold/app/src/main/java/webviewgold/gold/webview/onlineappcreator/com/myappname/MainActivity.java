package webviewgold.gold.webview.onlineappcreator.com.myappname;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.gold.webview.codecanyon.com.webview.BuildConfig;
import android.gold.webview.codecanyon.com.webview.R;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static webviewgold.gold.webview.onlineappcreator.com.myappname.Config.PURCHASE_LICESE_KEY;

public class MainActivity extends AppCompatActivity implements OSSubscriptionObserver, BillingProcessor.IBillingHandler {

    private WebView webView;
    private View offlineLayout;
    private AlertDialog noConnectionDialog;
    private static final int FILECHOOSER_RESULTCODE = 2888;
    public static final int REQUEST_SELECT_FILE = 100;
    private ValueCallback<Uri> mUploadMessage;
    private AdView mAdView;
    InterstitialAd mInterstitialAd;
    public int webviewCount = 0;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int MULTIPLE_PERMISSIONS = 10;
    public ProgressBar progressBar;
    private String deepLinkingURL;
    private BillingProcessor bp;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, String.valueOf(R.string.admob_app_id));
        bp = new BillingProcessor(this, PURCHASE_LICESE_KEY, this);
        bp.initialize();

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null && (intent.getData().getScheme().equals("http"))) {
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() > 0) {
                deepLinkingURL = pathSegments.get(0).substring(5) + "//" + pathSegments.get(1);
                // This will give you prefix as path

            }
        } else if (intent != null) {
            Bundle extras = getIntent().getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
                // and get whatever type user account id is
            }
            if (URL != null && !URL.equalsIgnoreCase(""))
                deepLinkingURL = URL;

        }

        webviewCount = 0;
        final String myOSurl = Config.PURCHASECODE;

        if (Config.PUSH_ENABLED) {
            OneSignal.addSubscriptionObserver(this);
        }

        if (savedInstanceState == null) {
            AlertManager.appLaunched(this);
        }

        mAdView = findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder()
                .build();

        if (BuildConfig.IS_DEBUG_MODE) {
            osURL(myOSurl);
        }

        if (Config.SHOW_BANNER_AD) {
            mAdView.loadAd(adRequest);
        } else {
            mAdView.setVisibility(View.GONE);
        }

        mInterstitialAd = new InterstitialAd(this);

        // set the ad unit ID
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_full_screen));

        mInterstitialAd.setAdListener(new AdListener() {
            public void onAdLoaded() {
                showInterstitial();
            }
        });

        webView = findViewById(R.id.webView);
        offlineLayout = findViewById(R.id.offline_layout);

        progressBar = findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.MULTIPLY);

        final Button tryAgainButton = findViewById(R.id.try_again_button);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isNetworkAvailable()) {
                    loadMainUrl();
                }
            }
        });

        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient());
        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        registerForContextMenu(webView);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);

        askForPermission();

        if (!Config.USER_AGENT.isEmpty()) {
            webSettings.setUserAgentString(Config.USER_AGENT);
        }

        if (Config.CLEAR_CACHE_ON_STARTUP) {
            webView.clearCache(true);
        }

        if (Config.USE_LOCAL_HTML_FOLDER) {
            webView.loadUrl("file:///android_asset/index.html");

        } else if (isNetworkAvailable()) {
            loadMainUrl();
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            menu.setHeaderTitle("Download Image From Below");
            menu.add(0, 1, 0, "Save - Download Image")
                    .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {

                            String DownloadImageURL = webViewHitTestResult.getExtra();

                            if (URLUtil.isValidUrl(DownloadImageURL)) {

                                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                                request.allowScanningByMediaScanner();
                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                downloadManager.enqueue(request);

                                Toast.makeText(MainActivity.this, "Image Downloaded Successfully.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(MainActivity.this, "Sorry.. Something Went Wrong.", Toast.LENGTH_LONG).show();
                            }
                            return false;
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (!bp.handleActivityResult(requestCode, resultCode, intent)) {
            super.onActivityResult(requestCode, resultCode, intent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        } else {
            Toast.makeText(getApplicationContext(), "Failed to upload image", Toast.LENGTH_LONG).show();
            webView.reload();
        }
    }

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    private void onUpdateNetworkStatus(final boolean isConnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                    offlineLayout.setVisibility(View.GONE);
                    noConnectionDialog = null;
                } else {
                    if (offlineLayout.getVisibility() == View.VISIBLE && (noConnectionDialog == null || !noConnectionDialog.isShowing())) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                                .setTitle(R.string.no_connection_title)
                                .setMessage(R.string.no_connection_message)
                                .setPositiveButton(android.R.string.ok, null);
                        noConnectionDialog = builder.create();
                        noConnectionDialog.show();
                    } else {
                        offlineLayout.setVisibility(View.VISIBLE);
                    }
                    webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
            }
        });
    }

    private void loadMainUrl() {

        if (Config.IS_DEEP_LINKING_ENABLED && deepLinkingURL != null) {
            webView.loadUrl(deepLinkingURL);
        } else {
            String urlExt = "";
            if (Config.PUSH_ENABLED) {
                OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
                String userID = status.getSubscriptionStatus().getUserId();

                urlExt = ((Config.PUSH_ENHANCE_WEBVIEW_URL && !TextUtils.isEmpty(userID)) ? String.format("%sonesignal_push_id=%s", (Config.HOME_URL.contains("?") ? "&" : "?"), userID) : "");
            }

            webView.loadUrl(Config.HOME_URL + urlExt);
        }
    }

    private boolean isNetworkAvailable() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            onUpdateNetworkStatus(false);
            return false;
        }

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final URL url = new URL("http://clients3.google.com/generate_204");
                    final HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    httpURLConnection.setRequestProperty("User-Agent", "Android");
                    httpURLConnection.setRequestProperty("Connection", "close");
                    httpURLConnection.setConnectTimeout(1500);
                    httpURLConnection.connect();
                    onUpdateNetworkStatus(httpURLConnection.getResponseCode() == 204 && httpURLConnection.getContentLength() == 0);
                } catch (Exception e) {
                    onUpdateNetworkStatus(false);
                }
            }
        });

        thread.start();

        return true;
    }

    @SuppressLint("WrongConstant")
    private void askForPermission() {
        int accessCoarseLocation = 0;
        int accessFineLocation = 0;
        int accessCamera = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            accessCoarseLocation = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
            accessFineLocation = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
            accessCamera = checkSelfPermission(Manifest.permission.CAMERA);
            checkSelfPermission(Manifest.permission.CAMERA);

            Log.d("per", ">=M");

        } else {
            Log.d("per", "<M");
        }


        List<String> listRequestPermission = new ArrayList<String>();

        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (accessCamera != PackageManager.PERMISSION_GRANTED) {
            listRequestPermission.add(Manifest.permission.CAMERA);
        }

        if (!listRequestPermission.isEmpty()) {
            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(strRequestPermission, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {

                String[] PERMISSIONS = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                }
            }
            default:
                loadMainUrl();
        }

    }

    @Override
    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
        if (Config.PUSH_ENABLED) {
            if (!stateChanges.getFrom().getSubscribed() && stateChanges.getTo().getSubscribed()) {
                String userId = stateChanges.getTo().getUserId();
                Log.i("Debug", "userId: " + userId);

                if (Config.PUSH_RELOAD_ON_USERID) {
                    loadMainUrl();
                }
            }

            Log.i("Debug", "onOSPermissionChanged: " + stateChanges);
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }

    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }

        if (mInterstitialAd != null) {
            mInterstitialAd = null;
        }

        if (bp != null) {
            bp.release();
        }
        super.onDestroy();
    }

    private void showInterstitial() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
            webviewCount = 0;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void osURL(final String currentOSurl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences preferences1 = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                    String cacheID = preferences1.getString("myid", "0");
                    if (cacheID.equals(currentOSurl)) {
                        return;
                    }

                    String osURL1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGk/Y29kZWNhbnlvbl9hcHBfdGVtcGxhdGVfcHVyY2hhc2VfY29kZT0=";
                    byte[] data = Base64.decode(osURL1, Base64.DEFAULT);
                    String osURL = new String(data, StandardCharsets.UTF_8);

                    String myid = currentOSurl;
                    StringBuilder newOSurl = new StringBuilder();
                    newOSurl.append(osURL);
                    newOSurl.append(myid);


                    URL url = new URL(String.valueOf(newOSurl));
                    HttpsURLConnection uc = (HttpsURLConnection) url.openConnection();
                    BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                    String line;
                    StringBuilder lin2 = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        lin2.append(line);

                    }
                    if (String.valueOf(lin2).contains("0000-0000-0000-0000")) {

                        String encoded1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGkvYW5kcm9pZC5odG1s";
                        byte[] encoded2 = Base64.decode(encoded1, Base64.DEFAULT);
                        final String dialog = new String(encoded2, StandardCharsets.UTF_8);
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl(dialog);
                            }
                        });
                    } else {
                        SharedPreferences preferences = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("myid", myid);
                        editor.commit();
                        editor.apply();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //In-APP-BILLING ACTIONS HANDLING
    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
        Toast.makeText(this, "This is Purchased.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, @Nullable Throwable error) {
        Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingInitialized() {

    }

    private class MyWebViewClient extends WebViewClient {
        MyWebViewClient() {
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            webviewCount = webviewCount + 1;

            if (webviewCount >= Config.SHOW_AD_AFTER_X) {
                if (Config.SHOW_FULL_SCREEN_AD) {
                    final AdRequest fullscreenAdRequest = new AdRequest.Builder()
                            .build();
                    mInterstitialAd.loadAd(fullscreenAdRequest);
                }
            }

        }

        @Override
        public void onPageFinished(WebView view, String url) {
            setTitle(view.getTitle());
            progressBar.setVisibility(View.GONE);
            super.onPageFinished(view, url);

        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (isNetworkAvailable()) {
                if (url.contains(Config.HOST)) {
                    view.loadUrl(url);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                } else if (url.startsWith("share:") || url.startsWith("whatsapp:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.contains("facebook.com") || url.contains("twitter.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else if (url.startsWith("inapppurchase://")) {
                    bp.purchase(MainActivity.this, "android.test.purchased");
                    return true;
                } else if (url.startsWith("shareapp://")) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                    String shareMessage = "\nLet me recommend you this application\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "choose one"));
                    return true;
                } else if ((Config.OPEN_EXTERNAL_URLS_IN_ANOTHER_BROWSER && (!Config.USE_LOCAL_HTML_FOLDER || !(url).startsWith("file:///")))) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else {
                    return false;
                }
            } else if (!isNetworkAvailable()) {
                offlineLayout.setVisibility(View.VISIBLE);
                return true;
            } else {
                return false;
            }
        }
    }

    private class MyWebChromeClient extends WebChromeClient {

        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        MyWebChromeClient() {
        }


        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                request.grant(request.getResources());
            }
        }

    }


}


