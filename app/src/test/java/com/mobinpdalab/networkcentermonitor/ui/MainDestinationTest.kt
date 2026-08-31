package com.mobinpdalab.networkcentermonitor.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MainDestinationTest {
    @Test
    fun canonicalNavigationHasFiveTabsInExpectedOrder() {
        assertEquals(
            listOf("خانه", "قطعی‌ها", "پیگیری‌ها", "گزارش‌ها", "تنظیمات"),
            MainDestination.entries.map { it.title }
        )
    }
}
