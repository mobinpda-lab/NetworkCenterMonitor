package com.mobinpdalab.networkcentermonitor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mobinpdalab.networkcentermonitor.domain.custom.CustomFieldDataType
import com.mobinpdalab.networkcentermonitor.domain.custom.CustomFieldDefinition

@Entity(tableName = "custom_field_definitions")
data class CustomFieldDefinitionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val dataType: String,
    val sortOrder: Int,
    val required: Boolean,
    val enabled: Boolean,
    val description: String? = null,
    val selectOptions: String = "",
)

fun CustomFieldDefinition.toEntity(): CustomFieldDefinitionEntity = CustomFieldDefinitionEntity(
    id = id,
    title = title,
    dataType = dataType.name,
    sortOrder = sortOrder,
    required = required,
    enabled = enabled,
    description = description,
    selectOptions = selectOptions.joinToString("\u001F"),
)

fun CustomFieldDefinitionEntity.toDomain(): CustomFieldDefinition = CustomFieldDefinition(
    id = id,
    title = title,
    dataType = CustomFieldDataType.valueOf(dataType),
    sortOrder = sortOrder,
    required = required,
    enabled = enabled,
    description = description,
    selectOptions = if (selectOptions.isBlank()) emptyList() else selectOptions.split("\u001F"),
)

/** Backup section coverage for the definition table and its value rows. */
object CustomFieldPersistenceCoverage {
    const val DEFINITIONS_TABLE = "custom_field_definitions"
    const val VALUES_TABLE = "custom_field_values"
    const val BACKUP_SECTION = "custom_fields"
}
