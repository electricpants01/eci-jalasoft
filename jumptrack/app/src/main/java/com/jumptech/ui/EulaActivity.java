package com.jumptech.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.jumptech.android.util.FileManager;
import com.jumptech.android.util.Message;
import com.jumptech.jumppod.R;
import com.jumptech.networking.JTRepository;
import com.jumptech.tracklib.db.TrackPreferences;

import java.io.IOException;

public class EulaActivity extends AppCompatActivity {

    public static final String DISPLAY_TERMS_OF_USE_READONLY_KEY = "displayTermsOfUseReadOnly";

    /**
     * Class tag
     */
    private static final String TAG = EulaActivity.class.getName();

    private Boolean displayTermsOfUseReadOnly = false;
    private View loadingLayout;
    private TextView loadingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.eula_activity);
        loadingLayout = findViewById(R.id.loadingPanel);
        loadingTextView = findViewById(R.id.loadingTextView);
        try {
            displayTermsOfUseReadOnly = getIntent().getExtras().getBoolean(DISPLAY_TERMS_OF_USE_READONLY_KEY);
            displayEulaContent();
            bindScreenAction();
        } catch (IOException e) {
            Log.e(TAG, "Unexpected error", e);
        }
    }

    /**
     * Binds the screen's actions
     */
    private void bindScreenAction() {
        Button acceptBtn = (Button) findViewById(R.id.btnAccept);
        if (displayTermsOfUseReadOnly) {
            acceptBtn.setText(getString(R.string.eula_ok_button_label));
        }
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!displayTermsOfUseReadOnly) {
                    performEulaRequest();
                } else {
                    finish();
                }
            }
        });
    }

    /**
     * Performs the request to notify to the server user has accepted the EULA
     */
    private void performEulaRequest() {
        loadingLayout.setVisibility(View.VISIBLE);
        loadingTextView.setVisibility(View.VISIBLE);
        loadingTextView.setText(R.string.authenticating);
        JTRepository.eula(this, new JTRepository.OnServiceResponse() {
            @Override
            public void success() {
                loadingLayout.setVisibility(View.GONE);
                loadingTextView.setVisibility(View.GONE);
                new TrackPreferences(EulaActivity.this).setIsEulaPrompt(false);
                startActivity(new Intent(EulaActivity.this, LauncherActivity.class));
                finish();
            }

            @Override
            public void error(String message) {
                loadingLayout.setVisibility(View.GONE);
                loadingTextView.setVisibility(View.GONE);
                Message.showSimpleError(message, EulaActivity.this);
            }
        });
    }

    /**
     * Reads a resource file and populates the respective view to display it
     */
    private void displayEulaContent() throws IOException {
        WebView webView = findViewById(R.id.webViewTOU);
        String content = FileManager.getContentFromRawResource(this, R.raw.eula);
        webView.loadData(content, "text/html", null);
    }
}
