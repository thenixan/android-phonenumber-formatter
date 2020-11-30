package ru.nixan.phoneformatter

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import org.xmlpull.v1.XmlPullParser
import java.lang.ref.WeakReference
import java.util.*

class PhoneUtils private constructor(context: Context) {

    private val phoneFormats: Array<PhoneFormat>

    init {
        val result = ArrayList<PhoneFormat>()
        val parser = context.resources.getXml(R.xml.phone_formats)
        while (parser.eventType != XmlPullParser.END_TAG || TAG_FORMAT_LIST != parser.name) {
            if (parser.eventType == XmlPullParser.START_TAG && TAG_PHONE_FORMAT == parser.name) {
                val countryCode = parser.getAttributeValue(null, ATTR_COUNTRY_CODE)
                val numberLength = parser.getAttributeIntValue(null, ATTR_PHONE_LENGTH, 0)
                val countryId = parser.getAttributeResourceValue(null, ATTR_COUNTRY, 0)
                val trunk = parser.getAttributeValue(null, ATTR_TRUNK)
                if (TextUtils.isEmpty(countryCode)) {
                    throw IllegalArgumentException("Country code must not be empty")
                }
                if (numberLength == 0) {
                    throw IllegalArgumentException("Number length cannot be empty")
                }
                if (countryId == 0) {
                    throw IllegalArgumentException("Country ID should be set")
                }
                val dividers = ArrayList<PhoneFormat.Divider>()
                val defCodeFormatVariants = ArrayList<DefcodeFormat>()
                val mccs = ArrayList<String>()
                while (parser.eventType != XmlPullParser.END_TAG || TAG_PHONE_FORMAT != parser.name) {
                    if (parser.eventType == XmlPullParser.START_TAG && TAG_NUMBER_FORMATTING == parser.name) {
                        while (parser.eventType != XmlPullParser.END_TAG || TAG_NUMBER_FORMATTING != parser.name) {
                            if (parser.eventType == XmlPullParser.START_TAG && TAG_SPLIT == parser.name) {
                                val dividerPosition = parser.getAttributeIntValue(null, ATTR_POSITION, 0)
                                val dividerCharacter = parser.getAttributeValue(null, ATTR_LETTER)
                                if (dividerPosition == 0) {
                                    throw IllegalArgumentException("Divider cannot have empty position")
                                }
                                if (TextUtils.isEmpty(dividerCharacter)) {
                                    throw IllegalArgumentException("Divider cannot be empty")
                                }
                                dividers.add(PhoneFormat.Divider(dividerPosition, dividerCharacter))
                            }
                            parser.next()
                        }
                    } else if (parser.eventType == XmlPullParser.START_TAG && TAG_DEFCODE_FORMATTING == parser.name) {
                        while (parser.eventType != XmlPullParser.END_TAG || TAG_DEFCODE_FORMATTING != parser.name) {
                            if (parser.eventType == XmlPullParser.START_TAG && TAG_DEFCODE_FORMAT == parser.name) {
                                val defCodeLength = parser.getAttributeIntValue(null, ATTR_DEFCODE_SIZE, 0)

                                if (defCodeLength == 0) {
                                    throw IllegalArgumentException("DEF code cannot be empty")
                                }
                                val defCodeDividers = when (parser.getAttributeValue(null, ATTR_DEFCODE_FORMAT)) {
                                    VALUE_DEFCODE_FORMATTING_BRACKETS -> DefcodeFormat.DefcodeDividers.BRACKETS
                                    VALUE_DEFCODE_FORMATTING_RIGHT_DASH -> DefcodeFormat.DefcodeDividers.RIGHTDASH
                                    VALUE_DEFCODE_FORMATTING_SPACE -> DefcodeFormat.DefcodeDividers.SPACE
                                    else -> throw IllegalArgumentException("Unknown DEF code dividers format")
                                }

                                val defcodeStartsWith = ArrayList<String>()
                                val defcodeNotStartsWith = ArrayList<String>()

                                while (parser.eventType != XmlPullParser.END_TAG || TAG_DEFCODE_FORMAT != parser.name) {
                                    if (parser.eventType == XmlPullParser.START_TAG && TAG_STARTS_WITH == parser.name) {
                                        defcodeStartsWith.add(parser.nextText())
                                    } else if (parser.eventType == XmlPullParser.START_TAG && TAG_NOT_STARTS_WITH == parser.name) {
                                        defcodeNotStartsWith.add(parser.nextText())
                                    }
                                    parser.next()
                                }
                                defCodeFormatVariants.add(DefcodeFormat(defCodeStarts = defcodeStartsWith.toTypedArray()
                                        , defCodeNotStarts = defcodeNotStartsWith.toTypedArray(), defCodeDividers = defCodeDividers
                                        , defCodeLength = defCodeLength))
                            }
                            parser.next()
                        }
                    } else if (parser.eventType == XmlPullParser.START_TAG && TAG_MCC_LIST == parser.name) {
                        while (parser.eventType != XmlPullParser.END_TAG || TAG_MCC_LIST != parser.name) {
                            if (parser.eventType == XmlPullParser.START_TAG && TAG_MCC == parser.name) {
                                mccs.add(parser.nextText())
                            }
                            parser.next()
                        }
                    }
                    parser.next()
                }
                result.add(PhoneFormat(mcc = mccs.toTypedArray(), countryCode = countryCode, countryId = countryId
                        , defCodeFormatVariants = defCodeFormatVariants.toTypedArray(), dividers = dividers.toTypedArray()
                        , numberLength = numberLength, trunk = trunk))
            }
            parser.next()
        }
        parser.close()
        phoneFormats = result.toTypedArray()
    }

    fun isFinal(phoneNumber: String, vararg possibleCountries: Int): Boolean {
        return phoneFormats
                .filter { possibleCountries.isEmpty() || possibleCountries.contains(it.countryId) }
                .any { it.validate(phoneNumber.filter(Char::isDigit)) == PhoneFormat.MatchResult.FULL }
    }

    fun getPhoneCountryFormat(phoneNumber: String, vararg possibleCountries: Int): PhoneFormat? {
        if (TextUtils.isEmpty(phoneNumber)) {
            return null
        }
        if (possibleCountries.size == 1) {
            return phoneFormats.first { it.countryId == possibleCountries.first() }
        }
        val possiblePhoneFormats = phoneFormats
                .filter { possibleCountries.isEmpty() || possibleCountries.contains(it.countryId) }
        possiblePhoneFormats.find { it.validate(phoneNumber) == PhoneFormat.MatchResult.FULL }?.let {
            return it
        }
        possiblePhoneFormats.find { it.validate(phoneNumber) == PhoneFormat.MatchResult.LONG }?.let {
            return it
        }
        possiblePhoneFormats.find { it.validate(phoneNumber) == PhoneFormat.MatchResult.SHORT }?.let {
            return it
        }
        return null
    }


    private fun checkFormatting(
            phoneFormat: Pair<PhoneFormat, PhoneFormat.MatchResult>,
            plainPhone: String
    ): Boolean {
        return when (phoneFormat.second) {
            PhoneFormat.MatchResult.FULL -> {
                phoneFormat.first.findBestDefCode(plainPhone)?.let { suitableDefCode ->
                    return when (suitableDefCode.second) {
                        PhoneFormat.MatchResult.FULL -> {
                            when (phoneFormat.first.validatePhoneNumber(
                                    plainPhone,
                                    suitableDefCode.first
                            )) {
                                PhoneFormat.MatchResult.FULL,
                                PhoneFormat.MatchResult.SHORT -> {
                                    true
                                }
                                else -> false
                            }
                        }
                        PhoneFormat.MatchResult.SHORT -> {
                            true
                        }
                        else -> false
                    }
                } ?: false
            }
            PhoneFormat.MatchResult.SHORT -> {
                true
            }
            else -> false
        }
    }

    private fun acceptDigit(input: Editable, phoneFormat: PhoneFormat): Int {
        phoneFormat.format(input)
        return phoneFormat.countryId
    }

    private fun declineDigit(input: Editable) {
        input.delete(input.length - 1, input.length)
    }

    @JvmOverloads fun format(input: Editable, vararg possibleCountries: Int): Int? {
        if (!input.startsWith("+")) {
            phoneFormats
                    .filter { possibleCountries.contains(it.countryId) }
                    .find {
                        it.trunk?.let { trunk ->
                            input.startsWith(trunk)
                        } ?: false
                    }?.let { input.replace(0, it.trunk!!.length, it.countryCode) }
        }

        if (possibleCountries.size == 1) {
            phoneFormats.first { it.countryId == possibleCountries.first() }.apply {
                format(input)
                return countryId
            }
        }


        while (true) {
            val plainPhone = input.toString().filter(Char::isDigit)
            if (TextUtils.isEmpty(plainPhone)) {
                input.clear()
                return null
            }

            val possiblePhoneFormats = phoneFormats
                    .filter { possibleCountries.isEmpty() || possibleCountries.contains(it.countryId) }

            possiblePhoneFormats
                    .map { it to it.validateCountryCode(plainPhone) }
                    .sortedBy {
                        when (it.second) {
                            PhoneFormat.MatchResult.FULL -> 0
                            PhoneFormat.MatchResult.LONG -> 1
                            PhoneFormat.MatchResult.SHORT -> 2
                            PhoneFormat.MatchResult.NO -> 3
                        }
                    }
                    .firstOrNull { checkFormatting(it, plainPhone) }
                    ?.let { phoneFormat ->
                        if (checkFormatting(phoneFormat, plainPhone)) {
                            return acceptDigit(input, phoneFormat.first)
                        } else {
                            declineDigit(input)
                        }
                    } ?: declineDigit(input)
        }
    }

    @JvmOverloads fun getCountryCodeByMCC(mcc: String, vararg possibleCountries: Int): String? {
        return phoneFormats
                .filter { possibleCountries.isEmpty() || possibleCountries.contains(it.countryId) }
                .find { it.mcc.contains(mcc) }?.countryCode
    }

    companion object {

        private val TAG_FORMAT_LIST = "phone-formats"

        private val TAG_PHONE_FORMAT = "phone"

        private val TAG_NUMBER_FORMATTING = "number-formatting"

        private val TAG_DEFCODE_FORMATTING = "defcode-formatting"

        private val TAG_DEFCODE_FORMAT = "format"

        private val TAG_STARTS_WITH = "starts-with"

        private val TAG_NOT_STARTS_WITH = "not-starts-with"

        private val TAG_SPLIT = "split"

        private val TAG_MCC_LIST = "mcc-list"

        private val TAG_MCC = "mcc"

        private val ATTR_COUNTRY_CODE = "country-code"

        private val ATTR_TRUNK = "trunk"

        private val ATTR_PHONE_LENGTH = "maxlength"

        private val ATTR_COUNTRY = "country"

        private val ATTR_LETTER = "letter"

        private val ATTR_POSITION = "position"

        private val ATTR_DEFCODE_SIZE = "length"

        private val ATTR_DEFCODE_FORMAT = "formatting"

        private val VALUE_DEFCODE_FORMATTING_SPACE = "space"

        private val VALUE_DEFCODE_FORMATTING_BRACKETS = "brackets"

        private val VALUE_DEFCODE_FORMATTING_RIGHT_DASH = "dashRight"

        private var mPhoneUtilsReference: WeakReference<PhoneUtils>? = null

        fun getInstance(context: Context): PhoneUtils {
            if (mPhoneUtilsReference == null || mPhoneUtilsReference!!.get() == null) {
                mPhoneUtilsReference = WeakReference(PhoneUtils(context))
            }
            return mPhoneUtilsReference!!.get()!!
        }
    }
}
