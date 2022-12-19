package com.example.paintondotmetrix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Arrays;
import java.util.Vector;

public class PaintView extends View {
    final int DOT_SIDE_NUM = 8;
    final int BRUSH_SIZE = 100;
    int x = 100, y = 100;
    Vector<MyPoint> points = new Vector<>();
    Bitmap mbitmap;
    Canvas mcanvas;
    Bitmap scaledBitmap = null;
    boolean[][] paintArray = new boolean[DOT_SIDE_NUM][DOT_SIDE_NUM];
    int[][] redArray = new int[DOT_SIDE_NUM][DOT_SIDE_NUM];

    public PaintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundColor(Color.BLACK);
    }

    public void printPaint() {
        scaledBitmap = Bitmap.createScaledBitmap(mbitmap, DOT_SIDE_NUM, DOT_SIDE_NUM, false);
        for (int _x = 0; _x < DOT_SIDE_NUM; _x++) {
            for (int _y = 0; _y < DOT_SIDE_NUM; _y++) {
                int pixel = scaledBitmap.getPixel(_x, _y);
                redArray[_x][_y] = Color.red(pixel);
                paintArray[_x][_y] = Color.red(pixel) > 128;
            }
        }
        ((MainActivity) MainActivity.context_main).dotmetrix(paintArray);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mbitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        mcanvas = new Canvas();
        mcanvas.setBitmap(mbitmap);

        Paint paint = new Paint();

        paint.setColor(Color.RED);
//        mcanvas.drawCircle(x, y, BRUSH_SIZE, paint);

        for (int i = 0; i < points.size() - 1; i++) {
            MyPoint cur = points.get(i);
            MyPoint next = points.get(i + 1);
            if (cur.end)
                continue;
            mcanvas.drawCircle(cur.x, cur.y, BRUSH_SIZE / 2, paint);
            mcanvas.drawCircle(next.x, next.y, BRUSH_SIZE / 2, paint);
            paint.setStrokeWidth(BRUSH_SIZE);
            mcanvas.drawLine(cur.x, cur.y, next.x, next.y, paint);
        }

        canvas.drawBitmap(mbitmap, 0, 0, null);

        this.getLayoutParams().height = getWidth();
        this.setLayoutParams(this.getLayoutParams());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = (int) event.getX();
        y = (int) event.getY();

        points.add(new MyPoint(x, y, event.getAction() == MotionEvent.ACTION_UP));
        invalidate();

        if (event.getActionMasked() == MotionEvent.ACTION_UP)
            printPaint();
        return true;
    }

    public void clearCanvas() {
        points.clear();
        mbitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        printPaint();
    }

    class MyPoint extends Point {
        boolean end = false;
        public MyPoint(int x, int y, boolean end) {
            super(x, y);
            this.end = end;
        }
    }
}