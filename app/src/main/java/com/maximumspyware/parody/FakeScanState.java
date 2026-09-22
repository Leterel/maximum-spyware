package com.maximumspyware.parody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Production timing/state; deliberately independent of Android and real device data. */
public final class FakeScanState {
    public static final int DURATION_MS = 22000;
    public static final class Entry {
        public final int atMs;
        public final String message;
        Entry(int atMs, String message) { this.atMs = atMs; this.message = message; }
    }
    private static final Entry[] ENTRIES = {
        new Entry(0, "SIMULATION initialisiert. Datenquelle: Fantasie."),
        new Entry(1200, "Imaginäre Firewall mit bösem Blick geöffnet."),
        new Entry(2600, "Gedankenleser gestartet … Gedanken nicht gefunden."),
        new Entry(4100, "Kühlschrank-Telemetrie: 3 verdächtige Käsereste."),
        new Entry(5800, "487 frei erfundene Kontakte monetarisiert."),
        new Entry(7600, "Standort ermittelt: irgendwo im Universum."),
        new Entry(9200, "Geheime Meme-Reserve: kritisch unterversorgt."),
        new Entry(10800, "Browserprofil gewürfelt: maximal fragwürdig."),
        new Entry(12300, "Datenverkauf vorbereitet. Käufer: niemand."),
        new Entry(14100, "0 Bytes übertragen. Dramatisch genug getan."),
        new Entry(15900, "Satelliten auf imaginären Gamingstuhl ausgerichtet."),
        new Entry(17700, "Bedrohungslevel: Bruder, geh mal schlafen."),
        new Entry(19800, "Bericht frei erfunden. Wirklichkeit unberührt."),
        new Entry(DURATION_MS, "FERTIG. Alles Fake. Du gehörst weiterhin dir.")
    };
    private int elapsedMs;
    private long lastTick = -1;
    private boolean foreground, paused, report;
    private int scanNumber;
    public FakeScanState() { this(0, false, false, 1); }
    public FakeScanState(int elapsed, boolean paused, boolean report, int number) {
        elapsedMs = Math.max(0, Math.min(DURATION_MS, elapsed));
        this.paused = paused; this.report = report && complete(); scanNumber = Math.max(1, number);
    }
    public void advance(long now) {
        if (foreground && !paused && !report && lastTick >= 0 && now >= lastTick)
            elapsedMs += (int) Math.min(DURATION_MS - elapsedMs, now - lastTick);
        lastTick = now;
    }
    public void resume(long now) { foreground = true; lastTick = now; }
    public void suspend(long now) { advance(now); foreground = false; }
    public void togglePause(long now) { advance(now); if (!complete()) paused = !paused; }
    public void showReport() { if (complete()) report = true; }
    public void restart(long now) {
        elapsedMs = 0; paused = false; report = false; lastTick = now;
        if (scanNumber < Integer.MAX_VALUE) scanNumber++;
    }
    public int elapsedMs() { return elapsedMs; }
    public int scanNumber() { return scanNumber; }
    public float progress() { return elapsedMs / (float) DURATION_MS; }
    public boolean complete() { return elapsedMs >= DURATION_MS; }
    public boolean paused() { return paused; }
    public boolean report() { return report; }
    public boolean running() { return foreground && !paused && !report && !complete(); }
    public List<Entry> visibleEntries() {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : ENTRIES) if (entry.atMs <= elapsedMs) result.add(entry);
        return Collections.unmodifiableList(result);
    }
    public String stage() {
        if (report) return "WIR HABEN\nNICHTS GEGEN DICH.";
        if (complete()) return "DU BIST ENTSPIONIERT.";
        if (paused) return "SPIONAGE MACHT PAUSE.";
        if (elapsedMs < 5800) return "DEINE AURA WIRD GELEAKT.";
        if (elapsedMs < 12300) return "WIR WISSEN GAR NICHTS.";
        if (elapsedMs < 17700) return "DATENVERKAUF AN NIEMANDEN.";
        return "LETZTE PEINLICHKEITEN SUCHEN.";
    }
}
