package com.ahmedsalah.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
                new WhatsAppBridge(),
                "AndroidWhatsAppBridge"
        );

        webView.loadUrl(
                "https://slah86845-cyber.github.io/ahmed-salah-app/"
        );

        setContentView(webView);
    }

    public class WhatsAppBridge {

        @JavascriptInterface
        public void sendImageToPhone(
                String phone,
                String dataUrl,
                String fileName
        ) {
            sendPaper(phone, dataUrl, fileName);
        }

        @JavascriptInterface
        public void shareImageToWhatsApp(
                String phone,
                String dataUrl,
                String fileName
        ) {
            sendPaper(phone, dataUrl, fileName);
        }

        @JavascriptInterface
        public void sendPaper(
                String phone,
                String dataUrl,
                String fileName
        ) {
            try {
                String cleanPhone =
                        phone.replaceAll("[^0-9]", "");

                // Normalize Iraqi mobile numbers for WhatsApp JID.
                if (cleanPhone.startsWith("00964")) {
                    cleanPhone = cleanPhone.substring(2);
                } else if (cleanPhone.startsWith("0")) {
                    cleanPhone = "964" + cleanPhone.substring(1);
                }

                String base64 = dataUrl;
                int comma = dataUrl.indexOf(",");

                if (comma >= 0) {
                    base64 = dataUrl.substring(comma + 1);
                }

                byte[] bytes =
                        Base64.decode(base64, Base64.DEFAULT);

                File folder =
                        new File(getCacheDir(), "whatsapp");

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                File imageFile =
                        new File(folder, fileName);

                FileOutputStream output =
                        new FileOutputStream(imageFile);

                output.write(bytes);
                output.flush();
                output.close();

                Uri imageUri =
                        FileProvider.getUriForFile(
                                MainActivity.this,
                                getPackageName() + ".provider",
                                imageFile
                        );

                Intent intent =
                        new Intent(Intent.ACTION_SEND);

                intent.setType("image/png");

                intent.putExtra(
                        Intent.EXTRA_STREAM,
                        imageUri
                );

                intent.putExtra(
                        "jid",
                        cleanPhone + "@s.whatsapp.net"
                );

                // Prefer regular WhatsApp, then WhatsApp Business.
                String whatsappPackage = "com.whatsapp";
                try {
                    getPackageManager().getPackageInfo(whatsappPackage, 0);
                } catch (Exception regularMissing) {
                    whatsappPackage = "com.whatsapp.w4b";
                }
                intent.setPackage(whatsappPackage);

                intent.addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );

                startActivity(intent);

            } catch (Exception error) {

                runOnUiThread(() ->
                        webView.evaluateJavascript(
                                "alert('تعذر إرسال الورقة عبر واتساب');",
                                null
                        )
                );
            }
        }
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
