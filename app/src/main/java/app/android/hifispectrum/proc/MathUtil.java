package app.android.hifispectrum.proc;

import java.util.ArrayList;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class MathUtil {

    public static double[] ToDouble(Iterable<Short> audio) {
        ArrayList<Double> items = new ArrayList<Double>();

        for(Short item : audio) {
            double doubleItem = (float)item / Short.MAX_VALUE;
            items.add(doubleItem);
        }

        double[] doubleArray = new double[items.size()];
        for (int i = 0; i < items.size(); i++) {
            doubleArray[i] = items.get(i).doubleValue();
        }

        return doubleArray;
    }

    public static double[] Zeros(int length) {
        return new double[length];
    }

    public static double Abs(double x, double y) {
        double abs = Math.sqrt(x*x + y*y);
        return abs;
    }

    public static double Max(double data[]) {
        double max = Double.MIN_VALUE;

        for(int i=0;i<data.length;i++) {
            if(max < data[i]) {
                max = data[i];
            }
        }

        return max;
    }

    public static double Min(double data[]) {
        double min = Double.MAX_VALUE;

        for(int i=0;i<data.length;i++) {
            if(min > data[i]) {
                min = data[i];
            }
        }

        return min;
    }

    public static double[] Scale(double[] data) {
        double max = MathUtil.Max(data);
        double min = MathUtil.Min(data);
        double range = max - min;

        double[] scaled = new double[data.length];
        for(int i=0;i<data.length;i++) {
            scaled[i] = (data[i] - min) / range;
        }

        return scaled;
    }
}
