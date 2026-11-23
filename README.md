# Like Hero To Zero

Webprototyp für fossile CO₂-Jahreswerte. Die Länderauswahl zeigt den jüngsten verfügbaren Eintrag pro Land oder Gebiet. Das Forschungskonto kann Jahreswerte ergänzen und korrigieren.

## Lokal starten

Voraussetzungen sind ein Java-21-JDK und eine Internetverbindung für den ersten Maven-Build. `java -version` zeigt die aktive Version. Der Maven Wrapper ist im Repository enthalten.

Verwende die CSV `GCB2025v15_MtCO2_flat.csv` von Andrew & Peters (2025), Version 2025v15. Im IU-Arbeitsordner verwendet die Anwendung standardmäßig die Datei aus dem benachbarten Verzeichnis `aufgabenunterlagen`. Für einen separaten Checkout lade die [CSV von Zenodo](https://zenodo.org/records/17417124/files/GCB2025v15_MtCO2_flat.csv?download=1) in den lokalen Ordner `data`.

```sh
export EMISSIONS_CSV="$PWD/data/GCB2025v15_MtCO2_flat.csv"
```

Starte im Projektordner. Der folgende Befehl fragt das lokale Testpasswort im Terminal ab, ohne es anzuzeigen.

```sh
printf 'Passwort für das Forschungskonto: '
read -r -s RESEARCH_PASSWORD
printf '\n'
export RESEARCH_PASSWORD
sh ./mvnw spring-boot:run
```

Öffne [http://127.0.0.1:8080/](http://127.0.0.1:8080/). Der Benutzername ist `forschung`. `RESEARCH_USERNAME` kann ihn ändern. Die öffentliche Ansicht funktioniert auch ohne konfiguriertes Passwort. Unter Windows kann der Maven Wrapper als `mvnw.cmd` gestartet werden.

## Daten und Ablauf

- In der Startansicht ein Land auswählen und **Wert anzeigen** wählen.
- Über **Forschungslogin** anmelden. In der Datenpflege einen neuen Wert erfassen oder einen vorhandenen Datensatz über Land und Bezugsjahr öffnen.
- Der Import speichert `Country`, den Ländercode aus `ISO 3166-1 alpha-3`, `Year` und `Total` in Mt CO₂. Leere Gesamtwerte sowie Welt- und internationale Transportaggregate werden übersprungen. [Datensatz und Metadaten](https://doi.org/10.5281/zenodo.17417124)
- Die H2-Datenbank liegt standardmäßig in `data/emissions.mv.db`. Der Anfangsimport betrifft eine leere Datenbank. Beim Neustart lädt die Anwendung die bestehende Datei einschließlich lokaler Änderungen. Über `EMISSIONS_DB_URL`, etwa `jdbc:h2:file:./data/demo`, lässt sich für eine separate Demonstration eine zweite Datenbank verwenden.

## Tests und ausführbares JAR

```sh
sh ./mvnw test
sh ./mvnw package
java -jar target/like-hero-to-zero-1.0.0.jar
```

Die Tests verwenden eine kleine CSV und eine separate In-Memory-Datenbank. Sie prüfen den neuesten verfügbaren Jahreswert, das Filtern von Aggregaten, Anmeldung und Rolle, das Anlegen und Korrigieren sowie ungültige, doppelte und veraltete Eingaben.
