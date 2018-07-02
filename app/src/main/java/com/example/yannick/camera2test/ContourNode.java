package com.example.yannick.camera2test;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ContourNode implements Comparable
{
    int x, y;
    ContourNode[] nodes; // 0 = top left, 1 = top middle, 2 = top right, 3 = middle left, 5 = middle right, 6 = bottom left, 7 = bottom middle, 8 = bottom right
    int size = 0;
    boolean splitNode, lesserSplitNode;

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

    ContourNode connectedRightToLeft(int yi){
        for (int i = -1; i < 2; i++) {
            ContourNode item = connectedNode(i, yi);
            if(item != null)
                return item;
        }
        return null;
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
    private List<List<int[]>> segments = null;
    private List<ContourNode> splitPoints = null;


    public void insert(int x, int y)
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

        // now iterate the segments and classify them
        // via automate
        for (int k = 1; k < segments.size(); k++) {
            int i = 0, j = 0;
            List<int[]> iSeg = segments.get(k - 1);
            List<int[]> jSeg = segments.get(k);
            int state = -1;

            while (i < iSeg.size() && j < jSeg.size()) {

                // case 1:
                if (iSeg.get(i)[1] + 1 < jSeg.get(j)[0]){
                    i++;
                    state = 1;
                }
                // case 2/5:
                else if(iSeg.get(i)[1] <= jSeg.get(j)[1]){

                    // test for merge
                    if(state == 2)
                        addSplitpixel(jSeg.get(j), k, result);

                    // test for split
                    if(state == 3)
                        addSplitpixel(iSeg.get(i), k - 1, result);

                    i++;
                    state = 2;
                }
                // case 3/6:
                else if(iSeg.get(i)[0] - 1 <= jSeg.get(j)[1]){

                    // test for merge
                    if(state == 2)
                        addSplitpixel(jSeg.get(j), k, result);

                    // test for split
                    if(state == 3)
                        addSplitpixel(iSeg.get(i), k - 1, result);

                    j++;
                    state = 3;
                }
                // case 4:
                else {
                    j++;
                    state = 4;
                }
            }
        }

        // save segments and splitpoints for later use
        this.segments = segments;
        this.splitPoints = result;
        return result;
    }


    /*List<ContourNode> getSplitpoints2()
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
                }

                // 2. case: new segment outside of the old (too small)
                else if (nSeg.get(j)[1] + 1 < oSeg.get(index)[0]) {
                    foundtype = 2;
                }

                // 3. case: new segment partially or completely inside old
                else if (nSeg.get(j)[1] + 1 >= oSeg.get(index)[0] && nSeg.get(j)[1] <= oSeg.get(index)[1]) {
                    // check if we found the split partner previously
                    if (foundtype == 3)
                        addSplitpixel(oSeg.get(index), i - 1, result);

                    foundtype = 3;
                }

                // 4. case: new segment partially outside the other side
                else if (nSeg.get(j)[0] > oSeg.get(index)[0] && nSeg.get(j)[1] > oSeg.get(index)[1]) {
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
                }

                // 2. case: old segment outside of new (too small)
                else if (oSeg.get(j)[1] + 1 < nSeg.get(index)[0]) {
                    foundtype = 2;
                }

                // 3. case: old segment partially or completely inside of new segment
                else if (oSeg.get(j)[1] + 1 >= nSeg.get(index)[0] && oSeg.get(j)[1] <= nSeg.get(index)[1]) {
                    if (foundtype == 3)
                        addSplitpixel(nSeg.get(index), i, result);

                    foundtype = 3;
                }

                // 4. case: old segment partially outside of new segment
                else if (oSeg.get(j)[0] <= nSeg.get(index)[1] + 1) {
                    if (foundtype == 3)
                        addSplitpixel(nSeg.get(index), i, result);

                    foundtype = 4;
                }
            }
        }

        // save segments and splitpoints for later use
        this.segments = segments;
        this.splitPoints = result;
        return result;
    }*/

    private void addSplitpixel(int[] seg, int i, List<ContourNode> result){
        // make sure the chosen pixel are connected to the right or left
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
                else
                    node.lesserSplitNode = true;
            }
    }

    void removeSplitpoints(){
        if(splitPoints == null)
            return;

        for (int j = 0; j < splitPoints.size(); j++) {
            ContourNode split = splitPoints.get(j);

            // iterate the neighbors
            for (int k = -1; k < 2; k++) {
                for (int l = -1; l < 2; l++) {
                    ContourNode item = split.connectedNode(k, l);
                    if (item != null) {
                        // cut the connections to other neighbors of the splitpoint
                        // including the splitpoint
                        for (int m = 0; m < 9; m++) {
                            if (item.nodes[m] != null) {
                                for (int n = 0; n < 9; n++) {
                                    if (item.nodes[m] == split.nodes[n] || item.nodes[m] == split) {
                                        item.nodes[m] = null;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // now remove the splitpoint itself
            splitLoop:
            for (int i = 0; i < data.size(); i++) {
                List<ContourNode> list = data.get(i);
                for (int k = 0; k < list.size(); k++) {
                    if(list.get(k) == split){
                        ContourNode node = list.get(k);
                        list.remove(k);

                        // if the list is empty now, remove it from the overall mix
                        if(list.size() < 1){
                            data.remove(i);
                            segments.remove(i);
                            break splitLoop;
                        }

                        // adjust the segments
                        List<int[]> seg = segments.get(i);
                        for (int l = 0; l < seg.size(); l++) {
                            int[] current = seg.get(l);

                            // find the segment that contains the splitpoint
                            if(current[0] <= node.x && current[1] >= node.x){
                                // check the edges, but make sure the node is not single
                                if(current[0] == current[1]){
                                    // single node, remove directly
                                    seg.remove(l);
                                }
                                else if(current[0] == node.x){
                                    current[0]++;
                                }
                                else if(current[1] == node.x){
                                    current[1]--;
                                }
                                else {
                                    // split the segment
                                    int[] newseg = new int[] {node.x + 1, current[1]};
                                    current[1] = node.x - 1;
                                    seg.add(l + 1, newseg);
                                }
                                break splitLoop;
                            }
                        }
                        break splitLoop;
                    }
                }
            }
        }
    }

    void removeSplitpoints2() {
        if (splitPoints == null)
            return;

        for (int i = 0; i < data.size(); i++) {
            List<ContourNode> list = data.get(i);
            for (int k = 0; k < list.size(); k++) {
                if (list.get(k).splitNode) {
                    ContourNode split = list.get(k);
                    for (int l = 0; l < 9; l++) {
                        ContourNode item = split.nodes[l];
                        if (item != null) {
                            // cut the connections to other neighbors of the splitpoint
                            // including the splitpoint
                            for (int m = 0; m < 9; m++) {
                                if (item.nodes[m] != null) {
                                    for (int n = 0; n < 9; n++) {
                                        if (item.nodes[m] == split.nodes[n] || item.nodes[m] == split) {
                                            item.nodes[m] = null;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    list.remove(k);
                    k--;

                    // adjust the segments
                    List<int[]> seg = segments.get(i);
                    for (int l = 0; l < seg.size(); l++) {
                        int[] current = seg.get(l);

                        // find the segment that contains the splitpoint
                        if(current[0] <= split.x && current[1] >= split.x){
                            // check the edges, but make sure the node is not single
                            if(current[0] == current[1]){
                                // single node, remove directly
                                seg.remove(l);
                            }
                            else if(current[0] == split.x){
                                current[0]++;
                            }
                            else if(current[1] == split.x){
                                current[1]--;
                            }
                            else {
                                // split the segment
                                int[] newseg = new int[] {split.x + 1, current[1]};
                                current[1] = split.x - 1;
                                seg.add(l + 1, newseg);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    List<List<Point>> convertToPoints(){
        List<List<Point>> result = new ArrayList<>();

        // maintain a list of the current lists we add to
        QueuedList current = new QueuedList();
        // init with the first segments
        int a = 0;
        for (int i = 0; i < segments.get(0).size(); i++) {
            current.add(i);
            a += addToCurrent(segments.get(0).get(i), a, data.get(0), current, i, false);
        }
        current.commit(result);

        // now iterate the segments and classify them
        for (int k = 1; k < segments.size(); k++) {
            int i = 0, j = 0;
            List<int[]> iSeg = segments.get(k - 1);
            List<int[]> jSeg = segments.get(k);
            int state = -1;
            List<ContourNode> segmentdata = data.get(k);
            int added = 0;

            Log.d("SPLIT", " k = " + k + ", current = " + current.size());
            Log.d("SPLIT", "     i: " + iSeg.size() + ", j: " + jSeg.size());

            // if this height is missing any segments, end all currently active contours
            if(jSeg.size() < 1){
                Log.d("SPLIT", "Continue");
                for (int l = 0; l < current.size(); l++) {
                    result.add(current.get(i));
                }
                current.clear();
                continue;
            }

            StringBuilder sb = new StringBuilder("  i = ");
            for (int l = 0; l < iSeg.size(); l++) {
                sb.append("[");
                sb.append(iSeg.get(l)[0]);
                sb.append(",");
                sb.append(iSeg.get(l)[1]);
                sb.append("], ");
            }
            sb.append("   j = ");
            for (int l = 0; l < jSeg.size(); l++) {
                sb.append("[");
                sb.append(jSeg.get(l)[0]);
                sb.append(",");
                sb.append(jSeg.get(l)[1]);
                sb.append("], ");
            }
            Log.d("SPLIT", sb.toString());

            while (i < iSeg.size() || j < jSeg.size()) {
                Log.d("SPLIT", "     i = " + i + ", j = " + j);
                // case 0:
                if(i >= iSeg.size()){
                    Log.d("SPLIT", "     state = 0.1");

                    // end of both segments
                    if(state == 2 && j + 1 >= jSeg.size())
                        break;

                    // insert the points
                    if(state == 2) {
                        current.add(j + 1);
                        added += addToCurrent(jSeg.get(j + 1), added, segmentdata, current, j + 1, false);
                    }
                    else {
                        current.add(j);
                        added += addToCurrent(jSeg.get(j), added, segmentdata, current, j, false);
                    }
                    j++;
                }
                else if(j >= jSeg.size()){
                    Log.d("SPLIT", "     state = 0.2");

                    // end of both segments
                    if(state == 3) {
                        if (i + 1 >= iSeg.size())
                            break;
                        else
                            current.remove(i + 1);
                    }
                    else
                        current.remove(i);

                    i++;
                }

                // case 1:
                else if (iSeg.get(i)[1] + 1 < jSeg.get(j)[0]){
                    Log.d("SPLIT", "     state = 1");

                    // remove the ended segment from current but only if it really is one
                    if(state != 3) {
                        //result.add(current.get(i));
                        current.remove(i);
                    }
                    i++;
                    state = 1;
                }
                // case 2/5:
                else if(iSeg.get(i)[1] <= jSeg.get(j)[1]){
                    Log.d("SPLIT", "     state = 2");

                    // test for merge
                    if(state == 2)
                        current.merge(i);

                    // test for split
                    else if(state == 3) {
                        current.split(j);
                        added += addToCurrent(jSeg.get(j), added, segmentdata, current, j, false);
                    }
                    else
                        // insert the points
                        added += addToCurrent(jSeg.get(j), added, segmentdata, current, j, false);

                    i++;
                    state = 2;
                }
                // case 3/6:
                else if(iSeg.get(i)[0] - 1 <= jSeg.get(j)[1]){
                    Log.d("SPLIT", "     state = 3");

                    // test for merge
                    if(state == 2)
                        current.merge(i);

                    // test for split
                    else if(state == 3) {
                        current.split(j);
                        added += addToCurrent(jSeg.get(j), added, segmentdata, current, j, true);
                    }
                    else
                        // insert the points
                        added += addToCurrent(jSeg.get(j), added, segmentdata, current, j, true);

                    j++;
                    state = 3;
                }
                // case 4:
                else {
                    Log.d("SPLIT", "     state = 4");

                    // add a new contour only if really a new start
                    if(state != 2) {
                        current.add(j);

                        // insert the points
                        added += addToCurrent(jSeg.get(j), added, segmentdata, current, j, true);
                    }
                    j++;
                    state = 4;
                }
            }
            // execute all
            current.commit(result);
        }

        // after iterating, merge all open ends who started together, end open ends
        current.finalCommit(result);

        return result;
    }

    private int addToCurrent(int[] segment, int added, List<ContourNode> segmentdata, QueuedList current, int index, boolean reversed){
        ArrayList<Point> tmp = new ArrayList<>(segment[1] - segment[0] + 1);
        for (int k = 0; k < segment[1] - segment[0] + 1; k++) {
            ContourNode item = segmentdata.get(k + added);
            if(!reversed)
                tmp.add(new Point(item.x, item.y));
            else
                tmp.add(0, new Point(item.x, item.y));
        }
        current.insert(index, tmp);
        return segment[1] - segment[0] + 1;
    }


    void log()
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).size(); j++) {
                ContourNode c = data.get(i).get(j);
                sb.append("(" + c.x + ", " + c.y + ", ");
                if(c.splitNode)
                    sb.append("S");
                else
                    sb.append(".");
                /*for (int k = -1; k < 2; k++) {
                    for (int l = -1; l < 2; l++) {
                        ContourNode n = c.connectedNode(k, l);
                        if(n != null){
                            sb.append("[" + n.x + "," + n.y + "],");
                        }
                    }
                }*/
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

        int count2 = 0;
        for (int i = 0; i < data.size(); i++) {
            List<ContourNode> nodes = data.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                count2++;

                if(nodes.get(j).splitNode)
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = 0xff00ff00;
                //else if(nodes.get(j).lesserSplitNode)
                //    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = 0xff0000ff;
                else
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = color;
            }
        }

        material.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);
    }

    void draw(Bitmap material, int color){
        int matWidth = material.getWidth();
        int matHeight = material.getHeight();

        int[] colors = new int[matWidth * matHeight];
        material.getPixels(colors, 0, matWidth, 0, 0, matWidth, matHeight);

        int count2 = 0;
        for (int i = 0; i < data.size(); i++) {
            List<ContourNode> nodes = data.get(i);
            for (int j = 0; j < nodes.size(); j++) {
                count2++;

                if(i == 99)
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = 0xffffffff;
                else if(nodes.get(j).splitNode)
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = 0xffffff00;
                    //else if(nodes.get(j).lesserSplitNode)
                    //    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = 0xff0000ff;
                else
                    colors[matWidth * nodes.get(j).y + nodes.get(j).x] = color;
            }
        }

        material.setPixels(colors, 0, matWidth, 0,0, matWidth, matHeight);
    }

    static ContourMap fromContour(Point[] points)
    {
        ContourMap map = new ContourMap();

        for (int i = 0; i < points.length; i++) {
            map.insert((int)points[i].x, (int)points[i].y);
        }

        return map;
    }
}
