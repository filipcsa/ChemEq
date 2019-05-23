package com.example.filip.chemeq.model;

public class Compound {
    private double confidence;
    private String compound = "";
    private boolean parsed = false;
    private String endCharacter = "";
    private int endPos;
    private boolean parenthesis = false;
    private String trivName = "";

    public Compound() {};

    public Compound(double confidence, String compound) {
        this.confidence = confidence;
        this.compound = compound;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getCompound() {
        return compound;
    }

    public void setCompound(String compound) {
        this.compound = compound;
    }

    public void addCharacter(String readCharacter) {
        compound += readCharacter;
    }

    public String getLastCharacter() {
        if (compound.length() == 0) return "";
        return compound.substring(compound.length()-1);
    }

    public boolean isParsed() {
        return parsed;
    }

    public void setParsed(boolean parsed) {
        this.parsed = parsed;
    }

    public String getEndCharacter() {
        return endCharacter;
    }

    public void setEndCharacter(String endCharacter) {
        this.endCharacter = endCharacter;
    }

    public int getEndPos() {
        return endPos;
    }

    public void setEndPos(int endPos) {
        this.endPos = endPos;
    }

    public void addConfidence(double characterConf) {
        this.confidence += characterConf;
    }

    public boolean isParenthesis() {
        return parenthesis;
    }

    public void setParenthesis(boolean parenthesis) {
        this.parenthesis = parenthesis;
    }

    public String getTrivName() {
        return trivName;
    }

    public void setTrivName(String trivName) {
        this.trivName = trivName;
    }

    public Compound getCopy() {
        Compound copy = new Compound(this.confidence, this.compound);
        copy.setParenthesis(this.parenthesis);
        return copy;
    }
}
