package app.android.hifispectrum.ui.anim;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;

import app.android.hifispectrum.proc.FFT;
import app.android.hifispectrum.proc.HsvUtil;
import app.android.hifispectrum.proc.MathUtil;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class SpectrumAnimation implements IAnimation {

    private final boolean _isGray;
    private final Paint _blackPaint;
    private final String _description;
    private int _width;
    private int _height;
    private boolean _isInitialized;
    private int _initSequence;

    private Object _lock;

    private HsvUtil _hsvUtil;
    private FFT _fft;

    private float[][] _fftBuffer;
    private int _fftIndex;
    private int _index;

    private Paint _paint;
    private int _interpolation;

    private Bitmap _bmp;

    public SpectrumAnimation(String description, HsvUtil hsvUtil, FFT fft, boolean isGray) {
        _description = description;
        _hsvUtil = hsvUtil;
        _fft = fft;
        _isGray = isGray;

        _paint = new Paint();
        _blackPaint = new Paint();
        _blackPaint.setColor(Color.BLACK);
        _lock = new Object();
    }

    @Override
    public void Init(int width, int height) {
        synchronized (_lock) {

            _width = width;
            _height = height;

            int halfFftLength = _fft.getLength() / 2;
            double interpolation = (double) _height / halfFftLength;
            interpolation = Math.round(interpolation);
            _interpolation = interpolation < 1 ? 1 : (int) interpolation;


            _fftBuffer = new float[width / _interpolation][halfFftLength];
            for (int i = 0; i < _fftBuffer.length; i++) {
                _fftBuffer[i] = new float[halfFftLength];
            }

            float stroke = _interpolation;
            if (_interpolation > 1) {
                stroke -= 1;
            }

            _paint.setStrokeWidth(stroke);
            _blackPaint.setStrokeWidth(stroke);

            _initSequence = 0;
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

            if (_initSequence < 5) {
                _initSequence++;
                canvas.drawColor(Color.BLACK);
                return;
            }

            UpdateFftBuffer(audio);

            float blueOffset = 0.666f;
            float[] line = _fftBuffer[_index];
            int x = _index * _interpolation;
            Canvas bmpCanvas = new Canvas(_bmp);

            bmpCanvas.drawBitmap(_bmp, -_interpolation, 0, null);
            float interpolationOffset = _interpolation-_interpolation/2f;
            for (int y = 0; y < line.length; y++) {


                int color = 0;
                if (_isGray) {
                    color = _hsvUtil.HsvGray(line[y]);
                } else {
                    color = _hsvUtil.Hsv((float) volume / Short.MAX_VALUE);
                    color = Color.argb((int) (255 * line[y]), Color.red(color), Color.green(color), Color.blue(color));
                }
                _paint.setColor(color);

                bmpCanvas.drawPoint(_fftBuffer.length * _interpolation -1, _height - y * _interpolation - interpolationOffset, _blackPaint);
                bmpCanvas.drawPoint(_fftBuffer.length * _interpolation -1, _height - y * _interpolation - interpolationOffset, _paint);
            }

            canvas.drawBitmap(_bmp, 0, 0, null);

            _index = (_index + 1) % _fftBuffer.length;
        }
    }

    @Override
    public void Release() {
        synchronized (_lock) {
            _isInitialized = false;
            _bmp.recycle();
            _fftBuffer = null;
        }
    }

    @Override
    public String Description() {
        return _description;
    }

    private void UpdateFftBuffer(Iterable<Short> audio) {

        int offset = 0;
        double[] x = MathUtil.ToDouble(audio);
        double[] y = MathUtil.Zeros(x.length);
        double[] mag = MathUtil.Zeros(_fft.getLength() / 2);

        _fft.transform(x, y);

        for(int i=0;i<mag.length;i++) {
            if(i < offset) {
                mag[i] = 0;
                continue;
            }

            mag[i] = MathUtil.Abs(x[i], y[i]);
        }

        mag = MathUtil.Scale(mag);

        for(int i=0;i<mag.length;i++) {
            _fftBuffer[_fftIndex][i] = (float)mag[i];
            continue;
        }

        _fftIndex = (_fftIndex+1) % _fftBuffer.length;
    }

}
