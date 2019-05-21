package com.example.filip.chemeq.detecting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Equation {

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

    public void equationEditedCallback(String newEq) {

    }

}
