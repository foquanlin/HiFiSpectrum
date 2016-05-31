package app.android.hifispectrum.ui.anim;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import app.android.hifispectrum.proc.FFT;
import app.android.hifispectrum.proc.HsvUtil;
import app.android.hifispectrum.proc.MathUtil;

/**
 * Created by Bartosz on 25/05/2015.
 */
public class FftAnimation implements IAnimation {

    private final String _description;
    private Object _lock;

    private final boolean _isGray;
    private int _width;
    private int _height;
    private boolean _isInitialized;

    private HsvUtil _hsvUtil;
    private FFT _fft;

    private float[] _fftBuffer;

    private Paint _paint;
    private int _interpolation;

    private Bitmap _bmp;

    public FftAnimation(String description, HsvUtil hsvUtil, FFT fft, boolean isGray) {
        _description = description;
        _lock = new Object();
        _hsvUtil = hsvUtil;
        _fft = fft;
        _isGray = isGray;

        _paint = new Paint();
    }

    @Override
    public void Init(int width, int height) {
        synchronized (_lock) {
            _width = width;
            _height = height;

            int halfFftLength = _fft.getLength() / 2;
            double interpolation = (double) _width / halfFftLength;
            interpolation = Math.round(interpolation);
            _interpolation = interpolation < 1 ? 1 : (int) interpolation;

            _fftBuffer = new float[halfFftLength];

            float stroke = _interpolation;
            if (_interpolation > 1) {
                stroke -= 1;
            }

            _paint.setStrokeWidth(stroke);

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

            UpdateFftBuffer(audio);

            float blueOffset = 0.666f;
            float[] line = _fftBuffer;
            Canvas bmpCanvas = new Canvas(_bmp);
            bmpCanvas.drawColor(Color.argb(0x120, 0, 0, 0));

            float interpolationOffset = _interpolation-_interpolation/2f;

            for (int i = 0; i < line.length; i++) {

                int color = 0;
                if (_isGray) {
                    color = Color.WHITE;
                } else {
                    color = _hsvUtil.Hsv((float) i / line.length);
                }
                _paint.setColor(color);

                float y = line[i] * _height;
                float x = i * _interpolation + interpolationOffset;
                DrawBoxes(bmpCanvas, x, y, _height - interpolationOffset, _paint);
            }

            canvas.drawBitmap(_bmp, 0, 0, null);
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

    private void DrawBoxes(Canvas bmpCanvas, float x, float y, float height, Paint paint) {
        float boxSize = paint.getStrokeWidth();
        int boxes = (int)Math.ceil(y / boxSize);

        float offset = height;
        for(int i=0;i<boxes;i++) {
            bmpCanvas.drawPoint(x, offset, _paint);
            offset -= boxSize+1;
        }
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
            _fftBuffer[i] = (float)mag[i];
        }

    }
}
