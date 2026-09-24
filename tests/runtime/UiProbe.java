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

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle bundle = new Bundle(); int exit = 1;
        try {
            ui = getUiAutomation();
            // A second guard protects against someone launching the helper directly.
            if (!"1".equals(shellRaw("getprop ro.kernel.qemu").trim()))
                throw new IllegalStateException("Refusing to operate outside an Android emulator");
            evidence = new File(getContext().getExternalFilesDir(null), "lifecycle");
            if (!evidence.isDirectory() && !evidence.mkdirs()) throw new IllegalStateException("No evidence directory");
            result.put("api", android.os.Build.VERSION.SDK_INT);
            result.put("test", "maximum-spyware black-box lifecycle");
            runScenario(); result.put("status", "passed"); exit = 0;
        } catch (Exception error) {
            try { result.put("status", "failed"); result.put("error", error.toString()); result.put("stack", android.util.Log.getStackTraceString(error)); }
            catch (Exception ignored) { }
        } finally {
            try {
                result.put("checks", checks); result.put("snapshots", snapshots);
                if (evidence != null) write(new File(evidence, "lifecycle-results.json"), result.toString(2));
                bundle.putString("result", result.toString());
            } catch (Exception error) { bundle.putString("error", error.toString()); exit = 1; }
            finish(exit, bundle);
        }
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
