package com.mobinpdalab.networkcentermonitor.domain.pc

import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcRemoteProfilesTest {
    private val deviceId = DeviceId("pc-1")

    private fun profile(
        id: String,
        method: RemoteMethod,
        priority: Int = 100,
        enabled: Boolean = true,
        port: RemotePortConfig = RemotePortConfig(NetworkPort(3389)),
    ) = RemoteProfile(
        id = RemoteProfileId(id),
        deviceId = deviceId,
        method = method,
        port = port,
        priority = priority,
        enabled = enabled,
        customLaunchTemplate = if (method == RemoteMethod.CUSTOM) "custom://{host}:{port}" else null,
    )

    @Test
    fun `custom port overrides default for health test and launch`() {
        val remote = profile(
            id = "rdp",
            method = RemoteMethod.RDP,
            port = RemotePortConfig(
                defaultPort = NetworkPort(3389),
                customPort = NetworkPort(3395),
            ),
        )

        assertEquals(3395, remote.healthCheckPort.value)
        assertEquals(3395, remote.connectionTestPort.value)
        assertEquals(3395, remote.launchPort.value)
    }

    @Test
    fun `one enabled remote profile launches directly`() {
        val decision = chooseRemoteLaunch(listOf(profile("rdp", RemoteMethod.RDP)))
        assertTrue(decision is RemoteLaunchDecision.Direct)
    }

    @Test
    fun `multiple enabled profiles produce priority ordered picker`() {
        val decision = chooseRemoteLaunch(
            listOf(
                profile("vnc", RemoteMethod.VNC, priority = 20),
                profile("rdp", RemoteMethod.RDP, priority = 10),
            ),
        ) as RemoteLaunchDecision.Pick

        assertEquals(RemoteMethod.RDP, decision.profiles.first().method)
        assertEquals(RemoteMethod.VNC, decision.profiles.last().method)
    }

    @Test
    fun `disabled profiles are ignored`() {
        val decision = chooseRemoteLaunch(
            listOf(profile("rdp", RemoteMethod.RDP, enabled = false)),
        )
        assertTrue(decision is RemoteLaunchDecision.NoneAvailable)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `credentials cannot silently rely on plaintext or missing secure storage`() {
        RemoteCredentialPolicy(
            secureCredentialRef = null,
            askOnConnect = false,
        )
    }

    @Test
    fun `default network policy forbids public internet dependency`() {
        val policy = RemoteNetworkPolicy()
        assertTrue(policy.privateNetworkFirst)
        assertFalse(policy.publicInternetDependencyAllowed)
    }
}
