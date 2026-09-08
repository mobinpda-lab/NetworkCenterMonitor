package com.mobinpdalab.networkcentermonitor.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class NcmDatabasePersistenceTest {
    private lateinit var context: Context
    private val databaseName = "data-033-persistence-proof.db"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun deviceAggregate_survives_closeAndReopen_withRelationsAndSourceMetadata() = runBlocking {
        val first = openDatabase()
        val dao = first.canonicalDao()
        dao.upsertCenter(CenterEntity("center-1", "province-1", "Test Center"))
        dao.upsertNetwork(
            NetworkEntity(
                id = "network-1",
                centerId = "center-1",
                name = "Test Network",
                type = "LAN",
                accessMethod = "STATIC",
                monitoringEnabled = true,
                discoveryEnabled = true,
            ),
        )

        val device = DeviceEntity(
            id = "device-1",
            centerId = "center-1",
            networkId = "network-1",
            displayName = "Router 1",
            type = "ROUTER",
            monitoringEnabled = true,
            status = "UP",
        )
        val interfaces = listOf(
            DeviceInterfaceEntity("if-1", "device-1", "eth0", "AA:BB:CC:DD:EE:01"),
            DeviceInterfaceEntity("if-2", "device-1", "eth1", "AA:BB:CC:DD:EE:02"),
        )
        val ips = listOf(
            InterfaceIpEntity("if-1", "192.0.2.10"),
            InterfaceIpEntity("if-1", "2001:db8::10"),
            InterfaceIpEntity("if-2", "198.51.100.10"),
        )
        val source = SourcedFieldEntity(
            ownerType = "DEVICE",
            ownerId = "device-1",
            fieldKey = "displayName",
            value = "Router 1",
            source = "MANUAL",
            lastUpdatedEpochMillis = 1000L,
        )

        dao.replaceDeviceAggregate(
            PersistedDeviceAggregate(
                device = device,
                interfaces = interfaces,
                interfaceIps = ips,
                sourcedFields = listOf(source),
            ),
        )
        first.close()

        val second = openDatabase()
        try {
            val restored = second.canonicalDao().loadDeviceAggregate("device-1")
            assertNotNull(restored)
            assertEquals(device, restored!!.device)
            assertEquals(interfaces, restored.interfaces)
            assertEquals(ips, restored.interfaceIps)
            assertEquals(listOf(source), restored.sourcedFields)
        } finally {
            second.close()
        }
    }

    @Test
    fun specializedProfiles_survive_closeAndReopen_andReferenceCanonicalDeviceIdentity() = runBlocking {
        val first = openDatabase()
        val dao = first.canonicalDao()
        dao.upsertCenter(CenterEntity("center-1", "province-1", "Test Center"))
        dao.upsertNetwork(
            NetworkEntity(
                id = "network-1",
                centerId = "center-1",
                name = "Test Network",
                type = "LAN",
                accessMethod = "STATIC",
                monitoringEnabled = true,
                discoveryEnabled = true,
            ),
        )

        val camera = DeviceEntity(
            id = "camera-device-1",
            centerId = "center-1",
            networkId = "network-1",
            displayName = "Camera 1",
            type = "CAMERA",
            monitoringEnabled = true,
            status = "UP",
        )
        val recorder = DeviceEntity(
            id = "recorder-device-1",
            centerId = "center-1",
            networkId = "network-1",
            displayName = "Recorder 1",
            type = "NVR",
            monitoringEnabled = true,
            status = "UP",
        )
        val pc = DeviceEntity(
            id = "pc-device-1",
            centerId = "center-1",
            networkId = "network-1",
            displayName = "PC 1",
            type = "PC",
            monitoringEnabled = true,
            status = "UP",
        )
        dao.upsertDevice(camera)
        dao.upsertDevice(recorder)
        dao.upsertDevice(pc)

        dao.upsertCameraProfile(
            CameraProfileEntity(
                id = "camera-profile-1",
                deviceId = camera.id,
                environmentName = "Entrance",
                cameraType = "DOME",
                recorderDeviceId = recorder.id,
                channelNumber = 1,
                capabilities = "ONVIF,RTSP",
            ),
        )
        dao.upsertRecorderProfile(
            RecorderProfileEntity(
                id = "recorder-profile-1",
                deviceId = recorder.id,
                recordingSupported = true,
                hddHealthSupported = true,
                onvifProfiles = "S,T",
            ),
        )
        dao.upsertPcProfile(
            PcProfileEntity(
                id = "pc-profile-1",
                deviceId = pc.id,
            ),
        )
        first.close()

        val second = openDatabase()
        try {
            assertEquals(camera.id, profileDeviceId(second, "camera_profiles", "camera-profile-1"))
            assertEquals(recorder.id, profileDeviceId(second, "recorder_profiles", "recorder-profile-1"))
            assertEquals(pc.id, profileDeviceId(second, "pc_profiles", "pc-profile-1"))

            assertNotNull(second.canonicalDao().getDevice(camera.id))
            assertNotNull(second.canonicalDao().getDevice(recorder.id))
            assertNotNull(second.canonicalDao().getDevice(pc.id))
        } finally {
            second.close()
        }
    }

    private fun profileDeviceId(
        database: NcmDatabase,
        table: String,
        profileId: String,
    ): String? {
        require(table in setOf("camera_profiles", "recorder_profiles", "pc_profiles"))
        database.openHelper.readableDatabase.query(
            "SELECT deviceId FROM $table WHERE id = ?",
            arrayOf(profileId),
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun openDatabase(): NcmDatabase = Room.databaseBuilder(
        context,
        NcmDatabase::class.java,
        databaseName,
    ).addMigrations(NcmDatabase.MIGRATION_1_2).build()
}
