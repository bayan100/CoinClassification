package com.example.yannick.camera2test.Sqlite;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class MatSerializer {
    public static byte[] matToBytes(Mat mat){
        if(mat.isContinuous()){
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();
            int type = mat.type();

            byte[] bytes;

            if( type == CvType.CV_32S || type == CvType.CV_32SC2 || type == CvType.CV_32SC3 || type == CvType.CV_16S) {
                int[] data = new int[cols * rows * elemSize];
                mat.get(0, 0, data);
                ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE / 8 * data.length);
                for (int value : data)
                    buffer.putInt(value);
                bytes = buffer.array();
            }
            else if( type == CvType.CV_32F || type == CvType.CV_32FC2 || type == 53) {
                float[] data = new float[cols * rows * elemSize];
                mat.get(0, 0, data);
                ByteBuffer buffer = ByteBuffer.allocate(Float.SIZE / 8 * data.length);
                for (float value : data)
                    buffer.putFloat(value);
                bytes = buffer.array();
            }
            else if( type == CvType.CV_64F || type == CvType.CV_64FC2) {
                double[] data = new double[cols * rows * elemSize];
                mat.get(0, 0, data);
                ByteBuffer buffer = ByteBuffer.allocate(Double.SIZE / 8 * data.length);
                for (double value : data)
                    buffer.putDouble(value);
                bytes = buffer.array();
            }
            else if( type == CvType.CV_8U ) {
                byte[] data = new byte[cols * rows * elemSize];
                mat.get(0, 0, data);
                bytes = data;
            }
            else {
                throw new UnsupportedOperationException("unknown type");
            }

            // Description
            byte[] desc = new byte[12];
            fromInteger(desc, 0, type);
            fromInteger(desc, 4, rows);
            fromInteger(desc, 8, cols);

            // concatenate both arrays
            byte[] total = new byte[bytes.length + 12];
            System.arraycopy(desc, 0, total, 0, desc.length);
            System.arraycopy(bytes, 0, total, 12, bytes.length);
            return total;
        }
        return null;
    }

    public static Mat matFromBytes(byte[] bytes){
        int type = fromByte(bytes, 0);
        int rows = fromByte(bytes, 4);
        int cols = fromByte(bytes, 8);

        Mat mat = new Mat(rows, cols, type);
        mat.put(0,0, Arrays.copyOfRange(bytes, 12, bytes.length));
/*
        if( type == CvType.CV_32S || type == CvType.CV_32SC2 || type == CvType.CV_32SC3 || type == CvType.CV_16S) {
            int[] data = SerializationUtils.toIntArray(Base64.decodeBase64(dataString.getBytes()));
            mat.put(0, 0, data);
        }
        else if( type == CvType.CV_32F || type == CvType.CV_32FC2) {
            float[] data = SerializationUtils.toFloatArray(Base64.decodeBase64(dataString.getBytes()));
            mat.put(0, 0, data);
        }
        else if( type == CvType.CV_64F || type == CvType.CV_64FC2) {
            double[] data = SerializationUtils.toDoubleArray(Base64.decodeBase64(dataString.getBytes()));
            mat.put(0, 0, data);
        }
        else if( type == CvType.CV_8U ) {
            byte[] data = Base64.decodeBase64(dataString.getBytes());
            mat.put(0, 0, data);
        }
        else {

            throw new UnsupportedOperationException("unknown type");
        }*/
        return mat;
    }

    private static int fromByte(byte[] bytes, int start){
        return  (bytes[start    ]<<24)&0xff000000|
                (bytes[start + 1]<<16)&0x00ff0000|
                (bytes[start + 2]<< 8)&0x0000ff00|
                (bytes[start + 3]    )&0x000000ff;
    }

    private static void fromInteger(byte[] bytes, int start, int value) {
        bytes[start    ] = (byte)(value >>> 24);
        bytes[start + 1] = (byte)(value >>> 16);
        bytes[start + 2] = (byte)(value >>> 8);
        bytes[start + 3] = (byte)value;
    }
}
