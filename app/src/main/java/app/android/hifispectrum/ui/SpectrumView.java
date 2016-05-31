package app.android.hifispectrum.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.List;

import app.android.hifispectrum.hw.IAudioProvider;
import app.android.hifispectrum.hw.IAudioProviderCallback;
import app.android.hifispectrum.proc.FFT;
import app.android.hifispectrum.proc.HsvUtil;
import app.android.hifispectrum.proc.PushQueue;
import app.android.hifispectrum.ui.anim.*;

/**
 * Created by Bartosz on 18/05/2015.
 */
public class SpectrumView implements IAudioProviderCallback {

    private final SurfaceView _surface;
    private final SurfaceHolder _holder;
    private final DrawThread _drawThread;
    private final Context _context;
    private final Tracker _tracker;

    private PushQueue<Short> _pushQueue;

    private volatile boolean _isCanvasReady;

    private final int _fftSize = 4096;
    private final FFT _fft;

    private double[] _fftReal;
    private double[] _fftImg;

    private int _width;
    private int _height;

    private boolean _isDisposed;
    private int _animation = 0;

    private List<IAnimation> _animations;
    private IAudioProvider audioProvider;

    public SpectrumView(SurfaceView surface, Context context, Tracker tracker) {

        _context = context;
        _surface = surface;
        _holder = _surface.getHolder();
        _isCanvasReady = false;
        _tracker = tracker;

        _fft = new FFT(_fftSize);

        _fftReal = new double[_fftSize];
        _fftImg = new double[_fftSize];

        HsvUtil hsvUtil = new HsvUtil();

        _animations = new ArrayList<>();
        _animations.add(new FilledDotAnimation("Vol 8", hsvUtil, true, 8));
        _animations.add(new FilledDotAnimation("Vol 16", hsvUtil, true, 16));
        _animations.add(new FilledDotAnimation("Vol 8 col", hsvUtil, false, 8));
        _animations.add(new FilledDotAnimation("Vol 16 col", hsvUtil, false, 16));
        _animations.add(new FftAnimation("FFT 512", hsvUtil, new FFT(512), true));
        _animations.add(new FftAnimation("FFT 128", hsvUtil, new FFT(128), true));
        _animations.add(new FftAnimation("FFT 32", hsvUtil, new FFT(32), true));
        _animations.add(new FftAnimation("FFT 16", hsvUtil, new FFT(16), true));
        _animations.add(new FftAnimation("FFT 512 freq", hsvUtil, new FFT(512), false));
        _animations.add(new FftAnimation("FFT 128 freq", hsvUtil, new FFT(128), false));
        _animations.add(new FftAnimation("FFT 32 freq", hsvUtil, new FFT(32), false));
        _animations.add(new FftAnimation("FFT 16 freq", hsvUtil, new FFT(16), false));
        _animations.add(new SpectrumAnimation("Spectrum 512", hsvUtil, new FFT(512), true));
        _animations.add(new SpectrumAnimation("Spectrum 128", hsvUtil, new FFT(128), true));
        _animations.add(new SpectrumAnimation("Spectrum 32", hsvUtil, new FFT(32), true));
        _animations.add(new SpectrumAnimation("Spectrum 16", hsvUtil, new FFT(16), true));
        _animations.add(new SpectrumAnimation("Spectrum 512 vol", hsvUtil, new FFT(512), false));
        _animations.add(new SpectrumAnimation("Spectrum 128 vol", hsvUtil, new FFT(128), false));
        _animations.add(new SpectrumAnimation("Spectrum 32 vol",hsvUtil, new FFT(32), false));
        _animations.add(new SpectrumAnimation("Spectrum 16 vol",hsvUtil, new FFT(16), false));
        _animations.add(new DotAnimation("Vol px", hsvUtil));
        _animations.add(new InvertedDotAnimation("Vol px Ambilight", hsvUtil));
        _animations.add(new EggAnimation("Egg 32 vol/col", hsvUtil, new FFT(32), true));
        _animations.add(new EggAnimation("Egg 32 vol", hsvUtil, new FFT(32), false));
        _animations.add(new CircleAnimation("Circle 128 vol", hsvUtil, new FFT(128), true, false));
        _animations.add(new CircleAnimation("Circle 128 vol/spd", hsvUtil, new FFT(128), true, true));
        _animations.add(new CircleAnimation("Circle 128 vol/col", hsvUtil, new FFT(128), false, false));
        _animations.add(new CircleAnimation("Circle 128 vol/col/spd", hsvUtil, new FFT(128), false, true));
        _animations.add(new BubbleAnimation("Bubble 128", hsvUtil, new FFT(128), true, false, false));
        _animations.add(new BubbleAnimation("Bubble 128 vol", hsvUtil, new FFT(128), true, true, false));
        _animations.add(new BubbleAnimation("Bubble 128 col", hsvUtil, new FFT(128), false, false, false));
        _animations.add(new BubbleAnimation("Bubble 128 vol/col", hsvUtil, new FFT(128), false, true, false));
        _animations.add(new BubbleAnimation("Square 128", hsvUtil, new FFT(128), true, false, true));
        _animations.add(new BubbleAnimation("Square 128 vol", hsvUtil, new FFT(128), true, true, true));
        _animations.add(new BubbleAnimation("Square 128 col", hsvUtil, new FFT(128), false, false, true));
        _animations.add(new BubbleAnimation("Square 128 vol/col", hsvUtil, new FFT(128), false, true, true));

        _pushQueue = new PushQueue<Short>(1792*2, (short)0);


        _drawThread = new DrawThread();
        _drawThread.start();
    }

    public void SetAudioProvider(IAudioProvider audioProvider){

        if(audioProvider != null) {
            audioProvider.Detach();
        }

        this.audioProvider = audioProvider;
        this.audioProvider.Attach(this);
    }

    public void Release() {
        if(_isDisposed){
            return;
        }

        _isDisposed = true;

        _fftImg = null;
        _fftReal = null;

        _animations.clear();

        _animations = null;

        if(audioProvider != null) {
            audioProvider.Detach();
        }
    }

    private short _currentVolumne = 0;

    private int i = 1;

    @Override
    public void onDataRecorded(short[] buffer, int n) {

        synchronized (this) {
            int skip = 1;
            _currentVolumne = Short.MIN_VALUE;
            for (int i = 0; i < buffer.length; i += skip) {
                short item = buffer[i];
                _pushQueue.push(item);

                if(_currentVolumne < buffer[i]) {
                    _currentVolumne = buffer[i];
                }
            }
        }
    }

    public class DrawThread extends Thread {

        private int col = Color.BLACK;

        private long mLastTime = 0;
        private int fps = 0, ifps = 0;

        @Override
        public void run() {

            //Log.d("-- Thread --", Thread.currentThread().getId() + ": Run");

            while(!_isDisposed) {

                if(!_isCanvasReady) {
                    //Log.d("-- Thread --", Thread.currentThread().getId() + ": Not ready");
                    continue;
                }


                Canvas canvas = null;
                try {
                    canvas = _holder.lockCanvas();

                    //Log.d("-- Thread --", Thread.currentThread().getId() + ": before sync");
                    synchronized (this) {
                        IAnimation animation = _animations.get(_animation);
                        animation.Draw(canvas, _currentVolumne, _pushQueue);
                    }
                    //Log.d("-- Thread --", Thread.currentThread().getId() + ": after sync");

                    long now = System.currentTimeMillis();

                    //MainActivity.DebugOut = String.valueOf(fps);

                    ifps++;
                    if (now > (mLastTime + 1000)) {
                        mLastTime = now;
                        fps = ifps;
                        ifps = 0;
                    }

                } catch (Exception ex) {
                    // Ups

                    //Log.d("-- Thread --", Thread.currentThread().getId() + ": catch");

                } finally {
                    try {
                        if (_holder != null) {
                            _holder.unlockCanvasAndPost(canvas);
                        }
                    } catch(Throwable t) {

                    }

                }
            }

            //Log.d("-- Thread --", Thread.currentThread().getId() + ": Finished");
        }

    }

    public void NextAnimation() {
        synchronized (this) {
            _animations.get(_animation).Release();
            _animation = (_animation + 1) % _animations.size();
            _animations.get(_animation).Init(_width, _height);
            String description = _animations.get(_animation).Description();

            Toast.makeText(_context, description, Toast.LENGTH_SHORT).show();
        }
    }

    public void PreviousAnimation() {
        synchronized (this) {
            _animations.get(_animation).Release();
            _animation = _animation - 1;
            _animation = _animation >= 0 ? _animation : (_animations.size() - 1);
            _animations.get(_animation).Init(_width, _height);
            String description = _animations.get(_animation).Description();

            Toast.makeText(_context, description, Toast.LENGTH_SHORT).show();
        }
    }

    public void SurfaceChanged(int width, int height)
    {
        if(width == 0 || height == 0) {
            return;
        }

        _width = width;
        _height = height;

        if(_animations == null){
            return;
        }

        _animations.get(_animation).Init(_width, _height);
    }

    public void SurfaceReady(boolean isReady) {
        _isCanvasReady = isReady;
    }

}
