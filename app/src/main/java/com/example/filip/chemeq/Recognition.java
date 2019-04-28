package com.example.filip.chemeq;

import android.graphics.RectF;

import java.io.Serializable;

public class Recognition implements Serializable {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private String id;

    /**
     * Display name for the recognition.
     */
    private String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private Float confidence;

    /** Optional location within the source image for the location of the recognized object. */
    private transient RectF location;

    /** The coordinates of the rectangle to easily pass to the next activity*/
    private float left, top, right, bottom;

    private int clss;

    public Recognition (
            final String id, String title, final Float confidence, final RectF location) {
        this.id = id;
        this.title = title;
        this.confidence = confidence;
        this.location = location;
        setCoordinatesFromLocation(location);
    }

    public Recognition(RectF location) {
        this.location = location;
        setCoordinatesFromLocation(location);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title){ this.title = title;}

    public Float getConfidence() {
        return confidence;
    }

    public RectF getLocation() {
        return new RectF(location);
    }

    public void setLocation(RectF location) {
        this.location = location;
        setCoordinatesFromLocation(location);
    }

    public float getLeft() {
        return left;
    }

    public void setLeft(float left) {
        this.left = left;
    }

    public float getTop() {
        return top;
    }

    public void setTop(float top) {
        this.top = top;
    }

    public float getRight() {
        return right;
    }

    public void setRight(float right) {
        this.right = right;
    }

    public float getBottom() {
        return bottom;
    }

    public void setBottom(float bottom) {
        this.bottom = bottom;
    }

    private void setCoordinatesFromLocation(RectF location){
        this.left = location.left;
        this.top = location.top;
        this.right = location.right;
        this.bottom = location.bottom;
    }

    @Override
    public String toString() {
        String resultString = "";
        if (id != null) {
            resultString += "[" + id + "] ";
        }

        if (title != null) {
            resultString += title + " ";
        }

        if (confidence != null) {
            resultString += String.format("(%.1f%%) ", confidence * 100.0f);
        }

        if (location != null) {
            resultString += location + " ";
        }

        return resultString.trim();
    }
}
