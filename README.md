# BanKa

Application Android native (Kotlin + Jetpack Compose, **Material 3 Expressive**) qui affiche tes
informations bancaires dans des dashboards stylisés. Aucun chiffrement bancaire requis : tu fournis
tes exports CSV, l'application fait le reste — 100 % hors ligne, aucune donnée envoyée sur internet.

## Fonctionnalités

- **Comptes illimités** : crée autant de comptes liés que tu veux (courant, épargne, livret…),
  chacun avec son icône et sa couleur. Chaque compte a **sa propre sauvegarde** :
  - sa base d'opérations isolée (Room),
  - son **dossier de fichiers CSV** importés (`files/accounts/<id>/`),
  - son historique de fichiers consultable et supprimable opérations par opérations.
- **Import CSV intelligent** (bouton +) :
  - détection automatique du séparateur (`;` `,` tabulation `|`),
  - détection automatique des colonnes (Date / Libellé / Montant / Débit / Crédit),
  - en-tête français ou anglais, ou aucune en-tête (colonnes déduites du contenu),
  - montants `1 234,56` / `1,234.56` / `(45,20)` / négatifs à droite, symbole €,
  - dates `31/12/2025`, `2025-12-31`, `31.12.2025`, etc.
  - encodage UTF-8 ou Windows-1252 détecté automatiquement,
  - **doublons ignorés** en cas d'import répété du même fichier,
  - aperçu avant confirmation (séparateur, colonnes, premières opérations).
- **Dashboard stylisé** :
  - solde total, revenus et dépenses du mois,
  - histogramme animé revenus/dépenses sur 6 mois,
  - donut des dépenses par catégorie, catégorisation automatique des libellés,
  - liste des opérations récentes.
- Bonus sur l'écran des comptes : **patrimoine total** sommé sur tous les comptes.

## Compilable & testé

```bash
./gradlew :app:testDebugUnitTest   # tests unitaires du parser CSV
./gradlew :app:assembleDebug       # APK debug
```

Ouvre aussi simplement le dossier dans Android Studio (Ladybug+) et lance sur un appareil Android 8+.

## Formats acceptés

Voir [`samples/`] : export type Crédit Agricole (`;` avec Débit/Crédit) et type Boursorama (`,`).

Convention de signe : les sorties d'argent sont négatives. Une colonne « Débit » est comptée
négative et « Crédit » positive automatiquement.

## Structure

- `app/src/main/java/com/banka/app/data/csv` — moteur CSV (détection, parsing, sauvegarde)
- `app/src/main/java/com/banka/app/data/db` — Room (comptes, opérations, DAOs)
- `app/src/main/java/com/banka/app/domain` — statistiques et catégorisation
- `app/src/main/java/com/banka/app/ui` — thème Material 3 Expressive et écrans Compose
