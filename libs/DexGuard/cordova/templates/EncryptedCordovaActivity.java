/*
 * ProGuard -- shrinking, optimization, obfuscation, and preverification
 *             of Java bytecode.
 *
 * Copyright (c) 2002-2018 GuardSquare NV
 */
package PACKAGE_NAME_PLACEHOLDER;

import android.os.Bundle;

import com.guardsquare.dexguard.runtime.encryption.EncryptedSystemWebViewClient;

import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

/**
 * Sample activity that shows a Cordova web view.
 */
public class EncryptedCordovaActivity extends CordovaActivity {

    // An arbitrary http URL prefix to refer to local assets
    // (encrypted or not).
    private static final String ENCRYPTED_ASSET_PREFIX = "file:///";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Start Cordova with the main page.
        super.onCreate(savedInstanceState);
        super.init();
        launchUrl = ENCRYPTED_ASSET_PREFIX + "www/index.html";
    }

    @Override
    protected CordovaWebViewEngine makeWebViewEngine() {
        CordovaWebViewEngine engine = super.makeWebViewEngine();

        SystemWebView webView = (SystemWebView) engine.getView();

        // Configure an encrypted SystemWebViewClient on the webView.
        EncryptedSystemWebViewClient webViewClient =
            new EncryptedSystemWebViewClient((SystemWebViewEngine) engine,
                                             getAssets(),
                                             ENCRYPTED_ASSET_PREFIX);

        webView.setWebViewClient(webViewClient);

        return engine;
    }
}
