package com.example.filip.chemeq.ocr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Pair;

import com.example.filip.chemeq.Recognition;
import com.example.filip.chemeq.detecting.ChemBase;
import com.example.filip.chemeq.detecting.ChemicalEquation;
import com.example.filip.chemeq.detecting.Compound;
import com.example.filip.chemeq.detecting.RecognitionListItem;
import com.example.filip.chemeq.util.Logger;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TessOCRAnalyzer {


    private static final Logger LOGGER = new Logger(TessOCRAnalyzer.class.getName());
    private TessBaseAPI tessBaseAPI;
    private final String lang = "chem";
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess/" ;

    private String[] elements_arr = {"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Tm", "Yb", "Lu", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr"};
    private List<String> elements = new ArrayList<String>(Arrays.asList(elements_arr));

    private String[] index_arr = {"₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};
    private List<String> indexes = new ArrayList<>(Arrays.asList(index_arr));

    private Matrix canvasToFrame;
    private Matrix frameToCanvas = new Matrix();
    private Matrix rotate;



    public TessOCRAnalyzer(Bitmap image, Matrix canvasToFrame) {
        // init tesseract
        tessBaseAPI = new TessBaseAPI();
        LOGGER.i("DATA_PATH: " + DATA_PATH);
        tessBaseAPI.init(DATA_PATH, lang);
        // the dot character makes a mess :/
        // tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        tessBaseAPI.setVariable("tessedit_char_blacklist", "·");

        /*

        this.canvasToFrame = canvasToFrame;
        canvasToFrame.invert(frameToCanvas);
        rotate = new Matrix();
        rotate.postRotate(90);
        */

        // threshold the image
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

    public ChemicalEquation testOCR(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String text = tessBaseAPI.getUTF8Text();


        // create a list of all choices at the level of single characters
        int level = TessBaseAPI.PageIteratorLevel.RIL_SYMBOL;

        ResultIterator ri = tessBaseAPI.getResultIterator();

        LOGGER.i("Result iterator retrieved");
        allChoices = new ArrayList<>();

        if (text.equals(""))
            return new ChemicalEquation();

        do {

            List<Pair<String, Double>> characterChoices = ri.getChoicesAndConfidence(level-1);
            allChoices.add(characterChoices);
            LOGGER.i("Character choices received");
        } while (ri.next(level));
        ri.delete();

        return parseChemicalEquation();

    }

    public List<Compound> test2OCR(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String text = tessBaseAPI.getUTF8Text();


        // create a list of all choices at the level of single characters
        int level = TessBaseAPI.PageIteratorLevel.RIL_SYMBOL;

        ResultIterator ri = tessBaseAPI.getResultIterator();

        LOGGER.i("Result iterator retrieved");
        allChoices = new ArrayList<>();

        if (text.equals(""))
            return new ArrayList<>();

        do {

            List<Pair<String, Double>> characterChoices = ri.getChoicesAndConfidence(level-1);
            allChoices.add(characterChoices);
            LOGGER.i("Character choices received");
        } while (ri.next(level));
        ri.delete();

        return parseCompound(State.START, 0, new Compound());

    }

    public RecognitionListItem doOCRonSingleExample(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String text = tessBaseAPI.getUTF8Text();

        // create a list of all choices at the level of single characters
        int level = TessBaseAPI.PageIteratorLevel.RIL_SYMBOL;
        ResultIterator ri = tessBaseAPI.getResultIterator();
        LOGGER.i("Result iterator retrieved");
        allChoices = new ArrayList<>();
        do {
            List<Pair<String, Double>> characterChoices = ri.getChoicesAndConfidence(level-1);
            allChoices.add(characterChoices);
            LOGGER.i("Character choices received");
        } while (ri.next(level));
        ri.delete();

        // INIT THE VARIABLES REQUIRED FOR THE STATE MACHINE
        equation = "";
        parsing_ended = false;


        // try to parse as a mathematical equation
        a = 0; b = 0; succ = false;
        tryToParseAsMathEq(MathState.START, 0);
        if (succ) {
            LOGGER.i("MATH EQ PARSED: " + a + op + b);
            RecognitionListItem recognitionListItem = new RecognitionListItem();
            recognitionListItem.setMath(true);
            recognitionListItem.setA(a);
            recognitionListItem.setB(b);
            recognitionListItem.setOp(op);
            return  recognitionListItem;
        }
        stateMachine(State.START, 0);
        fillTheNamesOfFormulas();

        equation = equation.replace("+", " + ");
        equation = equation.replace("→", " → ");

        String ret = "Raw detection: " + text + "\n";
        ret += "After parsing: " + equation;

        RecognitionListItem recognitionListItem = new RecognitionListItem();
        recognitionListItem.setEquation(ret);
        recognitionListItem.setLeftSideCompounds(leftSideCompounds);
        recognitionListItem.setRightSideCompounds(rightSideCompounds);


        return recognitionListItem;
    }

    /** bordel pro parsovani cmatematickejch rovnic aaaa **/
    private boolean succ;
    private int a, b;
    char op;
    private enum MathState{
        START,
        FIRST,
        OP,
        SECOND
    }
    /** Tries to parse as math eq **/
    private void tryToParseAsMathEq(MathState state, int pos) {
        if (pos == allChoices.size()){
            if (state == MathState.SECOND){
                succ = true;
            }
            return;
        }

        for (int i = 0; i < allChoices.get(pos).size(); i++) {
            char character = allChoices.get(pos).get(i).first.charAt(0);
            if (character == ' ')
                tryToParseAsMathEq(state, pos+1);
            LOGGER.i("MMM" + " character read " + character);
            switch (state) {
                case START:
                    if ('0' <= character && character <= '9') {
                        a = a*10 + (character - '0');
                        LOGGER.i("MMM" + " from START to FIRST on " + character);
                        tryToParseAsMathEq(MathState.FIRST, pos + 1);
                    }
                    break;

                case FIRST:
                    if ('0' <= character && character <= '9') {
                        a = a*10 + (character - '0');
                        LOGGER.i("MMM" + " from FIRST to FIRST on " + character);
                        tryToParseAsMathEq(MathState.FIRST, pos + 1);
                    }
                    if (character == '+' || character == '-' || character == '×' || character == '÷') {
                        op = character;
                        LOGGER.i("MMM" + " from FIRST to OP on " + character);
                        tryToParseAsMathEq(MathState.OP, pos+1);
                    }
                    break;

                case OP:
                    if ('0' <= character && character <= '9') {
                        b = b * 10 + (character - '0');
                        LOGGER.i("MMM" + " from OP to SECOND on " + character);
                        tryToParseAsMathEq(MathState.SECOND, pos + 1);
                    }
                    break;

                case SECOND:
                    if ('0' <= character && character <= '9') {
                        b = b * 10 + (character - '0');
                        LOGGER.i("MMM" + " from SECOND to SECOND on " + character);
                        tryToParseAsMathEq(MathState.SECOND, pos + 1);
                    }
                    if (character == '='){
                        LOGGER.i("MMM" + " " + character);
                        succ = true;
                    }
            }

            if (succ) {
                return;
            }

        }

    }

    /** Fills the left and right side lists with the formulas and their names **/
    private void fillTheNamesOfFormulas() {
        leftSideCompounds = new ArrayList<>();
        rightSideCompounds = new ArrayList<>();
        String leftSideString = equation.split("→")[0];
        String rightSideString = "";

        // to prevent null ptr exception in case the eq is bad
        if (equation.split("→").length == 2)
            rightSideString = equation.split("→")[1];

        String[] leftSideFormulas = leftSideString.split("\\+");
        String[] rightSideFormulas = rightSideString.split("\\+");

        for (String formula : leftSideFormulas) {
            String formulaName = ChemBase.getNameOfFormula(formula);
            leftSideCompounds.add(new Pair<>(formula, formulaName));
            LOGGER.i(formula + " is " + formulaName);
        }

        for (String formula : rightSideFormulas) {
            String formulaName = ChemBase.getNameOfFormula(formula);
            rightSideCompounds.add(new Pair<>(formula, formulaName));
            LOGGER.i(formula + " is " + formulaName);
        }
    }

    // the states of the nfa ish thing (basically it is a automaton with stack
    // but it is easier to model as nfa (* more details in the paper))
    private enum State {
        START,
        NUM,
        ELEM,
        NO_ELEM,
        IDX
    }

    private List<List<Pair<String, Double>>> allChoices;
    private List<Pair<String, String>> leftSideCompounds;
    private List<Pair<String, String>> rightSideCompounds;
    private String equation = "";
    private boolean parsing_ended = false;
    private boolean parenthesis = false;

    /** A crazy complicated recursive state machine for parsing the chemical equation by characters **/
    private void stateMachine( State state, int pos) {
        // end condition
        if (pos == allChoices.size()){
            if (state == State.ELEM || state == State.IDX){
            LOGGER.i("PARSED EQUATION: " + equation);
            parsing_ended = true;
            }
            return;
        }

        for (int i = 0; i < allChoices.get(pos).size(); i++) {
            char character = allChoices.get(pos).get(i).first.charAt(0);
            LOGGER.i("Character: " + character + " on position :" + pos);
            equation += character;
            LOGGER.i("Equation: " + equation);

            // + or → or ending , its here instead and not checking IDX and ELEM so the parsing is not that strict
            if (character == '+' || character == '→' || pos == allChoices.size()-1){
                LOGGER.i("Read + or arrow, so going to START " + character + "\n");
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



    private ChemicalEquation parseChemicalEquation() {
        String raw = tessBaseAPI.getUTF8Text();
        ChemicalEquation chemeq = new ChemicalEquation();
        chemeq.setRawDetection(raw);
        int pos = 0;
        String endCharacter = null;
        // leftside
        do {
            List<Compound> possibleCompounds = parseCompound(State.START, pos, new Compound());
            chemeq.addLeftPossibleCompounds(possibleCompounds);
            Compound resultCompound = possibleCompounds.get(0);
            for (Compound compound : possibleCompounds) {
                String trivName = ChemBase.getNameOfFormula(compound.getCompound());
                compound.setTrivName(trivName);
                if (!trivName.equals("unknown")){
                    resultCompound = compound;
                    break;
                }
            }
            chemeq.addLeftCompound(resultCompound);
            pos = resultCompound.getEndPos()+1;
            endCharacter = resultCompound.getEndCharacter();

        } while (endCharacter.equals("+"));

        //there is no right side omg lol
        if (!endCharacter.equals("→"))
            return chemeq;

        // right side
        do {
            List<Compound> possibleCompounds = parseCompound(State.START, pos, new Compound());
            chemeq.addRightPossibleCompounds(possibleCompounds);
            Compound resultCompound = possibleCompounds.get(0);
            for (Compound compound : possibleCompounds) {
                String trivName = ChemBase.getNameOfFormula(compound.getCompound());
                compound.setTrivName(trivName);
                if (!trivName.equals("unknown")){
                    resultCompound = compound;
                    break;
                }
            }
            chemeq.addRighCompound(resultCompound);
            pos = resultCompound.getEndPos()+1;
            endCharacter = resultCompound.getEndCharacter();
        } while (endCharacter.equals("+"));

        return chemeq;
    }

    /**
     * Returns list of syntactically correct compounds
     * @param state
     * @param pos
     * @param compound
     */
    private List<Compound> parseCompound(State state, int pos, Compound compound) {
        List<Compound> possibleCompounds = new ArrayList<>();
        if (pos == allChoices.size()){
            if (state == State.ELEM || state == State.IDX){
                compound.setParsed(true);
            }
            compound.setEndPos(pos);
            possibleCompounds.add(compound);
            return possibleCompounds;
        }


        for (int i = 0; i < allChoices.get(pos).size(); i++) {
            String character = allChoices.get(pos).get(i).first;
            Compound tempCompound = compound.getCopy();
            tempCompound.addCharacter(character);
            tempCompound.addConfidence(allChoices.get(pos).get(i).second);

            if (character.equals("+") || character.equals("→")){ // ma tu byt to posledni??
                LOGGER.i("Read + or arrow, so going to START " + character + "\n");
                if (state == State.ELEM || state == State.IDX) {
                    compound.setParsed(true);
                }
                compound.setEndPos(pos);
                compound.setEndCharacter(character);
                possibleCompounds.add(compound);
                return possibleCompounds;
            }

            if (character.equals("(")) {
                if (state != State.NO_ELEM) {
                    tempCompound.setParenthesis(true);
                    List<Compound> parsed = parseCompound(State.START, pos+1, tempCompound);
                    possibleCompounds.addAll(parsed);
                }
                continue;
            }
            if (character.equals(")")) {
                if (tempCompound.isParenthesis() && (state == State.ELEM || state == State.IDX)) {
                    tempCompound.setParenthesis(false);
                    List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                    possibleCompounds.addAll(parsed);
                }
                continue;
            }

            switch (state) {
                // number
                case START:
                    if (isStringInteger(character)) {
                        List<Compound> parsed = parseCompound(State.NUM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // uppercase is element
                    if (isStringElement(character)) {
                        List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // uppercase not element
                    else if (isStringUppercaseLetter(character)) {
                        List<Compound> parsed = parseCompound(State.NO_ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    break;

                case NUM:
                    // uppercase is element
                    if (isStringElement(character)) {
                        List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // uppercase not element
                    else if (isStringUppercaseLetter(character)) {
                        List<Compound> parsed = parseCompound(State.NO_ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    break;

                case ELEM:
                    // lowercase which is an element with the previous
                    if (isStringLowercaseLetter(character)) {
                        String lastTwo = compound.getLastCharacter() + character;
                        if (isStringElement(lastTwo)) {
                            List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                            possibleCompounds.addAll(parsed);
                        }
                    }
                    // uppercase is element
                    if (isStringElement(character)) {
                        List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // uppercase not element
                    else if (isStringUppercaseLetter(character)) {
                        List<Compound> parsed = parseCompound(State.NO_ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // index
                    if (isStringIndex(character)) {
                        List<Compound> parsed = parseCompound(State.IDX, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    break;

                case NO_ELEM:
                    // lowercase
                    if (isStringLowercaseLetter(character)) {
                        String lastTwo = compound.getLastCharacter() + character;
                        if (isStringElement(lastTwo)) {
                            List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                            possibleCompounds.addAll(parsed);
                        }
                    }
                    break;

                case IDX:
                    // uppercase is element
                    if (isStringElement(character)) {
                        List<Compound> parsed = parseCompound(State.ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // uppercase not element
                    else if (isStringUppercaseLetter(character)) {
                        List<Compound> parsed = parseCompound(State.NO_ELEM, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }
                    // index
                    if (isStringIndex(character)) {
                        List<Compound> parsed = parseCompound(State.IDX, pos+1, tempCompound);
                        possibleCompounds.addAll(parsed);
                    }

            }
        }

        // if there are some successfully parsed compounds return
        // else try to leave out character on this position
        if (possibleCompounds.size() != 0) return possibleCompounds;
        return parseCompound(state, pos+1, compound);
    }

    private boolean isStringInteger(String str) {
        char ch = str.charAt(0);
        if ('2' <= ch && ch <= '9')
            return true;
        return false;
    }

    private boolean isStringUppercaseLetter(String str) {
        if ('A' <= str.charAt(0) && str.charAt(0) <= 'Z')
            return true;
        return false;
    }

    private boolean isStringLowercaseLetter(String str) {
        if ('a' <= str.charAt(0) && str.charAt(0) <= 'z')
            return true;
        return false;
    }
}
