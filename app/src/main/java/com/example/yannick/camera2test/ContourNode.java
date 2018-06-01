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
    ContourNode[] nodes; // 0 = top left, 1 = top middle, 2 = top right, 3 = middle left, 5 = middle right, 6 = bottom left, 7 = bottom middle, 8 = bottom right
    int size = 0;
    boolean splitNode;

    ContourNode(int x, int y){
        this.x = x;
        this.y = y;

        //nodes = new ArrayList<>();
        nodes = new ContourNode[9];
    }

    boolean connectedIn(int xi, int yi){
        int index = (yi + 1) * 3 + xi + 1;
        return nodes[index] != null;
    }

    ContourNode connectedNode(int xi, int yi){
        int index = (yi + 1) * 3 + xi + 1;
        return nodes[index];
    }

    void addNeighbor(ContourNode item){
        int index = (item.y - y + 1) * 3 + item.x - x + 1;
        nodes[index] = item;
        size++;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        if(o.getClass() == ContourNode.class && ((ContourNode)o).y == y && ((ContourNode)o).x == x)
            return 0;
        else if(o.getClass() == Point.class && ((Point)o).y == y && ((Point)o).x == x)
            return 0;
        else
            return -1;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && compareTo(obj) == 0;
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
        }
        else {
            // binary search in y
            int a = 0, b = data.size();
            int i = (b - a) / 2, oldi = -1;
            while (true) {
                ContourNode c = data.get(i).get(0);

                if (c.y == y) {
                    List<ContourNode> list = data.get(i);

                    // check if node does not already exist
                    int[] xIndex = getIndex(x, list);
                    if (xIndex[0] == 0) {
                        // add the node
                        ContourNode node = new ContourNode(x, y);
                        list.add(xIndex[1], node);

                        // accumulate its neighbors
                        addNeighbours(node, i);
                        return;
                    }
                    return;
                } else if (c.y < y)
                    a = i;
                else
                    b = i;

                oldi = i;
                i = (b - a) / 2 + a;

                // When reached a new y-spot
                if (i == oldi) {
                    // new List with single Element
                    List<ContourNode> item = new ArrayList<>();
                    ContourNode node = new ContourNode(x, y);
                    item.add(node);
                    if(i == 0 && data.get(0).get(0).y > y)
                        i = -1;
                    data.add(i + 1, item);

                    // accumulate its neighbors
                    addNeighbours(node, i + 1);
                    return;
                }
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
                            item.addNeighbor(c);
                            c.addNeighbor(item);
                        }
                    }
                }
            }
        }
    }

    private int[] getIndex(int x, List<ContourNode> list) {
        // binary search in y
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
            if (i == oldi) {
                // check if inserting in front of the first element
                if (i == 0 && list.get(0).x > x)
                    return new int[]{0, 0};

                // no element with this particular x-Koordinate
                return new int[]{0, i + 1};
            }
        }
    }

    List<ContourNode> getSplitpoints()
    {
        List<ContourNode> result = new ArrayList<>();

        Log.d("SEGMENTS", "datasize: " + data.size());

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

                    // start new segment
                    if(j + 1 < list.size())
                        current = new int[] {list.get(j + 1).x, 0};
                }
            }
        }

        // now iterate the segments and count them
        for (int i = 1; i < segments.size(); i++) {
            List<int[]> oSeg = segments.get(i - 1);
            List<int[]> nSeg = segments.get(i);

            // seek for the split. The new segments both must at least touch the old segment
            int index = 0;
            int foundtype = 0;
            for (int j = 0; j < nSeg.size() && index < oSeg.size(); j++) {

                // 1. case: segment cutoff (indicates end of a line, no split)
                //          try to resume with another old segment
                if (nSeg.get(j)[0] > oSeg.get(index)[1] + 1) {
                    index++;
                    j--;
                    foundtype = 1;
                    continue;
                }

                // 2. case: new segment outside of the old (too small)
                if (nSeg.get(j)[1] + 1 < oSeg.get(index)[0]) {
                    foundtype = 2;
                    continue;
                }

                // 3. case: new segment partially or completely inside old
                if (nSeg.get(j)[1] + 1 >= oSeg.get(index)[0] && nSeg.get(j)[1] <= oSeg.get(index)[1]) {
                    // check if we found the split partner previously
                    if (foundtype == 3)
                        addSplitpixel(oSeg.get(index), i - 1, result);

                    foundtype = 3;
                    continue;
                }

                // 4. case: new segment partially outside the other side
                if (nSeg.get(j)[0] > oSeg.get(index)[0] && nSeg.get(j)[1] > oSeg.get(index)[1]) {
                    // check if we found the split partner previously
                    if (foundtype == 3)
                        addSplitpixel(oSeg.get(index), i - 1, result);

                    foundtype = 4;
                }
            }

            index = 0;
            foundtype = 0;
            for (int j = 0; j < oSeg.size() && index < nSeg.size(); j++) {

                // 1. case: segment start (indicates start of a line, no split)
                //          try to resume with another old segment
                if (oSeg.get(j)[0] > nSeg.get(index)[1] + 1) {
                    index++;
                    j--;
                    foundtype = 1;
                    continue;
                }

                // 2. case: old segment outside of new (too small)
                if (oSeg.get(j)[1] + 1 < nSeg.get(index)[0]) {
                    foundtype = 2;
                    continue;
                }

                // 3. case: old segment partially or completely inside of new segment
                if (oSeg.get(j)[1] + 1 >= nSeg.get(index)[0] && oSeg.get(j)[1] <= nSeg.get(index)[1]) {
                    if (foundtype == 3)
                        addSplitpixel(nSeg.get(index), i, result);

                    foundtype = 3;
                    continue;
                }

                // 4. case: old segment partially outside of new segment
                if (oSeg.get(j)[0] <= nSeg.get(index)[1] + 1) {
                    if (foundtype == 3)
                        addSplitpixel(nSeg.get(index), i, result);

                    foundtype = 4;
                }
            }
        }

        return result;
    }

    private void addSplitpixel(int[] seg, int i, List<ContourNode> result){
        // make sure the chosen pixel are connected to the right
        boolean connected = false;
        for (int k = seg[0]; k < seg[1] + 1; k++) {
            int ind = getIndex(k, data.get(i))[1];
            ContourNode node = data.get(i).get(ind);
            if(node.connectedIn(-1, 1) ||
                    node.connectedIn(0, 1) ||
                    node.connectedIn(1, 1)){
                connected = true;
                break;
            }
        }
        if(connected)
            for (int k = seg[0]; k < seg[1] + 1; k++) {
                // mark the nodes with the right x-value as splitnodes
                int ind = getIndex(k, data.get(i))[1];
                ContourNode node = data.get(i).get(ind);

                // only mark the true connector
                if(node.size > 2) {
                    node.splitNode = true;
                    result.add(data.get(i).get(ind));
                }
            }
    }

    void convertToPoints(ContourNode node, int dirBias, List<Point> result){

        // iterate the connected nodes, starting up, down, then the 3 in bias direction, last the remaining 3
        for (int i = 1; i < 3; i++) {
            for (int j = 1; j < 3; j++) {
                int index = (i % 2) * 3 + (j % 2 - 1) * dirBias + 1;
                if(node.nodes[index] != null){
                    // invalidate backwards direction
                    node.nodes[index].nodes[(2 + index * 5) % 12] = null;
                    // add node as point to the new contour and continue in chosen direction
                    result.add(new Point(node.x, node.y));
                    convertToPoints(node.nodes[index], dirBias, result);
                    // invalidate backwards for both partners
                    node.nodes[index] = null;
                }
            }
        }
/*
        // check up and down your own row first
        if(node.nodes[1] != null){
            node.nodes[1].nodes[7] = null;
            result.add(new Point(node.x, node.y));
            convertToPoints(node.nodes[1], dirBias, result);
            node.nodes[1] = null;
        }else if(node.nodes[7] != null){
            node.nodes[7].nodes[1] = null;
            result.add(new Point(node.x, node.y));
            convertToPoints(node.nodes[7], dirBias, result);
            node.nodes[7] = null;
        }

        // if bias check the 3 nodes in that direction
        if(dirBias != 0){
            for (int i = -1; i < 2; i++) {
                int index = (i + 1) * 3 + dirBias + 1;
                if(node.nodes[index] != null){
                    node.nodes[index].nodes[index - 2 * dirBias] = null;
                    result.add(new Point(node.x, node.y));
                    convertToPoints(node.nodes[index], dirBias, result);
                    node.nodes[index] = null;
                }
            }
        }
        // if not or no more nodes in that direction check all directions and find bias
*/

    }

    void log()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                ContourNode c = data.get(i).get(j);
                sb.append("(" + c.x + ", " + c.y + ", ");
                for (int k = -1; k < 2; k++) {
                    for (int l = -1; l < 2; l++) {
                        ContourNode n = c.connectedNode(k, l);
                        if(n != null){
                            sb.append("[" + n.x + "," + n.y + "],");
                        }
                    }
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
