package com.simon.wodtimer.ui

import com.simon.wodtimer.data.WorkoutRepository
import com.simon.wodtimer.model.Workout
import com.simon.wodtimer.model.WorkoutNote
import com.simon.wodtimer.platform.NoteImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkoutViewModel {

    private val _workouts = MutableStateFlow(WorkoutRepository.load())
    val workouts: StateFlow<List<Workout>> = _workouts.asStateFlow()

    private val _notes = MutableStateFlow(WorkoutRepository.loadNotes())
    val notes: StateFlow<List<WorkoutNote>> = _notes.asStateFlow()

    private val _soundEnabled = MutableStateFlow(WorkoutRepository.isSoundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    fun toggleSound() {
        val next = !_soundEnabled.value
        _soundEnabled.value = next
        WorkoutRepository.setSoundEnabled(next)
    }

    fun upsert(workout: Workout) {
        val current = _workouts.value.toMutableList()
        val existing = current.indexOfFirst { it.id == workout.id }
        if (existing >= 0) current[existing] = workout else current.add(0, workout)
        commit(current)
    }

    fun delete(workout: Workout) {
        commit(_workouts.value.filterNot { it.id == workout.id })
    }

    fun upsertNote(note: WorkoutNote) {
        val current = _notes.value.toMutableList()
        val existing = current.indexOfFirst { it.id == note.id }
        if (existing >= 0) current[existing] = note else current.add(0, note)
        commitNotes(current)
    }

    fun deleteNote(note: WorkoutNote) {
        NoteImageStore.delete(note.imagePath)
        commitNotes(_notes.value.filterNot { it.id == note.id })
    }

    private fun commit(list: List<Workout>) {
        _workouts.value = list
        WorkoutRepository.save(list)
    }

    private fun commitNotes(list: List<WorkoutNote>) {
        _notes.value = list
        WorkoutRepository.saveNotes(list)
    }
}
