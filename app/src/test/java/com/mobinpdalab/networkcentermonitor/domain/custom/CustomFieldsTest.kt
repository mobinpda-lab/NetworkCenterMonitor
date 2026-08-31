package com.mobinpdalab.networkcentermonitor.domain.custom

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CustomFieldsTest {
    @Test
    fun requiredFieldRejectsBlankValue() {
        val definition = CustomFieldDefinition(
            id = "asset-room",
            title = "Asset Room",
            dataType = CustomFieldDataType.TEXT,
            required = true,
        )

        assertFalse(CustomFieldValueValidator.validate(definition, "  "))
        assertTrue(CustomFieldValueValidator.validate(definition, "Server Room"))
    }

    @Test
    fun selectFieldAcceptsOnlyConfiguredOptions() {
        val definition = CustomFieldDefinition(
            id = "priority",
            title = "Priority",
            dataType = CustomFieldDataType.SELECT,
            selectOptions = listOf("Low", "High"),
        )

        assertTrue(CustomFieldValueValidator.validate(definition, "High"))
        assertFalse(CustomFieldValueValidator.validate(definition, "Critical"))
    }

    @Test
    fun duplicateSelectOptionsAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            CustomFieldDefinition(
                id = "status",
                title = "Status",
                dataType = CustomFieldDataType.SELECT,
                selectOptions = listOf("A", "A"),
            )
        }
    }

    @Test
    fun archiveDisablesDefinitionWithoutDestroyingIdentity() {
        val active = CustomFieldDefinition(
            id = "rack",
            title = "Rack",
            dataType = CustomFieldDataType.TEXT,
        )

        val archived = active.archive()
        assertFalse(archived.enabled)
        assertTrue(archived.id == active.id)
    }
}
