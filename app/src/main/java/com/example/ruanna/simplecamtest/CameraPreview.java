package com.example.ruanna.simplecamtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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

import java.io.ByteArrayOutputStream;
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
	private boolean hasSurface = false;

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
		hasSurface = true;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		Log.d(TAG, "SurfaceDestroyed");
		hasSurface = false;
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
		Display display =
			(
				(WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)
			).getDefaultDisplay();
		previewProvider.startPreview(camera, lastReportedWidth,lastReportedHeight, display);
	}

	public void takePicture() {
		if (hasSurface) {
			camera.autoFocus(
				new Camera.AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						camera.takePicture(null, null, mPicture);
					}
				}
			);
		}
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
					float width = bmp.getWidth();
					float height = bmp.getHeight();

					Log.d("TEST","actual photo width="+width);
					Log.d("TEST","actual photo height"+height);


					//TODO this will have to be calculated in % from what the width and height are
					//my test values are 1632, 1224
					int qrAreaWidth = 632;
					int qrAreaHeight = 600;
					int[] pixels = new int[qrAreaWidth * qrAreaHeight];
					bmp.getPixels(pixels, 0, qrAreaWidth, 1000, 0, qrAreaWidth, qrAreaHeight);

					//TODO update below
					LuminanceSource source = new RGBLuminanceSource(qrAreaWidth, qrAreaHeight, pixels);
					BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
					Reader reader = new MultiFormatReader();
					try {
						Result result = reader.decode(bitmap);
						String contents = result.getText();
						byte[] rawBytes = result.getRawBytes();
						BarcodeFormat format = result.getBarcodeFormat();
						ResultPoint[] points = result.getResultPoints();


						Log.d("TEST","==================");
						for (ResultPoint wer : points) {
							Log.d("TEST","x="+ wer.getX());
							Log.d("TEST","y="+ wer.getY());
						}

						Toast toast1 = Toast.makeText(getContext(), "CONTENTS: " + contents, Toast.LENGTH_LONG);
						toast1.show();


						//write the file
						FileOutputStream fos = new FileOutputStream(pictureFile, true);




						Log.d("TESTWR", "BEFORE width=" + width);
						Log.d("TESTWR", "BEFORE height=" + height);


						int savingWidth = (int) Math.ceil(width / 100 * 80);
						int xStart = (int) Math.ceil(width / 100 * 10);

						int savingHeight = (int) (height / 100 * 80);
						int yStart = (int) (height / 100 * 10);


						Log.d("TESTWR", "AFTER xWidth=" + savingWidth);
						Log.d("TESTWR", "AFTER yWidth=" + savingHeight);


						Bitmap trimBmp = changeBitmapContrastBrightness(bmp, 1f, 75);


						Bitmap batchBitmap = Bitmap.createBitmap(trimBmp, xStart, yStart, savingWidth, savingHeight);
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						batchBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
						fos.write(bos.toByteArray());
						fos.close();
						bmp.recycle();
						bmp = null;
						Toast toast = Toast.makeText(getContext(), "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
						toast.show();


//						fos.write(data);
//						fos.close();
//						Toast toast = Toast.makeText(getContext(), "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
//						toast.show();


						System.gc();


//
//
//
//
//						//TODO this will have to be calculated in % from what the width and height are
//						//my test values are 1632, 1224
//						//save the image a 100 rows at a time
//
//
//						int rowsDone = 0;
//						int[] batchPixels;
//						Bitmap batchBitmap;
//						ByteArrayOutputStream bos;
//						int batchRowsAtATime = 100;
//						int rowsToGo;
//						int yBatchStart;
//
//						while (rowsDone < yWidth) {
//							rowsToGo = yWidth - rowsDone;
//							yBatchStart = rowsDone + yStart;
//							if (rowsToGo > batchRowsAtATime) {
//								rowsDone += batchRowsAtATime;
//								int[] batchPixels = new int[xWidth * batchRowsAtATime]; //Doing 100 rows at a time
//							} else {
//								rowsDone += rowsToGo;
//								batchRowsAtATime = rowsToGo;
//								batchPixels = new int[xWidth * rowsToGo];
//							}
//
//
//							Log.d("TESTWR", "xWidth=" + xWidth);
//							Log.d("TESTWR", "xStart=" + xStart);
//							Log.d("TESTWR", "yBatchStart=" + yBatchStart);
//							Log.d("TESTWR", "batchRowsAtATime=" + batchRowsAtATime);
//
//
//
//							bmp.getPixels(batchPixels, 0, xWidth, xStart, yBatchStart, xWidth, batchRowsAtATime);
//							Bitmap batchBitmap = Bitmap.createBitmap(batchPixels, 0, xWidth, xWidth, batchRowsAtATime, Bitmap.Config.ARGB_8888);
//							ByteArrayOutputStream bos = new ByteArrayOutputStream();
//							batchBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
//
//
//
//
//							Log.d("TESTWR", "write rowsDone=" + rowsDone);
//							fos.write(bos.toByteArray());
//						}
//
//
//						fos.close();
//						bmp.recycle();
//						bmp = null;
//						Toast toast = Toast.makeText(getContext(), "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
//						toast.show();


					} catch (NotFoundException | ChecksumException | FormatException e) {
						e.printStackTrace();
						Toast toast = Toast.makeText(getContext(), "NO QR found!", Toast.LENGTH_LONG);
						toast.show();
					}
					catch (FileNotFoundException e) {
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

	/**
	 *
	 * @param bmp input bitmap
	 * @param contrast 0..10 1 is default
	 * @param brightness -255..255 0 is default
	 * @return new bitmap
	 */
	public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness)
	{
		ColorMatrix cm = new ColorMatrix(new float[]
				{
						contrast, 0, 0, 0, brightness,
						0, contrast, 0, 0, brightness,
						0, 0, contrast, 0, brightness,
						0, 0, 0, 1, 0
				});

		Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

		Canvas canvas = new Canvas(ret);

		Paint paint = new Paint();
		paint.setColorFilter(new ColorMatrixColorFilter(cm));
		canvas.drawBitmap(bmp, 0, 0, paint);

		return ret;
	}

	/**
	 *
	 * @param src
	 * @return
	 */
	public static Bitmap createBlackAndWhite(Bitmap src) {
		int width = src.getWidth();
		int height = src.getHeight();
		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;

		// scan through all pixels
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);

				// use 128 as threshold, above -> white, below -> black
				if (gray > 128)
					gray = 255;
				else
					gray = 0;
				// set new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
			}
		}
		return bmOut;
	}

	public static Bitmap createGrayscale(Bitmap src) {
		int width = src.getWidth();
		int height = src.getHeight();
		// create output bitmap
		Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
		// color information
		int A, R, G, B;
		int pixel;

		// scan through all pixels
		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				// get pixel color
				pixel = src.getPixel(x, y);
				A = Color.alpha(pixel);
				R = Color.red(pixel);
				G = Color.green(pixel);
				B = Color.blue(pixel);
				int gray = (int) (0.2989 * R + 0.5870 * G + 0.1140 * B);
				// set new pixel color to output bitmap
				bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
			}
		}
		return bmOut;
	}

}
