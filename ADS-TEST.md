# Optionales Google-Testwerbeprofil

Der normale manuelle APK-Build und `build-bundle.ps1` bleiben ohne Werbe-SDK,
Consent-SDK und Berechtigungen. `build-bundle.ps1` setzt ausdrücklich
`-PadsTest=false`. Das zusätzliche Profil wird mit `-PadsTest=true`
eingeschaltet; es hat eine eigene Paketkennung mit `.adstest` und einen
sichtbaren Hinweis „Testwerbung“. Keine Produktivkennungen oder Einnahmen.

## Bauen

```powershell
./build-ads-test.ps1 -SdkPath C:\Android\Sdk `
  -JavaHome 'C:\Program Files\Eclipse Adoptium\jdk-21' `
  -GradleCache C:\Build\Gradle -AndroidUserHome C:\Build\Android `
  -OutputDirectory build\mein-neuer-testwerbe-build -Online
```

JDK 21, Android SDK/API 36 und Buildtools 36.0.0 werden benötigt. Beim ersten
Build lädt `-Online` die Google-Bibliotheken. Weitere Builds können ohne diesen
Schalter aus dem gefüllten Cache laufen. Das Skript verwendet die Debugsignatur,
prüft vor dem Export die Sample-ID-Whitelist und `.adstest`-Paketkennung und
erhält frühere Ausgabeordner. APK, Manifestinformationen und SHA256 stehen im
Ausgabeordner. Nicht als Play-Produktionspaket hochladen.

## Verhalten

- Kein Anzeigenaufruf beim Start, beim Beenden oder während des Scans.
  Nur im fertigen Ergebnis beziehungsweise geöffneten Fake-Bericht wird eine
  getrennte, klar beschriftete Bannerfläche angeboten.
- UMP aktualisiert den Datenschutzstatus und zeigt erforderliche Formulare.
  Erst nach erfolgreichem Ablauf und `canRequestAds()` wird GMA auf einem
  Hintergrundthread initialisiert. Vor jeder Anzeigenanfrage wird erneut geprüft.
  UMPs Datenschutzprüfung selbst verwendet Internetzugriff.
- Ein erforderlicher Datenschutzoptionen-Knopf bleibt neben der Anzeige
  erreichbar. Vor dem erneuten Datenschutzdialog wird das Banner entfernt;
  nachher wird die aktuelle Freigabe geprüft.
- Netz-/Formularfehler lassen das Ergebnis benutzbar und erzeugen keine
  Anzeigenanfrage. Die Netzwerkprüfung ermöglicht nach 15 Sekunden einen
  erneuten Versuch; ein bereits sichtbares Nutzerformular wird nicht abgebrochen.
- Pause, neuer Scan und Destroy entfernen/destroyen die Banneransicht und
  entwerten frühere Callbacks. Wiederholte UI-Refreshes erzeugen keine parallelen
  Anzeigenanfragen. Alte Antworten dürfen keine neue Sitzung ändern.

## IDs, Abhängigkeiten und Grenzen

GMA Next-Gen **1.5.0**, UMP **4.0.0**, Sample-App-ID
`ca-app-pub-3940256099942544~3347511713`, Banner-Test-ID
`ca-app-pub-3940256099942544/9214589741`. Die Java-/Manifestdateien unter
`app/src/adsTest` und `app/src/offline` werden alternativ kompiliert.

Das Testprofil enthält Google-SDKs und transitive Komponenten/Berechtigungen,
darunter Internet, Netzwerkstatus und WorkManager. Telefonstatus, Werbe-ID und
AdServices-ID/Topics/Attribution-Berechtigungen werden explizit entfernt.
Der automatische AndroidX-Startprovider wird ebenfalls entfernt; WorkManager
wird erst im freigegebenen Hintergrund-Initialisierungspfad eingerichtet.
Der Export bricht bei verbleibenden Identifierberechtigungen oder automatischen
Content-Providern ab. Das Google-SDK ist trotzdem kein datenfreies Produkt:
Netzwerkmetadaten und SDK-interne Verarbeitung sind dadurch nicht ausgeschlossen.
Es ist kein Offline-/Null-Daten-Profil. Der Scan greift nicht auf persönliche Inhalte oder
Überwachungsdaten zu; die Profilhinweise unterscheiden das ausdrücklich.

Die Sample-App-ID ersetzt keine eigene AdMob-Privacy-Messaging-Konfiguration.
Wenn UMP dafür kein gültiges Formular bereitstellt, bleibt das Profil ohne
Anzeigenanfrage. Ein Compile beweist keine Consentformular-/Bannerlieferung.
Eigene AdMob-/Betreiberangaben, produktive Privacy-Nachrichten,
Datenschutz-/Data-Safety-Angaben, Produktionssignierung und Storefreigabe
bleiben für eine spätere Veröffentlichung offen.

## Nachweise

21.10.2026: SDK-/UMP-Profil kompiliert und mit Test-ID-Whitelist exportiert;
normale manuelle APKs und unsigned Baseline-AABs ebenfalls gebaut.
`tests/test-consent-gate.ps1 -JavaHome ...` besteht 18 Checks für Ergebnis-/
Consent-Gating, doppelte Requests, Pause/Resume, veraltete Callbacks, neue Scans
und zerstörte Instanzen. Emulatorbelege werden getrennt dokumentiert;
kein physisches Handy verwendet.

Quellen: [GMA-Setup](https://developers.google.com/admob/android/next-gen/quick-start),
[Banner und Test-ID](https://developers.google.com/admob/android/next-gen/banner),
[UMP und Privacy Options](https://developers.google.com/admob/android/privacy).

## 23.10.2026 — reproduzierbare Emulatorprüfung

`tests/emulator-ads-failclosed.ps1` prüft das Google-Testprofil mit expliziter
Emulatorseriennummer, festem APK-SHA256 und erwarteter Android-API. Vor jedem
ADB-Schritt muss ro.kernel.qemu=1 sein. Ein vorhandenes Belegverzeichnis wird
nicht überschrieben. Rotationszustand wird anschließend wiederhergestellt.

Die aktuellen APKs bestehen auf API 34 und API 36: keine Werbung vor dem
Ergebnis/Report, benutzbares Ergebnis bei UMP-Fehler, erneuter Versuch,
Hintergrund/Rückkehr, Rotation und lebender App-Prozess ohne PID-bezogene
Fatal Exception. Die API-36-Läufe wurden am 22.10.2026 abgeschlossen.
Dies prüft ausdrücklich den Fehlerpfad der Sample-Konfiguration. Erfolgreiche
Consentformulare, Bannerlieferung, Einnahmen und Netzwerkverkehrsmessung sind
nicht nachgewiesen. Belege liegen im Chat unter outputs/android-ads-tests.
