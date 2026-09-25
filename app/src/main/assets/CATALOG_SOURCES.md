# Bundled catalogue provenance and limitations

This app bundles `catalog.json` for offline use. The data is a normalized catalogue, without card artwork.

- Legion: unit names, ranks, points and upgrade-slot categories were normalized from `DoctorDizzee/Star-Wars-Legion-As-Text` (`compiled/all_units.json`, plus the project's `data/points_master.json` for two named unit costs). At extraction time its README describes a machine-readable post-2.5 dataset updated May 25, 2026; no explicit LICENSE file was present in the cloned repository. Inclusion is for this local test build only; permission to redistribute is not established. 129 priced profiles included (including the two entries filled from `points_master.json`); entries with no published cost were omitted. Recheck against current Atomic Mass Games points/errata before tournament use.
- Armada: ships, squadrons, upgrade cards, costs and upgrade-slot categories were normalized from `rkbodenner/star-wars-armada-data`, which declares MIT and includes a 2020/FAQ 5.1.1 era dataset. 46 ship variants, 76 squadrons and 228 upgrade cards included (the 24 commander-slot upgrade cards are represented as commanders). This source predates the latest errata/rebalance and is NOT a verified 2026 V1.5 rules dataset; use for offline builder testing and validate legality/costs against current sources.
- Source data included names, categories, costs and slot categories only. Official card scans/illustrations, card text and image endpoints are deliberately omitted. `imageUrl` and `imageAssetPath` are null. Coil/AsyncImage supports a later licensed image pack.

No association with Lucasfilm, Disney, Atomic Mass Games, or Asmodee is implied. App version is a fan-made prototype. Catalog source data may contain game trademarks in names.
