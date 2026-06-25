package com.simon.wodtimer.ui

import com.simon.wodtimer.model.PhaseKind
import com.simon.wodtimer.model.RoundSplit
import com.simon.wodtimer.model.RunSpec
import com.simon.wodtimer.model.TimerPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

class TimerEngine(
    private val scope: CoroutineScope,
    private val spec: RunSpec,
    private val onCountBeep: () -> Unit,
    private val onPhaseChangeBeep: (PhaseKind) -> Unit
) {
    private val manualCounter = isManualCounter(spec)
    private val repCounter = isRepCounter(spec)
    private val clock = TimeSource.Monotonic.markNow()

    private fun nowMillis(): Long = clock.elapsedNow().inWholeMilliseconds

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<RunState> = _state.asStateFlow()

    private var tickJob: Job = Job().also { it.complete() }
    private var index = 0
    private var remaining = firstPhase().totalSeconds
    private var elapsed = 0
    private var bodyElapsed = 0
    private var inPrep = specHasPrep()
    private var roundCount = 0
    private var workIntervalIndex = 0
    private var roundInInterval = 0
    private var currentIntervalStartMillis = 0L
    private val splits = mutableListOf<RoundSplit>()
    private var paused = false
    private var flashSeq = 0
    private var bodyStartMillis = 0L
    private var pauseStartMillis = 0L
    private var pausedAccumMillis = 0L

    private fun specHasPrep(): Boolean = when (spec) {
        is RunSpec.CountUp -> spec.prepSeconds > 0
        is RunSpec.Repeat -> spec.prepSeconds > 0
        is RunSpec.InfiniteEmom -> true
        is RunSpec.Fixed -> spec.phases.firstOrNull()?.kind == PhaseKind.PREP
    }

    private fun firstPhase(): TimerPhase = when (spec) {
        is RunSpec.Fixed -> spec.phases.first()
        is RunSpec.CountUp -> prepOr(spec.prepSeconds, TimerPhase(PhaseKind.COUNT_UP, 0, 0, 0, "Clock"))
        is RunSpec.Repeat -> prepOr(spec.prepSeconds, TimerPhase(PhaseKind.INTERVAL, spec.intervalSeconds, 1, 0, "Round 1"))
        is RunSpec.InfiniteEmom -> TimerPhase(PhaseKind.PREP, spec.prepSeconds, 0, 0, "Get ready")
    }

    private fun prepOr(prepSeconds: Int, body: TimerPhase): TimerPhase =
        if (prepSeconds > 0) TimerPhase(PhaseKind.PREP, prepSeconds, 0, 0, "Get ready") else body

    private fun initialState(): RunState {
        val first = firstPhase()
        val countUp = spec is RunSpec.CountUp && first.kind == PhaseKind.COUNT_UP
        return RunState(
            phase = first,
            displaySeconds = if (countUp) 0 else first.totalSeconds,
            phaseIndex = 0,
            totalPhases = if (spec is RunSpec.Fixed) spec.phases.size else 0,
            isRunning = false,
            isFinished = false,
            isCountUp = countUp,
            elapsedSeconds = 0,
            manualCounter = manualCounter,
            repCounter = repCounter,
            roundCount = 0,
            currentReps = 0,
            canCount = isCountablePhase(first.kind),
            splits = emptyList(),
            flashSeconds = 0,
            flashSeq = 0
        )
    }

    fun start() {
        if (_state.value.isRunning || _state.value.isFinished) return
        val resuming = paused
        paused = false
        if (resuming && pauseStartMillis > 0L) {
            if (bodyStartMillis != 0L) pausedAccumMillis += nowMillis() - pauseStartMillis
            pauseStartMillis = 0L
        }
        if (!resuming && !inPrep && _state.value.phase.kind != PhaseKind.PREP) {
            markBodyStart()
            if (repCounter && isCountablePhase(_state.value.phase.kind)) startWorkInterval()
        }
        _state.value = _state.value.copy(isRunning = true)
        tickJob = scope.launch {
            if (!resuming) onPhaseChangeBeep(_state.value.phase.kind)
            while (isActive) {
                delay(ONE_SECOND_MS)
                tick()
                if (_state.value.isFinished) break
            }
        }
    }

    fun pause() {
        tickJob.cancel()
        paused = true
        pauseStartMillis = nowMillis()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun resume() = start()

    fun stop() {
        tickJob.cancel()
        index = 0
        elapsed = 0
        bodyElapsed = 0
        remaining = firstPhase().totalSeconds
        inPrep = specHasPrep()
        roundCount = 0
        workIntervalIndex = 0
        roundInInterval = 0
        currentIntervalStartMillis = 0L
        splits.clear()
        paused = false
        flashSeq = 0
        bodyStartMillis = 0L
        pauseStartMillis = 0L
        pausedAccumMillis = 0L
        _state.value = initialState()
    }

    fun end() {
        tickJob.cancel()
        paused = false
        finish(_state.value.totalPhases, roundCount)
    }

    fun incrementRound() {
        if (_state.value.isFinished || isInPrep()) return
        if (manualCounter) {
            roundCount += 1
            splits += RoundSplit(roundCount, bodyElapsed, nowBodyMillis())
            publishCounter()
        } else if (repCounter && canCountNow()) {
            roundInInterval += 1
            roundCount += 1
            splits += RoundSplit(
                round = roundInInterval,
                elapsedSeconds = bodyElapsed,
                atMillis = nowBodyMillis(),
                interval = workIntervalIndex,
                intervalStartMillis = currentIntervalStartMillis
            )
            _state.value = _state.value.copy(currentReps = roundInInterval, roundCount = roundCount, splits = splits.toList())
        }
    }

    private fun isInPrep(): Boolean = _state.value.phase.kind == PhaseKind.PREP

    private fun canCountNow(): Boolean = isCountablePhase(_state.value.phase.kind)

    private fun isCountablePhase(kind: PhaseKind): Boolean =
        kind == PhaseKind.WORK || kind == PhaseKind.INTERVAL

    private fun startWorkInterval() {
        workIntervalIndex += 1
        roundInInterval = 0
        currentIntervalStartMillis = nowBodyMillis()
    }

    private fun markBodyStart() {
        if (bodyStartMillis == 0L) bodyStartMillis = nowMillis()
    }

    private fun nowBodyMillis(): Long {
        if (bodyStartMillis == 0L) return bodyElapsed * ONE_SECOND_MS
        return nowMillis() - bodyStartMillis - pausedAccumMillis
    }

    private fun tick() {
        when (spec) {
            is RunSpec.Fixed -> tickFixed(spec)
            is RunSpec.CountUp -> tickCountUp(spec)
            is RunSpec.Repeat -> tickRepeat(spec)
            is RunSpec.InfiniteEmom -> tickInfiniteEmom(spec)
        }
    }

    private fun tickCountUp(spec: RunSpec.CountUp) {
        if (inPrep) {
            remaining -= 1
            if (remaining in 1..COUNT_BEEP_THRESHOLD) onCountBeep()
            if (remaining > 0) {
                publishPrep(spec.prepSeconds, flashFor(remaining))
                return
            }
            inPrep = false
            elapsed = 0
            markBodyStart()
            _state.value = _state.value.copy(
                phase = TimerPhase(PhaseKind.COUNT_UP, 0, 0, 0, "Clock"),
                displaySeconds = 0,
                isRunning = true,
                isCountUp = true,
                flashSeconds = 0,
                flashSeq = ++flashSeq
            )
            onPhaseChangeBeep(PhaseKind.COUNT_UP)
            return
        }
        elapsed += 1
        bodyElapsed += 1
        val toTarget = if (spec.targetSeconds > 0) spec.targetSeconds - elapsed else NO_FLASH
        if (toTarget in 1..COUNT_BEEP_THRESHOLD) onCountBeep()
        if (spec.targetSeconds > 0 && elapsed >= spec.targetSeconds) {
            finish()
            return
        }
        _state.value = _state.value.copy(
            displaySeconds = elapsed,
            elapsedSeconds = bodyElapsed,
            isRunning = true,
            isCountUp = true,
            flashSeconds = flashFor(toTarget),
            flashSeq = ++flashSeq
        )
    }

    private fun tickRepeat(spec: RunSpec.Repeat) {
        remaining -= 1
        if (!inPrep) bodyElapsed += 1
        if (remaining in 1..COUNT_BEEP_THRESHOLD) onCountBeep()
        if (remaining > 0) {
            if (inPrep) publishPrep(spec.prepSeconds, flashFor(remaining)) else publishBody(flashFor(remaining))
            return
        }
        if (inPrep) {
            inPrep = false
            markBodyStart()
            remaining = spec.intervalSeconds
            _state.value = _state.value.copy(
                phase = TimerPhase(PhaseKind.INTERVAL, spec.intervalSeconds, 1, 0, "Round 1"),
                displaySeconds = remaining,
                isRunning = true,
                flashSeconds = 0,
                flashSeq = ++flashSeq
            )
            onPhaseChangeBeep(PhaseKind.INTERVAL)
            return
        }
        val nextRound = _state.value.phase.roundIndex + 1
        remaining = spec.intervalSeconds
        _state.value = _state.value.copy(
            phase = TimerPhase(PhaseKind.INTERVAL, spec.intervalSeconds, nextRound, 0, "Round $nextRound"),
            displaySeconds = remaining,
            elapsedSeconds = bodyElapsed,
            isRunning = true,
            flashSeconds = 0,
            flashSeq = ++flashSeq
        )
        onPhaseChangeBeep(PhaseKind.INTERVAL)
    }

    private fun tickInfiniteEmom(spec: RunSpec.InfiniteEmom) {
        remaining -= 1
        if (_state.value.phase.kind != PhaseKind.PREP) bodyElapsed += 1
        if (remaining in 1..COUNT_BEEP_THRESHOLD) onCountBeep()
        if (remaining > 0) {
            publishBody(flashFor(remaining))
            return
        }
        val current = _state.value.phase
        if (current.kind != PhaseKind.PREP) recordAutoSplit(current.roundIndex) else markBodyStart()
        val nextRound = if (current.kind == PhaseKind.PREP) 1 else current.roundIndex + 1
        remaining = spec.intervalSeconds
        _state.value = _state.value.copy(
            phase = TimerPhase(PhaseKind.WORK, spec.intervalSeconds, nextRound, 0, "Round $nextRound"),
            displaySeconds = remaining,
            elapsedSeconds = bodyElapsed,
            roundCount = roundCount,
            splits = splits.toList(),
            isRunning = true,
            flashSeconds = 0,
            flashSeq = ++flashSeq
        )
        onPhaseChangeBeep(PhaseKind.WORK)
    }

    private fun tickFixed(spec: RunSpec.Fixed) {
        remaining -= 1
        if (spec.phases[index].kind != PhaseKind.PREP) bodyElapsed += 1
        if (remaining in 1..COUNT_BEEP_THRESHOLD) onCountBeep()
        if (remaining > 0) {
            publishFixed(spec, flashFor(remaining))
            return
        }
        val leavingPrep = spec.phases[index].kind == PhaseKind.PREP
        index += 1
        if (leavingPrep) markBodyStart()
        if (index >= spec.phases.size) {
            finish(spec.phases.size, spec.phases.last().totalRounds)
            return
        }
        if (repCounter && isCountablePhase(spec.phases[index].kind)) startWorkInterval()
        remaining = spec.phases[index].totalSeconds
        publishFixed(spec, 0)
        onPhaseChangeBeep(spec.phases[index].kind)
    }

    private fun recordAutoSplit(round: Int) {
        roundCount = round
        splits += RoundSplit(round, bodyElapsed, nowBodyMillis())
    }

    private fun flashFor(secondsLeft: Int): Int =
        if (secondsLeft in 1..COUNT_BEEP_THRESHOLD) secondsLeft else 0

    private fun publishPrep(prepSeconds: Int, flash: Int) {
        _state.value = _state.value.copy(
            phase = TimerPhase(PhaseKind.PREP, prepSeconds, 0, 0, "Get ready"),
            displaySeconds = remaining,
            isRunning = true,
            isCountUp = false,
            flashSeconds = flash,
            flashSeq = ++flashSeq
        )
    }

    private fun publishBody(flash: Int) {
        _state.value = _state.value.copy(
            displaySeconds = remaining,
            elapsedSeconds = bodyElapsed,
            isRunning = true,
            flashSeconds = flash,
            flashSeq = ++flashSeq
        )
    }

    private fun publishCounter() {
        _state.value = _state.value.copy(roundCount = roundCount, splits = splits.toList())
    }

    private fun publishFixed(spec: RunSpec.Fixed, flash: Int) {
        _state.value = _state.value.copy(
            phase = spec.phases[index],
            displaySeconds = remaining,
            phaseIndex = index,
            elapsedSeconds = bodyElapsed,
            currentReps = roundInInterval,
            canCount = isCountablePhase(spec.phases[index].kind),
            isRunning = true,
            isFinished = false,
            flashSeconds = flash,
            flashSeq = ++flashSeq
        )
    }

    private fun finish(totalPhases: Int = 0, totalRounds: Int = 0) {
        tickJob.cancel()
        _state.value = _state.value.copy(
            phase = TimerPhase(PhaseKind.DONE, 0, totalRounds, totalRounds, "Done"),
            displaySeconds = 0,
            phaseIndex = totalPhases,
            totalPhases = totalPhases,
            elapsedSeconds = bodyElapsed,
            roundCount = roundCount,
            splits = splits.toList(),
            isRunning = false,
            isFinished = true,
            isCountUp = false,
            flashSeconds = 0,
            flashSeq = ++flashSeq
        )
    }

    companion object {
        private const val ONE_SECOND_MS = 1000L
        private const val COUNT_BEEP_THRESHOLD = 3
        private const val NO_FLASH = -1

        private fun isManualCounter(spec: RunSpec): Boolean = when (spec) {
            is RunSpec.CountUp -> true
            is RunSpec.Repeat -> true
            is RunSpec.InfiniteEmom -> false
            is RunSpec.Fixed -> spec.phases.none { it.totalRounds > 1 }
        }

        private fun isRepCounter(spec: RunSpec): Boolean =
            spec is RunSpec.Fixed && spec.phases.any { it.totalRounds > 1 }
    }
}
