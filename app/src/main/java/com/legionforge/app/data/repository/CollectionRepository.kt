package com.legionforge.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Collection personnelle de l'utilisateur : l'ensemble des cartes possédées (achetées).
 *  Stockée localement (SharedPreferences), offline-first, instantané.
 *  L'écran Ma collection (Réglages) coche les cartes ; le catalogue du builder peut
 *  être filtré "Ma collection" pour n'afficher que ce qu'on possède vraiment. */
object CollectionRepository {
    private const val PREFS = "legionforge"
    private const val KEY_OWNED = "owned_cards"

    private lateinit var prefs: SharedPreferences
    private val _owned = MutableStateFlow<Set<String>>(emptySet())
    val owned: StateFlow<Set<String>> = _owned.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _owned.value = prefs.getStringSet(KEY_OWNED, emptySet()) ?: emptySet()
    }

    fun isOwned(cardId: String): Boolean = cardId in _owned.value

    fun toggle(cardId: String) {
        val current = _owned.value.toMutableSet()
        if (!current.add(cardId)) current.remove(cardId)
        _owned.value = current
        prefs.edit().putStringSet(KEY_OWNED, current).apply()
    }
}