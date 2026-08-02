package com.example.mstrackerapp.ui.main

import com.example.mstrackerapp.presentation.navigation.AppTab
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenViewModelTest {
    @Test
    fun testAppTabValues() {
        assertEquals(AppTab.OVERVIEW, AppTab.valueOf("OVERVIEW"))
    }
}
