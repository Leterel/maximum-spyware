package de.leterel.ads;

public final class ConsentGateTest {
    private static int checks;
    private static void check(boolean expected) { checks++; if (!expected) throw new AssertionError("Check " + checks); }
    public static void main(String[] args) {
        ConsentGate gate = new ConsentGate();
        check(!gate.beginRequest());
        gate.resume(); gate.consent(true);
        check(!gate.beginRequest()); // No result yet, even if old consent exists.
        gate.result(true); int first = gate.token();
        check(gate.current(first)); check(gate.beginRequest());
        gate.result(true); gate.resume();
        check(!gate.beginRequest()); // Repeated UI refresh/resume never duplicates.
        gate.pause(); check(!gate.current(first)); check(!gate.beginRequest());
        gate.resume(); check(!gate.current(first)); check(gate.beginRequest());
        int resumed = gate.token();
        gate.consent(false); check(!gate.current(resumed)); check(!gate.beginRequest());
        gate.consent(true); check(gate.beginRequest());
        int previousScan = gate.token();
        gate.result(false); check(!gate.current(previousScan)); check(!gate.beginRequest());
        gate.result(true); check(gate.beginRequest());
        int beforeDestroy = gate.token(); gate.destroy(); gate.resume(); gate.result(true); gate.consent(true);
        check(!gate.current(beforeDestroy)); check(!gate.visible()); check(!gate.beginRequest());
        System.out.println(checks + " consent/lifecycle gate checks passed.");
    }
}
