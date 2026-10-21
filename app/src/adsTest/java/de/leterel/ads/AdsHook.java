package de.leterel.ads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.google.android.libraries.ads.mobile.sdk.MobileAds;
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig;
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize;
import com.google.android.libraries.ads.mobile.sdk.banner.AdView;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd;
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest;
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback;
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import java.util.concurrent.CompletableFuture;

/** Optional networked test profile: official sample IDs only, no interstitials. */
public final class AdsHook {
    public static final boolean ENABLED = true;
    private static final String APP_ID = "ca-app-pub-3940256099942544~3347511713";
    private static final String BANNER_ID = "ca-app-pub-3940256099942544/9214589741";
    private static CompletableFuture<Void> initialization;
    private final Activity activity;
    private final LinearLayout panel, bannerSlot;
    private final TextView status;
    private final Button privacy, retry;
    private final ConsentInformation information;
    private final ConsentGate gate = new ConsentGate();
    private boolean consentBusy, consentChecked;
    private AdView adView;
    private final Handler timeouts = new Handler(Looper.getMainLooper());

    public AdsHook(Activity activity, LinearLayout container) {
        this.activity = activity;
        information = UserMessagingPlatform.getConsentInformation(activity);
        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, 32, 0, 16);
        panel.setVisibility(View.GONE);
        container.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        TextView label = new TextView(activity);
        label.setText("WERBUNG · GOOGLE-TESTANZEIGE · KEINE EINNAHMEN");
        label.setTextColor(Color.LTGRAY); label.setTextSize(11);
        panel.addView(label);
        status = new TextView(activity);
        status.setTextColor(Color.LTGRAY); status.setTextSize(12);
        panel.addView(status);
        bannerSlot = new LinearLayout(activity);
        bannerSlot.setGravity(android.view.Gravity.CENTER);
        panel.addView(bannerSlot, new LinearLayout.LayoutParams(-1, -2));
        privacy = new Button(activity); privacy.setText("Datenschutzoptionen");
        privacy.setVisibility(View.GONE);
        privacy.setOnClickListener(v -> privacyOptions()); panel.addView(privacy);
        retry = new Button(activity); retry.setText("Datenschutz / Testwerbung erneut prüfen");
        retry.setOnClickListener(v -> { consentChecked = false; gate.consent(false); clearBanner(); checkConsent(); });
        panel.addView(retry);
    }

    public void resume() { gate.resume(); update(); }
    public void pause() { gate.pause(); consentBusy = false; timeouts.removeCallbacksAndMessages(null); clearBanner(); }
    public void destroy() { gate.destroy(); timeouts.removeCallbacksAndMessages(null); clearBanner(); }
    public void result(boolean ready) {
        gate.result(ready);
        if (!ready) { consentBusy = false; timeouts.removeCallbacksAndMessages(null); clearBanner(); }
        update();
    }
    private boolean current(int token) {
        return gate.current(token) && !activity.isFinishing() && !activity.isDestroyed();
    }
    private void update() {
        panel.setVisibility(gate.visible() ? View.VISIBLE : View.GONE);
        if (!gate.visible()) return;
        if (!consentChecked) checkConsent(); else loadIfAllowed();
    }
    private void privacyVisibility() {
        privacy.setVisibility(information.getPrivacyOptionsRequirementStatus()
            == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED ? View.VISIBLE : View.GONE);
    }
    private void checkConsent() {
        if (!gate.visible() || consentBusy) return;
        consentBusy = true;
        retry.setEnabled(false);
        gate.consent(false);
        final int token = gate.token();
        status.setText("Datenschutz wird vor der SDK-Initialisierung geprüft …");
        timeouts.postDelayed(() -> {
            if (!current(token) || !consentBusy) return;
            gate.invalidate(); consentBusy = false; consentChecked = true; retry.setEnabled(true);
            status.setText("Datenschutzprüfung dauert zu lange. Keine Anzeigenanfrage. Erneut prüfen ist möglich.");
        }, 15000);
        information.requestConsentInfoUpdate(activity, new ConsentRequestParameters.Builder().build(),
            () -> activity.runOnUiThread(() -> {
                if (!current(token)) return;
                timeouts.removeCallbacksAndMessages(null);
                privacyVisibility();
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, error -> activity.runOnUiThread(() -> {
                    if (!current(token)) return;
                    consentBusy = false; consentChecked = true; retry.setEnabled(true);
                    privacyVisibility();
                    gate.consent(error == null && information.canRequestAds());
                    if (!gate.allowed()) status.setText("Keine Testwerbung: Datenschutz nicht freigegeben oder Formular nicht verfügbar.");
                    loadIfAllowed();
                }));
            }), error -> activity.runOnUiThread(() -> {
                if (!current(token)) return;
                timeouts.removeCallbacksAndMessages(null);
                consentBusy = false; consentChecked = true; retry.setEnabled(true);
                privacyVisibility();
                status.setText("Datenschutzdienst nicht erreichbar. Ergebnis bleibt vollständig nutzbar. Keine Anzeigenanfrage.");
            }));
    }
    private void privacyOptions() {
        if (!gate.visible() || consentBusy) return;
        gate.consent(false); clearBanner(); consentBusy = true;
        final int token = gate.token();
        UserMessagingPlatform.showPrivacyOptionsForm(activity, error -> activity.runOnUiThread(() -> {
            if (!current(token)) return;
            consentBusy = false;
            gate.consent(error == null && information.canRequestAds());
            privacyVisibility();
            status.setText(gate.allowed() ? "Datenschutz aktualisiert." : "Keine Testwerbung nach Datenschutzentscheidung.");
            loadIfAllowed();
        }));
    }
    private static synchronized CompletableFuture<Void> initialize(Context context) {
        if (initialization == null) {
            initialization = new CompletableFuture<>();
            final CompletableFuture<Void> pending = initialization;
            new Thread(() -> {
                try {
                    // Its automatic manifest initializer is removed in this profile.
                    // Initialize the SDK's background-work dependency only after consent.
                    WorkManager.initialize(context, new Configuration.Builder().build());
                    MobileAds.initialize(context, new InitializationConfig.Builder(APP_ID).build(), ignored -> pending.complete(null));
                } catch (RuntimeException failure) { pending.completeExceptionally(failure); }
            }, "test-ads-init").start();
        }
        return initialization;
    }
    private void loadIfAllowed() {
        // Recheck the live UMP state before SDK initialization AND before loadAd.
        if (!information.canRequestAds() || !gate.beginRequest()) return;
        final int token = gate.token();
        status.setText("Google-Testwerbung wird geladen …");
        initialize(activity.getApplicationContext()).whenComplete((ignored, failure) -> activity.runOnUiThread(() -> {
            if (!current(token) || !gate.allowed() || !information.canRequestAds()) return;
            if (failure != null) { status.setText("Testwerbung konnte nicht initialisiert werden."); return; }
            bannerSlot.post(() -> {
                if (!current(token) || !gate.allowed() || !information.canRequestAds()) return;
                int width = Math.max(1, Math.round(bannerSlot.getWidth() / activity.getResources().getDisplayMetrics().density));
                if (width < 120) { status.setText("Zu wenig Platz für eine Testanzeige."); return; }
                adView = new AdView(activity);
                final AdView requestedView = adView;
                bannerSlot.addView(requestedView);
                AdSize size = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, width);
                requestedView.loadAd(new BannerAdRequest.Builder(BANNER_ID, size).build(), new AdLoadCallback<BannerAd>() {
                    @Override public void onAdLoaded(BannerAd ad) { activity.runOnUiThread(() -> {
                        if (!current(token) || requestedView != adView) return;
                        status.setText("Google-Testanzeige. Keine produktive Werbung und keine Einnahmen.");
                    }); }
                    @Override public void onAdFailedToLoad(LoadAdError error) { activity.runOnUiThread(() -> {
                        if (!current(token) || requestedView != adView) return;
                        clearBanner(); status.setText("Keine Testanzeige verfügbar. Das Ergebnis bleibt nutzbar.");
                    }); }
                });
            });
        }));
    }
    private void clearBanner() {
        if (adView != null) { bannerSlot.removeView(adView); adView.destroy(); adView = null; }
    }
}
