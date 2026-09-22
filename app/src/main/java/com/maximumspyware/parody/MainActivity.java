package com.maximumspyware.parody;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;

/** Local fiction. No permissions, sensors, identifiers, storage or network access. */
public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(10,14,12), PANEL = Color.rgb(21,27,22);
    private static final int BORDER = Color.rgb(45,58,46), LIME = Color.rgb(195,252,101);
    private static final int PAPER = Color.rgb(239,244,232), MUTED = Color.rgb(161,173,163);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FakeScanState state = new FakeScanState();
    private Dashboard dashboard;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(INK); getWindow().setNavigationBarColor(INK);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        if (saved != null && saved.getBoolean("dashboard")) {
            state = new FakeScanState(saved.getInt("elapsed"), saved.getBoolean("paused"), saved.getBoolean("report"), saved.getInt("scan"));
            showDashboard();
        } else showDisclaimer();
    }
    private void display(View view) {
        if (Build.VERSION.SDK_INT >= 30) view.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        setContentView(view);
    }
    @Override protected void onResume() {
        super.onResume(); state.resume(SystemClock.elapsedRealtime());
        if (dashboard != null) dashboard.refresh();
    }
    @Override protected void onPause() {
        state.suspend(SystemClock.elapsedRealtime()); handler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        state.advance(SystemClock.elapsedRealtime());
        out.putBoolean("dashboard", dashboard != null); out.putInt("elapsed", state.elapsedMs());
        out.putBoolean("paused", state.paused()); out.putBoolean("report", state.report()); out.putInt("scan", state.scanNumber());
        super.onSaveInstanceState(out);
    }
    private void showDisclaimer() {
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(INK); scroll.setFillViewport(true);
        LinearLayout root = column(); root.setPadding(dp(24),dp(28),dp(24),dp(30)); scroll.addView(root);
        LinearLayout head = row(); weighted(head,text("MS / 002",12,LIME,true),0); head.addView(text("100% FAKE",12,LIME,true)); add(root,head,0);
        add(root,text("DEINE\nPRIVATSPHÄRE\nHAT FEIERABEND.",35,PAPER,true),36);
        add(root,text("MAXIMUM SPYWARE",15,LIME,true),12);
        addHeight(root,new RadarView(),24,170);
        LinearLayout card = panel(); add(root,card,20);
        add(card,text("Alles Show. Wirklich alles.",20,LIME,true),0);
        add(card,text("Diese Spaß-App simuliert Überwachung. Jeder Fund ist ausgedacht. Sie liest keine persönlichen Daten und greift weder auf Kamera, Mikrofon, Standort noch Internet zu.",16,PAPER,false),14);
        add(card,text("KEIN INTERNET · KEINE TRACKER · KEINE BERECHTIGUNGEN",11,MUTED,true),14);
        Button enter = button("OKAY, SPIONIER MICH FAKE AUS →"); enter.setOnClickListener(v -> { state = new FakeScanState(); state.resume(SystemClock.elapsedRealtime()); showDashboard(); }); add(root,enter,24);
        add(root,text("NULL DATEN. MAXIMALES THEATER.",11,MUTED,true),22);
        display(scroll);
    }
    private void showDashboard() { dashboard = new Dashboard(); display(dashboard); dashboard.refresh(); }

    private final class Dashboard extends ScrollView {
        private final TextView progress, stage, terminal, drama, session;
        private final RadarView radar;
        private final Button report, pause;
        private final Runnable tick = () -> { state.advance(SystemClock.elapsedRealtime()); refresh(); };
        Dashboard() {
            super(MainActivity.this); setFillViewport(true); setBackgroundColor(INK);
            LinearLayout root = column(); root.setPadding(dp(20),dp(20),dp(20),dp(28)); addView(root);
            LinearLayout head = row(); weighted(head,text("MAXIMUM\nSPYWARE",20,PAPER,true),0); head.addView(text("● SIMULATION",11,LIME,true)); add(root,head,0);
            session=text("",10,MUTED,true); add(root,session,25);
            stage=text("",29,PAPER,true); add(root,stage,16);
            LinearLayout scan=panel(); add(root,scan,18); radar=new RadarView(); addHeight(scan,radar,0,210);
            progress=text("0%",25,LIME,true); progress.setGravity(Gravity.END); add(scan,progress,8);
            add(scan,text("IMAGINÄRER TIEFENSCAN",10,MUTED,true),5);
            LinearLayout metrics=row(); weighted(metrics,metric("ECHTE DATEN","0"),0); weighted(metrics,metric("UPLOAD","0 B"),8);
            LinearLayout dramaBox=panel(); add(dramaBox,text("DRAMA",9,MUTED,true),0); drama=text("0%",23,LIME,true); add(dramaBox,drama,9); weighted(metrics,dramaBox,8); add(root,metrics,16);
            terminal=text("",12,LIME,false); terminal.setTypeface(android.graphics.Typeface.MONOSPACE); add(root,terminal,22);
            report=button("MEINEN FAKE-BERICHT ANSEHEN →"); report.setOnClickListener(v -> { state.showReport(); refresh(); smoothScrollTo(0,0); }); add(root,report,18);
            pause=button("PAUSE"); pause.setOnClickListener(v -> { state.togglePause(SystemClock.elapsedRealtime()); refresh(); }); add(root,pause,12);
            Button again=button("NEUER SCAN"); again.setOnClickListener(v -> { state.restart(SystemClock.elapsedRealtime()); refresh(); smoothScrollTo(0,0); }); add(root,again,10);
            add(root,text("Alle Meldungen sind erfunden. Es werden keine Daten gelesen oder übertragen.\n\nNULL DATEN. MAXIMALES THEATER.",11,MUTED,false),20);
        }
        void refresh() {
            handler.removeCallbacks(tick);
            float p=state.progress(); radar.update(p,state.running()); progress.setText(Math.round(p*100)+"%"); drama.setText(Math.round(p*999)+"%");
            session.setText("LOKALES THEATER / SITZUNG "+state.scanNumber()); stage.setText(state.stage());
            if (state.report()) {
                terminal.setText("BEISPIELBERICHT / ALLES FREI ERFUNDEN\n\n01 / SOZIALES KAPITAL\n487 Kontakte — keiner wurde gelesen.\n\n02 / AKTUELLER STANDORT\nPlanet Erde™ — nicht nachgesehen.\n\n03 / GEDANKENPROFIL\nNur Memes — Gedankenleser aus Pappe.\n\n04 / DEIN MARKTWERT\n0,0042 € — kein Käufer, kein Verkauf.\n\n05 / ENTWICKLERGEWINN\n9.999.999 € in Monopoly-Geld.\n\n0 B gesendet. 0 echte Funde.\n100% verschwendete Lebenszeit.");
            } else {
                List<FakeScanState.Entry> entries=state.visibleEntries(); StringBuilder lines=new StringBuilder();
                for(int i=Math.max(0,entries.size()-5);i<entries.size();i++) {
                    FakeScanState.Entry e=entries.get(i); lines.append('[').append(e.atMs/1000).append("] > ").append(e.message).append('\n');
                }
                terminal.setText(lines.toString());
            }
            report.setVisibility(state.complete()&&!state.report()?VISIBLE:GONE);
            pause.setVisibility(state.complete()||state.report()?GONE:VISIBLE); pause.setText(state.paused()?"WEITER":"PAUSE");
            if(state.running()) handler.postDelayed(tick,50);
        }
    }
    private LinearLayout metric(String label,String value) { LinearLayout box=panel(); add(box,text(label,9,MUTED,true),0); add(box,text(value,23,LIME,true),9); return box; }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout panel() { LinearLayout l=column(); l.setPadding(dp(14),dp(17),dp(14),dp(18)); l.setBackground(round(PANEL,18,BORDER)); return l; }
    private TextView text(String value,float size,int color,boolean bold) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setIncludeFontPadding(false); t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0)); t.setLineSpacing(dp(3),1f); return t; }
    private Button button(String value) { Button b=new Button(this); b.setText(value); b.setTextColor(INK); b.setTextSize(12); b.setAllCaps(false); b.setTypeface(android.graphics.Typeface.DEFAULT,1); b.setBackground(round(LIME,10,LIME)); b.setMinHeight(dp(54)); b.setPadding(dp(14),dp(12),dp(14),dp(12)); return b; }
    private void add(LinearLayout parent,View child,int top) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.topMargin=dp(top); parent.addView(child,p); }
    private void weighted(LinearLayout parent,View child,int gap) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1); p.leftMargin=dp(gap); parent.addView(child,p); }
    private void addHeight(LinearLayout parent,View child,int top,int height) { LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(height)); p.topMargin=dp(top); parent.addView(child,p); }
    private android.graphics.drawable.GradientDrawable round(int color,int radius,int stroke) { android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); d.setStroke(dp(1),stroke); return d; }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private final class RadarView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG); private float progress,angle; private boolean running;
        RadarView() { super(MainActivity.this); setContentDescription("Dekoratives Radar einer erfundenen Scan-Simulation"); }
        void update(float p,boolean animate) { progress=p; running=animate; if(animate)angle=(angle+5)%360; invalidate(); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c); float r=Math.min(getWidth(),getHeight())*.42f,cx=getWidth()/2f,cy=getHeight()/2f;
            paint.setStrokeWidth(dp(1)); paint.setStyle(Paint.Style.STROKE); paint.setColor(BORDER);
            for(int i=1;i<=4;i++)c.drawCircle(cx,cy,r*i/4,paint); c.drawLine(cx-r,cy,cx+r,cy,paint);c.drawLine(cx,cy-r,cx,cy+r,paint);
            paint.setColor(LIME);c.save();c.rotate(angle,cx,cy);Path sweep=new Path();sweep.moveTo(cx,cy);sweep.lineTo(cx+r,cy);sweep.lineTo(cx+r*.35f,cy-r*.35f);sweep.close();paint.setStyle(Paint.Style.FILL);paint.setAlpha(32);c.drawPath(sweep,paint);paint.setStyle(Paint.Style.STROKE);paint.setAlpha(190);c.drawLine(cx,cy,cx+r,cy,paint);c.restore();
            paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);
            for(int i=0;i<6;i++){double a=i*1.71+.65;float rr=r*(.26f+(i%3)*.22f);if(progress>i*.14f||!running){float x=cx+(float)Math.cos(a)*rr,y=cy+(float)Math.sin(a)*rr;paint.setAlpha(35);c.drawCircle(x,y,dp(11),paint);paint.setAlpha(255);c.drawCircle(x,y,dp(3),paint);}}
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setAlpha(255);c.drawCircle(cx,cy,dp(5),paint);
        }
    }
}
