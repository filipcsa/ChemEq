package com.example.filip.chemeq.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

/**
 * The corner of the adjustableRect
 */
public class ColorBall {

    Bitmap bitmap;
    Context mContext;
    Point point;
    int id;
    int count = 0;

    public ColorBall(Context context, int resourceId, Point point) {
        this.id = count++;
        bitmap = BitmapFactory.decodeResource(context.getResources(),
                resourceId);
        mContext = context;
        this.point = point;
    }

    public ColorBall(Context context, int resourceId, Point point, int id) {
        this.id = id;
        bitmap = BitmapFactory.decodeResource(context.getResources(),
                resourceId);
        mContext = context;
        this.point = point;
    }

    public int getWidthOfBall() {
        return bitmap.getWidth();
    }

    public int getHeightOfBall() {
        return bitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getX() {
        return point.x - getWidthOfBall() / 2;
    }

    public int getY() {
        return point.y - getHeightOfBall() / 2;
    }

    public int getID() {
        return id;
    }

    public void setX(int x) {
        point.x = x + getWidthOfBall() / 2;
    }

    public void setY(int y) {
        point.y = y + getHeightOfBall()/ 2;
    }
}
