package com.example.filip.chemeq.detecting;

import android.util.Pair;

import com.example.filip.chemeq.util.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

public class RecognitionListItem {

    private String[] elements_arr = {"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Tm", "Yb", "Lu", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr"};
    private List<String> elements = new ArrayList<String>(Arrays.asList(elements_arr));
    private String[] index_arr = {"₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};
    private List<String> indexes = new ArrayList<>(Arrays.asList(index_arr));
    private static final Logger LOGGER = new Logger(RecognitionListItem.class.getName());

    private Equation equationTest;

    public void setEquationTest(Equation equation) {
        this.equationTest = equation;
    }

    public Equation getEquationTest() {
        return this.equationTest;
    }


    private boolean math;

    private int a, b;

    private char op;

    private String equation = "";

    private List<Pair<String, String>> leftSideCompounds = new ArrayList<>();

    private List<Pair<String, String>> rightSideCompounds = new ArrayList<>();

    public String getEquation() {
        return equation;
    }

    public void setEquation(String equation) {
        this.equation = equation;
    }

    public List<Pair<String, String>> getLeftSideCompounds() {
        return leftSideCompounds;
    }

    public void setLeftSideCompounds(List<Pair<String, String>> leftSideCompounds) {
        this.leftSideCompounds = leftSideCompounds;
    }

    public List<Pair<String, String>> getRightSideCompounds() {
        return rightSideCompounds;
    }

    public void setRightSideCompounds(List<Pair<String, String>> rightSideCompounds) {
        this.rightSideCompounds = rightSideCompounds;
    }

    /** this is the main setter, which is used to create valid list items **/
    public void setAll(RecognitionListItem recognitionListItem) {

        setEquation(recognitionListItem.getEquation());
        setLeftSideCompounds(recognitionListItem.getLeftSideCompounds());
        setRightSideCompounds(recognitionListItem.getRightSideCompounds());
        setMath(recognitionListItem.isMath());
        setOp(recognitionListItem.getOp());
        setA(recognitionListItem.getA());
        setB(recognitionListItem.getB());

        if (!isMath())
            balanceEquation();
    }

    public boolean isMath() {
        return math;
    }

    public void setMath(boolean math) {
        this.math = math;
    }

    public int getA() {
        return a;
    }

    public void setA(int a) {
        this.a = a;
    }

    public int getB() {
        return b;
    }

    public void setB(int b) {
        this.b = b;
    }

    public char getOp() {
        return op;
    }

    public void setOp(char op) {
        this.op = op;
    }

    /** vycisleni rovnice na rychlo aby se nereklo ze to nemam lol**/
    // TODO make it more efficient somehow
    private void balanceEquation() {
        Map<String, Integer> totalLeft = new HashMap<>();
        Map<String, Integer> totalRight = new HashMap<>();
        List<Map<String, Integer>> leftDicts = new ArrayList<>();
        List<Map<String, Integer>> rightDicts = new ArrayList<>();
        for (Pair<String, String> compound : leftSideCompounds) {
            Map<String, Integer> parentDict = new HashMap<>();
            boolean parentOpened = false;
            String component = compound.first;
            int i = 0;
            int c = 1;
            Map<String, Integer> leftCounts = new HashMap<>();
            while (i < component.length()) {
                // number in beginning
                if (isStringInteger(component.substring(i, i+1))){
                    if (c == 1) c = Integer.parseInt(component.substring(0, 1));
                    else
                        c = 10*c + Integer.parseInt(component.substring(0, 1));
                    i++;
                    continue;
                }

                // parenthesis
                if (component.substring(i, i+1).equals("(")){
                    i++;
                    parentOpened = true;
                    continue;
                }

                int number = 1;
                if (component.substring(i, i+1).equals(")")) {
                    parentOpened = false;
                    LOGGER.i("Parenthesis closed");
                    if ((i+1 < component.length() && isStringIndex(component.substring(i+1, i+2)))) {
                        char idx = component.substring(i+1, i+2).charAt(0);
                        number = idx - '₀';
                        i++;
                    }

                    for (String key : parentDict.keySet()){
                        int inParent = parentDict.get(key) * number;
                        if (totalLeft.containsKey(key))
                            totalLeft.put(key, totalLeft.get(key)+inParent);
                        else
                            totalLeft.put(key, inParent);
                        if (leftCounts.containsKey(key))
                            leftCounts.put(key, leftCounts.get(key)+inParent);
                        else
                            leftCounts.put(key, inParent);
                    }
                    i++;
                    continue;
                }


                String element;
                // 2 letter element
                if (i+1 < component.length() && isStringElement(component.substring(i, i+2))) {
                    element = component.substring(i, i+2);
                    i += 1;
                }
                // 1 letter element
                else {
                    element = component.substring(i, i+1);
                }

                if (i+1 < component.length() && isStringIndex(component.substring(i+1, i+2))) {
                    char idx = component.substring(i+1, i+2).charAt(0);
                    number = idx - '₀';
                    i += 1;
                }
                number *= c;

                if (parentOpened) {
                    if (parentDict.containsKey(element))
                        parentDict.put(element, parentDict.get(element)+number);
                    else
                        parentDict.put(element, number);
                    i++;
                    continue;
                }

                if (totalLeft.containsKey(element))
                    totalLeft.put(element, totalLeft.get(element)+number);
                else
                    totalLeft.put(element, number);
                if (leftCounts.containsKey(element))
                    leftCounts.put(element, leftCounts.get(element)+number);
                else
                    leftCounts.put(element, number);
                i += 1;
            }
            leftDicts.add(leftCounts);
        }
        LOGGER.i("IMPORTANTTT L\n" + leftDicts.toString());

        // RIGHT SIDE
        for (Pair<String, String> compound : rightSideCompounds) {
            Map<String, Integer> parentDict = new HashMap<>();
            boolean parentOpened = false;
            String component = compound.first;
            int i = 0;
            int c = 1;
            Map<String, Integer> rightCounts = new HashMap<>();
            while (i < component.length()) {
                if (isStringInteger(component.substring(i, i+1))){
                    if (c == 1) c = Integer.parseInt(component.substring(0, 1));
                    else
                        c = 10*c + Integer.parseInt(component.substring(0, 1));
                    i++;
                    continue;
                }

                // parenthesis
                if (component.substring(i, i+1).equals("(")){
                    i++;
                    parentOpened = true;
                    continue;
                }

                int number = 1;
                if (component.substring(i, i+1).equals(")")) {
                    parentOpened = false;
                    LOGGER.i("Parenthesis closed");
                    if ((i+1 < component.length() && isStringIndex(component.substring(i+1, i+2)))) {
                        char idx = component.substring(i+1, i+2).charAt(0);
                        number = idx - '₀';
                        i++;
                    }

                    for (String key : parentDict.keySet()){
                        int inParent = parentDict.get(key) * number;
                        if (totalRight.containsKey(key))
                            totalRight.put(key, totalRight.get(key)+inParent);
                        else
                            totalRight.put(key, inParent);
                        if (rightCounts.containsKey(key))
                            rightCounts.put(key, rightCounts.get(key)+inParent);
                        else
                            rightCounts.put(key, inParent);
                    }
                    i++;
                    continue;
                }

                String element;
                // 2 letter element
                if (i+1 < component.length() && isStringElement(component.substring(i, i+2))) {
                    element = component.substring(i, i+2);
                    i += 1;
                }
                // 1 letter element
                else {
                    element = component.substring(i, i+1);
                }

                if (i+1 < component.length() && isStringIndex(component.substring(i+1, i+2))) {
                    char idx = component.substring(i+1, i+2).charAt(0);
                    number = idx - '₀';
                    i += 1;
                }
                number *= c;

                if (parentOpened) {
                    if (parentDict.containsKey(element))
                        parentDict.put(element, parentDict.get(element)+number);
                    else
                        parentDict.put(element, number);
                    i++;
                    continue;
                }

                if (totalRight.containsKey(element))
                    totalRight.put(element, totalRight.get(element)+number);
                else
                    totalRight.put(element, number);
                if (rightCounts.containsKey(element))
                    rightCounts.put(element, rightCounts.get(element)+number);
                else
                    rightCounts.put(element, number);
                i += 1;
            }
            rightDicts.add(rightCounts);
        }
        LOGGER.i("IMPORTANTTT R\n" + rightDicts.toString());

        if (rightDicts.equals(leftDicts))
            LOGGER.i("EQUAL SIDES!");

        int balanceIdx = isEquationBalancable(totalLeft, totalRight);
        if (balanceIdx == 2) {
            LOGGER.i("Equation balanced!!!");
            return;
        }
        if (balanceIdx == 0){
            LOGGER.i("Not balancable");
            return;
        }

        LOGGER.i("Total left: " + totalLeft.toString());
        LOGGER.i("Total left size: " + totalLeft.size());

        int rows = totalLeft.size();
        int cols = leftDicts.size() + rightDicts.size();
        double[][] data = new double[rows][cols];
        int elIdx = 0;
        int compIdx = 0;
        for (int i = 0; i < leftDicts.size(); i++) {
            elIdx = 0;
            for (String el : totalLeft.keySet()) {
                int elementsInComp = 0;

                if (leftDicts.get(i).containsKey(el))
                    elementsInComp = leftDicts.get(i).get(el);

                data[elIdx][compIdx] = elementsInComp;
                elIdx++;
            }
            compIdx++;
        }

        for (int i = 0; i < rightDicts.size(); i++) {
            elIdx = 0;
            for (String el : totalLeft.keySet()) {
                int elementsInComp = 0;

                if (rightDicts.get(i).containsKey(el))
                    elementsInComp = rightDicts.get(i).get(el);

                data[elIdx][compIdx] = elementsInComp;
                elIdx++;
            }
            compIdx++;
        }

        SimpleMatrix sm = new SimpleMatrix(data);
        LOGGER.i("EQUATION MATRIX " + sm.toString());
        DenseMatrix64F red = CommonOps.rref(sm.getMatrix(), -1, null);
        LOGGER.i("REDUCED MATRIX: " + red.toString());

        /*

        List<Integer> leftCoef = new ArrayList<>();
        List<Integer> rightCoef = new ArrayList<>();
        Random r = new Random();
        boolean balanced = false;
        while (!balanced) {
            totalLeft = new HashMap<>();
            totalRight = new HashMap<>();
            List<Map<String, Integer>> tempLeft = new ArrayList<>();
            List<Map<String, Integer>> tempRight = new ArrayList<>();

            for (Map<String, Integer> item : leftDicts) {
                Map<String, Integer> newdict = new HashMap<>();
                for (String key : item.keySet()) {
                    newdict.put(key, item.get(key));
                }
                tempLeft.add(newdict);
            }

            for (Map<String, Integer> item : rightDicts) {
                Map<String, Integer> newdict = new HashMap<>();
                for (String key : item.keySet()) {
                    newdict.put(key, item.get(key));
                }
                tempRight.add(newdict);
            }

            leftCoef = new ArrayList<>();
            rightCoef = new ArrayList<>();
            for (int i = 0; i < tempLeft.size(); i++) {
                leftCoef.add(r.nextInt(2)+1);
            }
            for (int i = 0; i < tempRight.size(); i++) {
                rightCoef.add(r.nextInt(2)+1);
            }

            for (int i = 0; i < leftCoef.size(); i++) {
                for (String key : tempLeft.get(i).keySet()) {
                    tempLeft.get(i).put(key, tempLeft.get(i).get(key)*leftCoef.get(i));
                    if (!totalLeft.containsKey(key))
                        totalLeft.put(key, tempLeft.get(i).get(key));
                    else
                        totalLeft.put(key, totalLeft.get(key) + tempLeft.get(i).get(key));
                }
            }

            for (int i = 0; i < rightCoef.size(); i++) {
                for (String key : tempRight.get(i).keySet()) {
                    tempRight.get(i).put(key, tempRight.get(i).get(key)*rightCoef.get(i));
                    if (!totalRight.containsKey(key))
                        totalRight.put(key, tempRight.get(i).get(key));
                    else
                        totalRight.put(key, totalRight.get(key) + tempRight.get(i).get(key));
                }
            }

            balanced = true;
            for (String key : totalLeft.keySet()) {
                if (totalLeft.get(key) != totalRight.get(key)) {
                    balanced = false;
                    break;
                }
            }
        }

        LOGGER.i(leftCoef.toString() + "     " + rightCoef.toString());

        // pridat
        */


    }

    /* 0 no 1 yes 2 is balanced*/
    private int isEquationBalancable(Map<String, Integer> left, Map<String, Integer> right) {
        for (String k : left.keySet()) {
            if (!right.containsKey(k))
                return 0;
            if (left.get(k) != right.get(k))
                return 1;
        }
        return 2;
    }

    private boolean isStringElement(String str) {
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

    private boolean isStringInteger(String str) {
        char ch = str.charAt(0);
        if ('2' <= ch && ch <= '9')
            return true;
        return false;
    }
}
