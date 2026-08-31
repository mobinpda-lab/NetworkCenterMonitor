package com.mobinpdalab.networkcentermonitor.data.local

import com.mobinpdalab.networkcentermonitor.domain.model.AssetMetadata
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

fun Device.toPersistedAggregate(): PersistedDeviceAggregate {
    val sourced = buildList {
        identity.manufacturer?.let { add(it.toEntity(id.value, "manufacturer")) }
        identity.brand?.let { add(it.toEntity(id.value, "brand")) }
        identity.model?.let { add(it.toEntity(id.value, "model")) }
        identity.hostname?.let { add(it.toEntity(id.value, "hostname")) }
        identity.serialNumber?.let { add(it.toEntity(id.value, "serialNumber")) }
        identity.firmware?.let { add(it.toEntity(id.value, "firmware")) }
        identity.operatingSystem?.let { add(it.toEntity(id.value, "operatingSystem")) }
        identity.imei?.let { add(it.toEntity(id.value, "imei")) }
        identity.simSerial?.let { add(it.toEntity(id.value, "simSerial")) }
        identity.operatorName?.let { add(it.toEntity(id.value, "operatorName")) }
        identity.dedicatedNumber?.let { add(it.toEntity(id.value, "dedicatedNumber")) }
        identity.phoneNumber?.let { add(it.toEntity(id.value, "phoneNumber")) }
    }

    val interfaceEntities = interfaces.map { item ->
        DeviceInterfaceEntity(
            id = item.id.value,
            deviceId = id.value,
            name = item.name,
            macAddress = item.macAddress?.toString(),
        )
    }
    val ipEntities = interfaces.flatMap { item ->
        item.ipAddresses.map { address -> InterfaceIpEntity(item.id.value, address.value) }
    }
    val mergedCustomFields = asset.customFieldValues + customFieldValues

    return PersistedDeviceAggregate(
        device = DeviceEntity(
            id = id.value,
            centerId = centerId.value,
            networkId = networkId?.value,
            displayName = displayName,
            type = type.name,
            monitoringEnabled = monitoringEnabled,
            status = status.name,
            lastSeenEpochMillis = lastSeenEpochMillis,
            lastStatusChangeEpochMillis = lastStatusChangeEpochMillis,
            lastDiscoveryEpochMillis = lastDiscoveryEpochMillis,
            notes = notes,
            emergencyPhone = asset.emergencyPhone,
            assetNumber = asset.assetNumber,
            imei = asset.imei,
            simSerialNumber = asset.simSerialNumber,
            deviceSerialNumber = asset.deviceSerialNumber,
            dedicatedNumber = asset.dedicatedNumber,
            operatorName = asset.operatorName,
            manufacturer = asset.manufacturer,
            model = asset.model,
        ),
        interfaces = interfaceEntities,
        interfaceIps = ipEntities,
        tags = tags.sorted().map { DeviceTagEntity(id.value, it) },
        customFields = mergedCustomFields.toSortedMap().map { (key, value) ->
            CustomFieldValueEntity("DEVICE", id.value, key, value)
        },
        sourcedFields = sourced,
    )
}

fun PersistedDeviceAggregate.toDomainDevice(): Device {
    val ipsByInterface = interfaceIps.groupBy { it.interfaceId }
    val fields = sourcedFields.associateBy { it.fieldKey }
    val custom = customFields.associate { it.fieldKey to it.value }

    return Device(
        id = DeviceId(device.id),
        centerId = CenterId(device.centerId),
        networkId = device.networkId?.let(::NetworkId),
        displayName = device.displayName,
        type = DeviceType.valueOf(device.type),
        identity = DeviceIdentity(
            manufacturer = fields["manufacturer"]?.toSourcedString(),
            brand = fields["brand"]?.toSourcedString(),
            model = fields["model"]?.toSourcedString(),
            hostname = fields["hostname"]?.toSourcedString(),
            serialNumber = fields["serialNumber"]?.toSourcedString(),
            firmware = fields["firmware"]?.toSourcedString(),
            operatingSystem = fields["operatingSystem"]?.toSourcedString(),
            imei = fields["imei"]?.toSourcedString(),
            simSerial = fields["simSerial"]?.toSourcedString(),
            operatorName = fields["operatorName"]?.toSourcedString(),
            dedicatedNumber = fields["dedicatedNumber"]?.toSourcedString(),
            phoneNumber = fields["phoneNumber"]?.toSourcedString(),
        ),
        interfaces = interfaces.map { item ->
            DeviceInterface(
                id = DeviceInterfaceId(item.id),
                deviceId = DeviceId(device.id),
                name = item.name,
                macAddress = item.macAddress?.let(::MacAddress),
                ipAddresses = ipsByInterface[item.id].orEmpty().map { Ipv4Address(it.address) },
            )
        },
        asset = AssetMetadata(
            emergencyPhone = device.emergencyPhone,
            assetNumber = device.assetNumber,
            imei = device.imei,
            simSerialNumber = device.simSerialNumber,
            deviceSerialNumber = device.deviceSerialNumber,
            dedicatedNumber = device.dedicatedNumber,
            operatorName = device.operatorName,
            manufacturer = device.manufacturer,
            model = device.model,
            customFieldValues = custom,
        ),
        monitoringEnabled = device.monitoringEnabled,
        status = MonitoringStatus.valueOf(device.status),
        lastSeenEpochMillis = device.lastSeenEpochMillis,
        lastStatusChangeEpochMillis = device.lastStatusChangeEpochMillis,
        lastDiscoveryEpochMillis = device.lastDiscoveryEpochMillis,
        tags = tags.map { it.tag }.toSet(),
        notes = device.notes,
        customFieldValues = custom,
    )
}

private fun SourcedValue<String>.toEntity(ownerId: String, key: String) = SourcedFieldEntity(
    ownerType = "DEVICE",
    ownerId = ownerId,
    fieldKey = key,
    value = value,
    source = source.name,
    lastUpdatedEpochMillis = lastUpdatedEpochMillis,
    lastDiscoveryEpochMillis = lastDiscoveryEpochMillis,
)

private fun SourcedFieldEntity.toSourcedString() = SourcedValue(
    value = value,
    source = FieldSource.valueOf(source),
    lastUpdatedEpochMillis = lastUpdatedEpochMillis,
    lastDiscoveryEpochMillis = lastDiscoveryEpochMillis,
)
