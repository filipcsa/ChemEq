package com.example.filip.chemeq;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;

import com.example.filip.chemeq.util.BoundingBox;
import com.example.filip.chemeq.util.Logger;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 */
public class TFLiteYoloDetectionAPI implements Classifier {
    private static final Logger LOGGER = new Logger(TFLiteYoloDetectionAPI.class.getName());

    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;
    // Number of threads in the java app
    private static final int NUM_THREADS = 16;

    private ByteBuffer imgData;
    // Quantized model deals with smaller number bitwise
    private boolean isModelQuantized;
    // Size og the input, basically width*height of the input 'picture'
    private int inputSize;
    // Vector holding labels of classes to detect
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;


    // the output tensor
    private float[][][][] output;
    //yolo grid of cells dimension
    private static final int GRID_WIDTH = 13;
    private static final int GRID_HEIGHT = 13;
    private static final int NUM_BOXES_PER_CELL = 5;
    // TODO recalculate anchors!
    private final static double anchors[] = {162,22, 204,25, 249,27, 304,27, 374,27};
    //{1.08,1.19,  3.42,4.41,  6.63,11.38,  9.42,5.11,  16.62,10.52};
    private static final double MIN_CONFIDENCE = 0.2;

    private Interpreter tfLite;

    /** Memory-map the model file in Assets. */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        LOGGER.i("loadModelFileDone");
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize The size of image input
     * @param isQuantized Boolean representing model is quantized or not
     */
    public static Classifier create(AssetManager assetManager, String modelFilename, String labelFilename,
                                    int inputSize, boolean isQuantized) throws IOException {
        TFLiteYoloDetectionAPI d = new TFLiteYoloDetectionAPI();

        InputStream labelsInput = assetManager.open(labelFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            LOGGER.w(line);
            d.labels.add(line);
        }
        br.close();

        Interpreter.Options tfliteOptions = new Interpreter.Options();
        tfliteOptions.setNumThreads(NUM_THREADS);
        tfliteOptions.setAllowFp16PrecisionForFp32(true);
        //tfliteOptions.setUseNNAPI(true);

        LOGGER.i("Loading model ...");
        d.inputSize = inputSize;
        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename), tfliteOptions);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOGGER.i("Model loaded!");

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        // TODO do something with NNAPI
        //d.tfLite.setUseNNAPI(true);
        //d.tfLite.setNumThreads(8);
        // TODO count the last value dynamically for yolov2
        // 13 13 30
        d.output = new float[1][GRID_WIDTH][GRID_HEIGHT][30];
        return d;
    }

    @Override
    public List<Recognition> recognizeImage(Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection(); // preprocessBitmap

        // Run the inference call.
        Trace.beginSection("run");
        tfLite.run(imgData, output);
        Trace.endSection();

        // BoundingBox have center coordinates, width, height
        // whereas Recognition has RectF
        List<BoundingBox> orderedBBs = processOutput(output);
        final List<Recognition> recognitions = getTopRecognitions(orderedBBs);
        Trace.endSection(); // "recognizeImage"
        return recognitions;
    }

    private List<BoundingBox> processOutput(float[][][][] tfLiteOutput) {
        float[][][] tensor = tfLiteOutput[0];
        float blockSize = 32f; //416 / 13

        ArrayList<BoundingBox> allPredictions = new ArrayList<>();

        for (int cy = 0; cy < GRID_HEIGHT; cy++) {
            for (int cx = 0; cx < GRID_WIDTH; cx++) {
                for (int b = 0; b < NUM_BOXES_PER_CELL; b++) {
                    BoundingBox prediction = new BoundingBox();
                    prediction.setX((cx + sigmoid(tensor[cy][cx][(NUM_BOXES_PER_CELL + 1) * b + 0])) * blockSize);
                    prediction.setY((cy + sigmoid(tensor[cy][cx][(NUM_BOXES_PER_CELL + 1) * b + 1])) * blockSize);
                    prediction.setWidth((float) (Math.exp(tensor[cy][cx][(NUM_BOXES_PER_CELL + 1) * b + 2]) * anchors[2 * b] * 32));
                    prediction.setHeight((float) (Math.exp(tensor[cy][cx][(NUM_BOXES_PER_CELL + 1) * b + 3]) * anchors[2 * b + 1] * 32));
                    prediction.setConfidence(sigmoid(tensor[cy][cx][(NUM_BOXES_PER_CELL + 1) * b + 4]));
                    allPredictions.add(prediction);
                }
            }
        }
        List<BoundingBox> sortedPredictions = allPredictions.stream()
                .sorted(Comparator.comparing(BoundingBox::getConfidence).reversed())
                .collect(Collectors.toList());
        return sortedPredictions;
    }

    private float sigmoid(float x){
        return (float) (1.0 / (1.0 + Math.exp((double) (-x))));
    }

    private List<Recognition> getTopRecognitions(List<BoundingBox> orderedBBs){
        final ArrayList<Recognition> recognitions = new ArrayList<>(1);
        for (BoundingBox b : orderedBBs){
            if (b.getConfidence() < MIN_CONFIDENCE)
                break;
            RectF detection = new RectF(b.getX() - b.getWidth()/2, b.getY() - b.getHeight()/2,
                    b.getX() + b.getWidth()/2, b.getY() + b.getHeight()/2);
            recognitions.add(new Recognition("0", labels.get(0),
                    b.getConfidence(), detection));
        }
        return recognitions;
    }

    @Override
    public void enableStatLogging(boolean debug) {

    }

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {
    }
}
