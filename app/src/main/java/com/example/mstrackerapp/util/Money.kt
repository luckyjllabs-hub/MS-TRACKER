package com.example.mstrackerapp.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object Money {
    fun toMinor(rupees: Double): Long {
        return (rupees * 100).toLong()
    }

    fun toMajor(minor: Long): Double {
        return minor / 100.0
    }

    fun format(minor: Long, showSymbol: Boolean = true, absolute: Boolean = false): String {
        val valMinor = if (absolute) abs(minor) else minor
        val rupees = valMinor / 100.0
        val prefix = if (!absolute && minor < 0) "-" else ""

        val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
        val formatter = NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }

        val formattedWithSymbol = formatter.format(rupees)
        val formattedNumber = formattedWithSymbol.replace("₹", "").trim()

        return if (showSymbol) {
            "${prefix}₹$formattedNumber"
        } else {
            "${prefix}$formattedNumber"
        }
    }
}
