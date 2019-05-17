package com.example.filip.chemeq.detecting;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChemicalEquation {

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
}
