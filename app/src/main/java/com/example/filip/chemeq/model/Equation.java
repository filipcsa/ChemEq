package com.example.filip.chemeq.model;

import com.example.filip.chemeq.service.ChemBase;
import com.example.filip.chemeq.util.Logger;

import org.apache.commons.math3.fraction.Fraction;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Equation {

    private Logger LOGGER = new Logger(Equation.class.getName());

    public Equation(Type type) {
        this.equationType = type;
    }

    public enum Type{
        CHEM,
        MATH
    }

    private Type equationType;

    public Type getEquationType() {
        return equationType;
    }

    // MATH
    int a, b;
    private char op;

    // CHEM

    public enum Balance{
        BALANCED,
        BALANCABLE, // if its possible to balance but i am not able to
        UNBALANCED // 4 now same as not balancable
    }

    private Balance balance;

    public Balance getBalance() {
        return balance;
    }

    private String rawDetection = "";

    private List<Compound> leftCompounds = new ArrayList<>();
    private List<Compound> rightCompounds = new ArrayList<>();

    private List<List<Compound>> allPossibleLeft = new ArrayList<>();
    private List<List<Compound>> allPossibleRight = new ArrayList<>();


    public void addLeftCompound(Compound compound) {
        leftCompounds.add(compound);
    }

    public void addRighCompound(Compound compound) {
        rightCompounds.add(compound);
    }

    public List<Compound> getLeftCompounds() {
        return leftCompounds;
    }

    public List<Compound> getRightCompounds() {
        return rightCompounds;
    }

    public String getRawDetection() {
        return rawDetection;
    }

    public void setRawDetection(String rawDetection) {
        this.rawDetection = rawDetection;
    }

    public String getFullEquation() {
        String eq = "";
        if (equationType == Type.CHEM) {
            Iterator<Compound> it = leftCompounds.iterator();
            while (it.hasNext()) {
                eq += it.next().getCompound();
                if (it.hasNext()) eq += " + ";
            }

            if (rightCompounds.size() > 0)
                eq += " → ";

            it = rightCompounds.iterator();
            while (it.hasNext()) {
                eq += it.next().getCompound();
                if (it.hasNext()) eq += " + ";
            }
        }
        else {
            eq += a + " " + op + b + " = ";
        }

        return eq;
    }

    public List<List<Compound>> getAllPossibleLeft() {
        return allPossibleLeft;
    }

    public void addLeftPossibleCompounds(List<Compound> possible) {
        allPossibleLeft.add(possible);
    }

    public void addRightPossibleCompounds(List<Compound> possible) {
        allPossibleRight.add(possible);
    }

    public List<List<Compound>> getAllPossibleRight() {
        return allPossibleRight;
    }

    public void setA(int a) {
        this.a = a;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setOp(char op) {
        this.op = op;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public char getOp() {
        return op;
    }

    // TODO finish this also for math eq aaaa
    // i cant take this anymore
    public void equationEditedCallback(String newEq) {
        leftCompounds.clear();
        rightCompounds.clear();
        // chem eq
        LOGGER.i("EQUATION EDITED, NEW EQUATION: " + newEq);
        String eq = newEq.replace(" ", "");
        String[] split = eq.split("→");
        if (split.length == 1)
            split = eq.split("=");
        String[] leftComps = split[0].split("\\+");

        for (String leftComp : leftComps) {
            Compound compound = new Compound();
            leftComp = makeFormulaGreatAgain(leftComp);
            compound.setCompound(leftComp);
            compound.setTrivName(ChemBase.getNameOfFormula(leftComp));
            leftCompounds.add(compound);
        }

        if (split.length == 1)
            return;

        String[] rightComps = split[1].split("\\+");
        for (String rightComp : rightComps) {
            Compound compound = new Compound();
            rightComp = makeFormulaGreatAgain(rightComp);
            compound.setCompound(rightComp);
            compound.setTrivName(ChemBase.getNameOfFormula(rightComp));
            rightCompounds.add(compound);
        }

        balanceEquation();
    }

    /**
     * Replaces number with indexes
    */
    private String makeFormulaGreatAgain(String comp) {
        StringBuilder formula = new StringBuilder(comp);
        boolean beggining = true;
        for (int i = 0; i < formula.length(); i++) {
            String c = formula.substring(i, i+1);
            // change integer to index
            if (!beggining && isStringInteger(c)) {
                char num = c.charAt(0);
                int number = num - '0';
                char idx = (char)('₀' + number);
                formula.setCharAt(i, idx);
            }
            if (beggining && !isStringInteger(c))
                beggining = false;
        }

        return formula.toString();
    }


    // CODE FOR BALANCING THE EQUATION
    private String[] elements_arr = {"H", "He", "Li", "Be", "B", "C", "N", "O", "F", "Ne", "Na", "Mg", "Al", "Si", "P", "S", "Cl", "Ar", "K", "Ca", "Sc", "Ti", "V", "Cr", "Mn", "Fe", "Co", "Ni", "Cu", "Zn", "Ga", "Ge", "As", "Se", "Br", "Kr", "Rb", "Sr", "Y", "Zr", "Nb", "Mo", "Tc", "Ru", "Rh", "Pd", "Ag", "Cd", "In", "Sn", "Sb", "Te", "I", "Xe", "Cs", "Ba", "La", "Hf", "Ta", "W", "Re", "Os", "Ir", "Pt", "Au", "Hg", "Tl", "Pb", "Bi", "Po", "At", "Rn", "Fr", "Ra", "Ac", "Rf", "Db", "Sg", "Bh", "Hs", "Mt", "Ds", "Rg", "Cn", "Tm", "Yb", "Lu", "Th", "Pa", "U", "Np", "Pu", "Am", "Cm", "Bk", "Cf", "Es", "Fm", "Md", "No", "Lr"};
    private List<String> elements = new ArrayList<String>(Arrays.asList(elements_arr));
    private String[] index_arr = {"₀", "₁", "₂", "₃", "₄", "₅", "₆", "₇", "₈", "₉"};
    private List<String> indexes = new ArrayList<>(Arrays.asList(index_arr));

    // this one is called after parsing the equation in the analyzer
    public void balanceEquation() {
        Map<String, Integer> totalLeft = new HashMap<>();
        Map<String, Integer> totalRight = new HashMap<>();
        List<Map<String, Integer>> leftDicts = new ArrayList<>();
        List<Map<String, Integer>> rightDicts = new ArrayList<>();
        for (Compound compound : leftCompounds) {
            Map<String, Integer> parentDict = new HashMap<>();
            boolean parentOpened = false;
            String component = compound.getCompound();
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

                boolean first = true;
                while (i+1 < component.length() && isStringIndex(component.substring(i+1, i+2))) {
                    char idx = component.substring(i+1, i+2).charAt(0);
                    if(first){ number = idx - '₀'; first=false;}
                    else number = number*10 + (idx - '₀');
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
        for (Compound compound : rightCompounds) {
            Map<String, Integer> parentDict = new HashMap<>();
            boolean parentOpened = false;
            String component = compound.getCompound();
            int i = 0;
            int c = 1;
            Map<String, Integer> rightCounts = new HashMap<>();

            // nekde tady nakej problem asi
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

                boolean first = true;
                while (i+1 < component.length() && isStringIndex(component.substring(i+1, i+2))) {
                    char idx = component.substring(i+1, i+2).charAt(0);
                    if (first) {number = idx - '₀'; first = false;}
                    else
                        number = number*10 + (idx - '₀');
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
            balance = Balance.BALANCED;
            return;
        }
        if (balanceIdx == 0){
            LOGGER.i("Not balancable");
            balance = Balance.UNBALANCED;
            return;
        }

        //LOGGER.i("Total left: " + totalLeft.toString());
        //LOGGER.i("Total left size: " + totalLeft.size());

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

        // TODO kontrolovat, zda to vůbec můžu provádět, zda je pocet neznamych dost
        balance = Balance.BALANCED;

        SimpleMatrix sm = new SimpleMatrix(data);
        LOGGER.i("EQUATION MATRIX " + sm.toString());
        DenseMatrix64F red = CommonOps.rref(sm.getMatrix(), -1, null);
        LOGGER.i("REDUCED MATRIX: " + red.toString());

        List<Double> orderedCoefs = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            // na tohle sem malem zapomnel, pak by byly casty nuly
            orderedCoefs.add(red.get(i, cols-1));
        }
        orderedCoefs.add(red.get(0, 0));


        List<Fraction> fractions = orderedCoefs.stream().map(d -> new Fraction(Math.abs(d)))
                .collect(Collectors.toList());
        for (Fraction f : fractions) {
            LOGGER.i(f.toString());
        }
        List<Integer> finalCoefs = fractions2IntsWithLCM(fractions);
        for (Integer i : finalCoefs) {
            LOGGER.i(i.toString());
        }

        finalCoefs = finalCoefs.stream().filter(x -> x != 0).collect(Collectors.toList());
        if (finalCoefs.size() != cols) {
            balance = Balance.BALANCABLE;
            return;
        }

        // TODO get the number in beginning if there is and multiply it by the coef
        // for example if one of the compound has a number prefix, this wont work correctly
        int i = 0;
        for (Compound compound : leftCompounds) {
            if (finalCoefs.get(i) != 1)
                compound.setCompound(finalCoefs.get(i) + compound.getCompound());
            i++;
        }

        for (Compound compound : rightCompounds) {
            if (finalCoefs.get(i) != 1)
                compound.setCompound(finalCoefs.get(i) + compound.getCompound());
            i++;
        }
    }

    /*
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
    */

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

    private boolean isStringInteger(String str) {
        char ch = str.charAt(0);
        if ('0' <= ch && ch <= '9')
            return true;
        return false;
    }

    private boolean isStringIndex(String str) {
        for (String idx : indexes) {
            if (idx.equals(str))
                return true;
        }
        return false;
    }

    /*
    private class Fraction {
        // terminator
        int num, denom;

        public Fraction(double d) {
            String s = String.valueOf(d);
            int digitsDec = s.length() - 1 - s.indexOf('.');

            int denom = 1;
            for(int i = 0; i < digitsDec; i++){
                d *= 10;
                denom *= 10;
            }
            int num = (int) Math.round(d);

            // heavy weaponry, do i nee dit tho?
            BigInteger numerator = BigInteger.valueOf(num);
            BigInteger denominator = BigInteger.valueOf(denom);
            BigInteger _gcd = numerator.gcd(denominator);
            int gcd = _gcd.intValue();
            this.num = num / gcd;
            this.denom = denom / gcd;

            // a ted prasarna, nedomyslel sem zaokrouhleni :/
            if (d == 0.333){
                this.num = 1; this.denom = 3;
            }
            if (d == 0.666){
                this.num = 2; this.denom = 3;
            }
            if (d == 0.999) {
                this.num = 1; this.denom = 1;
            }
        }

        public String toString() {
            return num + "/" + denom;
        }

    }
    */



    // need to find the lcm of fractions
    private List<Integer> fractions2IntsWithLCM(List<Fraction> fractions) {
        List<Integer> denoms = fractions.stream().map(f -> f.getDenominator()).collect(Collectors.toList());
        int lcm = lcm(denoms);
        return fractions.stream().map(f -> (f.getNumerator()*lcm)/f.getDenominator()).collect(Collectors.toList());
    }

    // Euclid
    private int gcd(int a, int b) {
        if (b == 0) return a;
        return gcd(b, a % b);
    }

    // lambda magic
    private int lcm(List<Integer> numbers) {
        return numbers.stream().reduce(1, (x, y) -> x * (y / gcd(x, y)));
    }
}
