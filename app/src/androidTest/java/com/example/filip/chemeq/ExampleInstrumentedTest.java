package com.example.filip.chemeq;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.example.filip.chemeq.detecting.ChemBase;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.example.filip.chemeq", appContext.getPackageName());
    }


    Context context;
    @Before
    public void setUp() {
        context = InstrumentationRegistry.getTargetContext();
        OutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        System.setOut(ps);
    }

    @Test
    public void loadJSONtest() throws JSONException {
        ChemBase.loadJSON(context);
    }

    @Test
    public void loadJSONAndTryH2O() throws JSONException {
        ChemBase.loadJSON(context);
        String name = ChemBase.getNameOfFormula("H₂O");
        assertEquals("water", name);
    }
}
