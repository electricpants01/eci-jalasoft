package eci.technician.helpers

import org.junit.Test
import org.junit.Assert.*

class DecimalsHelperTest {

    @Test
    fun `with decimal values should return with decimals`() {
        val numberWithDecimal:Double = 15.36
        val res:String = DecimalsHelper.getValueFromDecimal(numberWithDecimal)
        assertEquals("15.36", res)
    }

    @Test
    fun `with a double without decimals values should return without decimals`() {
        val numberWithDecimal:Double = 15.00
        val res:String = DecimalsHelper.getValueFromDecimal(numberWithDecimal)
        assertEquals("15", res)
    }

    @Test
    fun `with a double with a lot of decimals values should return with decimals`() {
        val numberWithDecimal:Double = 15.576
        val res:String = DecimalsHelper.getValueFromDecimal(numberWithDecimal)
        assertEquals("15.58", res)
    }

    @Test
    fun `with a double with a lot of decimals values should return without decimals`() {
        val numberWithDecimal:Double = 15.0009
        val res:String = DecimalsHelper.getValueFromDecimal(numberWithDecimal)
        assertEquals("15", res)
    }
}