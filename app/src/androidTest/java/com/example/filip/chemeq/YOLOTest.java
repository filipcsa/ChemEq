package com.example.filip.chemeq;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;

import com.example.filip.chemeq.detecting.AdjustableRecognitionRect;
import com.example.filip.chemeq.tracking.MultiBoxTracker;
import com.example.filip.chemeq.util.ImageUtils;

import org.junit.Before;
import org.junit.Test;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class YOLOTest {

    Context testContext;
    Classifier detector;
    String modelFilename = "yolo_n.tflite";
    String labelFilename = "yolo.txt";
    int inputSize = 416;
    Matrix frameToCropTransform = new Matrix();
    Matrix cropToFrameTransform = new Matrix();
    MultiBoxTracker tracker;

    // problem 134
    String[] testSamples = {"195.png"}; //, "10.png", "101.png", "102.png", "103.png", "105.png", "106.png", "107.png", "108.png", "11.png", "113.png", "114.png", "115.png", "116.png", "118.png", "12.png", "123.png", "124.png", "127.png", "128.png", "129.png", "132.png", "134.png", "137.png", "139.png", "14.png", "141.png", "146.png", "148.png", "151.png", "152.png", "153.png", "154.png", "155.png", "156.png", "158.png", "159.png", "161.png", "162.png", "164.png", "166.png", "168.png", "17.png", "171.png", "172.png", "174.png", "175.png", "179.png", "18.png", "180.png", "181.png", "186.png", "189.png", "19.png", "190.png", "192.png", "194.png", "195.png", "196.png", "199.png", "2.png", "20.png", "200.png", "201.png", "203.png", "205.png", "207.png", "208.png", "21.png", "210.png", "211.png", "212.png", "213.png", "214.png", "218.png", "221.png", "224.png", "228.png", "229.png", "23.png", "230.png", "231.png", "232.png", "233.png", "234.png", "235.png", "236.png", "237.png", "238.png", "239.png", "240.png", "241.png", "242.png", "244.png", "245.png", "246.png", "247.png", "248.png", "249.png", "250.png", "251.png", "252.png", "255.png", "256.png", "30.png", "31.png", "35.png", "36.png", "38.png", "39.png", "4.png", "41.png", "42.png", "43.png", "44.png", "45.png", "46.png", "48.png", "49.png", "50.png", "51.png", "52.png", "53.png", "57.png", "58.png", "59.png", "6.png", "60.png", "61.png", "62.png", "65.png", "66.png", "67.png", "7.png", "71.png", "73.png", "75.png", "76.png", "77.png", "78.png", "79.png", "8.png", "80.png", "83.png", "84.png", "85.png", "86.png", "88.png", "89.png", "93.png", "97.png"};


    @Before
    public void setUp() {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        try {
            detector = TFLiteYoloDetectionAPI.create(testContext.getAssets(), modelFilename,
                    labelFilename, inputSize, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        1080, 1440,
                        416, 416,
                        0, false);
        frameToCropTransform.invert(cropToFrameTransform);
        tracker = new MultiBoxTracker(testContext);


    }
    String folder = "dataset/";
    //String testSample = "2.png";

    @Test
    public void test1() {
        AssetManager assetManager = testContext.getAssets();
        int samples = 0;
        long dur = 0;

        for (String testSample : testSamples) {
            InputStream testInput = null;
            try {
                testInput = assetManager.open(folder + testSample);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Bitmap orig = BitmapFactory.decodeStream(testInput);

            Bitmap bitmap = Bitmap.createScaledBitmap(orig, 416, 416, false);
            //Bitmap.createBitmap(orig, 0, 0, 416, 416, frameToCropTransform, false);
            //saveImage(bitmap);
            Instant before = Instant.now();
            testOnImage(bitmap, testSample);
            Instant after = Instant.now();
            dur += Duration.between(before, after).toMillis();
            samples++;
        }

        String string = "Duration: " + dur + " ms\n";
        saveStringToFile(string, "test");

    }

    String folderPre = "pre/";
    String folderPost = "post/";

    public void testOnImage(Bitmap bitmap, String testSample) {
        List<Recognition> recognitions = detector.recognizeImage(bitmap);
        String size = "Recognitions: " + recognitions.size();
        removeSameResults(recognitions);

        String results = "";

        for (Recognition recognition : recognitions) {
            recognition.getLocation();
            RectF location = recognition.getLocation();
            cropToFrameTransform.mapRect(location);

            float relCenterX = location.centerX() / 1080;
            float relCenterY = location.centerY() / 1440;
            float relWidth = location.width() / 1080;
            float relHeight = location.height() / 1440;

            results += "0 " + relCenterX + " " + relCenterY + " " + relWidth + " " + relHeight + "\n";

        }
        saveStringToFile(results, folderPre + testSample);

        adjustResults(recognitions, bitmap);
        results = "";
        for (Recognition recognition : recognitions) {
            recognition.getLocation();
            RectF location = recognition.getLocation();
            cropToFrameTransform.mapRect(location);

            float relCenterX = location.centerX() / 1080;
            float relCenterY = location.centerY() / 1440;
            float relWidth = location.width() / 1080;
            float relHeight = location.height() / 1440;

            results += "0 " + relCenterX + " " + relCenterY + " " + relWidth + " " + relHeight + "\n";
        }
        saveStringToFile(results, folderPost + testSample);


    }



    public void saveStringToFile(String string, String filename) {
        filename += ".txt";
        File testFile = new File(Environment.getExternalStorageDirectory().toString(), filename);
        FileWriter writer;
        try {
            writer = new FileWriter(testFile);
            writer.append(string);
            writer.flush();
            writer.close();
            MediaScannerConnection.scanFile(testContext, new String[] { testFile.getAbsolutePath() }, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveImage(Bitmap bitmap) {
        File file = new File(Environment.getExternalStorageDirectory().toString(), "img.png");
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            MediaScannerConnection.scanFile(testContext, new String[] { file.getAbsolutePath() }, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeSameResults(List<Recognition> results){
        List<Recognition> copyResults = new ArrayList<>(results);
        int numResults = results.size();
        for (int i = 0; i < numResults; i++){
            for (int j = i+1; j < numResults; j++){
                RectF a = copyResults.get(i).getLocation();
                RectF b = copyResults.get(j).getLocation();

                if (RectF.intersects(a, b)){
                    if (copyResults.get(i).getConfidence() >= copyResults.get(j).getConfidence())
                        results.remove(copyResults.get(j));
                    else
                        results.remove(copyResults.get(i));
                }
            }
        }
    }

    public void adjustResults(List<Recognition> recognitions, Bitmap image) {
        Matrix m1 = new Matrix();
        m1.postRotate(90);

        if (OpenCVLoader.initDebug()) {
            saveStringToFile("OpenCVLoaded", "opencv");
        } else {
            saveStringToFile("Fail", "opencv");
        }


        Bitmap threshImage = image.copy(image.getConfig(), true);
        Mat imageMat = new Mat();
        Mat dest = new Mat();
        Utils.bitmapToMat(threshImage, imageMat);
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);
        Utils.matToBitmap(imageMat, threshImage);;
        Imgproc.adaptiveThreshold(imageMat, dest, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 131, 24);
        Utils.matToBitmap(dest, threshImage);
        saveImage(threshImage);

        for (Recognition rect : recognitions) {
            RectF location = rect.getLocation();

            if (location.left - 100 >= 0) location.left = location.left - 100;
            int size = (int) (location.bottom - location.top);
            if (size < 1) continue;
            int[] pixels;
            //threshImage.getPixels(pixels, 0, threshImage.getWidth(), (int) location.left, (int)location.top, 1, size);
            pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.left, (int) location.top, 1, size);
            int average = average(pixels);
            // the top rank should grow (left -)
            while (average < -180000) {
                location.left -= 2;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.left, (int) location.top, 1, size);
                average = average(pixels);
            }
            while (average > -180000) {
                location.left += 1;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.left, (int) location.top, 1, size);
                average = average(pixels);
            }

            //if (location.right + 100 < 1080) location.right = location.right + 100;
            pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.right, (int) location.top, 1, size);
            average = average(pixels);
            while (average < -180000) {
                location.right += 2;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.right, (int) location.top, 1, size);
                average = average(pixels);
            }
            while (average > -180000) {
                location.right -= 1;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.right, (int) location.top, 1, size);
                average = average(pixels);
            }


            size = (int) (location.right - location.left);
            pixels = new int[size];
            threshImage.getPixels(pixels, 0, size, (int) location.left, (int) location.top, size, 1);
            average = average(pixels);
            while (average < -180000) {
                location.top = location.top - 2;
                if (location.top <= 0) break;
                threshImage.getPixels(pixels, 0, size, (int) location.left, (int) location.top, size, 1);
                average = average(pixels);
            }

            threshImage.getPixels(pixels, 0, size, (int) location.left, (int) location.bottom, size, 1);
            average = average(pixels);
            while (average < -200000) {
                location.bottom += 2;
                if (location.bottom < location.top) break;
                threshImage.getPixels(pixels, 0, size, (int) location.left, (int) location.bottom, size, 1);
                average = average(pixels);
            }


            rect.setLocation(location);

/*
            // THE LEFT PART WHICH IS THE BOTTOM BEFORE ROTATING
            if (location.bottom + 100 < threshImage.getHeight()) location.bottom += 100;
            else location.bottom = threshImage.getHeight() - 1;
            threshImage.getPixels(pixels, 0, size, (int) location.left, (int) location.bottom, size, 1);
            average = average(pixels);
            while (average > -400000) {
                location.bottom -= 2;
                if (location.bottom < location.top) break;
                threshImage.getPixels(pixels, 0, size, (int) location.left, (int) location.bottom, size, 1);
                average = average(pixels);
            }


            // THE LOWER PART WHICH IS THE RIGHT BEFORE ROTATING
            pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.right, (int) location.top, 1, size);
            average = average(pixels);
            while (average < -200000) {
                location.right += 2;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.right, (int) location.top, 1, size);
                average = average(pixels);
            }
            while (average > -200000) {
                location.right -= 1;
                pixels = BitmapHelper.getBitmapPixels(threshImage, (int) location.right, (int) location.top, 1, size);
                average = average(pixels);
            }

            */

        }
    }

    private int average(int[] data) {
        int sum = 0;
        for (int d : data) sum += d;
        return sum / data.length;
    }
}
