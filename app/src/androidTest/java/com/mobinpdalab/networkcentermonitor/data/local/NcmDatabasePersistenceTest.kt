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

    private fun openDatabase(): NcmDatabase = Room.databaseBuilder(
        context,
        NcmDatabase::class.java,
        databaseName,
    ).addMigrations(NcmDatabase.MIGRATION_1_2).build()
}
