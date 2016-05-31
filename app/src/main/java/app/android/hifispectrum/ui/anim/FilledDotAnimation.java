package app.android.hifispectrum.ui.anim;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Objects;

import app.android.hifispectrum.proc.HsvUtil;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class FilledDotAnimation implements IAnimation {

    private final Object _lock;

    private final boolean _isGray;
    private final int _dotSize;
    private final String _description;
    private Bitmap _bmp;
    private boolean _isInitialized;

    private final HsvUtil _hsvUtil;
    private Paint _paint;

    private int _i;
    private int _width;
    private int _height;

    public FilledDotAnimation(String description, HsvUtil hsvUtil, boolean isGray, int dotSize) {
        _description = description;
        _hsvUtil = hsvUtil;
        _isGray = isGray;
        _dotSize = dotSize;
        
        _paint = new Paint();
        _paint.setStrokeWidth(dotSize);

        _lock = new Object();
        _isInitialized = false;
    }

    @Override
    public void Init(int width, int height) {
        synchronized (_lock) {
            _isInitialized = true;
            _width = width;
            _height = height;

            _i = 0;
            _isInitialized = true;
            _bmp = Bitmap.createBitmap(_width, _height, Bitmap.Config.ARGB_8888);
        }
    }

    @Override
    public void Draw(Canvas canvas, short volume, Iterable<Short> audio) {
        synchronized (_lock) {
            if (!_isInitialized) {
                return;
            }

            canvas.drawColor(Color.BLACK);

            if (_i % 5 == 0) {
                float ratio = (float) volume / Short.MAX_VALUE;
                int color = _hsvUtil.Hsv(ratio);
                _paint.setColor(color);
                _i = 1;
            }

            Canvas bmpCanvas = new Canvas(_bmp);
            bmpCanvas.drawColor(Color.argb(0x120, 0, 0, 0));

            int stroke = (int)_paint.getStrokeWidth();
            int i = 0;
            for (short item : audio) {

                if(i%(stroke+1) != 0) {
                    i++;
                    continue;
                }

                if (i > _width) {
                    break;
                }

                int color = 0;

                int x = i + stroke/2;
                i++;
                float y = ((float) item / (Short.MAX_VALUE)) * _height;


                DrawBoxes(bmpCanvas, x, y, _height, _paint);
            }

            canvas.drawBitmap(_bmp, 0, 0, null);
            _i++;
        }
    }

    private void DrawBoxes(Canvas bmpCanvas, float x, float y, float height, Paint paint) {
        float boxSize = paint.getStrokeWidth();
        int boxes = (int)Math.ceil(y / boxSize);
        int sign = boxes > 0 ? 1 : -1;

        float offset = 0;
        float greenOffset = 0.3333f;
        for(int i=0;i<Math.abs(boxes);i++) {
            int color = 0;
            if (_isGray) {
                color = Color.WHITE;
            } else {
                color = _hsvUtil.Hsv(Math.abs(y)/_height*2 + greenOffset);
            }
            _paint.setColor(color);

            bmpCanvas.drawPoint(x, _height/2f + offset, _paint);
            offset = offset + sign * ( boxSize+1 );
        }
    }

    @Override
    public void Release() {
        synchronized (_lock) {
            _isInitialized = false;
            _bmp.recycle();
        }
    }

    @Override
    public String Description() {
        return _description;
    }
}
