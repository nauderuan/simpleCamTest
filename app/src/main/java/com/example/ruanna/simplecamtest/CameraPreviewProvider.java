package com.example.ruanna.simplecamtest;

import android.hardware.Camera;
import android.view.Display;

interface CameraPreviewProvider {
	
	Camera openCamera();
	
	void startPreview(Camera camera, int previewWidth, int previewHeight, Display display);

}
