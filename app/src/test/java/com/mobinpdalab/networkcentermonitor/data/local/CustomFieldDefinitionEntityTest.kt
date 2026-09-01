package com.mobinpdalab.networkcentermonitor.data.local

import com.mobinpdalab.networkcentermonitor.domain.backup.BackupSection
import com.mobinpdalab.networkcentermonitor.domain.backup.CanonicalBackupCoverage
import com.mobinpdalab.networkcentermonitor.domain.custom.CustomFieldDataType
import com.mobinpdalab.networkcentermonitor.domain.custom.CustomFieldDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomFieldDefinitionEntityTest {
    @Test
    fun definitionRoundTripsThroughPersistenceRepresentation() {
        val definition = CustomFieldDefinition(
            id = "room-type",
            title = "Room Type",
            dataType = CustomFieldDataType.SELECT,
            sortOrder = 3,
            required = true,
            description = "Physical room classification",
            selectOptions = listOf("Server", "Office", "Outdoor"),
        )

        assertEquals(definition, definition.toEntity().toDomain())
    }

    @Test
    fun customFieldBackupSectionIsMandatory() {
        assertTrue(BackupSection.CUSTOM_FIELDS in CanonicalBackupCoverage.requiredSections)
        assertEquals("custom_fields", CustomFieldPersistenceCoverage.BACKUP_SECTION)
        assertEquals("custom_field_definitions", CustomFieldPersistenceCoverage.DEFINITIONS_TABLE)
        assertEquals("custom_field_values", CustomFieldPersistenceCoverage.VALUES_TABLE)
    }
}
