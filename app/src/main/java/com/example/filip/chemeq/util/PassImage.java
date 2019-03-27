package com.example.filip.chemeq.util;

import android.graphics.Bitmap;

public class PassImage {
    static Bitmap capturedImage = null;

    public static void setImage(Bitmap image) {
        capturedImage = image;
    }

    public static Bitmap getImage() {
        return capturedImage;
    }
}
