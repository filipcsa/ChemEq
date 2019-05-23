package com.example.filip.chemeq.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.filip.chemeq.DetectorActivity;
import com.example.filip.chemeq.model.AdjustableRect;
import com.example.filip.chemeq.model.ColorBall;
import com.example.filip.chemeq.model.Recognition;
import com.example.filip.chemeq.util.Logger;

import java.util.ArrayList;
import java.util.List;


public class RectDrawView extends View {
    private Logger LOGGER = new Logger(RectDrawView.class.getName());

    private List<AdjustableRect> rectangles = new ArrayList<>();
    private DetectorActivity da;


    int groupId = -1;
    private int balID = 0;
    Paint paint;
    Canvas canvas;

    public void initDrawView(Context context, List<Recognition> recognitions) {
        this.da = (DetectorActivity) context;
        for (Recognition recognition : recognitions) {
            AdjustableRect ar = new AdjustableRect(context, recognition);
            rectangles.add(ar);
            // don't want to run ocr before adjusting the rectangles in detectorActivity
            //((DetectorActivity) context).runOCRForRectangle(ar.getLocation(), ar.getEqListItem());
            ((DetectorActivity) context).addRecognitionListItem(ar.getEqListItem());
        }
    }

    public RectDrawView(Context context) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();

    }

    public RectDrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public RectDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    /** After add btn is clicked, adds a new rect to the drawView overlay **/
    public void createNewAdjustableRecognitionRect() {
        AdjustableRect rectangle = new AdjustableRect(da);
        rectangles.add(rectangle);
        da.addRecognitionListItem(rectangle.getEqListItem());
        // after adding a new rectangle it has to be drawn
        invalidate();
    }

    /** If there is a selected rect, remove it **/
    public void removeSelectedAdjustableRecognitionRect() {
        for (AdjustableRect rectangle : rectangles) {
            if (!rectangle.isSelected()) continue;
            rectangles.remove(rectangle);
            da.removeRecognitionListItem(rectangle.getEqListItem());
            invalidate();
            break;
        }
    }

    public void redraw() {
        invalidate();
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {

        paint.setAntiAlias(true);
        paint.setDither(true);

        paint.setColor(Color.parseColor("#00000000"));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.ROUND);

        paint.setStrokeWidth(5);

        canvas.drawPaint(paint);
        for (AdjustableRect rectangle : rectangles) {
            if (rectangle.isSelected()) paint.setColor(Color.parseColor("#550000FF"));
            else paint.setColor(Color.parseColor("#55FFFFFF"));
            if (groupId == 1) {

                canvas.drawRect(rectangle.getPoint1().x,
                        rectangle.getPoint3().y, rectangle.getPoint3().x, rectangle.getPoint1().y, paint);
            } else {

                canvas.drawRect(rectangle.getPoint2().x,
                        rectangle.getPoint4().y, rectangle.getPoint4().x, rectangle.getPoint2().y, paint);
            }

            // draw the balls on the canvas
            for (ColorBall ball : rectangle.getColorballs()) {
                canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                        new Paint());
            }
        }
    }

    private int prevX;
    private int prevY;

    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        LOGGER.i(X + " " + Y);

        da.looseFocus();

        switch (eventaction) {

            case MotionEvent.ACTION_DOWN:
                // touch down so check if the finger is on
                // a ball

                prevX = X;
                prevY = Y;
                balID = -1;
                groupId = -1;
                boolean isAlreadySelected = false;
                for (AdjustableRect rectangle : rectangles) {
                    rectangle.setSelected(false);
                    for (ColorBall ball : rectangle.getColorballs()) {
                        // check if inside the bounds of the ball (circle)
                        // get the center for the ball
                        int centerX = ball.getX() + ball.getWidthOfBall()/2;
                        int centerY = ball.getY() + ball.getHeightOfBall()/2;
                        // calculate the radius from the touch to the center of the ball
                        double radCircle = Math.sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y) * (centerY - Y)));

                        if (radCircle < ball.getWidthOfBall()) {
                            isAlreadySelected = true;
                            rectangle.setSelected(true);
                            LOGGER.i("BALL TOUCHED " + centerX + " " + centerY);
                            balID = ball.getID();
                            if (balID == 1 || balID == 3) {
                                groupId = 2;
                                canvas.drawRect(rectangle.getPoint1().x, rectangle.getPoint3().y, rectangle.getPoint3().x, rectangle.getPoint1().y,
                                        paint);
                            } else {
                                groupId = 1;
                                canvas.drawRect(rectangle.getPoint2().x, rectangle.getPoint4().y, rectangle.getPoint4().x, rectangle.getPoint2().y,
                                        paint);
                            }
                            invalidate();
                            break;
                        }
                        invalidate();
                    }
                    if (isAlreadySelected){
                        LOGGER.i("Already selected a different rectangle");
                        break;
                    }
                }

                break;

            case MotionEvent.ACTION_MOVE:
                // touch drag with the ball
                // move the balls the same as the finger
                for (AdjustableRect rectangle : rectangles) {
                    if (!rectangle.isSelected())
                        continue;
                    //LOGGER.i("ALMOST MOVING THE BALL, BALL ID: " + balID);
                    if (balID > -1) {
                        //LOGGER.i("MOVING THE BALL");
                        int dX = X - prevX;
                        int dY = Y - prevY;
                        prevX = X;
                        prevY = Y;
                        rectangle.getColorballs().get(balID).setX(rectangle.getColorballs().get(balID).getX() + dX);
                        rectangle.getColorballs().get(balID).setY(rectangle.getColorballs().get(balID).getY() + dY);


                        if (groupId == 1) {
                            rectangle.getColorballs().get(1).setX(rectangle.getColorballs().get(0).getX());
                            rectangle.getColorballs().get(1).setY(rectangle.getColorballs().get(2).getY());
                            rectangle.getColorballs().get(3).setX(rectangle.getColorballs().get(2).getX());
                            rectangle.getColorballs().get(3).setY(rectangle.getColorballs().get(0).getY());
                            canvas.drawRect(rectangle.getPoint1().x, rectangle.getPoint3().y, rectangle.getPoint3().x, rectangle.getPoint1().y,
                                    paint);
                        } else {
                            rectangle.getColorballs().get(0).setX(rectangle.getColorballs().get(1).getX());
                            rectangle.getColorballs().get(0).setY(rectangle.getColorballs().get(3).getY());
                            rectangle.getColorballs().get(2).setX(rectangle.getColorballs().get(3).getX());
                            rectangle.getColorballs().get(2).setY(rectangle.getColorballs().get(1).getY());
                            canvas.drawRect(rectangle.getPoint2().x, rectangle.getPoint4().y, rectangle.getPoint4().x, rectangle.getPoint2().y,
                                    paint);
                        }

                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                for (AdjustableRect rectangle : rectangles) {
                    // LOGGING
                    if (rectangle.isSelected()) {
                        LOGGER.i("BALL RELEASED");
                        da.runOCRForRectangle(rectangle.getLocation(), rectangle.getEqListItem());
                    }
                }
                break;
        }
        // redraw the canvas
        invalidate();
        return true;
    }

    public void runOCROnAllAdjustableRects() {
        for (AdjustableRect rect : rectangles) {
            da.runOCRForRectangle(rect.getLocation(), rect.getEqListItem());
        }
    }


    public List<AdjustableRect> getAdjustableRecognitionRects() {
        return rectangles;
    }
}
