package ru.nixan.phoneformatter

class DefcodeFormat(val defCodeStarts: Array<String>, val defCodeNotStarts: Array<String>, val defCodeDividers: DefcodeDividers, val defCodeLength: Int) {

    private fun isPossible(numberArray: CharArray, countryCodeLength: Int, position: Int): Boolean {
        if (defCodeStarts.any { it.length > position - countryCodeLength && numberArray[position] != it[position - countryCodeLength] }) {
            return false
        }
        if (defCodeNotStarts.any { it.length > position - countryCodeLength && numberArray[position] == it[position - countryCodeLength] }) {
            return false
        }
        return true
    }

    fun getDefCodeEndPosition(defcodeStartPosition: Int): Int {
        when (defCodeDividers) {
            DefcodeDividers.BRACKETS -> return defcodeStartPosition + defCodeLength + 4
            DefcodeDividers.SPACE -> return defcodeStartPosition + defCodeLength + 2
            DefcodeDividers.RIGHTDASH -> return defcodeStartPosition + defCodeLength + 2
        }
    }

    fun getDefCodeCharAt(position: Int, character: Char): Char? {
        val dividers = when (defCodeDividers) {
            DefcodeDividers.SPACE -> " " to " "
            DefcodeDividers.RIGHTDASH -> " " to "-"
            DefcodeDividers.BRACKETS -> " (" to ") "
        }
        if (0 <= position && position < dividers.first.length) {
            return dividers.first[position]
        } else if (dividers.first.length <= position && position < defCodeLength + dividers.first.length
                && character in '0'..'9') {
            return character
        } else if (dividers.first.length + defCodeLength <= position && position < dividers.first.length + defCodeLength + dividers.second.length) {
            return dividers.second[position - dividers.first.length - defCodeLength]
        } else {
            return null
        }

    }

    enum class DefcodeDividers {
        SPACE, BRACKETS, RIGHTDASH
    }

}
