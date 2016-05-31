package app.android.hifispectrum.ui.anim;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import app.android.hifispectrum.proc.FFT;
import app.android.hifispectrum.proc.HsvUtil;
import app.android.hifispectrum.proc.MathUtil;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class BubbleAnimation implements IAnimation {

    private final FFT _fft;
    private final String _description;
    private final Object _lock;
    private final HsvUtil _hsvUtil;
    private final Random _random;
    private final boolean _isGray;
    private final boolean _isVolume;
    private final boolean _isRectangle;

    private boolean _isInitialized;


    private Paint _paint;
    private Bitmap _bmp;
    private int _width;
    private int _height;

    public BubbleAnimation(String description, HsvUtil hsvUtil, FFT fft, boolean isGray, boolean isVolume, boolean isRectangle) {
        _lock = new Object();
        _description = description;
        _fft = fft;
        _hsvUtil = hsvUtil;
        _random = new Random();

        _isGray = isGray;
        _isVolume = isVolume;
        _isRectangle = isRectangle;

        _paint = new Paint();
        _paint.setStrokeWidth(3);

        if(!_isRectangle){
            _paint.setStyle(Paint.Style.STROKE);
        }
    }

    @Override
    public void Init(int width, int height) {
        synchronized (_lock) {

            _width = width;
            _height = height;

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

            float min = Math.min(_width, _height) / 5f;

            Canvas bmpCanvas = new Canvas(_bmp);
            bmpCanvas.drawColor(Color.argb(0x120, 0, 0, 0));
            float ratio = Math.abs((float) volume / Short.MAX_VALUE);

            float greenOffset = 0.333f;

            List<Integer> freqs = MainFrequency(audio);
            for(int freq : freqs) {

                int x = _random.nextInt(_width);
                int y = _random.nextInt(_height);

                float normalFreq = (float) freq / (_fft.getLength() / 2f);
                float uiFreq = min - (normalFreq * min);

                float unit = 0;
                if(_isVolume) {
                    unit = ratio + greenOffset;
                } else {
                    unit = normalFreq;
                }

                int color = 0;
                if(_isGray) {
                    color = _hsvUtil.HsvGray(unit);
                } else {
                    color = _hsvUtil.Hsv(unit);
                }
                _paint.setColor(color);

                if(_isRectangle) {
                    bmpCanvas.drawRect(x, y, x + uiFreq, y + uiFreq, _paint);
                } else {
                    bmpCanvas.drawCircle(x, y, uiFreq, _paint);
                }
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
            if(i < offset) {
                mag[i] = 0;
                continue;
            }

            if(mag[i] > z) {
                moreThanZ.add(i);
            }
        }

        return moreThanZ;
    }
}
