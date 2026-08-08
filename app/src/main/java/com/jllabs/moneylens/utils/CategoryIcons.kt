package com.jllabs.moneylens.utils

/**
 * Category icon helpers. Newly created categories often store corrupted emoji
 * (mojibake like "âœ¨"). Prefer a stable letter fallback when the icon is unusable.
 */
object CategoryIcons {

    fun letterFor(name: String): String {
        val ch = name.trim().firstOrNull { it.isLetter() }?.uppercaseChar()
        return (ch ?: '?').toString()
    }

    /** Icon safe for UI display; falls back to the category name's first letter. */
    fun display(icon: String?, name: String): String {
        val raw = icon?.trim().orEmpty()
        if (raw.isEmpty()) return letterFor(name)
        if (isBroken(raw)) return letterFor(name)
        return raw
    }

    /** Prefer a letter when creating categories unless the user typed a clean emoji. */
    fun sanitizeForStorage(icon: String?, name: String): String {
        val raw = icon?.trim().orEmpty()
        if (raw.isEmpty() || isBroken(raw)) return letterFor(name)
        // Single ASCII letter is fine
        if (raw.length == 1 && raw[0].isLetter()) return raw.uppercase()
        // Likely a real emoji / symbol (BMP symbol or surrogate pair)
        if (raw.any { it.isSurrogate() } || raw.any { it.code > 0x24F }) return raw
        // Latin-1 high bytes without surrogate → almost always mojibake
        if (raw.any { it.code in 0x80..0xFF }) return letterFor(name)
        return raw
    }

    fun isBroken(icon: String): Boolean {
        if (icon.isEmpty()) return true
        if (icon.contains('\uFFFD')) return true
        // Classic UTF-8-as-Latin-1 mojibake markers
        if (icon.any { it in setOf('â', 'ð', 'Ã', 'ï', 'Â') }) return true
        // "âœ¨" style: multiple Latin-1 chars that were meant to be one emoji
        val highLatin = icon.count { it.code in 0x80..0xFF }
        if (highLatin >= 2 && icon.none { it.isSurrogate() }) return true
        return false
    }
}
