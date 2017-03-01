package ru.nixan.phoneformatter

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.text.SpannableStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test, which will execute on an Android device.

 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    @Throws(Exception::class)
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()

        assertEquals("ru.nixan.phoneformatter.test", appContext.packageName)
    }

    @Test
    fun testRussianPhone() {
        val format = PhoneUtils.getInstance(InstrumentationRegistry.getTargetContext())
        val text = SpannableStringBuilder("79071234567")
        format.format(text, R.id.country_ru)
        assertEquals("+7 907 123-45-67", text.toString())
        text.clear()
        text.append("7907")
        format.format(text, R.id.country_ru)
        assertEquals("+7 907", text.toString())
        text.clear()
        text.append("7907ddd1234455")
        format.format(text, R.id.country_ru)
        assertEquals("+7 907 123-44-55", text.toString())
        text.clear()
        text.append("+7 907 1236677")
        format.format(text, R.id.country_ru)
        assertEquals("+7 907 123-66-77", text.toString())
    }

    @Test
    fun testMultipleFormats() {
        val format = PhoneUtils.getInstance(InstrumentationRegistry.getTargetContext())
        val text = SpannableStringBuilder("79071234567")
        format.format(text, R.id.country_ru, R.id.country_ua)
        assertEquals("+7 907 123-45-67", text.toString())
        text.clear()
        text.append("380123456789")
        format.format(text, R.id.country_ru, R.id.country_ua)
        assertEquals("+380 12 345 6789", text.toString())
    }

    @Test
    fun testRussianPhoneWithTrunk() {
        val format = PhoneUtils.getInstance(InstrumentationRegistry.getTargetContext())
        val text = SpannableStringBuilder("89071234567")
        format.format(text, R.id.country_ru)
        assertEquals("+7 907 123-45-67", text.toString())
    }
}
