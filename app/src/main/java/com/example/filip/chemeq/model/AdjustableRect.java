package com.example.filip.chemeq.model;

import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;

import com.example.filip.chemeq.R;
import com.example.filip.chemeq.util.Logger;

import java.util.ArrayList;

public class AdjustableRect {

    Point point1, point3;
    Point point2, point4;

    private EqListItem eqListItem = new EqListItem();

    int groupId = -1;
    private ArrayList<ColorBall> colorballs = new ArrayList <> ();
    private boolean selected = false;

    private Logger LOGGER = new Logger(AdjustableRect.class.getName());

    public AdjustableRect(Context context, Recognition recognition) {
        point1 = new Point();
        point1.x = (int) recognition.getLeft();
        point1.y = (int) recognition.getTop();

        point2 = new Point();
        point2.x = (int) recognition.getRight();
        point2.y = (int) recognition.getTop();

        point3 = new Point();
        point3.x = (int) recognition.getRight();
        point3.y = (int) recognition.getBottom();

        point4 = new Point();
        point4.x = (int) recognition.getLeft();
        point4.y = (int) recognition.getBottom();

        // declare each ball with the ColorBall class
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point1, 0));
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point2, 1));
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point3, 2));
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point4, 3));
    }

    public AdjustableRect(Context context) {
        point1 = new Point();
        point1.x = 300;
        point1.y = 300;

        point2 = new Point();
        point2.x = 700;
        point2.y = 300;

        point3 = new Point();
        point3.x = 300;
        point3.y = 500;

        point4 = new Point();
        point4.x = 700;
        point4.y = 500;

        // declare each ball with the ColorBall class
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point1, 0));
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point2, 1));
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point3, 2));
        colorballs.add(new ColorBall(context, R.drawable.gray_circle, point4, 3));
    }

    public Point getPoint1() {
        return point1;
    }

    public void setPoint1(Point point1) {
        this.point1 = point1;
    }

    public Point getPoint3() {
        return point3;
    }

    public void setPoint3(Point point3) {
        this.point3 = point3;
    }

    public Point getPoint2() {
        return point2;
    }

    public void setPoint2(Point point2) {
        this.point2 = point2;
    }

    public Point getPoint4() {
        return point4;
    }

    public void setPoint4(Point point4) {
        this.point4 = point4;
    }

    public int getGroupId() {
        return groupId;
    }

    public ArrayList<ColorBall> getColorballs() {
        return colorballs;
    }

    public void setColorballs(ArrayList<ColorBall> colorballs) {
        this.colorballs = colorballs;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public EqListItem getEqListItem() {
        return eqListItem;
    }

    public RectF getLocation() {
        int left = point1.x < point2.x ? point1.x : point2.x;
        left = left < point3.x ? left : point3.x;

        int right = point1.x > point2.x ? point1.x : point2.x;
        right = right > point3.x ? right : point3.x;

        int top = point1.y < point2.y ? point1.y : point2.y;
        top = top < point3.y ? top : point3.y;

        int bottom = point1.y > point2.y ? point1.y : point2.y;
        bottom = bottom > point3.y ? bottom : point3.y;

        return  new RectF(left, top, right, bottom);
    }

    public void setLocation(RectF rect) {
        int top = (int) rect.top;
        int left = (int) rect.left;
        int right = (int) rect.right;
        int bottom = (int) rect.bottom;

        point1.x = left;
        point1.y = top;

        point2.x = right;
        point2.y = top;

        point3.x = right;
        point3.y = bottom;

        point4.x = left;
        point4.y = bottom;
    }
}
