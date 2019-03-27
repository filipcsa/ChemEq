package com.example.filip.chemeq.ocr;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Environment;

import com.example.filip.chemeq.Recognition;
import com.example.filip.chemeq.util.Logger;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.List;

public class TessOCR {
    private static final Logger LOGGER = new Logger(TessOCR.class.getName());
    private TessBaseAPI tessBaseAPI;
    private final String lang = "chem";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess/" ;


    public TessOCR() {
        tessBaseAPI = new TessBaseAPI();
        LOGGER.i("DATA_PATH: " + DATA_PATH);
        tessBaseAPI.init(DATA_PATH, lang);
    }

    public void doOCR(Bitmap img, List<Recognition> results){
        tessBaseAPI.setImage(img);
        for (Recognition result : results){
            RectF rect = result.getLocation();

            tessBaseAPI.setRectangle((int)rect.left, (int)rect.top,
                    (int)(rect.right - rect.left),
                    (int)(rect.bottom - rect.top));
            result.setTitle(tessBaseAPI.getUTF8Text());
        }
    }

    public String doOCRonSingleExample(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        return tessBaseAPI.getUTF8Text();
    }
}
