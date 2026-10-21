package de.leterel.ads;

/** Pure lifecycle/request gate. All calls belong to the UI thread. */
public final class ConsentGate {
    private boolean active, resultReady, consent, destroyed, loading;
    private int generation;
    public void resume() { if (!destroyed) active = true; }
    public void pause() { active = false; invalidate(); }
    public void destroy() { destroyed = true; pause(); }
    public void result(boolean ready) {
        if (resultReady != ready) { resultReady = ready; invalidate(); }
    }
    public void consent(boolean allowed) { consent = allowed; if (!allowed) invalidate(); }
    public void invalidate() { generation++; loading = false; }
    public int token() { return generation; }
    public boolean current(int token) { return token == generation && active && resultReady && !destroyed; }
    public boolean visible() { return active && resultReady && !destroyed; }
    public boolean allowed() { return visible() && consent; }
    public boolean beginRequest() {
        if (!allowed() || loading) return false;
        loading = true; return true;
    }
}
