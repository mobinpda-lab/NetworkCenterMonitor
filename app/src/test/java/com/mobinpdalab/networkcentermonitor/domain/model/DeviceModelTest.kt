package com.mobinpdalab.networkcentermonitor.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceModelTest {
    private val centerId = CenterId("center-1")
    private val networkId = NetworkId("network-1")
    private val deviceId = DeviceId("device-1")

    @Test
    fun `device supports multiple IPs and MAC addresses through interfaces`() {
        val device = Device(
            id = deviceId,
            centerId = centerId,
            networkId = networkId,
            displayName = "Core Router",
            type = DeviceType.ROUTER,
            interfaces = listOf(
                DeviceInterface(
                    id = DeviceInterfaceId("if-1"),
                    deviceId = deviceId,
                    macAddress = MacAddress("AA:BB:CC:DD:EE:01"),
                    ipAddresses = listOf(Ipv4Address("10.0.0.1"), Ipv4Address("10.0.0.2")),
                ),
                DeviceInterface(
                    id = DeviceInterfaceId("if-2"),
                    deviceId = deviceId,
                    macAddress = MacAddress("AA-BB-CC-DD-EE-02"),
                    ipAddresses = listOf(Ipv4Address("192.168.1.1")),
                ),
            ),
        )

        assertEquals(3, device.ipAddresses.size)
        assertEquals(2, device.macAddresses.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `interface from another device is rejected`() {
        Device(
            id = deviceId,
            centerId = centerId,
            networkId = networkId,
            displayName = "Invalid",
            interfaces = listOf(
                DeviceInterface(
                    id = DeviceInterfaceId("if-1"),
                    deviceId = DeviceId("other-device"),
                ),
            ),
        )
    }

    @Test
    fun `manual value is never overwritten by auto discovery`() {
        val current = SourcedValue(
            value = "Manual Camera Name",
            source = FieldSource.MANUAL,
            lastUpdatedEpochMillis = 100,
        )

        val result = mergeDiscoveredValue(
            current = current,
            discoveredValue = "Auto Camera Name",
            discoveredAtEpochMillis = 200,
        )

        assertEquals(current, result.effective)
        assertTrue(result.requiresUserConfirmation)
        assertNotNull(result.proposed)
        assertEquals(FieldSource.AUTO, result.proposed?.source)
    }

    @Test
    fun `auto value can be refreshed automatically`() {
        val current = SourcedValue(
            value = "Old Model",
            source = FieldSource.AUTO,
            lastUpdatedEpochMillis = 100,
            lastDiscoveryEpochMillis = 100,
        )

        val result = mergeDiscoveredValue(
            current = current,
            discoveredValue = "New Model",
            discoveredAtEpochMillis = 200,
        )

        assertEquals("New Model", result.effective.value)
        assertEquals(FieldSource.AUTO, result.effective.source)
        assertFalse(result.requiresUserConfirmation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `device relation cannot point to itself`() {
        DeviceRelation(
            id = DeviceRelationId("relation-1"),
            fromDeviceId = deviceId,
            toDeviceId = deviceId,
            type = DeviceRelationType.CONNECTED_TO,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid vlan is rejected`() {
        CenterNetwork(
            id = networkId,
            centerId = centerId,
            name = "Camera VLAN",
            vlanId = 4095,
        )
    }
}
