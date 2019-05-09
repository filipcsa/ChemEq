package com.example.filip.chemeq.util;

import com.example.filip.chemeq.CameraActivity;

/**
 * Model to store the data of a bounding box
 *
 * Created by Zoltan Szabo on 12/17/17.
 * URL: https://github.com/szaza/android-yolo-v2
 */
public class BoundingBox {
    private Logger LOGGER = new Logger(BoundingBox.class.getName());
    private float x, y;
    private float width, height;
    private float confidence;
    private double[] classes;

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public double[] getClasses() {
        return classes;
    }

    public void setClasses(double[] classes) {
        this.classes = classes;
    }

    public int getObjectClass() {
        if (classes[0] > classes[1])
            return 0;
        return 1;
    }
}
