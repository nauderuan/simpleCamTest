package com.example.ruanna.simplecamtest;

import java.util.List;

import android.graphics.ImageFormat;
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
		List<Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();


//		previewWidth = 720;
//		previewHeight = 345;



		Log.d("CameraPreview", "Preview width:" + previewWidth + ", height: " + previewHeight);
		//We want a 4:3 aspect ratio so will intercept
		float optimalWidth = previewWidth;
		float optimalHeight = previewHeight;

		if ((optimalWidth / 3 * 4) > previewHeight) {
			optimalWidth = (int) Math.ceil((previewHeight / 4 * 3));
		} else {
			optimalHeight = (int) Math.ceil((optimalWidth / 3 * 4));
		}
		Log.d("CameraPreview", "optimalWidth :" + optimalWidth + ", optimalHeight: " + optimalHeight);




		Log.d("CameraPreview", "Rotation :" + displayRotation + ", Sizes: ");
		Size optimalPreviewSize = getOptimalPreviewSize(supportedPreviewSizes, (int)optimalWidth, (int)optimalHeight);

		if (optimalPreviewSize != null) {


			//TODO by the looks of it we can just find the biggest 4:3 aspect ratio and use that
			//since it will be stretched to fit inside our preview view
			Log.d("CameraPreview", "Optimal Size:" + optimalPreviewSize.width + ", " + optimalPreviewSize.height);

			int width = optimalPreviewSize.width, height = optimalPreviewSize.height;

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

//			if (doSwitch) {
//				Log.d("CameraPreview", "doSwitch : yes");
//				MainActivity.setSize(height, width);
//			} else {
				Log.d("CameraPreview", "doSwitch : no");
				MainActivity.setSize((int)optimalWidth, (int)optimalHeight);
//			}

			int result = (cameraOrientation - degrees + 360) % 360;
			camera.setDisplayOrientation(result);





			parameters.setPreviewSize(320, 240);
//			parameters.setPreviewSize(width, height);




			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			parameters.setColorEffect(Camera.Parameters.EFFECT_MONO);





			List<Size> supportedPicSizes = parameters.getSupportedPictureSizes();
			for (Size size : supportedPicSizes) {
				Log.d("TEST", "Photo Size: " + size.width + ", " + size.height);
			}





//			parameters.setJpegQuality(100);
//			parameters.setPictureFormat(ImageFormat.);


			parameters.setPictureSize(1632, 1224);



			//TODO
			// 0. Crop
			// 1. get 4:3 closest to HD (might need to consider preview space, or do own preview based on screen width)
			// 2. Can add code to determine how much of image is taken up by qr and deny if to big or small




			// 1. Set height based on the SCREEN width, if height is more than SCREEN height
			//    then set width based on SCREEN height. (4:3)
			// 2. Get the closest 4:3 PREVIEW ratio to what was set above.
			// 3. Set picture size as closest to 1500 4:3








			camera.setParameters(parameters);
			camera.startPreview();
		}
	}

	public Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		for (Size size : sizes) {
			Log.d("CameraPreview", "Size: " + size.width + ", " + size.height);
		}
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

