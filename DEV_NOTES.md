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
- Sources : `assets/catalog.json` (1032 cartes : 624 LEGION_V2 + 408 ARMADA_V15). Attributions dans `assets/CATALOG_SOURCES.md`.
- **Images de cartes (miniatures + mode partie)** : les cartes Armada ont des images (vraies cartes) depuis le dataset Ryan Kingston (`armada.ryankingston.com/img/cards/`), converties en WebP dans `assets/cards/` et référencées par `imageAssetPath`. Legion : images partielles depuis LegionHQ (v1, ~43% unités / 46% upgrades). Fallback : nom si pas d'image. `CardArtwork` (colonne gauche builder) et `CardPlayImage` (bas des pages mode partie) les affichent. **Structure** : `assets/cards/<swa|swl>/<faction>/<nom-officiel-anglais>.webp` (variantes de même nom suffixées par les points). Scripts : `enrich_armada_images.py` (génère) + `rename_card_images.py` (structure dossiers) + `fetch_ryk_images.py` (télécharge).
- **Factions Armada complètes** : le catalogue contient désormais les 4 factions (Empire, Rebel, Republic, Separatist) — vaisseaux/escadrons/commandants Republic+Sep ajoutés depuis `armada_bsdata_catalog.json` (stats shipStats incluses). `BuilderRepository.seedCatalog` force un re-seed si `armadaShipCount() < 60` (une vieille base à 46 vaisseaux sans Republic/Sep est re-seedée).
- **Stats Armada (mode partie)** : les vaisseaux/escadrons de `catalog.json` n'ont pas de stats dans rulesText. Elles sont injectées depuis `assets/armada_bsdata_catalog.json` (dataset BSData = « Armada Fleet Builder ») dans un champ JSON `shipStats` (hull, boucliers front/right/left/rear, maxSpeed) sur chaque carte, par `scripts/enrich_armada_stats.py` (match normalisé nom+faction, fuzzy : « Boba Fett (Slave I) », typo BSData « Dreadought »). Appelé en fin de `build_catalog.py` (flag `--skip-stats`). `BuilderRepository.seedCatalog` force un re-seed si des vaisseaux manquent de stats (`armadaUnitsWithoutStats()`), pas seulement si un système est vide.
- `BuilderRepository.seedCatalog` : seed si LEGION<190 OU ARMADA<40 OU des vaisseaux Armada sans stats OU <60 vaisseaux (comptage par gameSystem). Ne coupe plus sur un `cardCount()>=400` global (ça laissait une vieille DB périmée sans les cartes du bon gameSystem → "Catalogue vide" sans erreur).
- `CatalogJsonImporter` : import versionné hors-ligne (schemaVersion==1, IDs uniques, points>=0, nom non vide).
- `observeCards(system, factionId)` : `WHERE gameSystem=:system AND (factionId=:factionId OR factionId='neutral')`.
- **Référence Armada** : Rules Reference Guide 1.6.0 (janv 2025, AMG) + Errata 5.5 — https://atomicmassgames.com/swarmadadocs/. Dégâts critiques réservés aux vaisseaux (squadrons ne peuvent ni résoudre ni subir de critiques). Contraintes flotte : ≤1/3 pts en escadrons (arrondi sup), pas d'upgrade dupliquée sur un même vaisseau, 1 seul commandant.
- **PIÈGE Gson+Kotlin (cause racine du catalogue vide)** : Gson n'applique PAS les valeurs par défaut Kotlin. Un champ absent du JSON reste `null` (défaut JVM), pas `emptyMap()`. `CardDefinition.names` était `= emptyMap()` mais Gson le laissait null → `toJsonNames` faisait `names.isEmpty()` → NPE sur les 974 cartes → 0 mappées → catalogue vide (LEGION + Armada). Fix : `toJsonNames(names: Map<String,String>?)` avec null-check. Toujours null-checker les champs à défaut Kotlin après un `Gson().fromJson`.

## DB
- Room version 6 (`legionforge.db`), `fallbackToDestructiveMigration`, migrations 1→2 (catalog_cards), 2→3 (chosenSlot), 3→4 (names), 5→6 (shipStats) puis destructif au-delà.
- DAO polymorphe `PolymorphicGameDao` gère Legion V2 + Armada V1.5.

## App
- Kotlin/Compose, minSdk 26, targetSdk 34, JDK 17, Room 2.6.1, Gson 2.11.
- GameSystem : `LEGION_V2`, `ARMADA_V15`. CardKind : LEGION_UNIT/UPGRADE, ARMADA_SHIP/SQUADRON/UPGRADE, COMMANDER.
- Mode jeu : swipe cartes, blessures/boucliers directionnels, jetons défense vert/rouge, dégâts critiques, round tracker, activation upgrades, localisation FR/DE/ES.