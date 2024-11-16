package org.maplibre.navigation.android.navigation.v5.navigation

import android.content.Context
import android.location.Location
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.maplibre.geojson.utils.PolylineUtils
import org.maplibre.navigation.android.navigation.v5.BaseTest
import org.maplibre.navigation.android.navigation.v5.navigation.NavigationHelper.buildSnappedLocation
import org.maplibre.navigation.android.navigation.v5.utils.Constants
import java.io.IOException

class NavigationRouteProcessorTest : BaseTest() {
    private var routeProcessor: NavigationRouteProcessor? = null
    private var navigation: MapLibreNavigation? = null

    @Before
    @Throws(Exception::class)
    fun before() {
        routeProcessor = NavigationRouteProcessor()
        val options = MapLibreNavigationOptions()
        val context = mockk<Context>(relaxed = true) {
            every { applicationContext } returns this
        }
        navigation = MapLibreNavigation(context, options, mockk(relaxed = true))
        navigation!!.startNavigation(buildTestDirectionsRoute()!!)
    }

    @Test
    @Throws(Exception::class)
    fun sanity() {
        Assert.assertNotNull(routeProcessor)
    }

    @Test
    @Throws(Exception::class)
    fun onFirstRouteProgressBuilt_newRouteIsDecoded() {
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        assertEquals(0, progress!!.legIndex)
        assertEquals(0, progress!!.currentLegProgress!!.stepIndex)
    }

    @Test
    @Throws(Exception::class)
    fun onShouldIncreaseStepIndex_indexIsIncreased() {
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val currentStepIndex: Int = progress!!.currentLegProgress!!.stepIndex
        routeProcessor!!.onShouldIncreaseIndex()
        routeProcessor!!.checkIncreaseIndex(navigation!!)

        val secondProgress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val secondStepIndex: Int = secondProgress!!.currentLegProgress!!.stepIndex

        Assert.assertTrue(currentStepIndex != secondStepIndex)
    }

    @Test
    @Throws(Exception::class)
    fun onSnapToRouteEnabledAndUserOnRoute_snappedLocationReturns() {
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val snapEnabled = true
        val userOffRoute = false
        val coordinates = createCoordinatesFromCurrentStep(
            progress!!
        )
        //TODO fabi755: remote from list will not work
        val lastPointInCurrentStep = coordinates.toMutableList().removeAt(coordinates.size - 1)
        val rawLocation = buildDefaultLocationUpdate(
            lastPointInCurrentStep!!.longitude(), lastPointInCurrentStep!!.latitude()
        )

        val snappedLocation = buildSnappedLocation(
            navigation!!, snapEnabled, rawLocation!!, progress, userOffRoute
        )

        Assert.assertTrue(rawLocation != snappedLocation)
    }

    @Test
    @Throws(Exception::class)
    fun onSnapToRouteDisabledAndUserOnRoute_rawLocationReturns() {
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val snapEnabled = false
        val userOffRoute = false
        val coordinates = createCoordinatesFromCurrentStep(
            progress!!
        )
        //TODO fabi755: remote from list will not work
        val lastPointInCurrentStep = coordinates.toMutableList().removeAt(coordinates.size - 1)
        val rawLocation = buildDefaultLocationUpdate(
            lastPointInCurrentStep!!.longitude(), lastPointInCurrentStep!!.latitude()
        )

        val snappedLocation = buildSnappedLocation(
            navigation!!, snapEnabled, rawLocation!!, progress, userOffRoute
        )

        Assert.assertTrue(rawLocation == snappedLocation)
    }

    @Test
    @Throws(Exception::class)
    fun onSnapToRouteEnabledAndUserOffRoute_rawLocationReturns() {
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val snapEnabled = false
        val userOffRoute = false
        val coordinates = createCoordinatesFromCurrentStep(
            progress!!
        )
        //TODO fabi755: remote from list will not work
        val lastPointInCurrentStep = coordinates.toMutableList().removeAt(coordinates.size - 1)
        val rawLocation = buildDefaultLocationUpdate(
            lastPointInCurrentStep!!.longitude(), lastPointInCurrentStep!!.latitude()
        )

        val snappedLocation = buildSnappedLocation(
            navigation!!, snapEnabled, rawLocation!!, progress, userOffRoute
        )

        Assert.assertTrue(rawLocation == snappedLocation)
    }

    @Test
    @Throws(Exception::class)
    fun onStepDistanceRemainingZeroAndNoBearingMatch_stepIndexForceIncreased() {
        val firstProgress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val firstProgressIndex: Int = firstProgress!!.currentLegProgress!!.stepIndex!!
        val coordinates = createCoordinatesFromCurrentStep(
            firstProgress
        )
        //TODO fabi755: remote from list will not work
        val lastPointInCurrentStep = coordinates.toMutableList().removeAt(coordinates.size - 1)
        val rawLocation = buildDefaultLocationUpdate(
            lastPointInCurrentStep!!.longitude(), lastPointInCurrentStep!!.latitude()
        )

        val secondProgress = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            rawLocation!!
        )
        val secondProgressIndex: Int = secondProgress!!.currentLegProgress!!.stepIndex

        Assert.assertTrue(firstProgressIndex != secondProgressIndex)
    }

    @Test
    @Throws(Exception::class)
    fun onInvalidNextLeg_indexIsNotIncreased() {
        routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val legSize = navigation!!.route!!.legs!!.size

        for (i in 0 until legSize) {
            routeProcessor!!.onShouldIncreaseIndex()
            routeProcessor!!.checkIncreaseIndex(navigation!!)
        }
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))

        Assert.assertTrue(progress!!.legIndex === legSize - 1)
    }

    @Test
    @Throws(Exception::class)
    fun onInvalidNextStep_indexIsNotIncreased() {
        routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val stepSize = navigation!!.route!!.legs!![0].steps!!.size

        for (i in 0 until stepSize) {
            routeProcessor!!.onShouldIncreaseIndex()
            routeProcessor!!.checkIncreaseIndex(navigation!!)
        }
        val progress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))

        Assert.assertTrue(progress!!.currentLegProgress!!.stepIndex!! === stepSize - 1)
    }

    @Test
    @Throws(IOException::class)
    fun onNewRoute_testStepProgressSetCorrectly() {
        navigation!!.startNavigation(buildTestDirectionsRoute("directions_distance_congestion_annotation.json")!!)
        val firstOnRoute = buildDefaultLocationUpdate(-77.034043, 38.900205)
        val progress = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            firstOnRoute!!
        )
        assertEquals(
            35.1,
            progress!!.currentLegProgress!!.currentStepProgress!!.distanceRemaining!!,
            1.0
        )
        // If the step progress is calculated correctly, we must be on the first step of the route (index = 0)
        assertEquals(0, progress!!.currentLegProgress!!.currentLegAnnotation!!.index)

        val otherOnRoute = buildDefaultLocationUpdate(-77.033638, 38.900207)
        val progress2 = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            otherOnRoute!!
        )
        assertEquals(2, progress2!!.currentLegProgress!!.currentLegAnnotation!!.index)

        // Creating a new route should trigger a new routeProgress to be built. Annotation must be reset to 0 for same location
        val testRoute2 = buildTestDirectionsRoute("directions_distance_congestion_annotation.json")
        val decoded = PolylineUtils.decode(
            testRoute2!!.geometry!!, Constants.PRECISION_6
        )
        decoded.removeAt(0)
        val alteredGeometry = PolylineUtils.encode(decoded, Constants.PRECISION_6)
        navigation!!.startNavigation(testRoute2.copy(geometry = alteredGeometry))

        val progress3 = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            firstOnRoute
        )
        assertEquals(0, progress3!!.currentLegProgress!!.currentLegAnnotation!!.index)
    }

    @Test
    @Throws(IOException::class)
    fun onAdvanceIndices_testAnnotationsSetCorrectly() {
        navigation!!.startNavigation(buildTestDirectionsRoute("directions_two_leg_route_with_distances.json")!!)

        val progress0 = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            buildDefaultLocationUpdate(-74.220588, 40.745062)
        )
        assertEquals(0, progress0!!.currentLegProgress!!.stepIndex)
        // Location right before the via point, third annotation
        val location1 = buildDefaultLocationUpdate(-74.219569, 40.745062)
        val progress = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            location1!!
        )
        assertEquals(1, progress!!.currentLegProgress!!.stepIndex)
        assertEquals(0, progress!!.legIndex)
        assertEquals(2, progress!!.currentLegProgress!!.currentLegAnnotation!!.index)

        // Location shortly after the via point, must have a lower annotation index!
        val location2 = buildDefaultLocationUpdate(-74.219444, 40.745065)
        val progress2 = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            location2!!
        )
        assertEquals(1, progress2!!.legIndex)
        assertEquals(0, progress2!!.currentLegProgress!!.currentLegAnnotation!!.index)
        // This must mean that the annotation was reset correctly in the meantime
    }

    @Test
    @Throws(Exception::class)
    fun withinManeuverRadiusAndBearingMatches_stepIndexIsIncreased() {
        val firstProgress = routeProcessor!!.buildNewRouteProgress(navigation!!, mockk(relaxed = true))
        val firstProgressIndex: Int = firstProgress!!.currentLegProgress!!.stepIndex!!
        val coordinates = createCoordinatesFromCurrentStep(
            firstProgress
        )
        //TODO fabi755: remove from list will not work
        val lastPointInCurrentStep = coordinates.toMutableList().removeAt(coordinates.size - 1)
        val rawLocation = buildDefaultLocationUpdate(
            lastPointInCurrentStep!!.longitude(), lastPointInCurrentStep!!.latitude()
        )
        every { rawLocation.bearing } returns 145f

        val secondProgress = routeProcessor!!.buildNewRouteProgress(
            navigation!!,
            rawLocation
        )
        val secondProgressIndex: Int = secondProgress!!.currentLegProgress!!.stepIndex

        Assert.assertTrue(firstProgressIndex != secondProgressIndex)
    }
}
