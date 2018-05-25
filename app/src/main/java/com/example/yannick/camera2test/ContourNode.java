package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

class ContourNode implements Comparable
{
    int x, y;
    List<ContourNode> nodes;
    boolean splitNode;

    public ContourNode(int x, int y){
        this.x = x;
        this.y = y;

        nodes = new ArrayList<>();
    }

    boolean connected(ContourNode node) {
        return nodes.contains(node);
    }

    boolean connectedIn(int xi, int yi){
        for (int i = 0; i < nodes.size(); i++) {
            if(nodes.get(i).x == x + xi && nodes.get(i).y == y + yi)
                return true;
        }
        return false;
    }


    @Override
    public int compareTo(@NonNull Object o) {
        if(((ContourNode)o).y == y && ((ContourNode)o).x == x)
            return 0;
        else
            return -1;
    }
}

class ContourMap
{
    // [y][x]
    List<List<ContourNode>> data = new ArrayList<>();

    void insert(int x, int y)
    {
        // insert first element
        if(data.size() == 0){
            List<ContourNode> item = new ArrayList<>();
            item.add(new ContourNode(x, y));
            data.add(item);
            return;
        }

        // binary search in y
        int a = 0, b = data.size() - 1;
        int i = (b - a) / 2;
        while (true){
            ContourNode c = data.get(i).get(0);

            if(c.y == y)
            {
                List<ContourNode> list = data.get(i);

                // check if node does not already exist
                int[] xIndex = getIndex(x, list);
                if(xIndex[0] == 0)
                {
                    // add the node
                    ContourNode node = new ContourNode(x, y);
                    list.add(xIndex[1], node);

                    // accumulate its neighbors
                    addNeighbours(node, i);
                    return;
                }
                return;
            }
            else if(c.y < y)
                a = i;
            else
                b = i;

            i = (b - a) / 2 + a;

            // When reached a new y-spot
            if(a == i || b == i)
            {
                // new List with single Element
                List<ContourNode> item = new ArrayList<>();
                ContourNode node = new ContourNode(x, y);
                item.add(node);
                if(i == 0 && y < c.y)
                    i = -1;
                data.add(i + 1, item);

                // accumulate its neighbors
                addNeighbours(node, i + 1);
                return;
            }
        }
    }

    private void addNeighbours(ContourNode item, int yIndex)
    {
        // iterate 1 from left to right
        for (int i = (yIndex > 0) ? -1 : 0; i < 2 && i + yIndex < data.size(); i++) {
            // search for neighbors
            for (int j = -1; j < 2; j++) {
                if(i != 0 || j != 0) {
                    List<ContourNode> list = data.get(yIndex + i);

                    // check if gaps in between make the spacing larger than 1
                    if(Math.abs(list.get(0).y - item.y) < 2) {
                        // search for node with the requested x-value
                        int[] ci = getIndex(item.x + j, list);
                        if (ci[0] == 1) {
                            // linking on both sides
                            ContourNode c = list.get(ci[1]);
                            item.nodes.add(c);
                            c.nodes.add(item);
                        }
                    }
                }
            }
        }
    }

    private int[] getIndex(int x, List<ContourNode> list)
    {
        // binary search in y
        if(list.size() > 1) {
            int a = 0, b = list.size();
            int i = (b - a) / 2, oldi = -1;
            while (true) {
                ContourNode c = list.get(i);

                if (c.x == x)
                    return new int[]{1, i};
                else if (c.x < x)
                    a = i;
                else
                    b = i;

                oldi = i;
                i = (b - a) / 2 + a;

                // When reached a new x-spot
                if (i == oldi)
                    // no element with this particular x-Koordinate
                    return new int[]{0, i + 1};
            }
        }
        if(list.get(0).x > x)
            return new int[]{0, 0};
        else if(list.get(0).x < x)
            return new int[]{0, 1};
        return new int[]{1,0};
    }

    List<ContourNode> getSplitpoints2()
    {
        List<ContourNode> result = new ArrayList<>();

        // iterate every contournode
        for (int i = 0; i < data.size(); i++) {
            List<ContourNode> list = data.get(i);
            for (int j = 0; j < list.size(); j++) {
                // if it has at least 3 neighbors
                List<ContourNode> nodes = list.get(j).nodes;
                if(nodes.size() > 2){
                    Log.d("SPLIT", "Reached");
                    // check if at least 3 of those nodes are remote
                    int remoteCount = 0;
                    for (int k = 0; k < nodes.size(); k++)
                        for (int l = k; l < nodes.size(); l++) {
                            if(!nodes.get(k).connected(nodes.get(l)))
                                remoteCount++;
                        }

                    // remotepairs?
                    if(remoteCount > 2) {

                        // now check if the remot

                        // add to output
                        list.get(j).splitNode = true;
                        result.add(list.get(j));
                    }
                }
            }
        }

        return result;
    }

    List<ContourNode> getSplitpoints()
    {
        List<ContourNode> result = new ArrayList<>();

        Log.d("SEGMENTS", "datasize: " + data.size());

        StringBuilder sb2 = new StringBuilder();

        // scan the field top to bottom (scanline in y)
        List<List<int[]>> segments = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            segments.add(new ArrayList<int[]>());

            // count the connected strips of ContourNodes
            List<ContourNode> list = data.get(i);
            int[] current = new int[] {list.get(0).x, 0};
            for (int j = 0; j < list.size(); j++) {
                // check each node if connected to another node next in line
                if(!list.get(j).connectedIn(1, 0)){
                    // break in the line, stop current and add to stripes
                    current[1] = list.get(j).x;
                    segments.get(i).add(current);

                    sb2.append("[" + j + "] " + current[0] + ", " + current[1]);

                    // start new segment
                    if(j + 1 < list.size())
                        current = new int[] {list.get(j + 1).x, 0};
                }
            }
        }

        Log.d("SEGMENTS", sb2.toString());

        // now iterate the segments and count them
        int current = segments.get(0).size();
        StringBuilder sb = new StringBuilder();
        sb.append("c: " + segments.size() + ", ");
        for (int i = 1; i < segments.size(); i++) {
            sb.append(" " + current);

            // when we see current changing
            if(current != segments.get(i).size()){
                List<int[]> oSeg = segments.get(i - 1);
                List<int[]> nSeg = segments.get(i);

                // new split
                if(current < nSeg.size()){
                    sb.append(": ");

                    // seek for the split. The new segments both must at least touch the old segment
                    int index = 0;
                    int foundtype = 0;
                    for (int j = 0; j < nSeg.size() && index < oSeg.size(); j++) {

                        sb.append(" (" + j + "," + index + ")[" + nSeg.size() + "," + oSeg.size() + "] ");
                        sb.append("<" + nSeg.get(j)[0] + ", " + nSeg.get(j)[1] + "> <" + oSeg.get(index)[0] + ", " + oSeg.get(index)[1] + ">");

                        // 1. case: segment cutoff (indicates end of a line, no split)
                        //          try to resume with another old segment
                        if(nSeg.get(j)[0] > oSeg.get(index)[1] + 1){
                            index++;
                            j--;
                            foundtype = 1;
                            continue;
                        }

                        // 2. case: new segment outside of the old (too small)
                        if(nSeg.get(j)[1] + 1 < oSeg.get(index)[0]){
                            foundtype = 2;
                            continue;
                        }

                        // 3. case: new segment partially or completely inside old
                        if(nSeg.get(j)[1] + 1 >= oSeg.get(index)[0] && nSeg.get(j)[1] < oSeg.get(index)[1]) {
                            // check if we found the split partner previously
                            if(foundtype == 3){
                                for (int k = oSeg.get(index)[0]; k < oSeg.get(index)[1] + 1; k++) {
                                    data.get(i).get(k).splitNode = true;
                                    result.add(data.get(i).get(k));
                                }
                            }

                            foundtype = 3;
                            continue;
                        }

                        // 4. case: new segment partially outside the other side
                        if(nSeg.get(j)[0] > oSeg.get(index)[0] && nSeg.get(j)[1] > oSeg.get(index)[1]){
                            // check if we found the split partner previously
                            if(foundtype == 3){
                                for (int k = oSeg.get(index)[0]; k < oSeg.get(index)[1] + 1; k++) {
                                    data.get(i).get(k).splitNode = true;
                                    result.add(data.get(i).get(k));
                                }
                            }

                            foundtype = 4;
                        }
                    }
                }
            }

            current = segments.get(i).size();
        }
        sb.append(" -> " + result.size());

        Log.d("STRIPES", sb.toString());

        return result;
    }

    void log()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                ContourNode c = data.get(i).get(j);
                sb.append("(" + c.x + ", " + c.y + ", ");
                for (int k = 0; k < c.nodes.size(); k++) {
                    sb.append("[" + c.nodes.get(k).x + "," + c.nodes.get(k).y + "],");
                }
                sb.append("), ");
            }
            sb.append("\n");
        }
        Log.d("SPLITC", sb.toString());
    }

    void draw(Bitmap material){
        int matWidth = material.getWidth();
        int matHeight = material.getHeight();

        int[] colors = new int[matWidth * matHeight];
        material.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);

        int color = 0xffff0000;

        for (int i = 0; i < data.size(); i++) {
            List<ContourNode> nodes = data.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                if(!nodes.get(j).splitNode)
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = color;
                else
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = 0xff00ff00;
            }
        }

        material.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);
    }

    static ContourMap fromContour(MatOfPoint data)
    {
        ContourMap map = new ContourMap();
        Point[] points = data.toArray();
        for (int i = 0; i < points.length; i++) {
            map.insert((int)points[i].x, (int)points[i].y);
        }
        return map;
    }
}
