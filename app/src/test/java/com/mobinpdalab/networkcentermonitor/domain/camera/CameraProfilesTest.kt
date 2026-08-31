package com.mobinpdalab.networkcentermonitor.domain.camera

import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraProfilesTest {
    @Test
    fun `live defaults to sub stream unless main is explicitly requested`() {
        val policy = CameraBandwidthPolicy()

        assertEquals(StreamKind.SUB_STREAM, policy.chooseStream(mainStreamExplicitlyRequested = false))
        assertEquals(StreamKind.MAIN_STREAM, policy.chooseStream(mainStreamExplicitlyRequested = true))
    }

    @Test
    fun `persistent live streaming is forbidden and stream stops on view exit`() {
        val policy = CameraBandwidthPolicy()

        assertFalse(policy.persistentLiveStreamingAllowed)
        assertTrue(policy.stopStreamImmediatelyOnViewExit)
    }

    @Test
    fun `per-center concurrent stream limit is enforced`() {
        val policy = CameraBandwidthPolicy(maxConcurrentStreamsPerCenter = 2)

        assertTrue(policy.canOpenLive(0))
        assertTrue(policy.canOpenLive(1))
        assertFalse(policy.canOpenLive(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `camera cannot use itself as recorder`() {
        val id = DeviceId("camera-1")
        CameraProfile(
            id = CameraProfileId("profile-1"),
            deviceId = id,
            recorderDeviceId = id,
        )
    }

    @Test
    fun `onvif priority starts with profile T then legacy S`() {
        assertEquals(OnvifProfile.T, CameraDiscoveryPriority.onvifProfiles[0])
        assertEquals(OnvifProfile.S, CameraDiscoveryPriority.onvifProfiles[1])
    }
}
