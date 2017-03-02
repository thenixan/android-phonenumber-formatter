package ru.nixan.phoneformatter

import android.text.Editable

class PhoneFormat(val mcc: Array<String>, val dividers: Array<Divider>, val defCodeFormatVariants: Array<DefcodeFormat>, val countryId: Int, val countryCode: String, val numberLength: Int, val trunk: String?) {


    private val codePrefixStartPosition = 0

    private val codePrefixEndPosition = codePrefixStartPosition + 1

    private val countryCodeStartPosition = codePrefixEndPosition

    private val countryCodeEndPosition = countryCodeStartPosition + countryCode.length

    private val defCodeStartPosition = countryCodeEndPosition

    override fun toString(): String {
        val result = StringBuilder(CODE_PREFIX.toString())
        result.append(countryCode)
        defCodeFormatVariants.forEach {
            result.append(it.defCodeStarts.joinToString(separator = "|", prefix = "[", postfix = "]"))
            result.append(it.defCodeNotStarts.map { "!$it" }.joinToString(separator = "|", prefix = "[", postfix = "]"))
            result.append("D".repeat(it.defCodeLength))
        }
        result.append("x".repeat(numberLength))
        return result.toString()
    }

    fun format(input: Editable) {

        val format: DefcodeFormat? = findBestDefCode(input.toString().filter(Char::isDigit))?.first

        trunk?.let {
            if (input.length >= it.length && !input.startsWith("+") && input.startsWith(it)) {
                input.replace(0, it.length, "+$countryCode")
            }
        }

        var i = 0
        while (i < input.length) {
            getCharAt(i, input, format)?.let {
                if (input[i] != it) {
                    input.replace(i, i + if (input[i] in '0'..'9') 0 else 1, it.toString())
                }
            } ?: apply {
                input.delete(i, i + 1)
                i--
            }
            i++
        }
    }

    fun validate(phoneNumber: String): MatchResult {
        val countryCodeValidationResult = validateCountryCode(phoneNumber)
        if (countryCodeValidationResult == MatchResult.FULL) {
            val bestDefCode = findBestDefCode(phoneNumber)
            bestDefCode?.let {
                if (it.second == MatchResult.FULL) {
                    return validatePhoneNumber(phoneNumber, it.first)
                } else if (it.second == MatchResult.SHORT) {
                    return MatchResult.SHORT
                }
            }
            return MatchResult.NO
        } else {
            return countryCodeValidationResult
        }
    }

    private fun findBestDefCode(phoneNumber: String): Pair<DefcodeFormat, MatchResult>? {
        defCodeFormatVariants.find { validateDefCode(phoneNumber, it) == MatchResult.FULL }?.let {
            return it to MatchResult.FULL
        }
        return defCodeFormatVariants.find { validateDefCode(phoneNumber, it) == MatchResult.SHORT }?.let {
            it to MatchResult.SHORT
        }
    }

    private fun validateCountryCode(phoneNumber: String): MatchResult {
        if (phoneNumber.isEmpty()) {
            return MatchResult.NO
        } else if (phoneNumber.length < countryCode.length && phoneNumber.startsWith(countryCode.take(phoneNumber.length))) {
            return MatchResult.SHORT
        } else if (phoneNumber.startsWith(countryCode)) {
            return MatchResult.FULL
        } else {
            return MatchResult.NO
        }
    }

    private fun validateDefCode(phoneNumber: String, defCodeFormat: DefcodeFormat): MatchResult {
        if (phoneNumber.isEmpty() || phoneNumber.length < countryCode.length) {
            return MatchResult.NO
        } else if (phoneNumber.length >= countryCode.length) {
            val defCode = phoneNumber.drop(countryCode.length).take(defCodeFormat.defCodeLength)
            if ((defCodeFormat.defCodeStarts.isEmpty() || defCodeFormat.defCodeStarts.any { defCode.startsWith(it) }) && defCodeFormat.defCodeNotStarts.none { defCode.startsWith(it) }) {
                if (defCode.length == defCodeFormat.defCodeLength) {
                    return MatchResult.FULL
                } else {
                    return MatchResult.SHORT
                }
            } else {
                return MatchResult.NO
            }
        } else {
            return MatchResult.NO
        }
    }

    private fun validatePhoneNumber(phoneNumber: String, defCodeFormat: DefcodeFormat): MatchResult {
        if (phoneNumber.isEmpty() || phoneNumber.length < countryCode.length + defCodeFormat.defCodeLength) {
            return MatchResult.NO
        } else if (phoneNumber.length < countryCode.length + defCodeFormat.defCodeLength + numberLength) {
            return MatchResult.SHORT
        } else if (phoneNumber.length == countryCode.length + defCodeFormat.defCodeLength + numberLength) {
            return MatchResult.FULL
        } else {
            return MatchResult.LONG
        }
    }

    fun getCharAt(position: Int, editable: Editable, format: DefcodeFormat?): Char? {
        val character = editable[position]
        if (codePrefixStartPosition <= position && position < codePrefixEndPosition) {
            return CODE_PREFIX
        } else if (countryCodeStartPosition <= position && position < countryCodeEndPosition) {
            return countryCode[position - countryCodeStartPosition]
        } else {
            return format?.let {
                if (defCodeStartPosition <= position && position < it.getDefCodeEndPosition(defCodeStartPosition)) {
                    it.getDefCodeCharAt(position - defCodeStartPosition, character)
                } else if (getPhoneNumberStartPosition(it) <= position && position < getPhoneNumberEndPosition(it)) {
                    getNumberCharAt(position - getPhoneNumberStartPosition(it), character)
                } else {
                    null
                }
            }
        }
    }

    private fun getNumberCharAt(position: Int, character: Char): Char? {
        dividers.find { it.position <= position && position < it.position + it.formatting.length }?.let {
            return it.formatting[position - it.position]
        }
        return if (character in '0'..'9') character else null
    }

    private fun getPhoneNumberStartPosition(defcodeFormat: DefcodeFormat) = defcodeFormat.getDefCodeEndPosition(defCodeStartPosition)

    private fun getPhoneNumberEndPosition(defcodeFormat: DefcodeFormat) = getPhoneNumberStartPosition(defcodeFormat) + numberLength + dividers.sumBy { it.formatting.length }

    class Divider(val position: Int, val formatting: String)

    companion object {

        val CODE_PREFIX = '+'
    }

    sealed class MatchResult {
        object NO : MatchResult()
        object FULL : MatchResult()
        object SHORT : MatchResult()
        object LONG : MatchResult()
    }
}
