package com.jllabs.moneylens.ui.main

import com.jllabs.moneylens.presentation.navigation.AppTab
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun testAppTabValues() {
        assertEquals(AppTab.OVERVIEW, AppTab.valueOf("OVERVIEW"))
    }
}
