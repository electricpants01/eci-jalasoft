package eci.technician

import eci.technician.helpers.versionManager.CompareVersion
import org.junit.Test
import org.junit.Assert.assertEquals

class CompareVersionTest {
    companion object{
        private const val LESSER = -1
        private const val EQUALS = 0
        private const val GREATER = 1

    }
    @Test
    fun versionOneGreaterThanVersionTwo() {
        val versionOne = "1.1.2"
        val versionTwo = "1.1.1"
        val versionComparisonCode = CompareVersion.compareVersions(versionOne, versionTwo)
        val expectedComparisonCode = GREATER
        assertEquals(expectedComparisonCode, versionComparisonCode)
    }

    @Test
    fun versionOneLesserThanVersionTwo() {
        val versionOne = "1.1.1"
        val versionTwo = "1.1.2"
        val versionComparisonCode = CompareVersion.compareVersions(versionOne, versionTwo)
        val expectedComparisonCode = LESSER
        assertEquals(expectedComparisonCode, versionComparisonCode)
    }

    @Test
    fun versionOneEqualsVersionTwo() {
        val versionOne = "1.1.1"
        val versionTwo = "1.1.1"
        val versionComparisonCode = CompareVersion.compareVersions(versionOne, versionTwo)
        val expectedComparisonCode = EQUALS
        assertEquals(expectedComparisonCode, versionComparisonCode)
    }

    @Test
    fun versionOneEqualsVersionTwo_Test2() {
        val versionOne = "1.1.1.3"
        val versionTwo = "1.1.1"
        val versionComparisonCode = CompareVersion.compareVersions(versionOne, versionTwo)
        val expectedComparisonCode = EQUALS
        assertEquals(expectedComparisonCode, versionComparisonCode)
    }


    @Test
    fun versionOneEqualsVersionTwo_Test3() {
        val versionOne = "1.1.1"
        val versionTwo = "1.1.1.3"
        val versionComparisonCode = CompareVersion.compareVersions(versionOne, versionTwo)
        val expectedComparisonCode = EQUALS
        assertEquals(expectedComparisonCode, versionComparisonCode)
    }
}