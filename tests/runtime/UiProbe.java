package de.leterel.prototypeqa;

import android.app.Instrumentation;
import android.app.UiAutomation;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Black-box emulator test. The animated production APK is never instrumented or modified. */
public final class UiProbe extends Instrumentation {
    private static final String APP = "com.maximumspyware.parody";
    private final JSONObject result = new JSONObject();
    private final JSONArray checks = new JSONArray(), snapshots = new JSONArray();
    private UiAutomation ui;
    private File evidence;
    private String scenario = "lifecycle";

    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        if (args != null) scenario = args.getString("scenario", "lifecycle");
        start();
    }
    @Override public void onStart() {
        Bundle bundle = new Bundle(); int exit = 1;
        try {
            ui = getUiAutomation();
            // A second guard protects against someone launching the helper directly.
            if (!"1".equals(shellRaw("getprop ro.kernel.qemu").trim()))
                throw new IllegalStateException("Refusing to operate outside an Android emulator");
            if (!"lifecycle".equals(scenario) && !"process-death".equals(scenario))
                throw new IllegalArgumentException("Unknown scenario: " + scenario);
            evidence = new File(getContext().getExternalFilesDir(null), scenario);
            if (!evidence.isDirectory() && !evidence.mkdirs()) throw new IllegalStateException("No evidence directory");
            result.put("api", android.os.Build.VERSION.SDK_INT);
            result.put("test", "maximum-spyware black-box " + scenario);
            if ("process-death".equals(scenario)) runProcessDeath(); else runScenario();
            result.put("status", "passed"); exit = 0;
        } catch (Exception error) {
            try { result.put("status", "failed"); result.put("error", error.toString()); result.put("stack", android.util.Log.getStackTraceString(error)); }
            catch (Exception ignored) { }
        } finally {
            try {
                result.put("checks", checks); result.put("snapshots", snapshots);
                if (evidence != null) write(new File(evidence, scenario + "-results.json"), result.toString(2));
                bundle.putString("result", result.toString());
            } catch (Exception error) { bundle.putString("error", error.toString()); exit = 1; }
            finish(exit, bundle);
        }
    }

    private void launchFromLauncher() throws Exception {
        shell("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "
                + APP + "/.MainActivity");
    }
    private void runProcessDeath() throws Exception {
        // This initial force-stop creates a clean launch only. Every tested death below
        // uses am kill on a background process, retaining Android's activity task/state.
        shell("am force-stop " + APP);
        launchFromLauncher();
        click("OKAY, SPIONIER MICH FAKE AUS →");
        click("NEUER SCAN"); SystemClock.sleep(2000); click("PAUSE");
        JSONObject paused = snapshot("01-session-2-paused");
        int pausedPercent = paused.getInt("progressPercent");
        check(pausedPercent > 0 && pausedPercent < 100 && has("LOKALES THEATER / SITZUNG 2"),
                "Nondefault session and nonzero scan progress established");
        check(has("SPIONAGE MACHT PAUSE."), "Scan is paused before process death");
        killBackgroundAndRestore("paused");
        JSONObject restored = snapshot("02-paused-restored");
        check(restored.getInt("progressPercent") == pausedPercent && has("LOKALES THEATER / SITZUNG 2"),
                "Process recreation preserves exact paused progress and session 2");
        check(has("SPIONAGE MACHT PAUSE."), "Paused state survives process recreation");
        SystemClock.sleep(2000);
        check(snapshot("03-restored-still-paused").getInt("progressPercent") == pausedPercent,
                "Restored pause remains frozen for two seconds");

        click("WEITER"); SystemClock.sleep(1200); click("PAUSE");
        int runningCheckpoint = snapshot("04-before-running-death").getInt("progressPercent");
        check(runningCheckpoint > pausedPercent && runningCheckpoint < 85,
                "Restored scan continues before the running-state death test");
        click("WEITER");
        killBackgroundAndRestore("running");
        check(has("PAUSE") && !has("SPIONAGE MACHT PAUSE."),
                "A running scan is restored running instead of paused or reset");
        click("PAUSE");
        int returnedPercent = snapshot("05-running-restored").getInt("progressPercent");
        check(returnedPercent >= runningCheckpoint && returnedPercent - runningCheckpoint <= 20,
                "Recreated running scan excludes dead time (at most 4.4 seconds foreground overhead)");
        check(has("LOKALES THEATER / SITZUNG 2"), "Running-state recreation preserves session 2");

        click("WEITER"); SystemClock.sleep(23000);
        click("MEINEN FAKE-BERICHT ANSEHEN →");
        snapshot("06-report-before-death");
        check(has("WIR HABEN\nNICHTS GEGEN DICH."), "Scan completes and enters the real report");
        killBackgroundAndRestore("report");
        JSONObject report = snapshot("07-report-restored");
        check(report.getInt("progressPercent") == 100 && has("WIR HABEN\nNICHTS GEGEN DICH.")
                && has("LOKALES THEATER / SITZUNG 2"), "Completed report survives a third process recreation");
        click("NEUER SCAN"); click("PAUSE");
        JSONObject restarted = snapshot("08-restarted-after-restored-report");
        check(restarted.getInt("progressPercent") < 20 && has("LOKALES THEATER / SITZUNG 3")
                && has("SPIONAGE MACHT PAUSE."), "Restored report starts a working scan in session 3");
    }
    private void killBackgroundAndRestore(String label) throws Exception {
        String oldPid = shell("pidof " + APP).trim();
        check(oldPid.matches("[0-9]+"), label + ": one real production process exists");
        shell("input keyevent KEYCODE_HOME");
        SystemClock.sleep(1500); // Allow onStop/onSaveInstanceState to finish normally.
        AccessibilityNodeInfo background = ui.getRootInActiveWindow();
        check(background != null && !APP.contentEquals(background.getPackageName()),
                label + ": app is genuinely in the background before am kill");
        shell("am kill " + APP);
        String livePid = oldPid;
        for (int attempt = 0; attempt < 40; attempt++) {
            livePid = shell("pidof " + APP).trim();
            if (livePid.isEmpty()) break;
            SystemClock.sleep(100);
        }
        check(livePid.isEmpty(), label + ": production process is absent after am kill");
        long deadStart = SystemClock.elapsedRealtime();
        SystemClock.sleep(5000);
        check(shell("pidof " + APP).trim().isEmpty(), label + ": process remains dead for five seconds");
        long deadMs = SystemClock.elapsedRealtime() - deadStart;
        launchFromLauncher(); root();
        String newPid = shell("pidof " + APP).trim();
        check(newPid.matches("[0-9]+") && !oldPid.equals(newPid), label + ": relaunch creates a different process");
        JSONObject death = new JSONObject(); death.put("oldPid", oldPid); death.put("newPid", newPid);
        death.put("deadWaitMs", deadMs); death.put("method", "am kill while background; normal launcher resume");
        result.put(label + "ProcessRecreation", death);
    }

    private void runScenario() throws Exception {
        shell("am force-stop " + APP);
        shell("am start -W -n " + APP + "/.MainActivity");
        click("OKAY, SPIONIER MICH FAKE AUS →");
        click("PAUSE");
        JSONObject paused = snapshot("01-paused");
        check(has("SPIONAGE MACHT PAUSE."), "Pause button freezes the real dashboard");
        int pausedPercent = paused.getInt("progressPercent");
        check(pausedPercent < 100, "Pause reached before completion");
        SystemClock.sleep(3000);
        check(snapshot("02-paused-after-3s").getInt("progressPercent") == pausedPercent,
                "Three seconds paused do not advance progress");

        shell("wm user-rotation lock 1");
        SystemClock.sleep(1000);
        JSONObject rotated = snapshot("03-rotated-paused");
        check(rotated.getInt("screenWidth") > rotated.getInt("screenHeight"), "Emulator is actually landscape");
        check(has("SPIONAGE MACHT PAUSE.") && rotated.getInt("progressPercent") == pausedPercent
                && has("LOKALES THEATER / SITZUNG 1"), "Rotation preserves paused progress and session");

        click("WEITER"); SystemClock.sleep(1500); click("PAUSE");
        int resumedPercent = snapshot("04-resumed-progress").getInt("progressPercent");
        check(resumedPercent > pausedPercent, "Continue advances the real scan again");
        click("WEITER");
        shell("input keyevent KEYCODE_HOME");
        SystemClock.sleep(300);
        AccessibilityNodeInfo background = ui.getRootInActiveWindow();
        check(background != null && !APP.contentEquals(background.getPackageName()), "App really moved into background");
        long backgroundStart = SystemClock.elapsedRealtime();
        SystemClock.sleep(25000);
        long backgroundMs = SystemClock.elapsedRealtime() - backgroundStart;
        shell("am start -W -n " + APP + "/.MainActivity");
        click("PAUSE");
        int returnedPercent = snapshot("05-background-return").getInt("progressPercent");
        result.put("backgroundWaitMs", backgroundMs);
        check(backgroundMs >= 25000 && returnedPercent < 100, "25 seconds in background do not finish the 22-second scan");
        // Includes the foreground time spent scrolling to Pause before/after Home.
        check(returnedPercent >= resumedPercent && returnedPercent - resumedPercent <= 20,
                "Background interval is excluded from progress (at most 4.4 seconds foreground overhead)");
        check(has("LOKALES THEATER / SITZUNG 1"), "Background return preserves the scan session");

        click("WEITER"); SystemClock.sleep(23000);
        click("MEINEN FAKE-BERICHT ANSEHEN →");
        snapshot("06-report");
        check(has("WIR HABEN\nNICHTS GEGEN DICH."), "Resumed scan completes and opens its report");
        click("NEUER SCAN"); click("PAUSE");
        JSONObject restarted = snapshot("07-restarted");
        check(has("LOKALES THEATER / SITZUNG 2") && restarted.getInt("progressPercent") < 20,
                "New scan exits the report and restarts progress in session 2");
        check(has("SPIONAGE MACHT PAUSE."), "Restarted scan still responds to Pause");
    }

    private String shellRaw(String command) throws Exception {
        try (ParcelFileDescriptor descriptor = ui.executeShellCommand(command);
             ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096]; int length;
            while ((length = input.read(data)) != -1) output.write(data, 0, length);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
    private String shell(String command) throws Exception {
        if (!"1".equals(shellRaw("getprop ro.kernel.qemu").trim()))
            throw new IllegalStateException("Emulator guard failed");
        return shellRaw(command);
    }
    private AccessibilityNodeInfo root() throws Exception {
        for (int attempt = 0; attempt < 40; attempt++) {
            AccessibilityNodeInfo root = ui.getRootInActiveWindow();
            if (root != null && APP.contentEquals(root.getPackageName())) return root;
            SystemClock.sleep(50);
        }
        throw new IllegalStateException("Expected Maximum Spyware foreground window");
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo node, String text) {
        if (node.getText() != null && text.contentEquals(node.getText())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) { AccessibilityNodeInfo match = find(child, text); if (match != null) return match; }
        }
        return null;
    }
    private AccessibilityNodeInfo scrollView(AccessibilityNodeInfo node) {
        if (node.isScrollable()) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) { AccessibilityNodeInfo match = scrollView(child); if (match != null) return match; }
        }
        return null;
    }
    private boolean scroll(boolean forward) throws Exception {
        AccessibilityNodeInfo view = scrollView(root());
        boolean moved = view != null && view.performAction(forward
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        SystemClock.sleep(160); return moved;
    }
    private void top() throws Exception { for (int i = 0; i < 16 && scroll(false); i++) { } }
    private boolean has(String text) throws Exception { return find(root(), text) != null; }
    private void click(String text) throws Exception {
        for (int attempt = 0; attempt < 16; attempt++) {
            AccessibilityNodeInfo match = find(root(), text);
            if (match != null && match.isVisibleToUser() && match.isClickable()
                    && match.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { SystemClock.sleep(100); return; }
            if (!scroll(true)) SystemClock.sleep(100);
        }
        throw new IllegalStateException("Button not reachable: " + text);
    }
    private void collect(AccessibilityNodeInfo node, JSONArray nodes) throws Exception {
        if (node.getText() != null) {
            JSONObject item = new JSONObject(); item.put("text", node.getText().toString());
            item.put("visible", node.isVisibleToUser()); nodes.put(item);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i); if (child != null) collect(child, nodes);
        }
    }
    private JSONObject snapshot(String name) throws Exception {
        top(); JSONArray nodes = new JSONArray(); collect(root(), nodes);
        JSONObject item = new JSONObject(); item.put("name", name);
        item.put("elapsedRealtimeMs", SystemClock.elapsedRealtime()); item.put("nodes", nodes);
        // The progress readout precedes the separate Drama percentage in the hierarchy.
        for (int i = 0; i < nodes.length(); i++) {
            String text = nodes.getJSONObject(i).getString("text");
            if (text.matches("[0-9]+%")) { item.put("progressPercent", Integer.parseInt(text.replace("%", ""))); break; }
        }
        if (!item.has("progressPercent")) throw new IllegalStateException("No scan percentage visible: " + name);
        snapshots.put(item); root();
        Bitmap image = ui.takeScreenshot();
        if (image == null) throw new IllegalStateException("No emulator screenshot");
        item.put("screenWidth", image.getWidth()); item.put("screenHeight", image.getHeight());
        try (FileOutputStream out = new FileOutputStream(new File(evidence, name + ".png"))) {
            if (!image.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IllegalStateException("Screenshot save failed");
        } finally { image.recycle(); }
        return item;
    }
    private void check(boolean ok, String message) throws Exception {
        JSONObject item = new JSONObject(); item.put("passed", ok); item.put("description", message); checks.put(item);
        Bundle status = new Bundle(); status.putString("check", (ok ? "PASS: " : "FAIL: ") + message); sendStatus(0, status);
        if (!ok) throw new IllegalStateException(message);
    }
    private void write(File file, String text) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
    }
}
