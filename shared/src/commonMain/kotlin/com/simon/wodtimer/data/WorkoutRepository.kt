package com.simon.wodtimer.data

import com.simon.wodtimer.model.Workout
import com.simon.wodtimer.model.WorkoutNote
import com.simon.wodtimer.platform.Settings
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WorkoutRepository {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<Workout> {
        val raw = Settings.getString(KEY_WORKOUTS)
        if (raw == null) {
            val seeded = SeedWorkouts.all()
            save(seeded)
            return seeded
        }
        return try {
            json.decodeFromString<List<Workout>>(raw)
        } catch (e: Exception) {
            SeedWorkouts.all()
        }
    }

    fun save(workouts: List<Workout>) {
        Settings.putString(KEY_WORKOUTS, json.encodeToString(workouts))
    }

    fun loadNotes(): List<WorkoutNote> {
        val raw = Settings.getString(KEY_NOTES) ?: return emptyList()
        return try {
            json.decodeFromString<List<WorkoutNote>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveNotes(notes: List<WorkoutNote>) {
        Settings.putString(KEY_NOTES, json.encodeToString(notes))
    }

    fun isSoundEnabled(): Boolean = Settings.getInt(KEY_SOUND, 1) != 0

    fun setSoundEnabled(enabled: Boolean) {
        Settings.putInt(KEY_SOUND, if (enabled) 1 else 0)
    }

    private const val KEY_WORKOUTS = "workouts"
    private const val KEY_NOTES = "workout_notes"
    private const val KEY_SOUND = "sound_enabled"
}
