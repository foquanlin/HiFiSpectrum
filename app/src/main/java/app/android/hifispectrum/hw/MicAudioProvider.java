package app.android.hifispectrum.hw;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import app.android.hifispectrum.proc.PushQueue;

/**
 * Created by inter on 31/01/2016.
 */
public class MicAudioProvider implements IAudioProvider {

    public AudioRecord audioRecord;
    public static short[] buffer;


    private IAudioProviderCallback _onDataRecorded;
    private AudioThread _audioThread;

    private boolean _isDisposed;

    public void init() {

        int rate = 44100;
        int channel = AudioFormat.CHANNEL_IN_MONO;
        int encoding = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(rate, channel, encoding);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, channel, encoding, bufferSize);
        int minShortBuffer = bufferSize/2;
        int requestedBuffer = minShortBuffer*2;
        buffer = new short[requestedBuffer];

        _audioThread = new AudioThread();
    }

    public void Start(IAudioProviderCallback onDataRecorded) {
        _onDataRecorded = onDataRecorded;
    }

    @Override
    public void Attach(IAudioProviderCallback callback) {
        _onDataRecorded = callback;
    }

    @Override
    public void Detach() {
    _onDataRecorded = null;
    }

    @Override
    public void Start() {
        _isDisposed = false;
        init();
        _audioThread.start();
    }

    @Override
    public void Stop() {
        Release();
    }

    public void Release() {
        if(_isDisposed){
            return;
        }

        _isDisposed = true;
        _audioThread = null;
    }


    public class AudioThread extends Thread {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            while(audioRecord.getRecordingState() !=  AudioRecord.STATE_INITIALIZED) {
                continue;
            }

            audioRecord.startRecording();

            while (!_isDisposed) {


                try {

                    int read = audioRecord.read(buffer, 0, buffer.length);
                    short[] copy = copyBuffer(buffer, read);

                    IAudioProviderCallback callback = _onDataRecorded;
                    if(callback != null){
                        callback.onDataRecorded(copy, copy.length);
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            audioRecord.stop();
            audioRecord.release();
        }

        private short[] copyBuffer(short[] data, int length) {
            short[] buffer = new short[length];

            for(int i=0;i<length;i++) {
                buffer[i] = data[i];
            }
            return buffer;
        }
    }
}
