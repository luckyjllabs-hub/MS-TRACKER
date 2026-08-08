package com.jllabs.moneylens.parser.regex

object BankRegexPatterns {
    val HDFC_PATTERN = Regex("""(?:spend of|debited by|spent on)\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
    val ICICI_PATTERN = Regex("""(?:INR|Rs\.?|₹)\s*([\d,]+\.?\d*)\s*(?:debited|spent)""", RegexOption.IGNORE_CASE)
    val SBI_PATTERN = Regex("""(?:debited for|spent at)\s*(?:INR|Rs\.?|₹)?\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
}
