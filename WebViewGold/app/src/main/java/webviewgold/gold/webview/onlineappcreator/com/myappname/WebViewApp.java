package webviewgold.gold.webview.onlineappcreator.com.myappname;

import android.app.Application;

import com.onesignal.OneSignal;

public class WebViewApp extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();

        if(Config.PUSH_ENABLED)
        {
            // OneSignal Initialization
            OneSignal.startInit(this)
                    .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                    .setNotificationOpenedHandler(new MyNotificationOpenedHandler(this))
                    .unsubscribeWhenNotificationsAreDisabled(true)
                    .init();

        }
    }
}