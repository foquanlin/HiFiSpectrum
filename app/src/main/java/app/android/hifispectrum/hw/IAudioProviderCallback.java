package app.android.hifispectrum.hw;

public interface IAudioProviderCallback {
    void onDataRecorded(short[] buffer, int n);
}
