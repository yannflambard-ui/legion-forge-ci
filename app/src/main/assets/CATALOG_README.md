# Catalog data notes

`catalog.json` is schema version 1 and its `cards` array follows the app `CardDefinition` shape. The importer can ignore the extra top-level `metadata` object.

## Coverage

- Legion v2: 127 legacy profiles with an explicit points value (15 of 142 source profiles without points omitted), plus 181 LegionHQ2 units and 439 LegionHQ2 upgrades. The normalized catalog now has 308 unit profiles and 439 upgrades. Source-reported zero point values are retained.
- Armada v1.5: 350 cards total — 46 ships, 76 squadrons, 204 upgrades, and 24 commander cards. The donor contains 228 upgrade-category cards, of which 24 are commander cards.
- No artwork or rules/ability text is copied. Image fields and rulesText are null.

## Sources and cautions

Full upstream URLs, local snapshot commit hashes, selected fields, and caveats are embedded in `catalog.json` under `metadata.sources` and `metadata.cautions`. The Armada donor repository has an MIT LICENSE, but its README separately notes Lucasfilm copyright/trademark in card text and images. The Legion donor clone had no LICENSE file in the inspected checkout; its redistribution rights are unverified. The legacy Legion feed is a snapshot dated 2026-05-25; the LegionHQ2 page snapshot is dated 2026-09-14 and its embedded card history runs through 2026-06-19; Armada is a historical snapshot dated 2020-03-03. LegionHQ2 terms and Atomic Mass Games/Lucasfilm data rights do not establish redistribution permission. This catalog and APK are for personal testing only; do not publish or redistribute without the necessary permissions or independently licensed replacement data. Do not treat this catalog as current official points/rules. Verify current points, errata, and faction legality independently.

Unsupported Legion/Armada slot labels are normalized to the app enum `OTHER` because `CardDefinition.ArmadaSlot` has no dedicated enum member for those labels. The legacy Legion source does not reliably specify uniqueness, so its `unique: false` values are neutral defaults; they do not assert the units are non-unique. LegionHQ2 `isUnique` values are preserved for its imported unit and upgrade records. LegionHQ2 upgrades without a faction value use `factionId: "neutral"`. Only its 181 unit and 439 upgrade records are imported; command, counterpart, and battle records are excluded. One Legion record with no faction value uses its source path (`rebel/...`) to infer `factionId: "rebel"`.
