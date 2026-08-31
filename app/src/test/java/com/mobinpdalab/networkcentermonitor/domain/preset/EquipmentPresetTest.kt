package com.mobinpdalab.networkcentermonitor.domain.preset

import com.mobinpdalab.networkcentermonitor.domain.discovery.DiscoveryProbeMethod
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceType
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentPresetTest {
    @Test
    fun `camera preset produces real onvif rtsp and tcp discovery plan`() {
        val plan = buildPresetDiscoveryPlan(listOf(BuiltInEquipmentPresets.camera))

        assertTrue(DiscoveryProbeMethod.ONVIF in plan.methods)
        assertTrue(DiscoveryProbeMethod.RTSP in plan.methods)
        assertTrue(DiscoveryProbeMethod.TCP in plan.methods)
        assertEquals(setOf(80, 443, 554), plan.ports.map { it.value }.toSet())
    }

    @Test
    fun `multiple presets deduplicate shared ports`() {
        val plan = buildPresetDiscoveryPlan(
            listOf(BuiltInEquipmentPresets.camera, BuiltInEquipmentPresets.recorder),
        )

        assertEquals(listOf(80, 443, 554), plan.ports.map { it.value })
    }

    @Test
    fun `archived user preset does not enter discovery plan`() {
        val userPreset = EquipmentPreset(
            id = EquipmentPresetId("user-1"),
            name = "Legacy appliance",
            deviceType = DeviceType.CUSTOM,
            origin = PresetOrigin.USER,
            ports = listOf(PresetPort("custom", PresetProtocol.TCP, NetworkPort(12345))),
        ).archive()

        val plan = buildPresetDiscoveryPlan(listOf(userPreset))

        assertTrue(plan.ports.isEmpty())
        assertFalse(DiscoveryProbeMethod.TCP in plan.methods)
    }

    @Test
    fun `built in catalog contains operational camera pc and router presets`() {
        val types = BuiltInEquipmentPresets.all.map { it.deviceType }.toSet()
        assertTrue(DeviceType.CAMERA in types)
        assertTrue(DeviceType.PC in types)
        assertTrue(DeviceType.MIKROTIK in types)
        assertTrue(BuiltInEquipmentPresets.all.all { it.ports.isNotEmpty() })
    }
}
