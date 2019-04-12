package com.example.filip.chemeq.ocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Pair;

import com.example.filip.chemeq.Recognition;
import com.example.filip.chemeq.util.Logger;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

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

    private String[] index_arr = {"₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};
    private List<String> indexes = new ArrayList<>(Arrays.asList(index_arr));

    private Bitmap image; // achtung! it is turned by 90 degrees
    private Matrix canvasToFrame;
    private Matrix frameToCanvas = new Matrix();
    private Matrix rotate;



    public TessOCR(Bitmap image, Matrix canvasToFrame) {
        // init tesseract
        tessBaseAPI = new TessBaseAPI();
        LOGGER.i("DATA_PATH: " + DATA_PATH);
        tessBaseAPI.init(DATA_PATH, lang);
        // the dot character makes a mess :/
        // tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        tessBaseAPI.setVariable("tessedit_char_blacklist", "·");

        this.canvasToFrame = canvasToFrame;
        canvasToFrame.invert(frameToCanvas);
        rotate = new Matrix();
        rotate.postRotate(90);

        // threshold the image
        this.image = image.copy(image.getConfig(), true);
        /*
        Mat imageMat = new Mat();
        Utils.bitmapToMat(this.image, imageMat);
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(imageMat, imageMat, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 51, 4);
        Utils.matToBitmap(imageMat, this.image);
        tessBaseAPI.setImage(rotateBitmap(this.image));
        */

        // test();
    }

    private void test(){
        // tess before any tweaks
        Bitmap testImg = BitmapFactory.decodeFile(DATA_PATH + "test7.png");
        tessBaseAPI.setImage(testImg);
        String text = tessBaseAPI.getUTF8Text();
        LOGGER.i("Before tweaks: " + text);
        tessBaseAPI.getResultIterator().getChoicesAndConfidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        // first tweak
        Mat imgMat = new Mat();
        Utils.bitmapToMat(testImg, imgMat);
        Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(imgMat, imgMat, 255,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 31, 4);
        Utils.matToBitmap(imgMat, testImg);

        tessBaseAPI.setImage(testImg);
        text = tessBaseAPI.getUTF8Text();
        LOGGER.i("After first tweak: " + text);

    }

    public void adjustResultsInBitmap(List<Recognition> results){
        for (Recognition recognition : results) {
            RectF rect = recognition.getLocation();
            canvasToFrame.mapRect(rect);

            int left = (int) rect.left;
        }
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
        String text = tessBaseAPI.getUTF8Text();

        int level = TessBaseAPI.PageIteratorLevel.RIL_SYMBOL;
        ResultIterator ri = tessBaseAPI.getResultIterator();
        allChoices = new ArrayList<>();
        do {
            List<Pair<String, Double>> characterChoices = ri.getChoicesAndConfidence(level-1);
            allChoices.add(characterChoices);
            /*
            for (Pair<String, Double> choice : characterChoices) {
                LOGGER.i("\t Character " + choice.first + " with conf " + choice.second);
            }
            */
        } while (ri.next(level));

        LOGGER.i("Text before parsing: " + text + "\n");
        equation = "";
        parsing_ended = false;
        stateMachine(State.START, 0);

        equation = equation.replace("+", " + ");
        equation = equation.replace("→", " → ");

        String ret = "Raw detection: " + text + "\n";
        ret += "After parsing: " + equation;

        return ret;
    }

    // naprasenej state machine
    private enum State {
        START,
        NUM,
        ELEM,
        NO_ELEM,
        IDX

    }
    private List<List<Pair<String, Double>>> allChoices;
    private String equation = "";
    private boolean parsing_ended = false;
    private boolean parenthesis = false;

    private void stateMachine( State state, int pos) {
        // end condition
        if (pos == allChoices.size()){
            if (state == State.ELEM || state == State.IDX){
            LOGGER.i("PARSED EQUATION: " + equation);
            parsing_ended = true;
            }
            return;
        }

        //LOGGER.i("State: " + state);
        for (int i = 0; i < allChoices.get(pos).size(); i++) {
            char character = allChoices.get(pos).get(i).first.charAt(0);
            LOGGER.i("Character: " + character + " on position :" + pos);
            equation += character;
            LOGGER.i("Equation: " + equation);

            // + or → , its here instead of IDX and ELEM so the parsing is not that strict
            if (character == '+' || character == '→'){
                LOGGER.i("Read + or arrow, so going to start " + character + "\n");
                stateMachine(State.START, pos+1);
            }

            switch (state) {

                case START:
                    // number
                    if ('2' <= character && character <= '9') {
                        LOGGER.i("From START to NUM on" + character + "\n");
                        stateMachine(State.NUM, pos+1);
                    }
                    // uppercase, is element
                    if (isStringElement(String.valueOf(character))) {
                        LOGGER.i("From START to ELEM on" + character + "\n");
                        stateMachine(State.ELEM, pos+1);
                    }
                    // uppercase, not element
                    else if ('A' <= character && character <= 'Z'){
                        LOGGER.i("From START to NO_ELEM on" + character + "\n");
                        stateMachine(State.NO_ELEM, pos+1);
                    }
                    // parenthesis
                    if (character == '('){
                        LOGGER.i("From START to NUM on " + character + "\n");
                        parenthesis = true;
                        stateMachine(State.NUM, pos+1);
                    }
                    break;

                case NUM:
                    // uppercase element
                    if (isStringElement(String.valueOf(character))) {
                        LOGGER.i("From NUM to ELEM on " + character + "\n");
                        stateMachine(State.ELEM, pos+1);
                    }
                    // uppercase no element
                    else if ('A' <= character && character <= 'Z'){
                        LOGGER.i("From NUM to NO_ELEM on " + character + "\n");
                        stateMachine(State.NO_ELEM, pos+1);
                    }
                    // parenthesis
                    if (character == '('){
                        LOGGER.i("From NUM to NUM on " + character + "\n");
                        parenthesis = true;
                        stateMachine(State.NUM, pos+1);
                    }
                    break;

                case ELEM:
                    // lowercase which is an element together with the previous letter
                    if ('a' <= character && character <= 'z'){
                        if (equation.length() == 1) continue;
                        String prev = equation.substring(equation.length() - 2, equation.length()-1);
                        String lastTwo = prev + character;
                        if (isStringElement(lastTwo)) {
                            LOGGER.i("From ELEM to ELEM on " + character + "\n");
                            stateMachine(State.ELEM, pos+1);
                        }
                    }
                    // uppercase, is element
                    if (isStringElement(String.valueOf(character))) {
                        LOGGER.i("From ELEM to ELEM on" + character + "\n");
                        stateMachine(State.ELEM, pos+1);
                    }
                    // uppercase, not element
                    else if ('A' <= character && character <= 'Z') {
                        LOGGER.i("From ELEM to NO_ELEM");
                        stateMachine(State.NO_ELEM, pos+1);
                    }
                    // index
                    if (isStringIndex(String.valueOf(character))) {
                        LOGGER.i("From ELEM to IDX on " + character + "\n");
                        stateMachine(State.IDX, pos+1);
                    }
                    // parenthesis
                    if (character == '('){
                        LOGGER.i("From ELEM to NUM on " + character + "\n");
                        parenthesis = true;
                        stateMachine(State.NUM, pos+1);
                    }
                    // close parenthesis
                    if (character == ')' && parenthesis) {
                        LOGGER.i("From ELEM to ELEM on " + character + "\n");
                        parenthesis = false;
                        stateMachine(State.ELEM, pos+1);
                    }
                    break;

                case NO_ELEM:
                    // lowercase which is an element together with the previous letter
                    if ('a' <= character && character <= 'z'){
                        if (equation.length() == 1) continue;
                        String prev = equation.substring(equation.length() - 2, equation.length()-1);
                        String lastTwo = prev + character;
                        if (isStringElement(lastTwo)) {
                            LOGGER.i("From NO_ELEM to ELEM on " + character + "\n");
                            stateMachine(State.ELEM, pos+1);
                        }
                    }
                    break;

                case IDX:
                    // uppercase, is element
                    if (isStringElement(String.valueOf(character))) {
                        LOGGER.i("From IDX to ELEM on" + character + "\n");
                        stateMachine(State.ELEM, pos+1);
                    }
                    // uppercase, not element
                    else if ('A' <= character && character <= 'Z') {
                        LOGGER.i("From IDX to NO_ELEM");
                        stateMachine(State.NO_ELEM, pos+1);
                    }
                    // parenthesis
                    if (character == '('){
                        LOGGER.i("From IDX to NUM on " + character + "\n");
                        parenthesis = true;
                        stateMachine(State.NUM, pos+1);
                    }
                    // close parenthesis
                    if (character == ')' && parenthesis) {
                        LOGGER.i("From IDX to ELEM on " + character + "\n");
                        parenthesis = false;
                        stateMachine(State.ELEM, pos+1);
                    }
            }

            if (parsing_ended) return;
            equation = equation.substring(0, equation.length() - 1);
        }
        // no character fits so leave out
        stateMachine(state, pos+1);
    }

    private boolean isStringElement(String str) {
        LOGGER.i("Checking if " + str + " is an element");
        for (String element : elements) {
            if (element.equals(str))
                return true;
        }
        return false;
    }

    private boolean isStringIndex(String str) {
        for (String idx : indexes) {
            if (idx.equals(str))
                return true;
        }
        return false;
    }

    public String doOCR4Rectangle(RectF rectF) {
        tessBaseAPI.setImage(this.image);
        canvasToFrame.mapRect(rectF);
        rotate.mapRect(rectF);
        tessBaseAPI.setRectangle((int) rectF.left, (int) rectF.top, (int) rectF.width(), (int) rectF.height());

        String text = tessBaseAPI.getUTF8Text();
        LOGGER.i(text);

        /*
        ResultIterator ri = tessBaseAPI.getResultIterator();

        if (ri == null) LOGGER.i("RESULT ITERATOR IS NULL");
        List<Pair<String, Double>> wordsChoices = ri.getChoicesAndConfidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);

        for (Pair<String, Double> choice : wordsChoices){
            LOGGER.i(choice.first);
        }
        */
        ResultIterator ri = tessBaseAPI.getResultIterator();
        int level = TessBaseAPI.PageIteratorLevel.RIL_WORD;
        do {
            String word = ri.getUTF8Text(level);
            if (word != null)
                LOGGER.i(word);
        } while (ri.next(level));

        ri.delete();

        return text;
    }


    private Bitmap rotateBitmap(Bitmap bitmap){
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotate, true);
    }
}
