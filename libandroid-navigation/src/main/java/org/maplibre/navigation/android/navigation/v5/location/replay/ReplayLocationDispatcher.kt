package org.maplibre.navigation.android.navigation.v5.location.replay

import android.location.Location
import android.os.Handler
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet

internal class ReplayLocationDispatcher : Runnable {
    private var locationsToReplay: MutableList<Location>
    private var current: Location? = null
    private var handler: Handler
    private var replayLocationListeners: CopyOnWriteArraySet<ReplayLocationListener>

    constructor(locationsToReplay: List<Location>) {
        checkValidInput(locationsToReplay)
        this.locationsToReplay = CopyOnWriteArrayList(locationsToReplay)
        initialize()
        this.replayLocationListeners = CopyOnWriteArraySet()
        this.handler = Handler()
    }

    // For testing only
    @Suppress("unused")
    constructor(locationsToReplay: MutableList<Location>, handler: Handler) {
        checkValidInput(locationsToReplay)
        this.locationsToReplay = locationsToReplay
        initialize()
        this.replayLocationListeners = CopyOnWriteArraySet()
        this.handler = handler
    }

    override fun run() {
        current?.let { current ->
            dispatchLocation(current)
            scheduleNextDispatch()
        }
    }

    fun stop() {
        clearLocations()
        stopDispatching()
    }

    fun pause() {
        stopDispatching()
    }

    fun update(locationsToReplay: List<Location>) {
        checkValidInput(locationsToReplay)
        this.locationsToReplay = CopyOnWriteArrayList(locationsToReplay)
        initialize()
    }

    fun add(toReplay: List<Location>) {
        val shouldRedispatch = locationsToReplay.isEmpty()
        addLocations(toReplay)
        if (shouldRedispatch) {
            stopDispatching()
            scheduleNextDispatch()
        }
    }

    fun addReplayLocationListener(listener: ReplayLocationListener) {
        replayLocationListeners.add(listener)
    }

    fun removeReplayLocationListener(listener: ReplayLocationListener) {
        replayLocationListeners.remove(listener)
    }

    private fun checkValidInput(locations: List<Location>?) {
        val isValidInput = locations == null || locations.isEmpty()
        require(!isValidInput) { NON_NULL_AND_NON_EMPTY_LOCATION_LIST_REQUIRED }
    }

    private fun initialize() {
        current = locationsToReplay.removeAt(HEAD)
    }

    private fun addLocations(toReplay: List<Location>) {
        locationsToReplay.addAll(toReplay)
    }

    private fun dispatchLocation(location: Location) {
        for (listener in replayLocationListeners) {
            listener.onLocationReplay(location)
        }
    }

    private fun scheduleNextDispatch() {
        if (locationsToReplay.isEmpty()) {
            stopDispatching()
            return
        }
        val currentTime = current!!.time
        current = locationsToReplay.removeAt(HEAD)
        val nextTime = current!!.time
        val diff = nextTime - currentTime
        handler.postDelayed(this, diff)
    }

    private fun clearLocations() {
        locationsToReplay.clear()
    }

    private fun stopDispatching() {
        handler.removeCallbacks(this)
    }

    companion object {
        private const val NON_NULL_AND_NON_EMPTY_LOCATION_LIST_REQUIRED =
            "Non-null and non-empty location list required."
        private const val HEAD = 0
    }
}