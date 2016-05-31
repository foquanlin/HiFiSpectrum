package app.android.hifispectrum;

import app.android.hifispectrum.hw.IAudioProvider;
import app.android.hifispectrum.hw.MicAudioProvider;
import app.android.hifispectrum.hw.OutputAudioProvider;
import app.android.hifispectrum.proc.AdvancedTouchListener;
import app.android.hifispectrum.ui.SpectrumView;
import app.android.hifispectrum.util.SystemUiHider;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {

    public static GoogleAnalytics analytics;
    public static Tracker tracker;

    public static String DebugOut;

    private SurfaceView _surfaceView;
    private TextView _debugView;

    private AdvancedTouchListener _advancedTouchListener;

    private SpectrumView _sv;
    private IAudioProvider _ap;
    private boolean _isCanvasReady;
    private int _width;
    private int _height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        _surfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        _surfaceView.getHolder().addCallback(this);

        _debugView = (TextView)findViewById(R.id.debugView);

        InitTracker();

        _advancedTouchListener = new AdvancedTouchListener(this) {

            @Override
            public void onSwipeLeft() {
                if(_sv != null) {
                    _sv.NextAnimation();
                }
            }

            @Override
            public void onSwipeRight() {
                if(_sv != null) {
                    _sv.PreviousAnimation();
                }
            }

            @Override
            public void onDoubleTap() {
                if(_sv != null) {
                    if(_ap == null){
                        return;
                    }

                    _ap.Detach();
                    _ap.Stop();

                    if(_ap instanceof MicAudioProvider){
                        _ap = new OutputAudioProvider();
                        _ap.Start();
                    } else{
                        _ap = new MicAudioProvider();
                        _ap.Start();
                    }

                    _sv.SetAudioProvider(_ap);
                }
            }
        };

        ActivityDebugThread thread = new ActivityDebugThread();

        thread.start();
    }

    private void InitTracker() {

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(15);

        tracker = analytics.newTracker("UA-47380598-2"); // Replace with actual tracker/property Id
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        synchronized (this) {
            DisposeResources();

            _sv = new SpectrumView(_surfaceView, this, tracker);

            _ap = new MicAudioProvider();
            _ap.Start();
            _sv.SetAudioProvider(_ap);
            _sv.SurfaceReady(_isCanvasReady);
            _sv.SurfaceChanged(_width, _height);

            Toast.makeText(this, "Swipe to change animation", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Double tap to change source (Mic/Output)", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        synchronized (this) {
            DisposeResources();
        }
    }

    private void DisposeResources()
    {
        if(_ap != null){
            _ap.Detach();
            _ap.Stop();
            _ap = null;
        }

        if(_sv != null) {
            _sv.Release();
            _sv = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        _advancedTouchListener.onTouch(event);

        return super.onTouchEvent(event);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        _isCanvasReady = true;

        if(_sv != null){
            _sv.SurfaceReady(_isCanvasReady);
        }
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        _width = width;
        _height = height;

        if(_sv != null){
            _sv.SurfaceChanged( width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        _isCanvasReady = false;

        if(_sv != null){
            _sv.SurfaceReady(_isCanvasReady);
        }
    }

    public class ActivityDebugThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {

                    _debugView.post(new Runnable() {
                        @Override
                        public void run() {
                            _debugView.setText(DebugOut);
                        }
                    });
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        Toast.makeText(this, "Please rate the app if you like it.", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }
}
