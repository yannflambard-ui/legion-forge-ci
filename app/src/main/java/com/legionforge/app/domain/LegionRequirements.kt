package com.legionforge.app.domain

import com.legionforge.app.data.model.CardDefinition
import org.json.JSONArray
import org.json.JSONObject

/**
 * Évalue les `requirements` d'une carte upgrade Legion (stockés dans `rulesText`
 * sous forme JSON, ex. `{"requirements": ["OR", {"cardName": "Rebel Agent"}, ...]}`)
 * contre une unité cible. Retourne true si l'unité peut porter l'upgrade.
 *
 * Formes gérées (source : bundle LegionHQ V2) :
 *  - condition simple : {"cardName"|"faction"|"rank"|"cardSubtype"|"title"|"forceAffinity"|"upgradeBar"|"stats": ...}
 *  - combinateurs : ["OR", ...], ["AND", ...], ["NOT", ...]
 * Une condition inconnue / non évaluable est considérée satisfaite (ne bloque pas).
 */
object LegionRequirements {

    fun matches(requirementsJson: String?, target: CardDefinition): Boolean {
        if (requirementsJson.isNullOrBlank()) return true
        val req = try { JSONObject(requirementsJson).opt("requirements") } catch (_: Exception) { return true }
        if (req == null || req == JSONObject.NULL) return true
        return eval(req, target)
    }

    private fun eval(node: Any?, target: CardDefinition): Boolean {
        if (node is JSONArray) {
            if (node.length() == 0) return true
            val op = node.optString(0).uppercase()
            return when (op) {
                "OR" -> (1 until node.length()).any { eval(node.opt(it), target) }
                "AND" -> (1 until node.length()).all { eval(node.opt(it), target) }
                "NOT" -> !eval(node.opt(1), target)
                else -> (0 until node.length()).all { eval(node.opt(it), target) }
            }
        }
        if (node is JSONObject) {
            if (node.has("cardName")) return nameMatches(node.optString("cardName"), target)
            if (node.has("faction")) return factionMatches(node.optString("faction"), target)
            if (node.has("rank")) return rankMatches(node.optString("rank"), target)
            if (node.has("cardSubtype")) return subtypeMatches(node.optString("cardSubtype"), target)
            if (node.has("title")) return titleMatches(node.optString("title"), target)
            if (node.has("forceAffinity")) return affinityMatches(node.optString("forceAffinity"), target)
            if (node.has("upgradeBar")) return upgradeBarMatches(node.optJSONArray("upgradeBar"), target)
            if (node.has("stats")) return statsMatches(node.optJSONObject("stats"), target)
            return true // condition inconnue -> ne bloque pas
        }
        return true
    }

    // Nom de base de l'unité (sans le suffixe "(Titre)").
    private fun baseName(name: String): String {
        val i = name.indexOf('(')
        return if (i > 0) name.substring(0, i).trim() else name.trim()
    }

    private fun nameMatches(cardName: String, target: CardDefinition): Boolean {
        val req = cardName.trim().lowercase()
        return baseName(target.name).lowercase() == req || target.name.trim().lowercase() == req
    }

    private fun factionMatches(f: String, target: CardDefinition): Boolean {
        val req = f.trim().lowercase()
        val t = target.factionId.lowercase()
        return when (req) {
            "rebels", "rebel", "rebel alliance" -> t == "rebel"
            "empire", "galactic empire" -> t == "empire"
            "republic", "galactic republic" -> t == "republic"
            "separatists", "separatist", "separatist alliance" -> t == "separatists"
            "mercenary" -> t == "mercenary"
            "mandalorians" -> t == "mandalorians"
            "neutral" -> t == "neutral"
            else -> req == t
        }
    }

    private fun rankMatches(r: String, target: CardDefinition): Boolean {
        val req = r.trim().lowercase()
        val t = target.legionRank?.name?.lowercase() ?: ""
        return when (req) {
            "special" -> t == "special_forces"
            "corps" -> t == "corps"
            "commander" -> t == "commander"
            "operative" -> t == "operative"
            "support" -> t == "support"
            "heavy" -> t == "heavy"
            else -> req == t
        }
    }

    private fun subtypeMatches(s: String, target: CardDefinition): Boolean =
        target.cardSubtype?.trim()?.lowercase() == s.trim().lowercase()

    private fun titleMatches(t: String, target: CardDefinition): Boolean =
        target.title?.trim()?.lowercase() == t.trim().lowercase()

    private fun affinityMatches(a: String, target: CardDefinition): Boolean =
        target.forceAffinity?.trim()?.lowercase() == a.trim().lowercase()

    private fun upgradeBarMatches(bar: JSONArray?, target: CardDefinition): Boolean {
        if (bar == null || bar.length() == 0) return true
        val targetBar = parseUpgradeBar(target.legionStats)
        return (0 until bar.length()).all { i -> bar.optString(i).lowercase() in targetBar }
    }

    private fun parseUpgradeBar(legionStats: String?): Set<String> {
        if (legionStats.isNullOrBlank()) return emptySet()
        return try {
            val a = JSONObject(legionStats).optJSONArray("upgradeBar") ?: return emptySet()
            (0 until a.length()).map { a.optString(it).lowercase() }.toSet()
        } catch (_: Exception) { emptySet() }
    }

    private fun statsMatches(stats: JSONObject?, target: CardDefinition): Boolean {
        if (stats == null) return true
        val it = stats.keys()
        while (it.hasNext()) {
            val k = it.next()
            val want = stats.optString(k).lowercase()
            val got = when (k) {
                "defense" -> parseDefense(target.legionStats)?.lowercase()
                else -> null
            }
            if (got != null && got != want) return false
        }
        return true
    }

    private fun parseDefense(legionStats: String?): String? {
        if (legionStats.isNullOrBlank()) return null
        return try { JSONObject(legionStats).optString("defenseDie").takeIf { it.isNotBlank() } } catch (_: Exception) { null }
    }
}
