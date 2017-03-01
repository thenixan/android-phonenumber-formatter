package ru.nixan.phoneformatter

import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher

class PhoneNumberFormattingTextWatcher(context: Context, vararg val possibleCountries: Int) : TextWatcher {

    private var mEditing = false

    private var mCountryParsedFromPhoneListener: OnCountryParsedFromPhoneListener? = null

    private val phoneUtils = PhoneUtils.getInstance(context)

    fun setOnCountryParsedListener(listener: OnCountryParsedFromPhoneListener) {
        mCountryParsedFromPhoneListener = listener
    }


    @Synchronized override fun afterTextChanged(s: Editable) {
        if (!mEditing) {
            mEditing = true
            val countryID = phoneUtils.format(s, *possibleCountries)
            mEditing = false
            mCountryParsedFromPhoneListener?.let { listener ->
                countryID
                        ?.let { listener.onCountryFound(it, phoneUtils.isFinal(s.toString(), it)) }
                        ?: listener.onCountryLost()
            }
        }
    }

    fun isCountryFound(value: String): Boolean {
        val spannableStringBuilder = SpannableStringBuilder(value)
        val countryID = phoneUtils.format(spannableStringBuilder, *possibleCountries)
        return phoneUtils.isFinal(value) && possibleCountries.size == 1
                && possibleCountries[0] == countryID
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // TODO Auto-generated method stub

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        // TODO Auto-generated method stub

    }

    interface OnCountryParsedFromPhoneListener {

        fun onCountryFound(countryID: Int, isFinal: Boolean)

        fun onCountryLost()
    }

}
