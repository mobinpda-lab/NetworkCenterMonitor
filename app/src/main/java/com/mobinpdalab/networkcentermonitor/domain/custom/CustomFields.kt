package com.mobinpdalab.networkcentermonitor.domain.custom

enum class CustomFieldDataType {
    TEXT,
    NUMBER,
    DATE,
    DATE_TIME,
    PHONE,
    BOOLEAN,
    SELECT,
    MULTI_LINE,
}

data class CustomFieldDefinition(
    val id: String,
    val title: String,
    val dataType: CustomFieldDataType,
    val sortOrder: Int = 0,
    val required: Boolean = false,
    val enabled: Boolean = true,
    val description: String? = null,
    val selectOptions: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Custom field id must not be blank" }
        require(title.isNotBlank()) { "Custom field title must not be blank" }
        require(sortOrder >= 0) { "Custom field sort order must be non-negative" }
        require(dataType == CustomFieldDataType.SELECT || selectOptions.isEmpty()) {
            "Select options are only valid for SELECT custom fields"
        }
        if (dataType == CustomFieldDataType.SELECT) {
            val normalizedOptions = selectOptions.map { it.trim() }
            require(normalizedOptions.all { it.isNotEmpty() } && normalizedOptions.distinct().size == normalizedOptions.size) {
                "SELECT options must be non-blank and unique"
            }
        }
    }

    fun archive(): CustomFieldDefinition = copy(enabled = false)
}

object CustomFieldValueValidator {
    fun validate(definition: CustomFieldDefinition, value: String?): Boolean {
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty()) return !definition.required
        return when (definition.dataType) {
            CustomFieldDataType.TEXT, CustomFieldDataType.MULTI_LINE, CustomFieldDataType.PHONE -> true
            CustomFieldDataType.NUMBER -> normalized.toBigDecimalOrNull() != null
            CustomFieldDataType.DATE -> DATE.matches(normalized)
            CustomFieldDataType.DATE_TIME -> DATE_TIME.matches(normalized)
            CustomFieldDataType.BOOLEAN -> normalized.equals("true", true) || normalized.equals("false", true)
            CustomFieldDataType.SELECT -> normalized in definition.selectOptions
        }
    }

    private val DATE = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val DATE_TIME = Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?")
}
