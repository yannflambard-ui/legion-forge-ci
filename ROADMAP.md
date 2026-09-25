# Legion Forge — Roadmap produit

Application compagnon pour **Star Wars: Legion V2** et **Star Wars: Armada**.
Objectif : devenir indispensable **à côté de la table** (trouver une info en 2 secondes, construire une liste honnête, suivre une partie sans PDF ni mémorisation).

> Double filtre appliqué à chaque feature : **utilité réelle pour un joueur** PUIS **intégration propre** dans l'architecture existante (Compose, Room, catalogue JSON v2, mode partie par pager).
> Principe : **faire évoluer Legion Forge, pas reconstruire Legion Forge.**

---

## Priorités

- 🔴 **P0 — Essentiel** : forte valeur + cohérent avec l'existant
- 🟠 **P1 — Important** : vraie valeur, non essentiel
- 🟡 **P2 — Intéressant** : plus tard
- ⚪ **Backlog** : idées à reconsidérer
- ❌ **Rejeté** : complexité > valeur

---

## 🔴 P0 — Essentiel

### [ ] Glossaire / mots-clés cliquables
```
Jeu : Legion + Armada (Commun)
Priorité : P0 | Valeur joueur : Très haute | Complexité : Faible
État actuel : legionStats.keywords rempli dans le JSON v2 (173 unités), mais affiché en texte brut non-cliquable. Le dataset DoctorDizzee a keywords.json avec descriptions.
Écrans : LegionUnitPage (profil), ArmyBuilder, fiche carte
Données : legionStats.keywords + définitions de mots-clés

Pourquoi : l'info n°1 cherchée pendant une partie (« c'est quoi Combat X ? »). Aujourd'hui un mot-clé = string inerte.
Intégration : seed des définitions de mots-clés (pas forcément une nouvelle table — voir note architecture data), mots-clés rendus cliquables → bottom sheet glossaire. Réutiliser le pattern de fiche existant.
```

### [ ] Recherche globale dans le texte des cartes
```
Jeu : Commun
Priorité : P0 | Valeur joueur : Haute | Complexité : Moyenne
État actuel : recherche par nom seulement (filtre par faction).
Écrans : HomeScreen / barre d'ajout
Données : catalog_cards (name, rulesText, legionStats)

Pourquoi : « quelle unité/amélioration a Pierce 2 ? », « quel vaisseau a tel trait ? ».
Intégration : recherche sur name + rulesText + legionStats (LIKE ou FTS5 selon volume). Champ en haut → HomeScreen + barre d'ajout.
```

### [ ] Filtres de catalogue enrichis
```
Jeu : Commun
Priorité : P0 | Valeur joueur : Haute | Complexité : Faible-moyenne
État actuel : filtre faction + pré-filtre scoped upgrades (0.4.2). legionStats rempli.
Écrans : FactionPicker → liste, ArmyBuilder
Données : legionStats (keywords), rank, points, faction

Pourquoi : « les corps ≤80 pts avec Armor » avant/après sélection.
Intégration : chips (rang, tranche de points, mot-clé, slot) ; réutiliser le filtre mémoire + matchesFaction.
```

### [ ] Historique + favoris
```
Jeu : Commun
Priorité : P0 | Valeur joueur : Moyenne-haute | Complexité : Faible
État actuel : aucun.
Écrans : HomeScreen (récents + favoris)
Données : consultation (cardId, ts) + favoris (cardId)

Pourquoi : revenir vite vers une carte déjà
Intégration : intercepter l'ouverture de la fiche → journal ; chips Récents/Favoris. Risque quasi nul.
```

---

## 🟠 P1 — Important

### [ ] Collection personnelle (extensions / unités possédées)
```
Jeu : Commun
Priorité : P1 | Valeur joueur : Haute
Données : collection (cardId) + filtre « ma collection » (pattern matchesFaction)
Pourquoi : construire une liste honnête = ce qu'on possède vraiment.
```

### [ ] Mode partie Legion enrichi (jetons v2 + états auto)
```
Jeu : Legion
Priorité : P1 | Valeur : haute (tournoi / partie)
Base déjà refaite en 0.5.0 (PV réels, profil, armes, surges). À ajouter (référence LITKO 2024 Refresh) :
- jetons Aim / Dodge / Standby / Surge
- Suppression : état Supprimé (≥ courage) / Paniqué (≥ 2×courage) — auto-calcul depuis legionStats.courage
- Vehicle Damage (résilience) / Ion Damage (véhicules + droïdes)
Écrans : LegionUnitPage, TokenSection (généraliser)
```
> Killer feature Legion : ne plus rien mémoriser à la table.

### [ ] Comparateur (unité vs unité / vaisseau vs vaisseau)
```
Jeu : Commun
Priorité : P1 | Complexité : Moyenne
Écrans : écran comparaison
Données : legionStats / shipStats (déjà structurés)
Intégration : réutiliser StatChip.
```

### [ ] Séquence de tour (checklist activable) Legion / Armada
```
Jeu : Commun (déclinaison par jeu)
Priorité : P1
Écrans : overlay dans mode partie
Pourquoi : rappel des phases sans PDF.
```

---

## 🟡 P2 — Intéressant

- **Partage de liste** : export texte + QR code / deep link.
- **Import/export de listes** (JSON versionné, pattern CatalogJsonImporter réutilisable).
- **Changements / errata** : le bundle LegionHQ V2 a un champ `history` (dates + points/màj) → « Quoi de neuf ».
- **Recherche plein-texte FTS5** quand le catalogue grossit.
- **Versions de cartes** via `history`.

---

## ⚪ Backlog

- **Command cards v2** (125 dans le bundle) — pendule de commandement lié au round tracker.
- **Mode partie Armada avancé** (séquence commande, arcs, portée).
- **Scanner de cartes Android** (rapport valeur/complexité à re-évaluer).

---

## ❌ Rejeté (double filtre joueur × concepteur)

- **Scanner de cartes** : CameraX + matcher visuel pour 666 cartes = coût élevé, gain marginal (recherche par nom suffit).
- **Dice roller** : le jeu a ses dés physiques ; sans valeur de référence.
- **Compte cloud / multi-joueurs / profil** : hors périmètre "compagnon de table", ajoute auth+serveur pour rien (app offline-first).
- **KMP/iOS / reconstruction architecture** : casserait l'existant stable.

---

## 🚀 Killer features — copilote de table

1. **Glossaire mots-clés cliquables** (P0) — le geste le plus fréquent en jeu, résolu en 2 s, unifie Legion+Armada.
2. **Recherche globale dans le texte** (P0) — « laquelle a Pierce 2 » instantané, impossible dans un PDF.
3. **Mode partie Legion intelligent** (P1) — états auto-calculés (Supprimé/Paniqué, Aim/Dodge/Standby, Vehicle/Ion).
4. **Collection** (P1) — liste honnête selon ce qu'on possède.

Ensemble ils transforment l'app d'un *catalogue* en *copilote de table*, sur des données (legionStats, shipStats, keywords) **déjà structurées** en V2, sans nouvelle architecture.

---

## Architecture data — décision en cours

⚠️ Pour toute feature qui demande une **base de données dédiée / sync** (collection multi-appareils, favoris cloud, historique partagé), ne pas se lancer sur une migration lourde pour l'instant :
- Solution pressentie : **MongoDB Atlas (niveau gratuit)** — un compte existe. Les favoris/collection/historique iraient naturellement dans des collections Atlas, pas dans des tables Room locales dédiées.
- En attendant, privilégier les implémentations **sans nouvelle brique data** (glossaire seedé, recherche sur catalog_cards existant, états en mémoire du mode partie).
- À re-évaluer quand on touche au multi-appareils / collection.

---

*Roadmap basée sur l'état du projet au 25/09/2026 — catalogue Legion V2 (0.5.x) + Armada enrichie.*
