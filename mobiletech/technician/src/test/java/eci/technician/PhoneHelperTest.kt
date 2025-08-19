package eci.technician

import android.content.Context
import android.telephony.TelephonyManager
import eci.technician.helpers.AppAuth
import eci.technician.helpers.PhoneHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneHelperTest {

    private val testValueAllNumbers = "+59178757172"
    var tm = AppAuth.getInstance().context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    var countryCodeValue: String = tm.networkCountryIso!!

    @Test
    fun maxNumberOfDigitsTest() {
        val value = PhoneHelper.formatNumberInternationally(testValueAllNumbers, countryCodeValue)
        assertEquals("+591 78757172", value)
    }

}