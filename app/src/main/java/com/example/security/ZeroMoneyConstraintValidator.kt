package com.example.security

import android.content.Intent
import java.util.Locale
import java.util.regex.Pattern

data class SecurityCheckResult(
    val isAllowed: Boolean,
    val violationType: String? = null,
    val reason: String? = null
)

object ZeroMoneyConstraintValidator {

    private val REJECTED_KEYWORDS = listOf(
        // UPI & Payment Services
        "upi", "bhim", "gpay", "google pay", "phonepe", "paytm", "paypal", "venmo", "cashapp",
        "send money", "transfer money", "request money", "collect request", "scan to pay",
        // Banking & Cards
        "bank transfer", "netbanking", "routing number", "account number", "iban", "swift code",
        "credit card", "debit card", "cvv", "cvv2", "atm pin", "card expiry", "wire transfer",
        // Cryptocurrencies
        "crypto", "bitcoin", "btc", "ethereum", "eth", "solana", "sol", "usdt", "tether",
        "seed phrase", "private key", "wallet address", "metamask", "binance", "coinbase",
        // OTP & Authorization
        "transaction pin", "payment otp", "bank otp", "card authorization"
    )

    private val CARD_NUMBER_REGEX = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b")
    private val UPI_ID_REGEX = Pattern.compile("(?i)[a-zA-Z0-9.\\-_]{2,64}@(ok[a-zA-Z]+|upi|ybl|axl|paytm|barodampay|icici|sbi|ibl)")
    private val CRYPTO_ADDR_REGEX = Pattern.compile("\\b(0x[a-fA-F0-9]{40}|[13][a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[a-z0-9]{39,59})\\b")

    /**
     * Inspects plain text, OCR tokens, and transcribed voice commands against Zero Money Constraints.
     */
    fun validateText(input: String): SecurityCheckResult {
        val lower = input.lowercase(Locale.ROOT)

        // 1. Check keyword presence
        for (keyword in REJECTED_KEYWORDS) {
            if (lower.contains(keyword)) {
                return SecurityCheckResult(
                    isAllowed = false,
                    violationType = "ZERO_MONEY_KEYWORD_MATCH",
                    reason = "Forbidden financial/payment keyword detected: '$keyword'"
                )
            }
        }

        // 2. Check UPI Pattern
        if (UPI_ID_REGEX.matcher(input).find()) {
            return SecurityCheckResult(
                isAllowed = false,
                violationType = "UPI_IDENTIFIER_INTERCEPTED",
                reason = "UPI Virtual Payment Address pattern identified."
            )
        }

        // 3. Check Crypto Wallet Address Pattern
        if (CRYPTO_ADDR_REGEX.matcher(input).find()) {
            return SecurityCheckResult(
                isAllowed = false,
                violationType = "CRYPTO_ADDRESS_INTERCEPTED",
                reason = "Cryptographic wallet public key/address pattern identified."
            )
        }

        // 4. Check Credit/Debit Card Pattern (with basic length validation)
        val cardMatcher = CARD_NUMBER_REGEX.matcher(input)
        while (cardMatcher.find()) {
            val digitsOnly = cardMatcher.group().replace(Regex("[^0-9]"), "")
            if (digitsOnly.length in 13..19 && passesLuhnCheck(digitsOnly)) {
                return SecurityCheckResult(
                    isAllowed = false,
                    violationType = "PAYMENT_CARD_INTERCEPTED",
                    reason = "Valid payment card digit sequence identified."
                )
            }
        }

        return SecurityCheckResult(isAllowed = true)
    }

    /**
     * Validates Android Intents to guarantee zero financial, payment, or banking dispatching.
     */
    fun validateIntent(intent: Intent): SecurityCheckResult {
        val action = intent.action ?: ""
        val scheme = intent.data?.scheme?.lowercase(Locale.ROOT) ?: ""
        val dataStr = intent.dataString?.lowercase(Locale.ROOT) ?: ""

        val forbiddenSchemes = listOf("upi", "bitcoin", "ethereum", "solana", "lightning", "sol")
        if (scheme in forbiddenSchemes || forbiddenSchemes.any { dataStr.startsWith("$it:") }) {
            return SecurityCheckResult(
                isAllowed = false,
                violationType = "FORBIDDEN_INTENT_SCHEME",
                reason = "Disallowed payment scheme '$scheme' blocked by Intent Firewall."
            )
        }

        if (action.contains("PAY", ignoreCase = true) || dataStr.contains("payment", ignoreCase = true)) {
            return SecurityCheckResult(
                isAllowed = false,
                violationType = "PAYMENT_INTENT_ACTION_BLOCKED",
                reason = "Intent action or URI targets financial processing."
            )
        }

        return SecurityCheckResult(isAllowed = true)
    }

    private fun passesLuhnCheck(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i].digitToIntOrNull() ?: return false
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}
