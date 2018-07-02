package com.example.yannick.camera2test;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

class LocalOtsuProcessor {
    private int boxSize;
    private Mat mat;
    private Contour contour;
    private int x0, y0;
    private Orientation last;

    enum Orientation{
        UNDEFINED,
        TOP,
        BOTTOM,
        RIGHT,
        LEFT
    }

    public LocalOtsuProcessor(Mat source, Contour contour, int boxSize){
        this.boxSize = boxSize;
        this.mat = source;

        // calculate the initial Orientation to the start x0 and y0
        int[] init = contour.getEndDirection();
        x0 = init[0];
        y0 = init[1];
        last = Orientation.values()[init[2]];
        Log.d("OTSU", "initvals[2]: " + last);
        cutBox(source);
    }

    Mat run(){

        // TEST
        for (int i = 0; i < 3; i++) {
            Rect r = cutRect(mat);
            Mat otsu = otsuOnArea(cutBox(mat));

            Mat aux = mat.rowRange(r.y, r.y + r.height).colRange(r.x, r.x + r.width);
            otsu.copyTo(aux);
        }
        return mat;
    }

    private Rect cutRect(Mat source){
        Rect rect;
        switch (last){
            case TOP: rect = new Rect(x0 - boxSize / 2, y0 - boxSize, boxSize, boxSize); break;
            case BOTTOM: rect = new Rect(x0 - boxSize / 2, y0, boxSize, boxSize); break;
            case RIGHT: rect = new Rect(x0, y0 - boxSize / 2, boxSize, boxSize); break;
            default: rect = new Rect(x0 - boxSize, y0 - boxSize / 2, boxSize, boxSize);
        }
        return rect;
    }

    private Mat cutBox(Mat source){
        Rect rect;
        switch (last){
            case TOP: rect = new Rect(x0 - boxSize / 2, y0 - boxSize, boxSize, boxSize); break;
            case BOTTOM: rect = new Rect(x0 - boxSize / 2, y0, boxSize, boxSize); break;
            case RIGHT: rect = new Rect(x0, y0 - boxSize / 2, boxSize, boxSize); break;
            case LEFT: rect = new Rect(x0 - boxSize, y0 - boxSize / 2, boxSize, boxSize);
            default: rect = new Rect(0,0,0, 0);
        }
        Log.d("OTSU", "Rect: " + rect.toString());

        return new Mat(source, rect);
    }

    private Mat otsuOnArea(Mat area){
        Mat dest = new Mat();
        double threshold = Imgproc.threshold(area, dest, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);
        Log.d("OTSU", "threshold: " + threshold);

        // now get the contour
        byte[] colors = new byte[boxSize * boxSize];
        dest.get(0,0, colors);

        // now multiply by a unity vector, once normal, once transposed
        Mat xVec = new Mat();
        Core.multiply(dest, Mat.ones(1, boxSize, 0), xVec);
        xVec.mul(Mat.ones(xVec.size(), 0), (1/255.0));
        Mat yVec = new Mat();
        Core.multiply(dest.t(), Mat.ones(1, boxSize, 0), yVec);
        yVec.mul(Mat.ones(yVec.size(), 0), (1/255.0));

        return dest;
    }
}
