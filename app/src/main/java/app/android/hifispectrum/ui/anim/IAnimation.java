package app.android.hifispectrum.ui.anim;

import android.graphics.Canvas;

/**
 * Created by Bartosz on 25/05/2015.
 */
public interface IAnimation {
    void Init(int width, int height);
    void Draw(Canvas canvas, short volume, Iterable<Short> audio);
    void Release();

    String Description();
}
