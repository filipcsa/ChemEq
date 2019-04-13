package com.example.filip.chemeq.detecting;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.example.filip.chemeq.DetectorActivity;
import com.example.filip.chemeq.Recognition;
import com.example.filip.chemeq.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * DrawView class holds corner objects and handle them by drawing
 * overlay.
 *
 * Based on solution by Chintan Rathod (http://chintanrathod.com)
 */
public class DrawView extends View {
    private Logger LOGGER = new Logger(DrawView.class.getName());

    private List<AdjustableRecognitionRect> rectangles = new ArrayList<>();
    private DetectorActivity da;


    int groupId = -1;
    private int balID = 0;
    Paint paint;
    Canvas canvas;

    /*
    public DrawView(Context context, List<Recognition> recognitions) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
        this.da = (DetectorActivity) context;
        for (Recognition recognition : recognitions) {
            AdjustableRecognitionRect ar = new AdjustableRecognitionRect(context, recognition);
            rectangles.add(ar);
            ((DetectorActivity) context).runOCRForRectangle(ar.getLocation(), ar.getRecognitionListItem());
            ((DetectorActivity) context).addRecognitionListItem(ar.getRecognitionListItem());
        }

    }
    */

    public void initDrawView(Context context, List<Recognition> recognitions) {
        this.da = (DetectorActivity) context;
        for (Recognition recognition : recognitions) {
            AdjustableRecognitionRect ar = new AdjustableRecognitionRect(context, recognition);
            rectangles.add(ar);
            // don't want to run ocr before adjusting the rectangles in detectorActivity
            //((DetectorActivity) context).runOCRForRectangle(ar.getLocation(), ar.getRecognitionListItem());
            ((DetectorActivity) context).addRecognitionListItem(ar.getRecognitionListItem());
        }
    }

    public DrawView(Context context) {
        super(context);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();

    }

    public DrawView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    /** After add btn is clicked, adds a new rect to the drawView overlay **/
    public void createNewAdjustableRecognitionRect() {
        AdjustableRecognitionRect rectangle = new AdjustableRecognitionRect(da);
        rectangles.add(rectangle);
        da.addRecognitionListItem(rectangle.getRecognitionListItem());
        // after adding a new rectangle it has to be drawn
        invalidate();
    }

    /** If there is a selected rect, remove it **/
    public void removeSelectedAdjustableRecognitionRect() {
        for (AdjustableRecognitionRect rectangle : rectangles) {
            if (!rectangle.isSelected()) continue;
            rectangles.remove(rectangle);
            da.removeRecognitionListItem(rectangle.getRecognitionListItem());
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
        // canvas.drawColor(0xFFCCCCCC); //if you want another background color

        paint.setAntiAlias(true);
        paint.setDither(true);
        // no color for overlay
        paint.setColor(Color.parseColor("#00000000"));
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeJoin(Paint.Join.ROUND);
        // mPaint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5);

        canvas.drawPaint(paint);
        for (AdjustableRecognitionRect rectangle : rectangles) {
            if (rectangle.isSelected()) paint.setColor(Color.parseColor("#550000FF"));
            else paint.setColor(Color.parseColor("#55FFFFFF"));
            if (groupId == 1) {
                /*
                canvas.drawRect(point1.x + colorballs.get(0).getWidthOfBall() / 2,
                        point3.y + colorballs.get(2).getWidthOfBall() / 2, point3.x + colorballs.get(2).getWidthOfBall() / 2, point1.y + colorballs.get(0).getWidthOfBall() / 2, paint);
                        */
                canvas.drawRect(rectangle.getPoint1().x,
                        rectangle.getPoint3().y, rectangle.getPoint3().x, rectangle.getPoint1().y, paint);
            } else {
                /*
                canvas.drawRect(point2.x + colorballs.get(1).getWidthOfBall() / 2,
                        point4.y + colorballs.get(3).getWidthOfBall() / 2, point4.x + colorballs.get(3).getWidthOfBall() / 2, point2.y + colorballs.get(1).getWidthOfBall() / 2, paint);
                        */
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
    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        LOGGER.i(X + " " + Y);

        switch (eventaction) {

            case MotionEvent.ACTION_DOWN:
                // touch down so check if the finger is on
                // a ball

                prevX = X;
                prevY = Y;
                balID = -1;
                groupId = -1;
                boolean isAlreadySelected = false;
                for (AdjustableRecognitionRect rectangle : rectangles) {
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
                for (AdjustableRecognitionRect rectangle : rectangles) {
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
                for (AdjustableRecognitionRect rectangle : rectangles) {
                    // LOGGING
                    if (rectangle.isSelected()) {
                        LOGGER.i("BALL RELEASED");
                        da.runOCRForRectangle(rectangle.getLocation(), rectangle.getRecognitionListItem());
                    }
                }
                break;
        }
        // redraw the canvas
        invalidate();
        return true;
    }

    public void runOCROnAllAdjustableRects() {
        for (AdjustableRecognitionRect rect : rectangles) {
            da.runOCRForRectangle(rect.getLocation(), rect.getRecognitionListItem());
        }
    }


    public List<AdjustableRecognitionRect> getAdjustableRecognitionRects() {
        return rectangles;
    }
}
