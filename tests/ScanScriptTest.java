package com.maximumspyware.parody;

/** Regression checks against the actual production state class. */
public final class ScanScriptTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        FakeScanState s = new FakeScanState();
        s.resume(1000); s.advance(5000);
        check(s.elapsedMs() == 4000 && s.running(), "foreground time progresses");
        s.togglePause(6000); s.advance(60000);
        check(s.elapsedMs() == 5000 && s.paused() && !s.running(), "pause freezes time");
        s.togglePause(70000); s.advance(71000);
        check(s.elapsedMs() == 6000 && !s.paused(), "resume excludes paused duration");
        s.suspend(72000); s.advance(200000); s.resume(300000); s.advance(301000);
        check(s.elapsedMs() == 8000, "background time excluded");
        s.showReport(); check(!s.report(), "report cannot precede completion");
        s.advance(400000); s.showReport();
        check(s.complete() && s.report() && s.progress() == 1f, "completion clamps and enables report");
        s.restart(500000); s.advance(501000);
        check(s.elapsedMs() == 1000 && !s.report() && s.scanNumber() == 2 && s.running(), "report to restart runs again");
        FakeScanState restored = new FakeScanState(s.elapsedMs(), true, false, s.scanNumber());
        restored.resume(900000); restored.advance(999999);
        check(restored.elapsedMs() == 1000 && restored.paused(), "rotation restores paused state");
        restored.togglePause(1000000); restored.advance(1000100);
        check(restored.elapsedMs() == 1100, "restored scan resumes without old clock origin");
        FakeScanState timeline = new FakeScanState(); timeline.resume(0);
        for (int ms = 0; ms <= 23000; ms += 53) {
            timeline.advance(ms);
            for (FakeScanState.Entry entry : timeline.visibleEntries())
                check(entry.atMs <= timeline.elapsedMs(), "no future event leaks");
            if (!timeline.complete()) check(!timeline.visibleEntries().get(timeline.visibleEntries().size()-1).message.contains("FERTIG"), "finish message waits for end");
        }
        check(timeline.visibleEntries().get(0).message.contains("SIMULATION"), "fiction disclosed");
        check(timeline.visibleEntries().get(timeline.visibleEntries().size()-1).atMs == FakeScanState.DURATION_MS, "last event matches completion");
        FakeScanState invalid = new FakeScanState(-100, false, true, -1);
        check(invalid.elapsedMs() == 0 && !invalid.report() && invalid.scanNumber() == 1, "restored values validated");
        System.out.println("PASS: " + checks + " production checks: pause, lifecycle, report/restart, restore, timeline.");
    }
}
