package app.android.hifispectrum.hw;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.Visualizer;

/**
 * Created by inter on 31/01/2016.
 */
public class OutputAudioProvider implements IAudioProvider {

    private Visualizer audioOutput = null;
    private IAudioProviderCallback _callback;

    @Override
    public void Stop() {
        _callback = null;
        stopVisualizer();
        destroyVisualizer();
    }

    @Override
    public void Start() {
        createVisualizer();
        startVisualizer();
    }

    @Override
    public void Attach(IAudioProviderCallback callback) {

        this._callback = callback;
    }

    @Override
    public void Detach() {
        _callback = null;
    }

    private void createVisualizer() {
        audioOutput = new Visualizer(0); // get output audio stream

        audioOutput.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {

                if(_callback != null) {
                    short[] buffer = copyBuffer(waveform, waveform.length);
                    _callback.onDataRecorded(buffer, buffer.length);
                }

            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

            }



        }, Visualizer.getMaxCaptureRate(), true, false); // waveform not freq data


    }

    private void startVisualizer(){
        audioOutput.setEnabled(true);
    }

    private void stopVisualizer() {
        audioOutput.setEnabled(false);
    }

    private void destroyVisualizer(){
        audioOutput.release();
    }

    private short[] copyBuffer(byte[] data, int length) {
        byte[] buffer = new byte[length];
        short[] sbuffer = new short[length];

        System.arraycopy(data, 0, buffer, 0, length);

        for(int i=0;i<length;i++){
            sbuffer[i] = buffer[i];
            if(sbuffer[i] < 0){
                sbuffer[i] += 256;
            }

            sbuffer[i] -= 128;

            sbuffer[i] *= 128;
        }

        return sbuffer;
    }
}
