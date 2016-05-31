package app.android.hifispectrum.hw;

import app.android.hifispectrum.proc.PushQueue;

/**
 * Created by inter on 31/01/2016.
 */
public interface IAudioProvider {

    void Attach(IAudioProviderCallback callback);
    void Detach();

    void Start();
    void Stop();

}
