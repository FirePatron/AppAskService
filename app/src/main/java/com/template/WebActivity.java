package com.template;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity {

    private WebView view;
    private String url;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        view = findViewById(R.id.webView);

        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setDomStorageEnabled(true);

        String url = (String) getIntent().getSerializableExtra("url");

        view.loadUrl(url);
        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:(function() { " +
                        "var head = document.getElementsByTagName('header')[0];"
                        + "head.parentNode.removeChild(head);" +
                        "})()");
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (view.canGoBack()) {
            view.goBack();
        }
    }


    public void setUrl(String url) {
        this.url=url;
    }


}