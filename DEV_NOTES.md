# Legion Forge — Notes de développement

## Build & CI
- **TOUJOURS build par GitHub Actions** (workflow `.github/workflows/build.yml`), jamais en local.
- Le serveur Debian (8 Go RAM, gradle Xmx512m, workers=1) n'a **pas assez de mémoire** pour compiler : `Not enough memory to run compilation` (ksp/Room/Compose dépassent 512m). Inutile d'essayer.
- Workflow : `assembleDebug` + `testDebugUnitTest` sur ubuntu-latest (16 Go) → artifact `legion-forge-debug/app-debug.apk`.
- Boucle de livraison : push `master` → `gh run watch` → `gh run download -n legion-forge-debug` → upload APK sur Google Drive.
- APK Drive (même lien/fichier, renommé au fil des versions) :
  https://drive.google.com/file/d/1ep1Sr7eDoONqJx6jGfim6dfamurggY0-/view?usp=drivesdk

## Reporting remote des erreurs (diagnostic)
- L'app poste un issue GitHub quand le catalogue est vide **sans exception** (`CrashReporter.reportEvent`), via `POST /api.github.com/repos/yannflambard-ui/legion-forge-ci/issues`.
- **Crucial** : l'API GitHub exige une authentification. Sans token → HTTP 401 → `reportEvent` avale en silence → **rien ne remonte**. Le token `Authorization: Bearer` est obligatoire.
- Token fourni par `BuildConfig.GITHUB_TOKEN` :
  - CI : injecté par le workflow via `permissions: issues: write` + step `Inject GitHub token` (`GITHUB_TOKEN` natif → `app/github_token.properties`). Jamais en clair dans le repo.
  - Local : `app/github_token.properties` (gitignored, `legionforge_github_token=...`) → fichier absent du VCS.
- Le token fin-grained de secours (demo) doit avoir scope `issues: write` sur le repo UNIQUEMENT (pas `repo` global).
- Déduplication : `reportEvent` ignore si un issue de même titre est déjà open (search issues).

## Catalogue (seed)
- Sources : `assets/catalog.json` (974 cartes : 624 LEGION_V2 + 350 ARMADA_V15). Attributions dans `assets/CATALOG_SOURCES.md`.
- `BuilderRepository.seedCatalog` : seed si LEGION<190 OU ARMADA<40 (comptage par gameSystem). Ne coupe plus sur un `cardCount()>=400` global (ça laissait une vieille DB périmée sans les cartes du bon gameSystem → "Catalogue vide" sans erreur).
- `CatalogJsonImporter` : import versionné hors-ligne (schemaVersion==1, IDs uniques, points>=0, nom non vide).
- `observeCards(system, factionId)` : `WHERE gameSystem=:system AND (factionId=:factionId OR factionId='neutral')`.
- **Référence Armada** : Rules Reference Guide 1.6.0 (janv 2025, AMG) + Errata 5.5 — https://atomicmassgames.com/swarmadadocs/. Dégâts critiques réservés aux vaisseaux (squadrons ne peuvent ni résoudre ni subir de critiques). Contraintes flotte : ≤1/3 pts en escadrons (arrondi sup), pas d'upgrade dupliquée sur un même vaisseau, 1 seul commandant.
- **PIÈGE Gson+Kotlin (cause racine du catalogue vide)** : Gson n'applique PAS les valeurs par défaut Kotlin. Un champ absent du JSON reste `null` (défaut JVM), pas `emptyMap()`. `CardDefinition.names` était `= emptyMap()` mais Gson le laissait null → `toJsonNames` faisait `names.isEmpty()` → NPE sur les 974 cartes → 0 mappées → catalogue vide (LEGION + Armada). Fix : `toJsonNames(names: Map<String,String>?)` avec null-check. Toujours null-checker les champs à défaut Kotlin après un `Gson().fromJson`.

## DB
- Room version 4 (`legionforge.db`), `fallbackToDestructiveMigration`, migrations 1→2 (catalog_cards), 2→3 (chosenSlot), 3→4 (names) puis destructif au-delà.
- DAO polymorphe `PolymorphicGameDao` gère Legion V2 + Armada V1.5.

## App
- Kotlin/Compose, minSdk 26, targetSdk 34, JDK 17, Room 2.6.1, Gson 2.11.
- GameSystem : `LEGION_V2`, `ARMADA_V15`. CardKind : LEGION_UNIT/UPGRADE, ARMADA_SHIP/SQUADRON/UPGRADE, COMMANDER.
- Mode jeu : swipe cartes, blessures/boucliers directionnels, jetons défense vert/rouge, dégâts critiques, round tracker, activation upgrades, localisation FR/DE/ES.