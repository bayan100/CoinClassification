package com.example.yannick.camera2test;

import android.util.Log;
import android.view.OrientationEventListener;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.channels;

class LocalOtsuProcessor {
    private int boxSize;
    private Mat mat;
    public Contour contour;
    private int x0, y0;
    private Orientation last, center;
    private double savedThreshold = 128;
    private List<Point> points = new ArrayList<>();


    enum Orientation{
        UNDEFINED,
        RIGHT,
        LEFT,
        TOP,
        BOTTOM
    }

    public LocalOtsuProcessor(Mat source, Contour contour, int boxSize){
        this.boxSize = boxSize;
        this.contour = contour;
        this.mat = source;

        // calculate the initial Orientation to the start x0 and y0
        int[] init = contour.getEndDirection();
        x0 = init[1];
        y0 = init[0];
        last = Orientation.values()[init[2]];
        Log.d("OTSU", "last: " + last);
        mat.put(150, 20, new byte[] {(byte)255, (byte)255, (byte)255, (byte)255});

        mat.put(x0, y0 - 1, new byte[] {(byte)255, (byte)255, (byte)255, (byte)255});
        mat.put(x0, y0, new byte[] {(byte)128, (byte)128, (byte)128, (byte)128});
        mat.put(x0, y0 + 1, new byte[] {(byte)0, (byte)0, (byte)0, (byte)0});

        // find the center
        Mat dest = new Mat();
        Imgproc.threshold(cutBox(mat), dest, 0, 255, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY);

        Rect r = cutRect(mat);
        Mat aux = mat.colRange(r.y - r.height, r.y).rowRange(r.x, r.x + r.width);
        //dest.copyTo(aux);

        // now get the contour
        byte[] colors = new byte[boxSize * boxSize];
        dest.get(0,0, colors);

        switch (last){
            case RIGHT: center = (colors[0] < 0) ? Orientation.TOP : Orientation.BOTTOM; break;
            case LEFT: center = (colors[boxSize - 1] < 0) ? Orientation.TOP : Orientation.BOTTOM; break;
            case BOTTOM: center = (colors[0] < 0) ? Orientation.LEFT : Orientation.RIGHT; break;
            case TOP: center = (colors[boxSize * boxSize - 1] < 0) ? Orientation.RIGHT : Orientation.LEFT; break;
        }
        Log.d("OTSU", "center: " + center);

        dest.convertTo(dest, CV_32F);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < boxSize * boxSize; i++) {
            if(i % boxSize == 0)
                sb.append("\n");
            sb.append(colors[i]);
            sb.append(", ");
        }
        Log.d("OTSU", sb.toString());
    }

    Mat run(){

        // TEST
        Log.d("OTSU", "cols: " + mat.cols() + " rows: " + mat.rows());
        for (int i = 0; i < 12; i++) {
            Rect r = cutRect(mat);
            Mat aux = mat.colRange(r.y - r.height, r.y).rowRange(r.x, r.x + r.width);
            Mat otsu = otsuOnArea(cutBox(mat));
            otsu.copyTo(aux);
            //cutBox(mat).copyTo(aux);
        }

        // afterwards, add all the found contour points to the contour
        contour.appendPoints(points);

        return mat;
    }

    private Rect cutRect(Mat source){
        Log.d("OTSU", "last: " + last.toString());
        Log.d("OTSU", "x0: " + x0 + ", y0: " + y0);

        Rect rect;
        switch (last){
            case TOP: rect = new Rect(x0 - boxSize / 2, y0 + boxSize, boxSize, boxSize); break;
            case BOTTOM: rect = new Rect(x0 - boxSize / 2, y0, boxSize, boxSize); break;
            case RIGHT: rect = new Rect(x0, y0 + boxSize / 2, boxSize, boxSize); break;
            default: rect = new Rect(x0 - boxSize, y0 + boxSize / 2, boxSize, boxSize);
        }
        Log.d("OTSU", rect.toString());
        return rect;
    }

    private Mat cutBox(Mat source){
        Rect r = cutRect(source);
        return mat.colRange(r.y - r.height, r.y).rowRange(r.x, r.x + r.width);
    }

    private Mat otsuOnArea(Mat area) {
        Mat dest = new Mat();
        double threshold = Imgproc.threshold(area, dest, 0, 255, Imgproc.THRESH_OTSU);

        // keep an threshold save to prevent some images from unrealistic thresholds
        if(threshold > 32 && threshold < 224)
            savedThreshold = threshold;
        else
            // default on last 'good' value
            Imgproc.threshold(area, dest, savedThreshold, 255, Imgproc.THRESH_BINARY);
        Log.d("OTSU", "threshold: " + threshold);

        int oldType = dest.type();
        dest.convertTo(dest, CV_32F);

        // now get the contour
        //dest = dest.t();
        float[] colors = new float[boxSize * boxSize];
        dest.get(0,0, colors);
        float[] result = new float[boxSize * boxSize];
        Arrays.fill(result, 255);

        // start at the location of the last contour point
        Point endpoint = null;
        int cc = 0;
        start:
        while (true) {
            cc++;

            Point current = null, shift = null;
            switch (last) {
                case TOP:
                    current = tryAlternativesAlongBoxEdge(new Point(boxSize / 2, boxSize - 1), colors, 1, 0);
                    shift = new Point(boxSize / 2, boxSize - 1);
                    break;
                case BOTTOM:
                    current = tryAlternativesAlongBoxEdge(new Point(boxSize / 2, 0), colors, 1, 0);
                    shift = new Point(boxSize / 2, 0);
                    break;
                case RIGHT:
                    current = tryAlternativesAlongBoxEdge(new Point(0, boxSize / 2), colors, 0, 1);
                    shift = new Point(0, -boxSize / 2);
                    break;
                default:
                    current = tryAlternativesAlongBoxEdge(new Point(boxSize - 1, boxSize / 2), colors, 0, 1);
                    shift = new Point(boxSize - 1, -boxSize / 2);
                    break;
            }

            /*StringBuilder sb = new StringBuilder();
            for (int y = 0; y < boxSize; y++) {
                for (int x = 0; x < boxSize; x++) {
                    int index = (int) ((x + 1) * boxSize - y - 1);
                    sb.append((colors[index] < 100) ? "  0.0" : colors[index]);
                    sb.append(", ");
                }
                sb.append("\n");
            }
            Log.d("OTSU3", sb.toString());*/

            if(current == null)
                break start;
            points.add(new Point(current.x + x0, current.y + y0));

            // follow the contour line
            int count = 0;
            List<Point> previousIndices = new ArrayList<>();
            previousIndices.add(current);

            outer:
            while (true) {
                // try 3 different directions
                count++;
                //Log.d("OTSU2", "------");
                for (int i = 0; i < 3; i++) {
                    Point next = next(i, current);

                    // found next contour point
                    int index = (int) ((next.x + 1) * boxSize - next.y - 1);
                    //Log.d("OTSU2", "i: " + i + next.toString() + " -> " + colors[index]);

                    if (colors[index] < 128) {
                        //Log.d("OTSU2", "inner loop: " + next.toString());
                        previousIndices.add(current);
                        result[index] = 0;
                        colors[index] = 128;

                        points.add(new Point(next.x + x0 - shift.x, -next.y + y0 - shift.y));

                        // check if we reached a wall
                        if (points.size() > 1) {
                            // Left
                            if (next.x == 0 && current.x != 0) {
                                transitOrientations(Orientation.LEFT);
                                break outer;
                            }
                            // Right
                            else if (next.x == boxSize - 1 && current.x != boxSize - 1) {
                                transitOrientations(Orientation.RIGHT);
                                break outer;
                            }
                            // Top
                            else if (next.y == 0 && current.y != 0) {
                                transitOrientations(Orientation.TOP);
                                break outer;
                            }
                            // Bottom
                            else if (next.y == boxSize - 1 && current.y != boxSize - 1) {
                                transitOrientations(Orientation.BOTTOM);
                                break outer;
                            }
                        }

                        current = next;
                        continue outer;
                    }
                }
                // if we didn't find a way onward and are not on either side,
                // trace back to a previous position from where we can start anew
                if (previousIndices.size() > 0) {
                    Point previous = previousIndices.get(previousIndices.size() - 1);
                    previousIndices.remove(previousIndices.size() - 1);
                    colors[(int) ((previous.x + 1) * boxSize - previous.y - 1)] = 255;
                    current = previous;
                    continue outer;
                }

                // if no such position is found try to find a new starting spot
                else {
                    continue start;
                }
            }
            break start;
        }


        for (int i = 0; i < points.size(); i++) {
            Log.d("OTSU3", "p(" + i + "): " + points.get(i).toString());
        }

        // append points to the big contour

        if(points.size() > 0) {
            endpoint = points.get(points.size() - 1);
            x0 = (int) endpoint.x;
            y0 = (int) endpoint.y;
        }

        dest.put(0,0, colors);
        dest.convertTo(dest, oldType);
        return dest;
    }

    private Point tryAlternativesAlongBoxEdge(Point current, float[] colors, int xdir, int ydir){

        //List<Integer> debug = new ArrayList<>();

        for (int i = 0; i < boxSize; i++) {
            if(i == 1)
                continue;
            // cast: x_ = b - y - 1, y_ = x
            // ind: (x + 1) * b - y - 1
            int index = (int)(-(current.y + ydir * (i / 2) * ((i % 2 == 0) ? -1 : 1)) - 1 + boxSize * (current.x + xdir * (i / 2) * ((i % 2 == 0) ? -1 : 1) + 1));
            int index2 = (int)(-(current.y + ydir * ((i + 2) / 2) * ((i % 2 == 0) ? -1 : 1)) - 1 + boxSize * (current.x + xdir * ((i + 2) / 2) * ((i % 2 == 0) ? -1 : 1) + 1));
            int index3 = (int)(-(current.y + ydir * ((i - 2) / 2) * ((i % 2 == 0) ? -1 : 1)) - 1 + boxSize * (current.x + xdir * ((i - 2) / 2) * ((i % 2 == 0) ? -1 : 1) + 1));

            /*Log.d("OTSU3", "y: " + (current.y + ydir * (i / 2) * ((i % 2 == 0) ? -1 : 1)) + ", x: " + (current.x + xdir * (i / 2) * ((i % 2 == 0) ? -1 : 1)) + " r: " +
                    colors[index]);
            Log.d("OTSU3", "y: " + (current.y + ydir * ((i + 2) / 2) * ((i % 2 == 0) ? -1 : 1)) + ", x: " + (current.x + xdir * ((i + 2) / 2) * ((i % 2 == 0) ? -1 : 1))+ " r: ");
            Log.d("OTSU3", "y: " + (current.y + ydir * ((i - 2) / 2) * ((i % 2 == 0) ? -1 : 1)) + ", x: " + (current.x + xdir * ((i - 2) / 2) * ((i % 2 == 0) ? -1 : 1))
                    + " i: " + i + " i-2/2: " + ((i - 2) / 2) + " *ydir: " + (ydir * ((i - 2) / 2)) + " *%: " + ((i % 2 == 0) ? -1 : 1) + " r: ");
            Log.d("OTSU3", "index: " + index + ", index2: " + index2);*/

            //debug.add(index);

            if(index < colors.length && colors[index] < 128
               && ((index2 >= colors.length || (index2 < colors.length && (colors[index2] > 128)))
                   || (index3 < 0 || (index3 > 0 && (colors[index3] > 128))))){
                //Log.d("OTSU3","found!");


                /*for (Integer ind : debug) {
                    colors[ind] = 128;
                }*/

                return new Point(current.x + xdir * (i / 2) * ((i % 2 == 0) ? -1 : 1),current.y + ydir * (i / 2) * ((i % 2 == 0) ? -1 : 1));
            }
        }
        return null;
    }

    private Point next(int ntry, Point current){
        Point p = null;
        switch (ntry){
            case 0:
                // direction center
                p = orientationToPoint(center);
                break;
            case 1:
                // direction straight ahead
                p = orientationToPoint(last);
                break;
            case 2:
                // direction -center
                switch (center){
                    case TOP: p = orientationToPoint(Orientation.BOTTOM); break;
                    case BOTTOM: p = orientationToPoint(Orientation.TOP); break;
                    case LEFT: p = orientationToPoint(Orientation.RIGHT); break;
                    case RIGHT: p = orientationToPoint(Orientation.LEFT); break;
                }
        }
        return new Point(current.x + p.x, current.y + p.y);
    }

    private Point orientationToPoint(Orientation item){
        switch (item){
            case TOP: return new Point(0, -1);
            case BOTTOM: return new Point(0, 1);
            case LEFT: return new Point(-1, 0);
            case RIGHT: return new Point(1, 0);
        }
        return null;
    }

    private void transitOrientations(Orientation newO){
        if(last == newO)
            return;
        if(center == newO){
            // if new orientation in direction of the old center: invert the last direction as new center
            switch (last){
                case TOP: center = Orientation.BOTTOM; break;
                case BOTTOM: center = Orientation.TOP; break;
                case RIGHT: center = Orientation.LEFT; break;
                case LEFT: center = Orientation.RIGHT;
            }
        }
        else{
            // else take old direction as new center
            center = last;
        }

        last = newO;
    }

    private Mat otsuOnArea2(Mat area){
        Mat dest = new Mat();
        double threshold = Imgproc.threshold(area, dest, 0, 255, Imgproc.THRESH_OTSU);
        // | Imgproc.THRESH_BINARY

        int oldType = dest.type();
        dest.convertTo(dest, CV_32F);
        Log.d("OTSU", "threshold: " + threshold);

        // now get the contour
        float[] colors = new float[boxSize * boxSize];
        Arrays.fill(colors, 255);

        // dependent from which side we came:
        switch (last){
            case LEFT:
                // now multiply by a unity vector
                Log.d("OTSU", "s1: " + dest.t().size() + ", s2: " + Mat.ones(1, boxSize, dest.type()).size());

                Mat yVec = new Mat();
                Mat m = Mat.ones(boxSize, 1, CV_32F);
                Core.gemm(dest.t(), m, 1/255., new Mat(), 0, yVec, 0);

                float[] yVecD = new float[yVec.rows()];
                yVec.get(0,0, yVecD);

                // translate those results into contour points
                Point endpoint = null;
                List<Point> points = new ArrayList<>();
                for (int i = boxSize - 1; i > -1; i--) {
                    Point p = new Point();
                    p.x = x0 + (i - boxSize);
                    p.y = y0 + ((center == Orientation.BOTTOM) ? boxSize - yVecD[i] : yVecD[i]);

                    // DEBUG //
                    colors[(int)(((center == Orientation.BOTTOM) ? boxSize - yVecD[i] : yVecD[i]) * boxSize + (i - boxSize))] = (float) 128;

                    // search for endpoint
                    if(yVecD[i] == 0)
                        endpoint = p;
                    else if(endpoint != null) {
                        endpoint = null;
                        points.add(p);
                    }
                    else
                        points.add(p);
                }

                // get next contact point and correct orientations
                if(endpoint == null)
                    endpoint = points.get(points.size() - 1);
                else
                {
                    // off to top or bottom
                    if(endpoint.y > y0)
                        last = Orientation.BOTTOM;
                    else
                        last = Orientation.TOP;
                    center = Orientation.RIGHT;
                }
                x0 = (int)endpoint.x;
                y0 = (int)endpoint.y;

                // add to that contour
                //contour.appendPoints(points);

                // DEBUG
                //dest.put(0,0, colors);

                break;
            case RIGHT:

                break;
            case TOP:
                break;
            case BOTTOM:
                break;
        }

        dest.convertTo(dest, oldType);
        return dest.t();
    }

}
