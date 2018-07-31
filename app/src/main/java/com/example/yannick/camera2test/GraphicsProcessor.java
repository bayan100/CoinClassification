package com.example.yannick.camera2test;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.yannick.camera2test.Sqlite.DatabaseManager;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opencv.core.CvType.CV_8UC3;

public class GraphicsProcessor
{
    protected GData data;
    protected Map<String, Object> additionalData;
    protected DatabaseManager dbm;
    protected Activity activity;

    public enum Status
    {
        UNDEFINED,
        PASSED,
        FAILED
    }

    public String task;
    public static Map<String, Float> parameter = new HashMap<String, Float>();

    public GraphicsProcessor(GData data, String task)
    {
        this.data = data;
        this.task = task;
        this.additionalData = new HashMap<>();
    }

    public GraphicsProcessor(String task)
    {
        this.task = task;
        this.additionalData = new HashMap<>();
    }

    public static void initParameters()
    {
        // GrayScale
        //parameter = new HashMap<String, Float>();
        parameter.put("EDthreshold1", 50f);
        parameter.put("EDthreshold2", 130f);

        // Resize
        parameter.put("Rwidth", 256f);
        parameter.put("Rheight", -1f);

        // Medianblur
        parameter.put("MBksize", 7f);

        // FindEllipse
        parameter.put("FEminSize", 0.08f);
        parameter.put("FEminArea", 40f);

        // Filter
        parameter.put("FstraightnessThreshold", 0.5f);
        parameter.put("FnBasePoints", 11f);
        parameter.put("FnumberOfRetainedContours", 5f);

        // ConvertToBooleanmap
        parameter.put("CtBwidth", 32f);
        parameter.put("CtBheight", -1f);
    }

    public void passData(GData data) {
        if(this.data == null)
            this.data = data;
        Log.d("TENSOR", "pass: bmp == 0: " + (this.getBitmap() == null));
    }

    public GData getData() {
        return data;
    }

    public void passAdditionalData(Map<String, Object> additionalData) {
        if(this.additionalData == null)
            this.additionalData = additionalData;
        else
            this.additionalData.putAll(additionalData);
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void addAdditionaData(String key, Object value){
        additionalData.put(key, value);
    }

    public void passDBM(DatabaseManager dbm) {
        this.dbm = dbm;
    }

    public void passActivity(Activity activity){
        this.activity = activity;
    }

    public Status execute()
    {
        // start timer
        long starttime = System.nanoTime();

        // choose the right method to execute
        Status status;
        switch (task)
        {
            case "EdgeDetection": status = edgeDetection(); break;

            case "GrayScale": status = convertGreyscale(); break;

            case "MedianBlur": status = medianblur(); break;

            case "FindContours": status = findContoures(); break;

            case "SplitContours": status = splitContours(); break;

            case "FilterContours": status = filterContours(); break;

            case "DrawContours": status = drawContours(); break;

            case "FindEllipse": status = findEllipse(); break;

            case "DrawEllipse": status = drawEllipse(); break;

            case "LocalOtsu": status = localOtsu(); break;

            case "ResizeImage": status = resizeImage(); break;

            case "ConvertToBitmap": status = convertToBitmap(); break;

            case "ConvertToBooleanmap": status = convertToBooleanmap();  break;

            case "DoNothing": status = Status.PASSED; break;

            default: return Status.FAILED;
        }

        // timer stop
        Log.d("TIMER", "Task " + task + " completed in: " + ((System.nanoTime() - starttime) / 1000000) + " ms");

        return status;
    }

    //// Methods /////
    private Status convertGreyscale()
    {
        if (data.type != GData.Type.MAT)
            return Status.FAILED;

        Mat dest = new Mat();
        Imgproc.cvtColor(data.getMat(), dest, Imgproc.COLOR_RGB2GRAY);

        data.setMat(dest);
        return Status.PASSED;
    }

    private Status edgeDetection()
    {
        if (data.type != GData.Type.MAT)
            return Status.FAILED;

        int threshold1 = Math.round(parameter.get("EDthreshold1"));
        int threshold2 = Math.round(parameter.get("EDthreshold2"));

        Mat im_canny = new Mat();
        Imgproc.Canny(data.getMat(), im_canny, threshold1, threshold2);

        data.setMat(im_canny);

        return Status.PASSED;
    }

    private Status resizeImage()
    {
        if (data.type != GData.Type.MAT)
            return Status.FAILED;

        Mat resized = new Mat();

        Mat material = data.getMat();

        float scale = material.height() / (float)material.width();
        float width = parameter.get("Rwidth") > 0 ? parameter.get("Rwidth") : parameter.get("Rheight") / scale;
        float height = parameter.get("Rheight") > 0 ? parameter.get("Rheight") : parameter.get("Rwidth") * scale;

        Imgproc.resize(material, resized, new Size(width, height),
                        0, 0, Imgproc.INTER_LINEAR);
        data.setMat(resized);

        return Status.PASSED;
    }

    private Status medianblur()
    {
        if(data.type == GData.Type.MAT)
        {
            Mat image = data.getMat();

            int ksize = Math.round(parameter.get("MBksize"));
            Imgproc.medianBlur(image, image, ksize);
            data.setMat(image);
            return Status.PASSED;
        }
        return Status.FAILED;
    }

    private Status findContoures()
    {
        if (data.type == GData.Type.MAT)
        {
            Mat source = data.getMat();
            Mat hirachy = new Mat();
            ArrayList<MatOfPoint> tempContours = new ArrayList<>();

            Imgproc.findContours(source, tempContours, hirachy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

            // Filter the contours to prevent unrealistic ellipses
            ArrayList<Contour> contours = Contour.create(tempContours);
            int minArea = Math.round(parameter.get("FEminArea"));

            // remove those who are too small to be part of the main ellipse
            for (int i = 0; i < contours.size(); i++) {
                if(contours.get(i).area < minArea) {
                    contours.remove(i);
                    i--;
                }
            }

            // sort by maximum area DESC order
            Collections.sort(contours);

            additionalData.put("contours", contours);
            return Status.PASSED;
        }
        return Status.FAILED;
    }

    private Status filterContours(){
        if(additionalData != null) {
            ArrayList<Contour> contours = (ArrayList<Contour>)additionalData.get("contours");
            Log.d("COTOUR", "clen: " + contours.size());

            int nBasePoints = Math.round(parameter.get("FnBasePoints"));
            int numberOfRetainedContours = Math.round(parameter.get("FnumberOfRetainedContours"));

            // calculate the area under the contour with accurate trapeze shape
            // and find the centerpoint of the contour
            for (int i = 0; i < contours.size(); i++) {
                Contour c = contours.get(i);
                c.calculateArea(nBasePoints);
                c.calculateCenterPoint();
                c.calculateClosure();
            }

            // resort the contours
            Collections.sort(contours);

            // average those, weighted by their area for the best few
            double xmean = 0, ymean = 0, totalarea = 0;
            for (int i = 0; i < contours.size() /*&& i < numberOfRetainedContours*/; i++) {
                Contour c = contours.get(i);
                xmean += c.center.x * c.area;
                ymean += c.center.y * c.area;
                totalarea += c.area;
            }
            // approximate coin center
            additionalData.put("approximateCenter", new Point(xmean / totalarea, ymean / totalarea));
            //additionalData.put("contours", new ArrayList<Contour>(contours.subList(0, numberOfRetainedContours)));

            return Status.PASSED;
        }

        return Status.FAILED;
    }

    private Status splitContours()
    {
        if(additionalData != null) {
            ArrayList<Contour> contours = (ArrayList<Contour>)additionalData.get("contours");
            Log.d("COTOUR", "clen: " + contours.size());
            int minArea = Math.round(parameter.get("FEminArea"));

            // try to split each contour
            ArrayList<Contour> tempContours = new ArrayList<>();
            for (int i = 0; i < contours.size(); i++) {
                ContourMap map = ContourMap.fromContour(contours.get(i).points);

                // get the split-points
                List<ContourNode> splitpoints = map.getSplitpoints();

                // remove the splitpoints from the map data
                map.removeSplitpoints2();

                // remove the old contour
                contours.remove(i);
                i--;

                // convert the map back to single contours
                List<List<Point>> points = map.convertToPoints();
                for (int j = 0; j < points.size(); j++) {
                    if (points.get(j).size() > 5) {
                        MatOfPoint mat = new MatOfPoint();
                        mat.fromList(points.get(j));
                        Contour c = new Contour(mat);

                        // prefilter contour
                        if (c.area >= minArea) {
                            //Log.d("POINTS", "area = " + c.area + "    -+ " + minArea);
                            tempContours.add(c);
                        }
                    }
                }
            }

            contours.addAll(tempContours);

            // resort the contours
            Collections.sort(contours);

            return Status.PASSED;
        }

        return Status.FAILED;
    }

    private Status drawContours()
    {
        if(data.type == GData.Type.MAT && additionalData != null)
        {
            ArrayList<Contour> contours = (ArrayList<Contour>)additionalData.get("contours");
            Mat target = data.getMat();

            Mat contoursMat = Mat.zeros(target.rows(), target.cols(), CV_8UC3);
            Point approximateCenter = (Point)additionalData.get("approximateCenter");
            if(approximateCenter != null)
                Imgproc.drawMarker(contoursMat, approximateCenter, new Scalar(255,255,255));
            Bitmap contoursBM = getBitmap(contoursMat);

            //for (int i =  contours.size() - 4; i < contours.size() - 3; i++) {
            for (int i = 0; i < contours.size(); i++) {
                if(i != 300)
                    contours.get(i).draw(contoursBM, Color.rgb((int)((contours.size() - i) * (255f / contours.size())), (int)(i * (255f / contours.size())), 0));
                    //contours.get(i).draw(contoursBM, Color.HSVToColor(new float[] {i * (255f / contours.size()), 255f, 255f}));
                else {
                    contours.get(i).draw(contoursBM, Color.rgb(255, 255, 255));
                }
                //contours.get(i).drawMultiColored(contoursBM);
            }

            /*int matWidth = contoursBM.getWidth();
            int matHeight = contoursBM.getHeight();
            int[] colors = new int[matWidth * matHeight];
            contoursBM.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);
            colors[contoursBM.getWidth() * (int)approximateCenter.y + (int)approximateCenter.x] = Color.rgb(255, 255, 255);
            contoursBM.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);*/

            /*ContourMap map = new ContourMap();
            map.insert(3,-1);
            map.insert(2,-2);
            map.insert(1,-2);
            map.insert(0,-1);
            map.insert(0,0);
            map.insert(0,1);
            map.insert(0,2);
            map.insert(0,3);
            map.insert(1,4);
            map.insert(2,4);
            map.insert(3,3);
            map.insert(3,2);
            map.insert(4,1);

            map.getSplitpoints();
            map.removeSplitpoints2();
            List<List<Point>> points = map.convertToPoints();
            List<Contour> newContours = new ArrayList<>(points.size());
            for (int j = 0; j < points.size(); j++) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < points.get(j).size(); k++) {
                    sb.append(points.get(j).get(k).toString() + ", ");

                }
                Log.d("CONTOUR", "||| " + sb.toString());
            }
/*
            //for (int i = contours.size() - 19; i < 100 && i < contours.size() - 18; i++) {
            for (int i = 0; i < 1 && i < contours.size(); i++) {
                Log.d("Num", "i = " + i);
                long starttime = System.nanoTime();
                ContourMap map = ContourMap.fromContour(contours.get(i).data);
                Log.d("timer", "From Contour = " + ((System.nanoTime() - starttime) / 1000000) + " ms");

                starttime = System.nanoTime();
                List<ContourNode> splitp = map.getSplitpoints();
                Log.d("timer", "Get Splitpoints = " + ((System.nanoTime() - starttime) / 1000000) + " ms");

                starttime = System.nanoTime();
                map.removeSplitpoints2();
                Log.d("timer", "Remove Splitpoints = " + ((System.nanoTime() - starttime) / 1000000) + " ms");

                starttime = System.nanoTime();
                List<List<Point>> points = map.convertToPoints();
                List<Contour> newContours = new ArrayList<>(points.size());
                for (int j = 5; j < 6 && j < points.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    for (int k = 0; k < points.get(j).size(); k++) {
                        sb.append(points.get(j).get(k).toString() + ", ");
                        if(k % 10 == 0)
                            sb.append("\n");
                    }
                    Log.d("CONTOUR", "#-#-#" + sb.toString());
                    if(points.get(j).size() > 5) {


                        MatOfPoint mat = new MatOfPoint();
                        mat.fromList(points.get(j));
                        newContours.add(new Contour(mat));
                        //newContours.get(newContours.size() - 1).draw(contoursBM, Color.rgb((int) ((contours.size() - i) * (255f / contours.size())), (int) (i * (255f / contours.size())), 0));
                        newContours.get(newContours.size() - 1).draw(contoursBM, Color.HSVToColor(new float[] {j * (255f / contours.size()), 255f, 255f}));
                        //newContours.get(newContours.size() - 1).drawMultiColored(contoursBM);
                    }
                }
                //map.draw(contoursBM, Color.rgb((int)((contours.size() - i) * (255f / contours.size())), (int)(i * (255f / contours.size())), 0));

                Log.d("timer",  "Covert to points = " + ((System.nanoTime() - starttime) / 1000000) + " ms");
                starttime = System.nanoTime();
                //Log.d("timer",  ((System.nanoTime() - starttime) / 1000000) + " ms");

                //Imgproc.polylines(contoursMat, mp,false, new Scalar((contours.size() - i) * (255 / contours.size()), i * (255 / contours.size()), 0));
                //contours.get(i).draw(contoursBM, Color.rgb((int)((contours.size() - i) * (255f / contours.size())), (int)(i * (255f / contours.size())), 0));
                //contours.get(i).drawMultiColored(contoursBM);

                //map.draw(contoursBM);

            }*/

            //data.setMat(contoursMat);
            data.setBitmap(contoursBM);
            return Status.PASSED;
        }
        return Status.FAILED;
    }

    private Status findEllipse()
    {
        if(additionalData != null)
        {
            ArrayList<Contour> contours = (ArrayList<Contour>)additionalData.get("contours");
            ArrayList<RotatedRect> ellipses = new ArrayList<>(contours.size());

            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint2f dst = new MatOfPoint2f();
                contours.get(i).data.convertTo(dst, CvType.CV_32FC2);

                ellipses.add(Imgproc.fitEllipse(dst));
            }

            // check if the first Ellipses have similar center (indicating one is the outer and the other the inner ring
            if(ellipses.size() > 0) {
                RotatedRect e0 = ellipses.get(0);
                Log.d("ELLIPSE", "e0: " + e0.toString());

                for (int i = 1; i < ellipses.size(); i++) {
                    RotatedRect e1 = ellipses.get(i);
                    Log.d("ELLIPSE", e1.toString());
                    if (Math.sqrt((e1.center.x - e0.center.x) * (e1.center.x - e0.center.x) + (e1.center.y - e0.center.y) * (e1.center.y - e0.center.y)) < 5) {
                        // search for an ellipse that is bigger by a good margin than the first
                        if (e0.size.width * e0.size.height < 0.9 * e1.size.width * e1.size.height) {
                            ellipses.set(0, e1);
                            ellipses.set(i, e0);
                            break;
                        }
                    }
                }
            }

            /*
            Collections.sort(ellipses, new Comparator<RotatedRect>() {
                @Override
                public int compare(RotatedRect o1, RotatedRect o2) {
                    return -1 * Double.compare(o1.size.width * o1.size.height, o2.size.width * o2.size.height);
                }
            });
*/
            // average the first ellipses with close parameters
           /* e0 = ellipses.get(0);
            Log.d("ELLIPSE", "e0: " + e0.toString());
            for (int i = 1; i < 3 && i < ellipses.size(); i++) {
                RotatedRect e = ellipses.get(i);
                Log.d("ELLIPSE", e.toString());
                if(Math.sqrt((e.center.x - e0.center.x) * (e.center.x - e0.center.x) + (e.center.y - e0.center.y) * (e.center.y - e0.center.y)) < 5
                        && Math.abs(e.size.width - e0.size.width) < 10
                        && Math.abs(e.size.height - e0.size.height) < 10
                        ){
                    // average
                    e0.center = new Point((e.center.x + e0.center.x ) / 2, (e.center.y + e0.center.y) / 2);
                    e0.size = new Size((e.size.width + e0.size.width) / 2, (e.size.height + e0.size.height) / 2);
                    //e0.angle = (e.angle + e0.angle) / 2;
                    Log.d("ELLIPSE", "average nr. " + i);
                }
            }*/

            additionalData.put("ellipses", ellipses);
            return Status.PASSED;
        }
        return Status.FAILED;
    }

    private Status drawEllipse()
    {
        if (data.type == GData.Type.MAT && additionalData != null)
        {
            Mat image = data.getMat();
            ArrayList<RotatedRect> ellipses = (ArrayList<RotatedRect>)additionalData.get("ellipses");
            for (int i = 0; i < 1 && i < ellipses.size(); i++) {
                Imgproc.ellipse(image, ellipses.get(i), new Scalar(255, 0, 0), 2);
            }
            //Imgproc.ellipse(image, ellipses.get(0), new Scalar(255, 0, 0), 2);

            data.setMat(image);

            return Status.PASSED;
        }
        return Status.FAILED;
    }

    private Status localOtsu(){
        Mat source = data.getMat();
        Contour c = ((ArrayList<Contour>)additionalData.get("contours")).get(0);
        Log.d("OTSU", "size: " + c.points.length);
        LocalOtsuProcessor processor = new LocalOtsuProcessor(source, ((ArrayList<Contour>)additionalData.get("contours")).get(0), 16);
        data.setMat(processor.run());
        ((ArrayList<Contour>)additionalData.get("contours")).set(0, processor.contour);

        return Status.PASSED;

        //return Status.FAILED;
    }

    private Status findCorners()
    {
        Mat source = data.getMat();
        Mat dest = Mat.zeros(source.rows(), source.cols(), CvType.CV_32FC1);
        Imgproc.cornerHarris(source, dest, 2, 3, 0.04);

        Mat tmp = new Mat(), norm = new Mat();
        Core.normalize(dest, tmp, 0, 255, Core.NORM_MINMAX);
        Core.convertScaleAbs(tmp, norm);

        for (int i = 0; i < tmp.cols(); i++) {
            for (int j = 0; j < tmp.rows(); j++) {
                double[] value = tmp.get(j, i);
                if(value[0] > 180) {
                    Log.d("CORNERS", i + ", " + j);
                    Imgproc.circle(norm, new Point(i, j), 5, new Scalar(255, 0, 0));
                }
            }
        }

        data.setMat(norm);

        return Status.PASSED;
    }

    private Status convertToBooleanmap()
    {
        int width = Math.round(parameter.get("CtBwidth"));
        int height = Math.round(parameter.get("CtBheight"));

        Boolean[][] map = null;

        if (data.type == GData.Type.MAT)
        {
            // get aspect ratio
            Mat material = data.getMat();
            float wth = material.height() / (float)material.width();

            // set the dependent length
            if (height < 0)
                height = (int)(width * wth);
            else
                width = (int)(height / wth);

            // init the 2D array
            map =  new Boolean[height][width];

            float wstepf = material.width() / (float)width;
            int wstep = Math.round(wstepf);
            float hstepf = material.height() / (float)height;
            int hstep = Math.round(hstepf);

            // iterate the material in steps of the size of a resulting pixel
            for (int j = 0; j * hstepf < material.height(); j++) {
                for (int i = 0; i * wstepf < material.width(); i++) {
                    int ii = Math.round(i * hstepf);
                    int ji = Math.round(j * wstepf);

                    boolean pixel = false;
                    for (int k = 0; k < hstep && k + ji < material.height(); k++)
                        for (int l = 0; l < wstep && l + ii < material.width(); l++) {
                            // read pixel
                            if (material.get(ji + k, ii + l)[0] > 120) {
                                pixel = true;
                                break;
                            }
                        }

                    map[j][i] = pixel;
                }
            }
        }
        else
        {
            // get aspect ratio
            Bitmap material = data.getBitmap();
            int matWidth = material.getWidth();
            int matHeight = material.getHeight();

            int[] colors = new int[matWidth * matHeight];
            material.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);

            float wth = matHeight / (float)matWidth;

            // set the dependent length
            if (height < 0)
                height = (int)(width * wth);
            else
                width = (int)(height / wth);

            // init the 2D array
            map =  new Boolean[height][width];

            float wstepf = matWidth / (float)width;
            int wstep = (wstepf < 0.5) ? 1 : Math.round(wstepf);
            float hstepf = matHeight / (float)height;
            int hstep = (hstepf < 0.5) ? 1 : Math.round(hstepf);

            // iterate the material in steps of the size of a resulting pixel
            for (int i = 0; i * hstepf < matHeight; i++) {
                for (int j = 0; j * wstepf < matWidth; j++) {
                    int ii = Math.round(i * hstepf);
                    int ji = Math.round(j * wstepf);

                    boolean pixel = false;
                    for (int k = 0; k < hstep && k + ii < matHeight; k++)
                        for (int l = 0; l < wstep && l + ji < matWidth; l++) {
                            // read pixel
                            if (colors[(ji + l) + matWidth * (ii + k)] > 0xFF000000) {
                                pixel = true;
                                break;
                            }
                        }

                    map[i][j] = pixel;
                }
            }
        }

        data.setBooleanmap(map);

        return Status.PASSED;
    }

    private Status convertToBitmap()
    {
        if(data.type == GData.Type.MAT)
        {
            data.setBitmap(getBitmap(data.getMat()));
            return Status.PASSED;
        }
        else if(data.type == GData.Type.BOOLEANMAP)
        {
            Boolean[][] map = data.getBooleanmap();
            int width = map[0].length;
            Log.d("bFB", "width: " + width);
            int height = map.length;

            int[] pixels = new int[width * height];

            for (int i = 0; i < height; i++)
                for (int j = 0; j < width; j++) {
                    if(map[i][j])
                        pixels[width * i + j] = 0xFFFFFFFF;
                    else
                        pixels[width * i + j] = 0xFF000000;
                }

            Bitmap result = Bitmap.createBitmap(map[0].length, map.length, Bitmap.Config.ARGB_8888);
            result.setPixels(pixels, 0, width, 0,0, width, height);

            data.setBitmap(result);

            return Status.PASSED;
        }
        return Status.FAILED;
    }

    public Bitmap getBitmap() {
        if (data.type == GData.Type.BITMAP)
            return data.getBitmap();
        else if (data.type == GData.Type.MAT)
            return getBitmap(data.getMat());
        else if(data.type == GData.Type.BOOLEANMAP) {
            convertToBitmap();
            return data.getBitmap();
        }
        return null;
    }

    private Mat getMat(Bitmap data)
    {
        Mat image = new Mat();
        Bitmap bmp32 = data.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, image);
        return image;
    }

    private Bitmap getBitmap(Mat image)
    {
        Bitmap bmp32 = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bmp32);
        return bmp32;
    }
}
