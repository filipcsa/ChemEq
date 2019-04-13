package com.example.filip.chemeq.detecting;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class RecognitionListItem {

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
}
