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
import com.example.filip.chemeq.detecting.Equation;
import com.example.filip.chemeq.detecting.RecognitionListItem;
import com.example.filip.chemeq.util.Logger;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.time.Duration;
import java.time.Instant;
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

    }


    Instant parseStart;
    public Equation testOCR(Bitmap bitmap) {
        tessBaseAPI.setImage(bitmap);
        String text = tessBaseAPI.getUTF8Text();


        // create a list of all choices at the level of single characters
        int level = TessBaseAPI.PageIteratorLevel.RIL_SYMBOL;

        ResultIterator ri = tessBaseAPI.getResultIterator();

        LOGGER.i("Result iterator retrieved");
        allChoices = new ArrayList<>();

        if (text.equals(""))
            return new Equation(Equation.Type.CHEM);

        do {

            List<Pair<String, Double>> characterChoices = ri.getChoicesAndConfidence(level-1);
            allChoices.add(characterChoices);
            LOGGER.i("Character choices received");
        } while (ri.next(level));
        ri.delete();

        // try to parse as a mathematical equation
        parseStart = Instant.now();
        a = 0; b = 0; succ = false;
        tryToParseAsMathEq(MathState.START, 0);
        if (succ) {
            Equation equation = new Equation(Equation.Type.MATH);
            equation.setA(a);
            equation.setB(b);
            equation.setOp(op);
            return equation;
        }

        // LOG ALL CHOICES
        LOGGER.i("ALL CHOICES: " + allChoices.toString());

        // whena parsing nonsense, it might take really long to determine that there is no chemical equation
        // so after like 200ms i give up the parsing

        parseStart = Instant.now();
        return parseChemicalEquation();

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
        
        Instant now = Instant.now();
        long duration = Duration.between(parseStart, now).toMillis();
        if (duration > 100) {
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



    private Equation parseChemicalEquation() {

        String raw = tessBaseAPI.getUTF8Text();
        Equation chemeq = new Equation(Equation.Type.CHEM);
        chemeq.setRawDetection(raw);
        int pos = 0;
        String endCharacter = null;
        // leftside
        do {

            List<Compound> possibleCompounds = parseCompound(State.START, pos, new Compound());

            Instant now = Instant.now();
            long duration = Duration.between(parseStart, now).toMillis();
            if (duration > 200) {
                LOGGER.i("TIME OUT!");
                return chemeq;
            }

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

        // shiiit there is no right side
        if (!endCharacter.equals("→"))
            return chemeq;

        // right side
        do {
            List<Compound> possibleCompounds = parseCompound(State.START, pos, new Compound());

            Instant now = Instant.now();
            long duration = Duration.between(parseStart, now).toMillis();
            if (duration > 200) {
                LOGGER.i("TIME OUT!");
                return chemeq;
            }

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

        Instant now = Instant.now();
        long duration = Duration.between(parseStart, now).toMillis();
        if (duration > 200) {
            return possibleCompounds;
        }

        // if reached end, return
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

        LOGGER.i("LEAVING OUT CHARACTER AT POSITION " + pos);
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
