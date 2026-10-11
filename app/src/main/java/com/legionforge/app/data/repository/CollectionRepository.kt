package com.legionforge.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Un pack de « Ma collection » : une boîte d'extension du catalogue.
 *  cards = liste des id de cartes du catalogue incluses (une carte peut figurer
 *  dans plusieurs packs — ex. Clone Trooper Infantry dans Phase I ET Phase II). */
data class CollectionPack(
    val id: String,
    val gameSystem: String,
    val name: String,
    val imageName: String?,
    val cards: List<String> = emptyList()
)

/** Collection personnelle de l'utilisateur : les packs cochés (boîtes achetées).
 *
 *  La source de vérité est l'ensemble des PACKS cochés (SharedPreferences).
 *  Les cartes possédées sont DÉRIVÉES par union des cartes de chaque pack coché :
 *  une carte présente dans plusieurs packs reste possédée tant qu'au moins un
 *  de ces packs est coché (décocher un pack n'exclut pas la carte qu'un autre
 *  pack fournit). Offline-first, instantané.
 *
 *  L'écran Ma collection (Réglages) coche des packs ; le catalogue du builder
 *  peut être filtré « Ma collection » (n'affiche que les cartes possédées). */
object CollectionRepository {
    private const val PREFS = "legionforge"
    private const val KEY_PACKS = "owned_packs"
    private const val KEY_CARDS = "owned_cards" // cartes cochées individuellement (onglet Cartes)
    private const val KEY_COLLECTION_ONLY = "collection_only" // filtre catalogue "ma collection"
    private const val TAG = "CollectionRepo"

    private lateinit var prefs: SharedPreferences
    private val _ownedPacks = MutableStateFlow<Set<String>>(emptySet())
    val ownedPacks: StateFlow<Set<String>> = _ownedPacks.asStateFlow()

    /** Filtre catalogue « Ma collection » : n'afficher que le matériel possédé.
     *  Persistant (Réglages + menu ⚙ du builder). */
    private val _collectionOnly = MutableStateFlow(false)
    val collectionOnly: StateFlow<Boolean> = _collectionOnly.asStateFlow()

    /** Cartes possédées = union des cartes des packs cochés (mise à jour à chaque
     *  changement de ownedPacks, dérivé à la volée. */
    private val _ownedCards = MutableStateFlow<Set<String>>(emptySet())
    val ownedCards: StateFlow<Set<String>> = _ownedCards.asStateFlow()

    // Alias retrocompatible pour l'écran builder (lecture seule de ownedCards).
    val owned: StateFlow<Set<String>> = _ownedCards.asStateFlow()

    private val _individualCards = MutableStateFlow<Set<String>>(emptySet())
    val individualCards: StateFlow<Set<String>> = _individualCards.asStateFlow()

    private val _packs = MutableStateFlow<List<CollectionPack>>(emptyList())
    val packs: StateFlow<List<CollectionPack>> = _packs.asStateFlow()

    /** Mapping packId -> Set<cardId> pour calculer l'union sans re-parser. */
    private val packToCards = HashMap<String, Set<String>>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _ownedPacks.value = prefs.getStringSet(KEY_PACKS, emptySet()) ?: emptySet()
        _individualCards.value = prefs.getStringSet(KEY_CARDS, emptySet()) ?: emptySet()
        _collectionOnly.value = prefs.getBoolean(KEY_COLLECTION_ONLY, false)
        loadPacks(context.applicationContext)
    }

    /** Charge le mapping packs depuis l'asset packs.json (topologie pack -> cartes). */
    fun loadPacks(context: Context) {
        try {
            val json = context.assets.open("packs.json").bufferedReader().use { it.readText() }
            val doc = Gson().fromJson(json, PacksDocument::class.java)
            val list = doc?.packs?.map {
                CollectionPack(it.id, it.gameSystem, it.name, it.imageName, it.cards)
            } ?: emptyList()
            _packs.value = list
            packToCards.clear()
            list.forEach { pack -> packToCards[pack.id] = pack.cards.toSet() }
            recalcOwned()
        } catch (e: Exception) {
            Log.e(TAG, "loadPacks failed", e)
            _packs.value = emptyList()
        }
    }

    fun isPackOwned(packId: String): Boolean = packId in _ownedPacks.value

    fun isCardOwned(cardId: String): Boolean = cardId in _ownedCards.value

    /** Coche / décoche un pack. Décocher retire le pack de la source de vérité ;
     *  ownedCards est recalculé par union, donc une carte d'un autre pack coché reste. */
    fun togglePack(packId: String) {
        val current = _ownedPacks.value.toMutableSet()
        if (!current.add(packId)) current.remove(packId)
        _ownedPacks.value = current
        recalcOwned()
        prefs.edit().putStringSet(KEY_PACKS, current).apply()
    }

    /** Coche / décoche une carte individuellement (onglet Cartes). */
    fun toggleCard(cardId: String) {
        val current = _individualCards.value.toMutableSet()
        if (!current.add(cardId)) current.remove(cardId)
        _individualCards.value = current
        recalcOwned()
        prefs.edit().putStringSet(KEY_CARDS, current).apply()
    }

    /** Active/désactive le filtre catalogue « Ma collection » (persistant). */
    fun setCollectionOnly(enabled: Boolean) {
        _collectionOnly.value = enabled
        prefs.edit().putBoolean(KEY_COLLECTION_ONLY, enabled).apply()
    }

    private fun recalcOwned() {
        val union = HashSet<String>()
        _ownedPacks.value.forEach { pid -> packToCards[pid]?.let { union.addAll(it) } }
        union.addAll(_individualCards.value)
        _ownedCards.value = union
    }

    private class PacksDocument {
        var schemaVersion: Int = 0
        var packs: List<PackJson> = emptyList()
    }
    private class PackJson {
        var id: String = ""
        var gameSystem: String = ""
        var name: String = ""
        var imageName: String? = null
        var cards: List<String> = emptyList()
    }
}