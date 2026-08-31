package com.mobinpdalab.networkcentermonitor.data.local

import com.mobinpdalab.networkcentermonitor.domain.model.CenterId
import com.mobinpdalab.networkcentermonitor.domain.model.Device
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceIdentity
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceInterface
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceInterfaceId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceType
import com.mobinpdalab.networkcentermonitor.domain.model.FieldSource
import com.mobinpdalab.networkcentermonitor.domain.model.Ipv4Address
import com.mobinpdalab.networkcentermonitor.domain.model.MacAddress
import com.mobinpdalab.networkcentermonitor.domain.model.MonitoringStatus
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkId
import com.mobinpdalab.networkcentermonitor.domain.model.SourcedValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePersistenceMapperTest {
    @Test
    fun `canonical Device round trips interfaces source metadata tags and custom fields`() {
        val device = Device(
            id = DeviceId("device-1"),
            centerId = CenterId("center-1"),
            networkId = NetworkId("network-1"),
            displayName = "Core Camera",
            type = DeviceType.CAMERA,
            identity = DeviceIdentity(
                brand = SourcedValue("Axis", FieldSource.MANUAL, 10, 8),
                firmware = SourcedValue("11.2", FieldSource.AUTO, 12, 12),
            ),
            interfaces = listOf(
                DeviceInterface(
                    id = DeviceInterfaceId("if-1"),
                    deviceId = DeviceId("device-1"),
                    name = "eth0",
                    macAddress = MacAddress("AA:BB:CC:DD:EE:FF"),
                    ipAddresses = listOf(Ipv4Address("10.10.0.20"), Ipv4Address("10.10.0.21")),
                ),
            ),
            monitoringEnabled = true,
            status = MonitoringStatus.CONNECTED,
            lastSeenEpochMillis = 100,
            lastStatusChangeEpochMillis = 90,
            lastDiscoveryEpochMillis = 80,
            tags = setOf("camera", "critical"),
            notes = "rack A",
            customFieldValues = mapOf("room" to "server"),
        )

        val restored = device.toPersistedAggregate().toDomainDevice()

        assertEquals(device.id, restored.id)
        assertEquals(device.centerId, restored.centerId)
        assertEquals(device.networkId, restored.networkId)
        assertEquals(device.type, restored.type)
        assertEquals(device.interfaces, restored.interfaces)
        assertEquals(device.tags, restored.tags)
        assertEquals(device.customFieldValues, restored.customFieldValues)
        assertEquals(FieldSource.MANUAL, restored.identity.brand?.source)
        assertEquals("Axis", restored.identity.brand?.value)
        assertEquals(FieldSource.AUTO, restored.identity.firmware?.source)
    }

    @Test
    fun `backup coverage registry includes every first schema table`() {
        val required = setOf(
            "centers", "networks", "devices", "device_interfaces", "interface_ips",
            "ip_endpoints", "services", "device_relations", "sourced_fields", "device_tags",
            "custom_field_values", "camera_profiles", "recorder_profiles", "pc_profiles",
            "remote_profiles", "local_agents", "incidents", "follow_ups", "monitoring_state",
        )
        assertEquals(required, CanonicalPersistenceTables.names)
        assertTrue(CanonicalPersistenceTables.names.none { it.isBlank() })
    }
}
