package eci.technician.helpers

import java.text.NumberFormat
import java.util.*

object DecimalsHelper {
    fun getAmountWithCurrency(amount: Double): String {

        return String.format("%.2f", amount)
    }

    fun getAmountWithCurrency(amount: String): String {
        try {
            val doubleAmount = amount.toDouble();
            return getAmountWithCurrency(doubleAmount)
        } catch (e: Exception) {
            return ""
        }
    }

    fun getValueFromDecimal(value: Double): String {
        val numberFormat = NumberFormat.getInstance(Locale.getDefault())
        return if (value % 1.0 != 0.0) {
            numberFormat.maximumFractionDigits = 2
            numberFormat.format(value)
        } else {
            numberFormat.maximumFractionDigits = 0
            numberFormat.format(value)
        }
    }
}