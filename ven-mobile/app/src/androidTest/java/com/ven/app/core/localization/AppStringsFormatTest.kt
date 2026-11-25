package com.ven.app.core.localization

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ven.app.ui.screens.auth.PasswordValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStringsFormatTest {

    @Before
    fun setUp() {
        AppStrings.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    /** String berisi '%' literal tidak boleh memicu UnknownFormatConversionException
     *  saat diambil lewat AppStrings.get (yang selalu memanggil getString dengan vararg). */
    @Test
    fun passwordRuleLabels_renderLiteralPercentWithoutCrashing() {
        val rules = PasswordValidator.rules("Pass1234!")

        assertEquals(5, rules.size)
        assertTrue(rules.all { it.label.isNotBlank() })
        assertTrue(rules.any { it.label.contains("%") })
        assertTrue(rules.none { it.label.contains("%%") })
    }
}
