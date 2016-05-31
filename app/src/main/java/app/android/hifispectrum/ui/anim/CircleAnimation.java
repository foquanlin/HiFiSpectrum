package app.android.hifispectrum.ui.anim;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import app.android.hifispectrum.proc.FFT;
import app.android.hifispectrum.proc.HsvUtil;
import app.android.hifispectrum.proc.MathUtil;

/**
 * Created by Bartosz on 26/05/2015.
 */
public class CircleAnimation implements IAnimation {

    private final FFT _fft;
    private final String _description;
    private final Object _lock;
    private final HsvUtil _hsvUtil;
    private final boolean _isGray;
    private final boolean _isVariableSpeed;

    private boolean _isInitialized;
    private int _initSequence;

    private Paint _paint;
    private Bitmap _bmp;
    private int _width;
    private int _height;

    private int _iterator;

    public CircleAnimation(String description, HsvUtil hsvUtil, FFT fft, boolean isGray, boolean isVariableSpeed) {
        _lock = new Object();
        _description = description;
        _fft = fft;
        _hsvUtil = hsvUtil;

        _isGray = isGray;
        _isVariableSpeed = isVariableSpeed;

        _paint = new Paint();
        _paint.setStrokeWidth(3);
        _paint.setColor(Color.WHITE);

        _paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void Init(int width, int height) {
        synchronized (_lock) {

            _width = width;
            _height = height;

            _initSequence = 0;
            _isInitialized = true;
            _bmp = Bitmap.createBitmap(_width, _height, Bitmap.Config.ARGB_8888);
        }
    }

    @Override
    public void Draw(Canvas canvas, short volume, Iterable<Short> audio) {
        synchronized (_lock) {
            if(!_isInitialized) {
                return;
            }

            Canvas bmpCanvas = new Canvas(_bmp);

            if(_initSequence < 10) {
                _initSequence++;
                bmpCanvas.drawColor(Color.argb(0xff, 0, 0, 0));
                return;
            }

            float min = Math.min(_width, _height) / 8f;

            bmpCanvas.drawColor(Color.argb(0x01, 0, 0, 0));
            float ratio = Math.abs((float) volume / Short.MAX_VALUE);

            float greenOffset = 0.333f;

            List<Integer> freqs = MainFrequency(audio);
            int last = 0;
            for(int freq : freqs) {


                float x = (float)_width / 2f;
                float y = (float)_height / 2f;

                float normalFreq = (float) freq / (_fft.getLength() / 2f);
                float freqOffset = 3*min*normalFreq;

                RectF rect = new RectF(
                        x-min - freqOffset,
                        y-min - freqOffset,
                        x+min + freqOffset,
                        y+min + freqOffset);

                float from = _iterator % 360;

                _paint.setStrokeWidth(2f + ratio * 60f);

                if(_isGray) {
                    _paint.setColor(Color.WHITE);
                } else {
                    _paint.setColor(_hsvUtil.Hsv(normalFreq + greenOffset));
                }

                bmpCanvas.drawArc(rect, from, 7, false, _paint);
                last = freq;
            }

            if(_isVariableSpeed) {
                _iterator += last;
            } else {
                _iterator += 2;
            }


            canvas.drawBitmap(_bmp, 0, 0, null);
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

    private List<Integer> MainFrequency(Iterable<Short> audio) {

        int offset = _fft.getLength() / 10;
        double[] x = MathUtil.ToDouble(audio);
        double[] y = MathUtil.Zeros(x.length);
        double[] mag = MathUtil.Zeros(_fft.getLength() / 2);

        _fft.transform(x, y);

        List<Integer> moreThanZ = new ArrayList<>();

        for(int i=0;i<mag.length;i++) {
            mag[i] = MathUtil.Abs(x[i], y[i]);
        }

        double z = MathUtil.Max(mag) / 20f;

        for(int i=0;i<mag.length;i++) {
            if(mag[i] > z) {
                moreThanZ.add(i);
            }
        }

        return moreThanZ;
    }
}
