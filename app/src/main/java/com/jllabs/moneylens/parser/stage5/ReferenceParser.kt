package com.jllabs.moneylens.parser.stage5

object ReferenceParser {
    private val REF_PATTERN = Regex(
        """(?i)(?:Ref(?:erence)?\s*(?:No\.?|Id|Number)?|Txn\s*(?:ID|Ref)?|URN|UPI\s*Ref(?:No)?\.?|IMPS\s*Ref|NEFT\s*Ref|RRN)\s*[:\.\-#]?\s*([A-Za-z0-9]{6,20})"""
    )
    private val UPI_ID_PATTERN = Regex(
        """([a-zA-Z0-9.\-_+]+@[a-zA-Z0-9]+)"""
    )

    fun extractReference(body: String): String = REF_PATTERN.find(body)?.groupValues?.get(1) ?: ""
    fun extractUpiId(body: String): String = UPI_ID_PATTERN.find(body)?.groupValues?.get(1) ?: ""
}
