package com.example.filip.chemeq;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.filip.chemeq.detecting.ChemBase;
import com.example.filip.chemeq.tracking.MultiBoxTracker;
import com.example.filip.chemeq.util.BorderedText;
import com.example.filip.chemeq.util.ImageUtils;
import com.example.filip.chemeq.util.Logger;
import com.example.filip.chemeq.util.PassImage;
import com.example.filip.chemeq.util.ThreadUtils;

import org.json.JSONException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class CameraActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private final Logger LOGGER = new Logger(CameraActivity.class.getName());

    private static final int PERMISSIONS_REQUEST = 1;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(1440, 1080);

    private Handler handler;
    private HandlerThread handlerThread;
    private boolean useCamera2API;
    private boolean isProcessingFrame = false;

    private byte[][] yuvBytes = new byte[3][];
    private int yRowStride;
    private int[] rgbBytes = null;
    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Runnable imageConverter;
    private Runnable postInferenceCallback;

    private Button captureBtn;

    // Configuration values for yolo
    private static final int TF_OD_API_INPUT_SIZE = 416;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "yolo_n.tflite";
    private static final String TF_OD_API_LABELS_FILE = "yolo.txt";

    //
    // For detection
    //
    private boolean debug = false;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.f; // the limit is set in TFLiteYOLODetectionAPI
    private Integer sensorOrientation;
    private static final float TEXT_SIZE_DIP = 10;
    private BorderedText borderedText;
    private MultiBoxTracker tracker;
    private Classifier detector;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    OverlayView trackingOverlay;
    private long lastProcessingTimeMs;
    private long timestamp = 0;
    private boolean computingDetection = false;
    private byte[] luminanceCopy;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private List<Recognition> results = new ArrayList<>();

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGGER.d("OnCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (hasPermission()){
            setFragment();
        } else {
            LOGGER.d("No permission to use camera, permission requested");
            requestPermission();
        }

        captureBtn = findViewById(R.id.captureBtn);
        captureBtn.setOnClickListener(v -> captureImage());

        // needs the context to load from assets :/
        try {
            ChemBase.loadJSON(this);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private boolean cancelWork = false;

    /** On Capture button click **/
    public void captureImage() {
        LOGGER.i("1 CAPTURE BUTTON CLICKED ON THREAD: " + ThreadUtils.getThreadId());
        cancelWork = true;
        //TODO Make safer, this doesnt have to work
        // SAVE IMAGE
        Bitmap bitmap = Bitmap.createBitmap(DESIRED_PREVIEW_SIZE.getWidth(),
                DESIRED_PREVIEW_SIZE.getHeight(), Bitmap.Config.ARGB_8888);
        bitmap.setPixels(getRgbBytes(), 0, DESIRED_PREVIEW_SIZE.getWidth(),
                0, 0, DESIRED_PREVIEW_SIZE.getWidth(), DESIRED_PREVIEW_SIZE.getHeight());

        PassImage.setImage(bitmap);

        Intent intent = new Intent(this, DetectorActivity.class);
        Matrix frameToCanvasMatrix = tracker.getFrameToCanvasMatrix();
        float[] values = new float[9];
        frameToCanvasMatrix.getValues(values);
        intent.putExtra("matrix_values", values);
        LOGGER.i("4 MATRIX PASSED TO INTENT");

        results = tracker.getTrackedRecognitions();
        LOGGER.d("Putting " + results.size() + " results into Intent");
        intent.putExtra("results", (Serializable) results);
        startActivity(intent);
        LOGGER.d("5 STARTING NEW ACTIVITY");
    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    protected void setFragment() {

        // Xiaomi Redmi 5 doesn't support Camera2API
        // need to use deprecated API
        //TODO add fragment layout
        LOGGER.d("Setting fragment");
        int layout = R.layout.camera_connection_fragment;
        Fragment fragment = new LegacyCameraConnectionFragment(this,
                layout, DESIRED_PREVIEW_SIZE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.upper, fragment).commit();

    }

    private boolean hasPermission() {
        return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(CameraActivity.this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        //LOGGER.d("OnPreviewFrame");
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame");
            return;
        }

        try {
            // First time calling the function, initialize rgbBytes
            // choose preview Size and call onPreviewSizeChosen
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewWidth, previewHeight), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
            new Runnable() {
                @Override
                public void run() {
                    ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                }
            };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }

    //
    //LIFECYCLE
    //
    @Override
    protected void onStart() {
        LOGGER.d("onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        LOGGER.d("onResume");
        cancelWork = false;
        super.onResume();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        computingDetection = false;
    }

    //TODO solve the first run thing
    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);

        if (!isFinishing()) {
            //LOGGER.d("Requesting finish");
            //finish();
        }

        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
        super.onPause();
    }

    @Override
    public synchronized void onStop() {
        LOGGER.d("onStop " + this);
        super.onStop();
    }

    @Override
    public synchronized void onDestroy() {
        LOGGER.d("onDestroy " + this);
        super.onDestroy();
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            debug = !debug;
            requestRender();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void requestRender() {
        final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.postInvalidate();
        }
    }

    public void addCallback(final OverlayView.DrawCallback callback) {
        final OverlayView overlay = findViewById(R.id.debug_overlay);
        if (overlay != null) {
            overlay.addCallback(callback);
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    // Functions for detection
    //TODO implement these abstract functions
    // Sets up the classifier
    protected void onPreviewSizeChosen(final Size size, final int rotation){
        LOGGER.d("onPreviewSizeChosen");
        float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);
        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteYoloDetectionAPI.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: " + sensorOrientation);

        LOGGER.i("Initializing at size " + previewWidth + "x" + previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);
        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        if (!isDebug()) {
                            return;
                        }
                        final Bitmap copy = cropCopyBitmap;
                        if (copy == null) {
                            return;
                        }

                        final int backgroundColor = Color.argb(100, 0, 0, 0);
                        canvas.drawColor(backgroundColor);

                        final Matrix matrix = new Matrix();
                        final float scaleFactor = 2;
                        matrix.postScale(scaleFactor, scaleFactor);
                        matrix.postTranslate(
                                canvas.getWidth() - copy.getWidth() * scaleFactor,
                                canvas.getHeight() - copy.getHeight() * scaleFactor);
                        canvas.drawBitmap(copy, matrix, new Paint());

                        final Vector<String> lines = new Vector<String>();
                        if (detector != null) {
                            final String statString = detector.getStatString();
                            final String[] statLines = statString.split("\n");
                            for (final String line : statLines) {
                                lines.add(line);
                            }
                        }
                        lines.add("");

                        lines.add("Frame: " + previewWidth + "x" + previewHeight);
                        lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
                        lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
                        lines.add("Rotation: " + sensorOrientation);
                        lines.add("Inference time: " + lastProcessingTimeMs + "ms");

                        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
                    }
                });
    }

    protected void processImage(){
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);

        if (cancelWork) {
            LOGGER.d("Canceling work in processImage");
            return;
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        List<Recognition> results = detector.recognizeImage(croppedBitmap);
                        LOGGER.i("Detected " + results.size() + " equations");

                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Recognition> mappedRecognitions =
                                new LinkedList<Recognition>();

                        for (final Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                //what is this for??
                                canvas.drawRect(location, paint);
                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }
                        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                        trackingOverlay.postInvalidate();

                        requestRender();
                        computingDetection = false;
                    }
                }
        );

    }
}
