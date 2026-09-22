package de.leterel.prototypeqa;

import android.app.Instrumentation;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import org.json.JSONArray;
import org.json.JSONObject;

/** Emulator-only UI probe: avoids UiAutomator's idle wait during the animated scan. */
public final class UiProbe extends Instrumentation {
    private Bundle arguments;
    @Override public void onCreate(Bundle args) { arguments = args; start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            AccessibilityNodeInfo root = null;
            for (int i = 0; i < 20 && root == null; i++) {
                root = getUiAutomation().getRootInActiveWindow();
                if (root == null) SystemClock.sleep(100);
            }
            if (root == null || !"com.maximumspyware.parody".contentEquals(root.getPackageName()))
                throw new IllegalStateException("Maximum Spyware must be the foreground app");
            if ("click".equals(arguments.getString("action"))) {
                if (!click(root, arguments.getString("text"))) throw new IllegalStateException("Button not found");
                SystemClock.sleep(100);
                root = getUiAutomation().getRootInActiveWindow();
            }
            JSONArray nodes = new JSONArray(); collect(root, nodes);
            result.putString("nodes", nodes.toString()); finish(0, result);
        } catch (Exception error) { result.putString("error", error.toString()); finish(1, result); }
    }
    private boolean click(AccessibilityNodeInfo node, String text) {
        if (text != null && text.contentEquals(node.getText()) && node.isClickable())
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && click(child, text)) return true;
        }
        return false;
    }
    private void collect(AccessibilityNodeInfo node, JSONArray nodes) throws Exception {
        if (node == null) return;
        if (node.getText() != null) {
            JSONObject entry = new JSONObject(); entry.put("text", node.getText().toString());
            entry.put("visible", node.isVisibleToUser()); nodes.put(entry);
        }
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), nodes);
    }
}
