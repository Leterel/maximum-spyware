package de.leterel.ads;
import android.app.Activity;
import android.widget.LinearLayout;

/** Offline profile intentionally has no SDK, network calls or ad UI. */
public final class AdsHook {
    public static final boolean ENABLED = false;
    public AdsHook(Activity activity, LinearLayout container) {}
    public void resume() {}
    public void pause() {}
    public void destroy() {}
    public void result(boolean ready) {}
}
