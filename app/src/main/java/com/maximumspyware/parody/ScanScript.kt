package com.maximumspyware.parody

/** A script, not a scanner. No device, account, sensor, storage or network APIs. */
object ScanScript {
    const val DURATION_MS = 22_000
    const val TICK_MS = 100
    data class Entry(val atMs: Int, val message: String)
    data class Finding(val label: String, val value: String, val description: String)

    val entries = listOf(
        Entry(0, "SIMULATION initialisiert. Datenquelle: Fantasie."),
        Entry(1200, "Imaginäre Firewall mit bösem Blick geöffnet."),
        Entry(2600, "Gedankenleser gestartet … Gedanken nicht gefunden."),
        Entry(4100, "Kühlschrank-Telemetrie: 3 verdächtige Käsereste."),
        Entry(5800, "487 frei erfundene Kontakte monetarisiert."),
        Entry(7300, "Standort ermittelt: irgendwo im Universum."),
        Entry(9000, "Geheime Meme-Reserve: kritisch unterversorgt."),
        Entry(10600, "Browserprofil gewürfelt: maximal fragwürdig."),
        Entry(12300, "Datenverkauf vorbereitet. Käufer: niemand."),
        Entry(14100, "0 Bytes übertragen. Dramatisch genug getan."),
        Entry(15900, "Satelliten auf imaginären Gamingstuhl ausgerichtet."),
        Entry(17700, "Bedrohungslevel: Bruder, geh mal schlafen."),
        Entry(19800, "Bericht frei erfunden. Wirklichkeit unberührt."),
        Entry(DURATION_MS, "FERTIG. Alles Fake. Du gehörst weiterhin dir."),
    )

    val findings = listOf(
        Finding("SOZIALES KAPITAL", "487 Kontakte", "Komplett erfunden. Keiner wurde gelesen."),
        Finding("AKTUELLER STANDORT", "Planet Erde™", "Zufällig richtig? Wir haben nicht nachgesehen."),
        Finding("GEDANKENPROFIL", "Nur Memes", "Gedankenleser leider aus Pappe."),
        Finding("DEIN MARKTWERT", "0,0042 €", "Fantasiepreis. Keine Transaktion. Kein Käufer."),
        Finding("ENTWICKLERGEWINN", "9.999.999 €*", "*In Monopoly-Geld. Finanzamt entspannt."),
    )

    fun progress(elapsedMs: Int): Float = elapsedMs.coerceIn(0, DURATION_MS).toFloat() / DURATION_MS
    fun visibleEntries(elapsedMs: Int): List<Entry> = entries.filter { it.atMs <= elapsedMs.coerceAtLeast(0) }
    fun complete(elapsedMs: Int): Boolean = elapsedMs >= DURATION_MS
    fun stage(elapsedMs: Int): String = when {
        elapsedMs >= DURATION_MS -> "DU BIST ENTSPIONIERT."
        elapsedMs < 5800 -> "DEINE AURA WIRD GELEAKT."
        elapsedMs < 12300 -> "WIR WISSEN GAR NICHTS."
        elapsedMs < 17700 -> "DATENVERKAUF AN NIEMANDEN."
        else -> "LETZTE PEINLICHKEITEN SUCHEN."
    }
}
