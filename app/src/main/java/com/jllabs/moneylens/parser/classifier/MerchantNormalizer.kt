package com.jllabs.moneylens.parser.classifier

/**
 * Offline merchant alias → canonical name map.
 * One canonical merchant; many SMS aliases. Keys are uppercase.
 */
object MerchantNormalizer {

    /** alias (uppercase) → canonical display name */
    val BUILTIN_ALIASES: Map<String, String> = linkedMapOf(
        // Amazon
        "AMAZON" to "Amazon",
        "AMZN" to "Amazon",
        "AMAZON PAY" to "Amazon",
        "AMAZON SELLER" to "Amazon",
        "AMAZON SELLER S" to "Amazon",
        "AMAZON SELLER SERVICES" to "Amazon",
        "APAY" to "Amazon",
        "A.IN" to "Amazon",
        "QCAMZN" to "Amazon",

        // Food
        "SWIGGY" to "Swiggy",
        "SWIGGY LTD" to "Swiggy",
        "SWIGGY INSTAMART" to "Swiggy",
        "INSTAMART" to "Swiggy",
        "ZOMATO" to "Zomato",
        "ZOMATO LTD" to "Zomato",
        "DOMINOS" to "Dominos",
        "DOMINO'S" to "Dominos",
        "PIZZA HUT" to "Pizza Hut",
        "PIZZAHUT" to "Pizza Hut",
        "KFC" to "KFC",
        "KFC FORUM MALL" to "KFC",
        "MCDONALD" to "McDonald's",
        "MCDONALDS" to "McDonald's",
        "MCD" to "McDonald's",
        "STARBUCKS" to "Starbucks",
        "CAFE COFFEE DAY" to "Cafe Coffee Day",
        "CCD" to "Cafe Coffee Day",
        "HYDERABAD IRANI" to "Hyderabad Irani",
        "IRANI CAFE" to "Hyderabad Irani",
        "YASHIKA CHICKEN" to "Yashika Chicken",
        "FRESHTOHOME" to "FreshToHome",
        "FRESH TO HOME" to "FreshToHome",
        "EATSURE" to "EatSure",
        "FAASOS" to "Faasos",
        "BARBEQUE" to "Barbeque Nation",
        "BBQ NATION" to "Barbeque Nation",

        // Transport
        "UBER" to "Uber",
        "UBER INDIA" to "Uber",
        "UBER TRIP" to "Uber",
        "OLA" to "Ola",
        "OLA CABS" to "Ola",
        "RAPIDO" to "Rapido",
        "REDBUS" to "RedBus",
        "IRCTC" to "IRCTC",
        "INDIAN RAILWAYS" to "IRCTC",
        "NAMMA METRO" to "Namma Metro",
        "NAMMA YATRI" to "Namma Yatri",
        "CHALO" to "Chalo",

        // Fuel
        "INDIAN OIL" to "Indian Oil",
        "IOCL" to "Indian Oil",
        "HPCL" to "HP",
        "HP PETROL" to "HP",
        "BHARAT PETROLEUM" to "Bharat Petroleum",
        "BPCL" to "Bharat Petroleum",
        "SHELL" to "Shell",
        "ESSAR" to "Essar",
        "KESARI PETRO" to "Kesari Petro",
        "KESARI PETRO PA" to "Kesari Petro",
        "CHITHRA SERVICE" to "Chithra Service",

        // Shopping
        "FLIPKART" to "Flipkart",
        "MYNTRA" to "Myntra",
        "AJIO" to "Ajio",
        "MEESHO" to "Meesho",
        "NYKAA" to "Nykaa",
        "SNAPDEAL" to "Snapdeal",
        "RELIANCE DIGITAL" to "Reliance Digital",
        "RELIANCE" to "Reliance",
        "CROMA" to "Croma",
        "DMART" to "DMart",
        "D MART" to "DMart",
        "BIGBAZAAR" to "Big Bazaar",
        "BIG BASKET" to "BigBasket",
        "BIGBASKET" to "BigBasket",
        "BLINKIT" to "Blinkit",
        "ZEPTO" to "Zepto",
        "DUNZO" to "Dunzo",
        "GREEN CITY SUPE" to "Green City Supermarket",
        "GREEN CITY SUPER" to "Green City Supermarket",
        "GREEN CITY SUPERMARKET" to "Green City Supermarket",
        "SPAR" to "SPAR",
        "LENSKART" to "Lenskart",
        "BHIMA JEWELLERS" to "Bhima Jewellers",
        "BHIMA" to "Bhima Jewellers",
        "SAMSUNG" to "Samsung",

        // Entertainment
        "NETFLIX" to "Netflix",
        "SPOTIFY" to "Spotify",
        "HOTSTAR" to "Hotstar",
        "DISNEY HOTSTAR" to "Hotstar",
        "PRIME VIDEO" to "Prime Video",
        "AMAZON PRIME" to "Prime Video",
        "SONYLIV" to "SonyLiv",
        "BOOKMYSHOW" to "BookMyShow",
        "PVR" to "PVR",
        "INOX" to "INOX",
        "YOUTUBE PREMIUM" to "YouTube Premium",
        "YOUTUBE" to "YouTube Premium",

        // Health
        "APOLLO" to "Apollo",
        "APOLLO PHARMACY" to "Apollo",
        "MEDPLUS" to "MedPlus",
        "MEDPLUS CHANNAS" to "MedPlus",
        "PHARMEASY" to "PharmEasy",
        "1MG" to "1mg",
        "NETMEDS" to "Netmeds",
        "PRACTO" to "Practo",
        "ASTER" to "Aster",
        "ASTER DM HEALTH" to "Aster",
        "SVASTHA HOSPITA" to "Svastha Hospital",
        "SVASTHA HOSPITAL" to "Svastha Hospital",
        "SHREENITA HOSPI" to "Shreenita Hospital",
        "SHREENITA HOSPITAL" to "Shreenita Hospital",
        "REDCLIFFE LABS" to "Redcliffe Labs",
        "REDCLIFFE" to "Redcliffe Labs",

        // Bills / utilities / telecom
        "BESCOM" to "BESCOM",
        "TSSPDCL" to "TSSPDCL",
        "APSPDCL" to "APSPDCL",
        "MSEDCL" to "MSEDCL",
        "TATA POWER" to "Tata Power",
        "TATAPOWER" to "Tata Power",
        "GAIL" to "GAIL Gas",
        "GAIL GAS" to "GAIL Gas",
        "GAIL GAS LIMITE" to "GAIL Gas",
        "JIO" to "Jio",
        "JIO PREPAID" to "Jio",
        "JIO PREPAID REC" to "Jio",
        "AIRTEL" to "Airtel",
        "VODAFONE" to "Vi",
        "IDEA" to "Vi",
        "VI " to "Vi",
        "BSNL" to "BSNL",

        // Travel
        "MAKEMYTRIP" to "MakeMyTrip",
        "GOIBIBO" to "Goibibo",
        "YATRA" to "Yatra",
        "CLEARTRIP" to "Cleartrip",
        "INDIGO" to "IndiGo",
        "SPICEJET" to "SpiceJet",
        "AIR INDIA" to "Air India",
        "AIRINDIA" to "Air India",
        "OYO" to "OYO",
        "AIRBNB" to "Airbnb",

        // Education
        "COURSERA" to "Coursera",
        "UDEMY" to "Udemy",
        "BYJU" to "BYJU'S",
        "BYJUS" to "BYJU'S",
        "UNACADEMY" to "Unacademy",
        "VEDANTU" to "Vedantu",

        // Insurance / EMI / finance
        "LIC" to "LIC",
        "POLICYBAZAAR" to "PolicyBazaar",
        "BAJAJ FINANCE" to "Bajaj Finance",
        "BAJAJFINSERV" to "Bajaj Finance",
        "HDFC LOAN" to "HDFC Loan",
        "ICICI LOAN" to "ICICI Loan",

        // Investments
        "GROWW" to "Groww",
        "ZERODHA" to "Zerodha",
        "UPSTOX" to "Upstox",
        "PAYTM MONEY" to "Paytm Money",
        "PAYTMMONEY" to "Paytm Money",
        "ICICI DIRECT" to "ICICI Direct",
        "HDFC SECURITIES" to "HDFC Securities",
        "SBI MF" to "SBI MF",

        // Wallets / payments (canonical for merchant field; category often Transfer)
        "PHONEPE" to "PhonePe",
        "GOOGLE PAY" to "Google Pay",
        "GPAY" to "Google Pay",
        "PAYTM" to "Paytm",
        "BHIM" to "BHIM",

        // Urban Company etc.
        "URBANCOMPANY" to "Urban Company",
        "URBAN COMPANY" to "Urban Company"
    )

    /** Placeholders that are not real merchants */
    private val UNKNOWN_PLACEHOLDERS = setOf(
        "UNKNOWN", "UNKNOWN MERCHANT", "BANK TRANSACTION", "BANK DEPOSIT",
        "UPI TRANSFER", "CARD PURCHASE", "DEBIT", "CREDIT", "ATM"
    )

    /**
     * Normalize a raw extracted merchant string to a canonical name.
     * Longest alias match wins. Uses word-boundary matching so "OLA"
     * does not match inside "POLAMREDDY".
     */
    fun normalize(rawMerchant: String, extraAliases: Map<String, String> = emptyMap()): String {
        val cleaned = clean(rawMerchant)
        if (cleaned.isEmpty()) return "Unknown"
        val upper = cleaned.uppercase()

        val allAliases = (extraAliases.mapKeys { it.key.uppercase() } + BUILTIN_ALIASES)
            .entries
            .sortedByDescending { it.key.length }

        for ((alias, canonical) in allAliases) {
            if (upper == alias || aliasMatchesText(upper, alias)) {
                return canonical
            }
        }
        // Title-case residual merchant for display consistency (keep VPA-style handles lowercase)
        if (cleaned.contains('.') || cleaned.contains('_') || cleaned.contains('-') && cleaned.any { it.isDigit() }) {
            return cleaned.lowercase().take(40)
        }
        return cleaned.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.titlecase() }
        }.take(40)
    }

    fun isKnownMerchant(merchant: String): Boolean {
        val upper = merchant.uppercase().trim()
        if (upper.isEmpty() || UNKNOWN_PLACEHOLDERS.any { upper.contains(it) }) return false
        return BUILTIN_ALIASES.keys.any { aliasMatchesText(upper, it) || it.equals(upper, ignoreCase = true) } ||
            BUILTIN_ALIASES.values.any { it.equals(merchant, ignoreCase = true) }
    }

    /** True when [alias] appears as a whole token in [text] (not a substring of a longer word). */
    fun aliasMatchesText(text: String, alias: String): Boolean {
        if (alias.isBlank()) return false
        val escaped = Regex.escape(alias.trim())
        return Regex("""(?i)(?<![\p{L}\p{N}])$escaped(?![\p{L}\p{N}])""").containsMatchIn(text)
    }

    fun clean(raw: String): String {
        return raw
            .replace(Regex("""\d{6,}"""), "")
            .replace(Regex("""@.*$"""), "") // strip VPA domain only (do not cut on '.')
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('.', ',', '-', '/')
            .take(40)
    }
}
