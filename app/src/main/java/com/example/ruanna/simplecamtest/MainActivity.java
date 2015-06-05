package com.example.ruanna.simplecamtest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.google.zxing.integration.android.IntentIntegrator;

public class MainActivity extends Activity {

    static private RelativeLayout container;
    private Button capture;
    private CameraPreview captureButton;
    View.OnClickListener captrureListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            captureButton.takePicture();
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        container = (RelativeLayout) findViewById(R.id.container);
        captureButton = (CameraPreview) container.getChildAt(0);
        capture = (Button) findViewById(R.id.picture);
        capture.setOnClickListener(captrureListener);
    }

    static public void setSize(int w, int h) {
        container.setLayoutParams(new RelativeLayout.LayoutParams(w, h));
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("CameraPreviewActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("CameraPreviewActivity", "onPause");
    }

}