package com.example.ruanna.simplecamtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	SurfaceHolder holder;
	Camera camera;
	private int lastReportedWidth;
	private int lastReportedHeight;

	private static final String TAG = "CameraPreviewLog";
	private CameraPreviewProvider previewProvider;
	private Camera.PictureCallback mPicture;

	public CameraPreview(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		Log.d(TAG, "Constructing CameraPreview");

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			previewProvider = new CameraPreviewGingerbread();
		}
		mPicture = getPictureCallback();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "SurfaceCreated");
		preparePreview();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		Log.d(TAG, "SurfaceDestroyed");
		stopPreview();
	}

	private void preparePreview() {
		Log.d(TAG, "preparePreview");
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		try {
			camera = previewProvider.openCamera();
			camera.setPreviewDisplay(holder);
		} catch (Exception exception) {
			if (camera != null) {
				camera.release();
			}
			camera = null;
		}
	}

	private void stopPreview() {
		Log.d(TAG, "stopPreview");
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(TAG, "SurfaceChanged");
		this.lastReportedWidth = w;
		this.lastReportedHeight = h;
		if (camera != null) {
			startPreview();
		}
	}

	private void startPreview() {
		Log.d(TAG, "startPreview");
		// Now that the size is known, set up the camera parameters and begin
		// the preview.
		Display display = ((WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE)).getDefaultDisplay();
		previewProvider.startPreview(camera, lastReportedWidth,
				lastReportedHeight, display);
	}

	public void takePicture() {
		camera.autoFocus(
			new Camera.AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					camera.takePicture(null, null, mPicture);
				}
			}
		);
	}

	private Camera.PictureCallback getPictureCallback() {
		Camera.PictureCallback picture = new Camera.PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				//make a new picture file
				File pictureFile = getOutputMediaFile();

				if (pictureFile == null) {
					return;
				}

				Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bmp == null) {
					Toast.makeText(getContext(), "No data returned from camera", Toast.LENGTH_LONG).show();
				} else {
					int width = bmp.getWidth();
					int height = bmp.getHeight();

					Log.d("TEST","width="+width);
					Log.d("TEST","height"+height);

					int[] pixels = new int[width * height];
					bmp.getPixels(pixels, 0, width, 0, 0, width, height);
					bmp.recycle();
					bmp = null;

					LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
					Reader reader = new MultiFormatReader();
					try {
						Result result = reader.decode(bitmap);
						String contents = result.getText();
						byte[] rawBytes = result.getRawBytes();
						BarcodeFormat format = result.getBarcodeFormat();
						ResultPoint[] points = result.getResultPoints();


						Toast toast1 = Toast.makeText(getContext(), "CONTENTS: " + contents, Toast.LENGTH_LONG);
						toast1.show();



						//write the file
						FileOutputStream fos = new FileOutputStream(pictureFile);
						fos.write(data);
						fos.close();
						Toast toast = Toast.makeText(getContext(), "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
						toast.show();


					} catch (NotFoundException | ChecksumException | FormatException e) {
						e.printStackTrace();
						Toast toast = Toast.makeText(getContext(), "NO QR found!", Toast.LENGTH_LONG);
						toast.show();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

				//refresh camera to continue preview
				try {
					camera.stopPreview();
					camera.setPreviewDisplay(holder);
					camera.startPreview();
				} catch (Exception e) {
					// ignore: tried to stop a non-existent preview
				}

			}
		};
		return picture;
	}

	/**
	 * make picture and save to a folder
	 * @return
	 */
	private static File getOutputMediaFile() {
		//make a new file directory inside the "sdcard" folder
		File mediaStorageDir = new File("/sdcard/", "AAACamScanPOC");

		//if this "JCGCamera folder does not exist
		if (!mediaStorageDir.exists()) {
			//if you cannot make this folder return
			if (!mediaStorageDir.mkdirs()) {
				return null;
			}
		}

		//take the current timeStamp
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
		File mediaFile;
		//and make a media file:
		mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

		return mediaFile;
	}
}
