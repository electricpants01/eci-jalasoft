package eci.technician.helpers

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import eci.technician.MainApplication
import java.util.*


object PhoneHelper {
    const val TAG = "PhoneHelper"
    const val EXCEPTION = "Exception"

    fun formatNumberInternationally(phoneNumber: String, fallBackRegion: String): String {
        val phoneUtil = PhoneNumberUtil.getInstance()
        var res: String = phoneNumber.filter { it.isLetterOrDigit() }
        val ext: String
        if (res.contains("ex")) {
            ext = res.substringAfter("ex").filter { it.isDigit() }
            res = res.replace(res.substring(res.indexOf("ex")), "")
            res = ext + res
        }

        try {
            val parsedPhone = phoneUtil.parse(res, getCountryCode(fallBackRegion))
            if (phoneUtil.isValidNumberForRegion(parsedPhone, getCountryCode(fallBackRegion))) {
                res = phoneUtil.format(parsedPhone, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            } else {
                res = "+$res"
                val formattedPhone = phoneUtil.parse(res, null)
                res = if (phoneUtil.isValidNumber(formattedPhone)) {
                    phoneUtil.format(formattedPhone, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
                } else {
                    res.removePrefix("+")
                }

            }
        } catch (e: NumberParseException) {
            res = phoneNumber
        }
        return res
    }

    suspend fun getFormattedCustomerPhoneNumber(phone: String?, context: Context): String?{
            return try {
                if (phone != null) {
                    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                    val countryCodeValue = tm.networkCountryIso
                    val phoneValue = formatNumberInternationally(phone, countryCodeValue.uppercase())
                    phoneValue
                } else {
                    phone
                }
            } catch (e: Exception) {
                Log.e(TAG, EXCEPTION, e)
                phone
            }
        }

    fun getOnlyNumbersBeforeDialing(phoneNumber: String): String {
        var res: String = phoneNumber.filter { it.isLetterOrDigit() }
        val ext: String
        if (res.contains("ex")) {
            ext = res.substringAfter("ex").filter { it.isDigit() }
            res = res.replace(res.substring(res.indexOf("ex")), "")
            res = ext + res
        }
        return res.filter { it.isDigit() }
    }

    fun getCountryCode(fallBackRegion: String): String {
        var countryName = "US"
        try {
            val gcd = Geocoder(AppAuth.getInstance().context, Locale.getDefault())
            countryName = fallBackRegion
            MainApplication.lastLocation?.let {
                val addresses: List<Address> = gcd.getFromLocation(it.latitude, it.longitude, 1)
                if (addresses.isNotEmpty()) {
                    countryName = addresses[0].countryCode
                }
            }
            return countryName
        }catch (e:Exception){
            return countryName
        }
    }


}