package app.android.hifispectrum.proc;

import android.graphics.Color;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class HsvUtil {

    private float[] _hsv;

    public HsvUtil() {
        _hsv = new float[3];
        int col = Color.RED;
        Color.RGBToHSV(Color.red(col), Color.green(col), Color.blue(col), _hsv);
    }

    public int Hsv(float percent) {

        float hue = (float) (percent * 360.0);
        _hsv[0] = fmod(hue, 360);
        _hsv[1] = 1;
        _hsv[2] = 1;
        int col = Color.HSVToColor(_hsv);

        return col;
    }

    public int HsvGray(float percent) {

        _hsv[0] = 0;
        _hsv[1] = 0;
        _hsv[2] = fmod(percent*1f, 1f);
        int col = Color.HSVToColor(_hsv);

        return col;
    }

    private float fmod(float value, float max) {
        while(value > max) {
            value -= max;
        }

        return value;
    }
}
