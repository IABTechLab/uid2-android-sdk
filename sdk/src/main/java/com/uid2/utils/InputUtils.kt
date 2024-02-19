package com.uid2.utils

import com.uid2.InputValidationException
import com.uid2.data.IdentityRequest
import com.uid2.utils.InputUtils.EmailParsingState.Starting
import com.uid2.utils.InputUtils.EmailParsingState.SubDomain
import java.util.Locale

/**
 * This class contains a set of methods that will attempt to normalize a given [IdentityRequest]. This is to validate
 * what is provided to us to ensure it's in the expected format, before we attempt to consume it.
 */
internal class InputUtils {

    /**
     * Attempts to normalize the given [IdentityRequest.Phone]
     *
     * @throws InputValidationException Thrown if the input could not be normalized.
     */
    @Throws(InputValidationException::class)
    fun normalize(request: IdentityRequest.Phone): IdentityRequest.Phone {
        if (!isNormalized(request)) {
            throw InputValidationException("Phone number is not normalized to ITU E.164 standard")
        }

        return request
    }

    /**
     * Attempts to normalize the given [IdentityRequest.Email]
     *
     * @throws InputValidationException Thrown if the input could not be normalized.
     */
    @Throws(InputValidationException::class)
    fun normalize(request: IdentityRequest.Email): IdentityRequest.Email {
        val normalized = normalizeEmail(request.data)
            ?: throw InputValidationException("Invalid email address detected")

        return IdentityRequest.Email(normalized)
    }

    /**
     * Returns whether or not the given [IdentityRequest.Phone] has already been normalized. This will check against
     * the ITU E.164 Standard (https://en.wikipedia.org/wiki/E.164).
     */
    private fun isNormalized(request: IdentityRequest.Phone): Boolean {
        val number = request.data

        // Firstly, let's check to make sure we have a non-empty string
        if (number.isEmpty()) {
            return false
        }

        // The first character should be a '+'.
        if (number[0] != '+') {
            return false
        }

        // Check to make sure that only digits are contained.
        val allDigits = number.substring(1).all { isAsciiDigit(it) }
        if (!allDigits) {
            return false
        }

        // The number of digits (excluding the '+') should be in the expected range.
        val totalDigits = number.length - 1
        return !(totalDigits < MIN_PHONE_NUMBER_DIGITS || totalDigits> MAX_PHONE_NUMBER_DIGITS)
    }

    /**
     * Returns whether or not the given [String] contains only ASCII digits.
     */
    private fun isAsciiDigit(digit: Char): Boolean {
        return digit in '0'..'9'
    }

    private enum class EmailParsingState {
        Starting,
        SubDomain,
    }

    /**
     * This code will attempt to normalize a given email address. It's been translated from a Java reference, and
     * therefore been kept as close to the original implementation as possible (with some unused conditions removed).
     *
     * https://github.com/IABTechLab/uid2-operator/blob/a331b88bcb1d7a1a9f0128a7ca0ff4b1de6f0779/src/main/java/com/uid2/operator/service/InputUtil.java#L96
     */
    private fun normalizeEmail(email: String): String? {
        val preSubDomain = StringBuilder()
        val preSubDomainSpecialized = StringBuilder()
        val subDomain = StringBuilder()
        val subDomainWhiteSpace = StringBuilder()

        var parsingState = Starting
        var inExtension = false

        // Let's start by converting the given address to lower case, before iterating over the individual characters.
        val lower = email.lowercase(Locale.getDefault())
        lower.forEach { char ->
            when (parsingState) {
                Starting -> {
                    if (char == ' ') {
                        return@forEach
                    } else if (char == '@') {
                        parsingState = SubDomain
                    } else if (char == '.') {
                        preSubDomain.append(char)
                    } else if (char == '+') {
                        preSubDomain.append(char)
                        inExtension = true
                    } else {
                        preSubDomain.append(char)
                        if (!inExtension) {
                            preSubDomainSpecialized.append(char)
                        }
                    }
                }

                SubDomain -> {
                    if (char == '@') {
                        return null
                    } else if (char == ' ') {
                        subDomainWhiteSpace.append(char)
                        return@forEach
                    }

                    if (subDomainWhiteSpace.isNotEmpty()) {
                        subDomain.append(subDomainWhiteSpace.toString())
                        subDomainWhiteSpace.clear()
                    }

                    subDomain.append(char)
                }
            }
        }

        // Verify that we've parsed the subdomain correctly.
        if (subDomain.isEmpty()) {
            return null
        }

        // Verify that we've parsed the address part correctly.
        val addressPartToUse = if (DOMAIN_GMAIL == subDomain.toString()) {
            preSubDomainSpecialized
        } else {
            preSubDomain
        }

        if (addressPartToUse.isEmpty()) {
            return null
        }

        // Build the normalized version of the email address.
        return addressPartToUse.append('@').append(subDomain.toString()).toString()
    }

    private companion object {
        const val MIN_PHONE_NUMBER_DIGITS = 10
        const val MAX_PHONE_NUMBER_DIGITS = 15

        const val DOMAIN_GMAIL = "gmail.com"
    }
}
