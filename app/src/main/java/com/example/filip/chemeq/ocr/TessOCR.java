package com.example.filip.chemeq.ocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Environment;

import com.example.filip.chemeq.Recognition;
import com.example.filip.chemeq.util.Logger;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TessOCR {
    private static final Logger LOGGER = new Logger(TessOCR.class.getName());
    private TessBaseAPI tessBaseAPI;
    private final String lang = "chem";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess/" ;

    private String[] elements_arr = {"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Tm", "Yb", "Lu", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr"};
    private List<String> elements = new ArrayList<String>(Arrays.asList(elements_arr));



    public TessOCR() {
        tessBaseAPI = new TessBaseAPI();
        LOGGER.i("DATA_PATH: " + DATA_PATH);
        tessBaseAPI.init(DATA_PATH, lang);

        test();
    }

    private void test(){
        // tess before any tweaks
        Bitmap testImg = BitmapFactory.decodeFile(DATA_PATH + "test7.png");
        tessBaseAPI.setImage(testImg);
        String text = tessBaseAPI.getUTF8Text();
        LOGGER.i("Before tweaks: " + text);
        tessBaseAPI.getResultIterator().getChoicesAndConfidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

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
