package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class EllipseProcessor extends GraphicsProcessor {

    private Integer[][] pixels;
    private List<Integer> xPixel = new ArrayList<>();
    private List<Integer> yPixel = new ArrayList<>();

    private int npixels;

    public ArrayList<Ellipse> sortedResults;

    public EllipseProcessor(String task)
    {
        super(task);

        // initialize some parameters
        parameter = new HashMap<>();

        parameter.put("minEllipseWidth", 6f);
        parameter.put("minEllipseHeight", 6f);
        parameter.put("voteThreshold", 0.3f);
        parameter.put("drawEllipseColor", (float)(0xFFFF0000));
        parameter.put("scaleEllipse", 0f);
    }

    @Override
    public Status execute() {

        // start Timer
        long starttime = System.nanoTime();

        // switch task
        switch (task) {
            case "FindEllipse":
                // test the GData-Type
                if (data.type != GData.Type.BOOLEANMAP)
                    return Status.FAILED;

                // initialize
                initPixelArray();


                // timer stop
                Log.d("TIMER", "init completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");

                runDetection2();
                break;

            case "DrawEllipse":
                drawEllipse();
                break;
        }

        // timer stop
        Log.d("TIMER", "Task " + task + " completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");

        return Status.PASSED;
    }

    private void initPixelArray()
    {

        // iterate the Booleanmap to find all white edge pixel
        Boolean[][] map = data.getBooleanmap();
        for (int i = 0; i < map.length; i++)
            for (int j = 0; j < map[i].length; j++)
                if(map[i][j]) {
                    xPixel.add(j);
                    yPixel.add(i);
                }

        // convert Lists into Array
        pixels = new Integer[2][xPixel.size()];
        xPixel.toArray(pixels[0]);
        yPixel.toArray(pixels[1]);
        //npixels = pixels[0].length;
        npixels = xPixel.size();
    }

    private void runDetection()
    {
        // start Timer
        long starttime = System.nanoTime();

        // gather parameter
        int minEllipseWidth = Math.round(parameter.get("minEllipseWidth"));
        int minEllipseHeight = Math.round(parameter.get("minEllipseHeight"));
        int maxEllipseHeight = data.getBooleanmap().length;
        int matWidth = data.getBooleanmap()[0].length;
        Log.d("ELLIPSE", "width: " + data.getBooleanmap()[0].length + ", height: " + maxEllipseHeight);
        int voteThreshold = Math.round(npixels * parameter.get("voteThreshold"));
        Log.d("ELLIPSE", "voteThreshold: " + voteThreshold);

        Log.d("TIMER", "init2 completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");
        starttime = System.nanoTime();

        ArrayList<Ellipse> possibleResults = new ArrayList<>();

        // DEBUG
        int loopcount = 0;
        int calccount = 0;

        // iterate every pixel
        for (int i = 0; i < npixels; i++) {
            // for every other pixel
            for (int j = i; j < npixels; j++) {
                // if the distance is greater than the required least distance
                if(len(i, j) >= minEllipseWidth)
                {
                    // calculate the ellipse parameter
                    // center
                    int x0 = (xPixel.get(i) + xPixel.get(j)) / 2;
                    int y0 = (yPixel.get(i) + yPixel.get(j)) / 2;

                    // length major axis
                    double a = len(i, j) / 2;

                    // orientation

                    // create the accumulator array to vote on the minor axis
                    int[] accvalue = new int[maxEllipseHeight];

                    // a second array to save a least one pixel-index per minor axis value
                    // later we can recalculate the ellipse-parameters from the highest vote
                    int[] accindex = new int[maxEllipseHeight];

                    // for every third pixel
                    for (int k = 0; k < npixels; k++) {
                        loopcount++;

                        // if the distance is greater than the required least distance
                        // and check that the chosen pixel is between xi and xj so not out of bounds
                        if(len(k, x0, y0) >= minEllipseHeight &&
                            ((xPixel.get(k) - xPixel.get(i) >= 0 && xPixel.get(k) - xPixel.get(j) <= 0) ||
                             (xPixel.get(k) - xPixel.get(i) <= 0 && xPixel.get(k) - xPixel.get(j) >= 0)))
                        {
                            // length of the minor axis
                            int b = (int)Math.sqrt(
                                    ((yPixel.get(k) - y0) * (yPixel.get(k) - y0) * a * a) /
                                    (a * a - (xPixel.get(k) - x0) * (xPixel.get(k) - x0)));


                            calccount++;

                            // in case the ellipse parameter exceeds the height of the image or
                            // a < b, don't bother keeping it
                            if (b >= accvalue.length || b < minEllipseHeight)
                                continue;

                            // vote on the parameter
                            accvalue[b]++;

                            // save an index
                            accindex[b] = k;
                        }
                    }

                    // count the vote
                    int winner = maxVotedIndex(accvalue);

                    // check if Ellipse passed the vote threshold
                    if (accvalue[winner] > voteThreshold) {
                        // then put it into the pool of possible results
                        Ellipse item = Ellipse.from3Points(
                                xPixel.get(i), yPixel.get(i),
                                xPixel.get(j), yPixel.get(j),
                                xPixel.get(accindex[winner]), yPixel.get(accindex[winner]));
                        item.votes = accvalue[winner];
                        item.originalWidth = matWidth;
                        item.originalHeight = maxEllipseHeight;
                        appendToResults(item, possibleResults);

                        // remove the starting pixels
                        xPixel.remove(i);
                        yPixel.remove(i);
                        xPixel.remove(j - (i < j ? 1 : 0));
                        yPixel.remove(j - (i < j ? 1 : 0));

                        //Log.d("ELLIPSE", "FOUND ----------------- i: " + i + ", j: "+ j +", npixels: " + (npixels - 1));

                        // adjust the loop parameter
                        i--;
                        npixels -= 2;
                        break;
                    }
                }
            }
        }

        Log.d("TIMER", "loop completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");

        Log.d("ELLIPSE", "Loop: " + loopcount + ", Calc: " + calccount);

        // Sort by votes and thus likelihood
        Collections.sort(possibleResults);
        sortedResults = possibleResults;
        additionalData.put("ellipses", sortedResults);

        for (int i = 0; i < possibleResults.size(); i++) {
            Log.d("ELLIPSE", possibleResults.get(i).toString());
        }

        // DEBUG
        if(possibleResults.size() > 0) {
            Ellipse e = possibleResults.get(0);
            Boolean[][] map = data.getBooleanmap();
            map[e.y][e.x] = true;
            data.setBooleanmap(map);

            Log.d("ELLIPSE", possibleResults.get(0).toString());

            Log.d("ELLIPSE", "maxvotes: " + possibleResults.get(0).votes);
        }
    }

    private void runDetection2()
    {
        // start Timer
        long starttime = System.nanoTime();

        // gather parameter
        int minEllipseWidth = Math.round(parameter.get("minEllipseWidth"));
        int minEllipseHeight = Math.round(parameter.get("minEllipseHeight"));
        int maxEllipseHeight = data.getBooleanmap().length;
        int matWidth = data.getBooleanmap()[0].length;
        Log.d("ELLIPSE", "width: " + data.getBooleanmap()[0].length + ", height: " + maxEllipseHeight);
        int voteThreshold = Math.round(npixels * parameter.get("voteThreshold"));
        Log.d("ELLIPSE", "voteThreshold: " + voteThreshold);

        Log.d("TIMER", "init2 completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");
        starttime = System.nanoTime();

        ArrayList<Ellipse> possibleResults = new ArrayList<>();

        // DEBUG
        int loopcount = 0;
        int calccount = 0;

        // iterate every pixel
        for (int i = 0; i < npixels; i++) {
            // for every other pixel
            for (int j = i; j < npixels; j++) {
                // if the distance is greater than the required least distance
                if(len2(i, j) >= minEllipseWidth)
                {
                    // calculate the ellipse parameter
                    // center
                    int x0 = (pixels[0][i] + pixels[0][j]) / 2;
                    int y0 = (pixels[1][i] + pixels[1][j]) / 2;

                    // length major axis
                    double a = len2(i, j) / 2;

                    // orientation

                    // create the accumulator array to vote on the minor axis
                    int[] accvalue = new int[maxEllipseHeight];

                    // a second array to save a least one pixel-index per minor axis value
                    // later we can recalculate the ellipse-parameters from the highest vote
                    int[] accindex = new int[maxEllipseHeight];

                    // for every third pixel
                    for (int k = 0; k < npixels; k++) {
                        loopcount++;

                        // if the distance is greater than the required least distance
                        // and check that the chosen pixel is between xi and xj so not out of bounds
                        if(len2(k, x0, y0) >= minEllipseHeight &&
                                ((pixels[0][k] - pixels[0][i] >= 0 && pixels[0][k] - pixels[0][j] <= 0) ||
                                        (pixels[0][k] - pixels[0][i] <= 0 && pixels[0][k] - pixels[0][j] >= 0)))
                        {
                            // calculate missing parameter
                           /* // cos(tau)
                            double d = len(k, x0, y0);

                            // depending on which side of x0 xk lies, calculate the length to the nearest ellipse pixel
                            double f = (pixels[0][k] - x0 > 0) ? len(j,k) : len(i, k);

                            double h = (a*a + d*d - f*f) / (2 * a);
                            double costau = h / d;

                            // sin(tau)
                            double sintau = Math.sqrt(1 - h*h / (d*d));

                            Log.d("ELLIPSE", "xi(" + pixels[0][i] + ", " + pixels[1][i] + ") xj("
                                    + pixels[0][j] + ", " + pixels[1][j] + ")" +
                                    ") xk(" + pixels[0][k] + ", " + pixels[1][k] + ")" +
                                    ") x0(" + x0 + ", " + y0 + ")"
                                    + ", a: " + a + ", d: " + d + ", f: " + f + ", h: "+ h + ", cost: " + costau + ", sint: " + sintau);

                            // length minor axis squared
                            int b = (int)Math.sqrt((a*a*d*d*sintau*sintau) / (a*a - d*d*costau*costau));
                           */

                            // length of the minor axis
                            int b = (int)Math.sqrt(
                                    ((pixels[1][k] - y0) * (pixels[1][k] - y0) * a * a) /
                                            (a * a - (pixels[0][k] - x0) * (pixels[0][k] - x0)));


                            calccount++;

                            /*Log.d("ELLIPSE", "xi(" + pixels[0][i] + ", " + pixels[1][i] +
                                    ") xj(" + pixels[0][j] + ", " + pixels[1][j] +
                                    ") xk(" + pixels[0][k] + ", " + pixels[1][k] +
                                    ") x0(" + x0 + ", " + y0 + ")"
                                    + ", a: " + a + ", b: " + b);*/

                            // in case the ellipse parameter exceeds the height of the image or
                            // a < b, don't bother keeping it
                            if (b >= accvalue.length || b < minEllipseHeight)
                                continue;

                            // vote on the parameter
                            accvalue[b]++;

                            // save an index
                            accindex[b] = k;
                        }
                    }

                    // count the vote
                    int winner = maxVotedIndex(accvalue);

                    // check if Ellipse passed the vote threshold
                    if (accvalue[winner] > voteThreshold) {
                        // then put it into the pool of possible results
                        Ellipse item = Ellipse.from3Points(
                                pixels[0][i], pixels[1][i],
                                pixels[0][j], pixels[1][j],
                                pixels[0][accindex[winner]], pixels[1][accindex[winner]]);
                        item.votes = accvalue[winner];
                        item.originalWidth = matWidth;
                        item.originalHeight = maxEllipseHeight;

                        appendToResults(item, possibleResults);
                    }
                }
            }
        }

        Log.d("TIMER", "loop completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");

        Log.d("ELLIPSE", "Loop: " + loopcount + ", Calc: " + calccount);

        // Sort by votes and thus likelihood
        Collections.sort(possibleResults);
        sortedResults = possibleResults;
        additionalData.put("ellipses", sortedResults);

        for (int i = 0; i < possibleResults.size(); i++) {
            Log.d("ELLIPSE", possibleResults.get(i).toString());
        }

        // DEBUG
        if(possibleResults.size() > 0) {
            Ellipse e = possibleResults.get(0);
            Boolean[][] map = data.getBooleanmap();
            map[e.y][e.x] = true;
            data.setBooleanmap(map);

            Log.d("ELLIPSE", possibleResults.get(0).toString());

            Log.d("ELLIPSE", "maxvotes: " + possibleResults.get(0).votes);
        }
    }

    // helper functions //
    private double len(int index1, int index2)
    {
        return Math.sqrt((xPixel.get(index1) - xPixel.get(index2)) * (xPixel.get(index1) - xPixel.get(index2)) +
                         (yPixel.get(index1) - yPixel.get(index2)) * (yPixel.get(index1) - yPixel.get(index2)));
    }

    private double len(int index, int x0, int y0)
    {
        return Math.sqrt((xPixel.get(index) - x0) * (xPixel.get(index) - x0) +
                         (yPixel.get(index) - y0) * (yPixel.get(index) - y0));
    }


    private double len2(int index1, int index2)
    {
        return Math.sqrt((pixels[0][index1] - pixels[0][index2]) * (pixels[0][index1] - pixels[0][index2]) +
                (pixels[1][index1] - pixels[1][index2]) * (pixels[1][index1] - pixels[1][index2]));
    }

    private double len2(int index, int x0, int y0)
    {
        return Math.sqrt((pixels[0][index] - x0) * (pixels[0][index] - x0) +
                (pixels[1][index] - y0) * (pixels[1][index] - y0));
    }

    private int maxVotedIndex(int[] values)
    {
        int index = -1, value = -1;
        for (int i = 0; i < values.length; i++) {
            if(values[i] > value)
            {
                value = values[i];
                index = i;
            }
        }
        return index;
    }

    private void appendToResults(Ellipse item, ArrayList<Ellipse> results){
        for (int i = 0; i < results.size(); i++) {
            if(results.get(i).equals(item)) {
                results.get(i).votes += item.votes;
                return;
            }
        }
        results.add(item);
    }

    private Status drawEllipse()
    {
        if(data.type == GData.Type.BITMAP && sortedResults != null && sortedResults.size() > 0)
        {
            // take the average over the highest voted 3 Ellipses
            Ellipse e = new Ellipse(0,0,0,0);
            for (int i = 0; i < 3 && i < sortedResults.size(); i++) {
                Ellipse item = sortedResults.get(i);
                e.x += item.x; e.y += item.y; e.a += item.a; e.b += item.b;
            }
            int c = (sortedResults.size() >= 3) ? 3 : sortedResults.size();
            e.x /= c; e.y /= c; e.a /= c; e.b /= c;
            e.originalWidth = sortedResults.get(0).originalWidth;
            e.originalHeight = sortedResults.get(0).originalHeight;

            int ellipseColor = Math.round(parameter.get("drawEllipseColor"));

            // get the colors of the Bitmap
            Bitmap material = data.getBitmap();
            int matWidth = material.getWidth();
            int matHeight = material.getHeight();


            // scale the ellipse if wanted
            boolean scale = false;
            if(parameter.get("scaleEllipse") > 0) {
                e.scale(matWidth, matHeight);
                scale = true;
            }


            int[] colors = new int[matWidth * matHeight];
            material.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);

            // draw the ellipse
            // along the width of the ellipse
            for (int i = 0; i < Math.round(e.a); i++) {
                // find the correct y-pixel
                int y = (int)Math.round(Math.sqrt((1f - i*i / ((float)e.a*e.a))*e.b*e.b));
                //Log.d("EP", "i: "+ i + ", a: " + e.a + ", b: " + e.b + ", y: " + y);

                // color the ellipse pixels
                for (int j = -1; j < 2; j += 2) {
                    for (int k = -1; k < 2; k += 2) {
                        if(matWidth * (e.y + j * y) + e.x + k * i < colors.length && matWidth * (e.y + j * y) + e.x + k * i >= 0) {
                            colors[matWidth * (e.y + j * y) + e.x + k * i] = ellipseColor;
                            // bigger lines
                            if(scale) {
                                colors[matWidth * (e.y + j * (y - 1)) + e.x + k * i] = ellipseColor;
                                colors[matWidth * (e.y + j * (y + 1)) + e.x + k * i] = ellipseColor;
                                colors[matWidth * (e.y + j * y) + e.x + k * (i - 1)] = ellipseColor;
                                colors[matWidth * (e.y + j * y) + e.x + k * (i + 1)] = ellipseColor;
                            }
                        }
                    }
                }
                /*if(matWidth * (e.y + y) + e.x + i < colors.length)
                    colors[matWidth * (e.y + y) + e.x + i] = ellipseColor;
                if(matWidth * (e.y - y) + e.x + i < colors.length && matWidth * (e.y - y) + e.x - i >= 0)
                    colors[matWidth * (e.y - y) + e.x + i] = ellipseColor;
                if(matWidth * (e.y + y) + e.x - i < colors.length && matWidth * (e.y + y) + e.x - i >= 0)
                    colors[matWidth * (e.y + y) + e.x - i] = ellipseColor;
                if(matWidth * (e.y - y) + e.x - i < colors.length && matWidth * (e.y - y) + e.x - i >= 0)
                    colors[matWidth * (e.y - y) + e.x - i] = ellipseColor;*/
            }

            // put colors back into the Bitmap
            material.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);

            return Status.PASSED;
        }
        return Status.FAILED;
    }
}
