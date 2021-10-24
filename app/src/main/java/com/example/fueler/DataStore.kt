package com.example.fueler

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

internal object DataStore {

    private val Context.dataStore by preferencesDataStore(name = "data")

    private val FUEL_DATA = stringPreferencesKey("fuelData")

    internal fun read(context: Context): Flow<List<FuelEntry>> {
        return context.dataStore.data.map {
            it[FUEL_DATA]?.let { string ->
                Json.decodeFromString(ListSerializer(FuelEntry.serializer()), string = string).sortedBy { it.timestamp }.reversed()
            } ?: run {
                FuelEntry.generateDummySequence(150).toList().apply {
                    write(context = context, this)
                }
            }
        }
    }

    private suspend fun write(context: Context, values: List<FuelEntry>) {
        context.dataStore.edit { settings ->
            settings[FUEL_DATA] = Json.encodeToString(ListSerializer(FuelEntry.serializer()), values)
        }
    }

    internal suspend fun write(context: Context, value: FuelEntry) {
        context.dataStore.edit { settings ->
            val values = Json.decodeFromString(ListSerializer(FuelEntry.serializer()), settings[FUEL_DATA] ?: "[]")
            settings[FUEL_DATA] = Json.encodeToString(ListSerializer(FuelEntry.serializer()), values.plus(value))
        }
    }
}