# PizzaOrder

Bestell-App für Pizza-Abende: Gäste stellen am Tablet ihre Pizza zusammen, die Bestellung
wird als Bon auf einem Bluetooth-Thermodrucker ausgedruckt und erscheint in der Bestellliste.

<!-- TODO: Screenshots -->

## Drucker

Unterstützt werden ESC/POS-Thermodrucker mit 58 mm Papier (die üblichen günstigen Bluetooth-Bondrucker).

1. Drucker in den Android-Bluetooth-Einstellungen mit dem Tablet koppeln.
2. App starten und die Berechtigung „Geräte in der Nähe“ erlauben.

Die App druckt auf dem **ersten gekoppelten** Gerät, also am besten nur einen Drucker koppeln.

## Admin-Panel

Über das Admin-Symbol oben rechts lässt sich das Admin-Panel öffnen. Entsperrt wird es mit einem „P“
als Muster (Punkte 4 → 5 → 2 → 1 → 7). Dort lassen sich Menü (Saucen, Beläge, Pizzen), Ausdruck-Design
und Links bearbeiten sowie ein Testdruck auslösen.

Die Sperre soll nur Gäste fernhalten und ist keine echte Sicherheit.

## Bauen

Android Studio öffnen, `app` starten. Vor jedem Debug-Build laufen die Unit-Tests
(`./gradlew testDebugUnitTest`), damit ein ungültiges Standard-Menü nicht erst beim App-Start auffällt.

Das Standard-Menü steht in `MenuConfig.kt` (`MenuConfig.Standard`).

## Releases

Commit-Messages folgen [Conventional Commits](https://www.conventionalcommits.org/de/). Bei jedem Push auf
`main` baut und testet GitHub Actions die App; `feat:`/`fix:`-Commits erzeugen per semantic-release eine
neue Version mit `CHANGELOG.md`-Eintrag und einem GitHub Release samt signierter APK. Die Version in
`app/build.gradle.kts` setzt semantic-release, nicht von Hand ändern.

Signiert wird mit dem Release-Keystore aus den Repo-Secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS` und `KEY_PASSWORD`. Lokale Builds sind davon nicht betroffen.

## Lizenz

[MIT](LICENSE)
