package com.example.ruanna.simplecamtest;

import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

class CameraPreviewGingerbread implements CameraPreviewProvider {
	
	private int cameraId = -1;
	private int cameraOrientation;

	public Camera openCamera() {
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				cameraId = i;
				cameraOrientation = cameraInfo.orientation;
				return Camera.open(cameraId);
			}
		}
		return null;
	}
	
	public void startPreview(Camera camera, int previewWidth, int previewHeight, Display display) {
		int displayRotation = display.getRotation();
		Camera.Parameters parameters = camera.getParameters();
		List<Size> supportedPreviewSizes = parameters
				.getSupportedPreviewSizes();
		Log.d("CameraPreview", "Rotation :" + displayRotation + ", Sizes: ");
		for (Size size : supportedPreviewSizes) {
			Log.d("CameraPreview", "Size: " + size.width + ", " + size.height);
		}
		Size optimalPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, previewWidth, previewHeight);
		if (optimalPreviewSize != null) {
			Log.d("CameraPreview", "Optimal Size:" + optimalPreviewSize.width + ", " + optimalPreviewSize.height);
			int width = optimalPreviewSize.width, height = optimalPreviewSize.height;


			Log.d("TEST", "width=" + optimalPreviewSize.width);
			Log.d("TEST", "height=" + optimalPreviewSize.height);

			MainActivity.setSize(optimalPreviewSize.width, optimalPreviewSize.height);

			boolean doSwitch = false;
			int degrees = 0;
			switch (displayRotation) {
			case Surface.ROTATION_0:
				degrees = 0;
				doSwitch = true;
				break;
			case Surface.ROTATION_90:
				degrees = 90;
				doSwitch = false;
				break;
			case Surface.ROTATION_180:
				degrees = 180;
				doSwitch = true;
				break;
			case Surface.ROTATION_270:
				degrees = 270;
				doSwitch = false;
				break;
			}

			if (doSwitch) {
				MainActivity.setSize(optimalPreviewSize.height, optimalPreviewSize.width);
			} else {
				MainActivity.setSize(optimalPreviewSize.width, optimalPreviewSize.height);
			}

			int result = (cameraOrientation - degrees + 360) % 360;
			camera.setDisplayOrientation(result);
			parameters.setPreviewSize(width, height);

			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);


			parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);


//			parameters.setPictureSize(480, 720);

			camera.setParameters(parameters);
			camera.startPreview();
		}
	}

	public Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		final double MAX_DOWNSIZE = 1.5;

		double targetRatio = (double) w / h;
		if (sizes == null) return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			double downsize = (double) size.width / w;
			if (downsize > MAX_DOWNSIZE) {
				//if the preview is a lot larger than our display surface ignore it
				//reason - on some phones there is not enough heap available to show the larger preview sizes
				continue;
			}
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		//keep the max_downsize requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				double downsize = (double) size.width / w;
				if (downsize > MAX_DOWNSIZE) {
					continue;
				}
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		//everything else failed, just take the closest match
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}

		return optimalSize;
	}

}

