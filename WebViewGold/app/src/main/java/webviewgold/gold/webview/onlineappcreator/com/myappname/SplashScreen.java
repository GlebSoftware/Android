package webviewgold.gold.webview.onlineappcreator.com.myappname;

import android.content.Intent;
import android.gold.webview.codecanyon.com.webview.R;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import static webviewgold.gold.webview.onlineappcreator.com.myappname.Config.SPLASH_TIMEOUT;

public class SplashScreen extends AppCompatActivity {
    //Timeout configuration in Config.java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorWhite)); //The Bar in the bottom
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorWhite));

        final ImageView splash = findViewById(R.id.splash);
        splash.setImageResource(R.mipmap.ic_launcher);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SplashScreen.this.startActivity(new Intent(SplashScreen.this, MainActivity.class));
                SplashScreen.this.finish();
            }
        }, SPLASH_TIMEOUT);

    }
}
