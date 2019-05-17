package com.example.filip.chemeq;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;

import java.io.IOException;

public class YOLOTest {

    Context testContext;
    Classifier detector;
    String modelFilename;
    String labelFilename;
    int inputSize;


    @Before
    public void setUp() {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
        try {
            detector = TFLiteYoloDetectionAPI.create(testContext.getAssets(), modelFilename,
                    labelFilename, inputSize, false);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
