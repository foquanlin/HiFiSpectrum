package app.android.hifispectrum.ui.anim;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import app.android.hifispectrum.proc.HsvUtil;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class DotAnimation implements IAnimation {

    private final String _description;
    private boolean _isInitialized;

    private final HsvUtil _hsvUtil;
    private Paint _paint;

    private int _i;
    private int _width;
    private int _height;

    public DotAnimation(String description, HsvUtil hsvUtil) {
        _description = description;
        _hsvUtil = hsvUtil;

        _paint = new Paint();
        _paint.setStrokeWidth(10);

        _isInitialized = false;
    }

    @Override
    public void Init(int width, int height) {
        _isInitialized = true;
        _width = width;
        _height = height;

        _i = 0;
        _isInitialized = true;
    }

    @Override
    public void Draw(Canvas canvas, short volume, Iterable<Short> audio) {

        if(!_isInitialized){
            return;
        }

        canvas.drawColor(Color.BLACK);

        if(_i % 5 == 0) {
            float ratio = (float)volume / Short.MAX_VALUE;
            int color = _hsvUtil.Hsv(ratio);
            _paint.setColor(color);
            _i = 1;
        }

        int i=0;
        for(short item : audio) {

            if (i > _width) {
                break;
            }

            int x = i++;
            float y = ((float)item / (Short.MAX_VALUE))*_height + _height / 2.0f;

            if(!_isInitialized){
                canvas.drawColor(Color.BLACK);
                return;
            }

            canvas.drawPoint(x, y, _paint);
        }

        _i++;
    }

    @Override
    public void Release() {
        _isInitialized = false;
    }

    @Override
    public String Description() {
        return _description;
    }
}
