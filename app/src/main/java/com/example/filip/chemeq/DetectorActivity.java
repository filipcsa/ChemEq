package com.example.filip.chemeq;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.example.filip.chemeq.detecting.AdjustableRecognitionRect;
import com.example.filip.chemeq.detecting.Equation;
import com.example.filip.chemeq.detecting.RecognitionAdapter;
import com.example.filip.chemeq.detecting.RecognitionListItem;
import com.example.filip.chemeq.ocr.TessOCRAnalyzer;
import com.example.filip.chemeq.detecting.DrawView;
import com.example.filip.chemeq.util.ImageUtils;
import com.example.filip.chemeq.util.Logger;
import com.example.filip.chemeq.util.PassImage;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectorActivity extends AppCompatActivity {

    private ImageView imageView;
    private Bitmap image;
    private DrawView drawView;

    private ListView listView;
    private RecognitionAdapter listAdapter;
    private List<RecognitionListItem> recognitionList = new ArrayList<>();

    private List<Recognition> results;
    private Matrix canvasToFrameMatrix = new Matrix();
    private Matrix frameToCanvasMatrix = new Matrix();

    private final Logger LOGGER = new Logger(DetectorActivity.class.getName());

    private TessOCRAnalyzer tessOCR;

    private static final String path = Environment.getExternalStorageDirectory().toString() + "/dataset";
    private static int HIGHEST_FILENAME = getHighestFilename();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LOGGER.d("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detector);

        imageView = findViewById(R.id.imageView);
        listView = findViewById(R.id.listView);

        image = PassImage.getImage();
        imageView.setImageBitmap(rotateBitmap(image));

        results = (List<Recognition>) getIntent().getSerializableExtra("results");

        // Create ArrayAdapter using the planet list
        listAdapter = new RecognitionAdapter(this, recognitionList);
        listView.setAdapter(listAdapter);

        frameToCanvasMatrix = ImageUtils.getFrameToCanvasMatrix();
        ImageUtils.getFrameToCanvasMatrix().invert(canvasToFrameMatrix);

        tessOCR = new TessOCRAnalyzer(image, canvasToFrameMatrix);

        LOGGER.i("Detected results: " + results.size());

        // init the draw view with adjustable recognition rectangles
        this.drawView = findViewById(R.id.drawView);
        drawView.initDrawView(this, results);

        // adjust rectangles and run ocr for the first time
        adjustResults();
        drawView.runOCROnAllAdjustableRects();

        // BUTTONS
        Button addBtn = findViewById(R.id.addButton);
        addBtn.setOnClickListener(v -> onAddButton());

        Button removeBtn = findViewById(R.id.removeButton);
        removeBtn.setOnClickListener(v -> onRemoveButton());

        Button saveBtn = findViewById(R.id.saveButton);
        saveBtn.setOnClickListener(v -> onSaveButton());

    }

    public void looseFocus() {
        listAdapter.looseFocus();
    }

    /** On add rect button clicked **/
    private void onAddButton() {
        LOGGER.i("ADD BUTTON CLICKED");
        drawView.createNewAdjustableRecognitionRect();
    }

    /** On remove rect button clicked if there is a selected rect **/
    private void onRemoveButton() {
        LOGGER.i("REMOVE BUTTON CLICKED");
        drawView.removeSelectedAdjustableRecognitionRect();
    }

    /** Saves the image as png and create data about rectangles for training / testing **/
    private void onSaveButton() {
        LOGGER.i("SAVE BUTTON CLICKED");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Wanna save example to dataset?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
        switch (which){
            // save example and finish current activity
            case DialogInterface.BUTTON_POSITIVE:
                LOGGER.i("SAVING EXAMPLE");
                saveExampleToDataset();
                finish();
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                LOGGER.i("NOT SAVING EXAMPLE");
                break;
        }
    };

    private void saveExampleToDataset() {
        // the IMAGE
        File file = new File(path + "/" + HIGHEST_FILENAME + ".png");
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);
            rotateBitmap(image).compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.i("Problem while saving image");
        }

        // the TEXT
        file = new File(path + "/" + HIGHEST_FILENAME + ".txt");
        try {
            FileWriter writer = new FileWriter(file);
            writer.append(getTrainingDataFromRectangles());
            writer.flush();
            writer.close();
            MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.i("Problem while saving the text file");
        }
        HIGHEST_FILENAME++;
    }

    private String getTrainingDataFromRectangles() {
        String data = "";
        Matrix rotateMatrix = new Matrix();
        rotateMatrix.postRotate(90);
        List<AdjustableRecognitionRect> adjustableRects = drawView.getAdjustableRecognitionRects();
        for (AdjustableRecognitionRect ar : adjustableRects) {
            RectF rect = ar.getLocation();
            canvasToFrameMatrix.mapRect(rect);
            rotateMatrix.mapRect(rect);

            // that part with the 1 + ... is weird but it works although i don't know why
            // probably something with the rotation matrix, rect.left is negative
            float xRelativeCenter = 1 + (rect.left + (rect.width() / 2)) / 1080;
            float yRelativeCenter = (rect.top + (rect.height() / 2)) / 1440;
            float relativeWidth = rect.width() / 1080;
            float relativeHeight = rect.height() / 1440;

            LOGGER.i("X_center: " + xRelativeCenter);

            data += 0 + " " + xRelativeCenter + " " + yRelativeCenter + " " +
                    relativeWidth + " " + relativeHeight + "\n";
        }
        return data;
    }


    public void addRecognitionListItem(RecognitionListItem recognitionListItem) {
        this.recognitionList.add(recognitionListItem);
        listAdapter.notifyDataSetChanged();
    }

    public void removeRecognitionListItem(RecognitionListItem recognitionListItem) {
        this.recognitionList.remove(recognitionListItem);
        listAdapter.notifyDataSetChanged();
    }

    /** Gonna be called when the adjustable rectangle is adjusted */
    public void runOCRForRectangle(RectF rect, RecognitionListItem recognitionListItem) {
        canvasToFrameMatrix.mapRect(rect);
        Bitmap croppedResult = null;
        try {
            croppedResult = Bitmap.createBitmap(image,
                    (int) rect.left, (int) rect.top, (int) (rect.right - rect.left), (int) (rect.bottom - rect.top));
        } catch (IllegalArgumentException e) {
            LOGGER.e(e, "Wrong coordinates of rectangle");
            return;
        }
        // RecognitionListItem rli = tessOCR.doOCRonSingleExample(rotateBitmap(croppedResult));
        // tessOCR.doOCR4Rectangle(newRect);
        Equation equation = tessOCR.testOCR(rotateBitmap(croppedResult));
        recognitionListItem.setEquationTest(equation);
        // recognitionListItem.setAll(rli);
        listAdapter.notifyDataSetChanged();
        LOGGER.i("There are " + listAdapter.getCount() + " in list adapter");
    }

    private Bitmap rotateBitmap(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static int getHighestFilename() {
        File directory = new File(path);
        // create if non existent
        if (!directory.exists())
            directory.mkdirs();
        File[] files = directory.listFiles();
        // each example of the dataset has 2 files: a png and a txt file, therefore i divide by 2
        return files.length / 2;
    }


    private void adjustResults() {

        if(OpenCVLoader.initDebug()){
            LOGGER.i("OpenCV loaded");
        }else {
            LOGGER.i("OpenCV failed");
        }
        Bitmap threshImage = image.copy(image.getConfig(), true);
        Mat imageMat = new Mat();
        Utils.bitmapToMat(threshImage, imageMat);
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(imageMat, imageMat, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 131, 24);
        Utils.matToBitmap(imageMat, threshImage);

        List<AdjustableRecognitionRect> rects = drawView.getAdjustableRecognitionRects();

        for (AdjustableRecognitionRect rect : rects) {
            RectF location = rect.getLocation();
            canvasToFrameMatrix.mapRect(location);

            // TODO finish the bottom part, run ocr after this, not before
            // try to locate a larger whitespace on left and right side
            // and only then locate the text
            // THE RIGHT PART WHICH IS THE TOP BEFORE ROTATING
            if (location.top - 100 >= 0) location.top = location.top - 100;
            else location.top = 0;
            int size = (int) (location.right - location.left);
            int[] pixels = new int[size];
            threshImage.getPixels(pixels, 0, size, (int)location.left, (int)location.top, size, 1);
            int average = average(pixels);
            while (average > -300000) {
                location.top = location.top + 2;
                if (location.top > threshImage.getHeight() - 1) break;
                threshImage.getPixels(pixels, 0, size, (int)location.left, (int)location.top, size, 1);
                average = average(pixels);
            }

            // THE LEFT PART WHICH IS THE BOTTOM BEFORE ROTATING
            if (location.bottom + 100 < threshImage.getHeight()) location.bottom += 100;
            else location.bottom = threshImage.getHeight() - 1;
            threshImage.getPixels(pixels, 0, size, (int)location.left, (int)location.bottom, size, 1);
            average = average(pixels);
            while (average > -400000) {
                location.bottom -= 2;
                if (location.bottom < location.top) break;
                threshImage.getPixels(pixels, 0, size, (int)location.left, (int)location.bottom, size, 1);
                average = average(pixels);
            }

            // THE UPPER PART WHICH IS THE LEFT BEFORE ROTATING
            if (location.left < 0) location.left = 0;
            size = (int) (location.bottom - location.top);
            if (size < 1) continue;
            pixels = new int[size];
            //threshImage.getPixels(pixels, 0, threshImage.getWidth(), (int) location.left, (int)location.top, 1, size);
            pixels = BitmapHelper.getBitmapPixels(threshImage, (int)location.left, (int)location.top, 1, size);
            average = average(pixels);
            // the top rank should grow (left -)
            while (average < -180000) {
                location.left -= 2;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int)location.left, (int)location.top, 1, size);
                average = average(pixels);
            }
            while (average > -180000) {
                location.left += 1;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int)location.left, (int)location.top, 1, size);
                average = average(pixels);
            }

            // THE LOWER PART WHICH IS THE RIGHT BEFORE ROTATING
            pixels = BitmapHelper.getBitmapPixels(threshImage, (int)location.right, (int)location.top, 1, size);
            average = average(pixels);
            while (average < -200000) {
                location.right += 2;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int)location.right, (int)location.top, 1, size);
                average = average(pixels);
            }
            while (average > -200000) {
                location.right -= 1;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int)location.right, (int)location.top, 1, size);
                average = average(pixels);
            }

            // set the adjustable rectangle to the adjusted location
            frameToCanvasMatrix.mapRect(location);
            rect.setLocation(location);
        }


        // save for debug
        File file = new File(path + "/" + "THRESH" + ".png");
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);
            threshImage.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.i("Problem while saving image");
        }
    }


    private int average(int[] data) {
        int sum = 0;
        for (int d : data) sum += d;
        return sum / data.length;
    }
}

class BitmapHelper {

    public static int[] getBitmapPixels(Bitmap bitmap, int x, int y, int width, int height) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), x, y,
                width, height);
        final int[] subsetPixels = new int[width * height];
        for (int row = 0; row < height; row++) {
            System.arraycopy(pixels, (row * bitmap.getWidth()),
                    subsetPixels, row * width, width);
        }
        return subsetPixels;
    }
}
