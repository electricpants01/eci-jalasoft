package eci.technician.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*


class UserPreferencesRepositoryTest {

    private val currentDate = Date()
    private val oneHourInSeconds = 3600
    private val oneDayInSeconds = oneHourInSeconds * 24
    private val oneMinuteInSeconds = 60

    @Test
    fun `with 2 day difference with OneDay cacheTime should return true`() {
        val twoDaysInSeconds = oneDayInSeconds * 2
        val twoDaysOld = Date(currentDate.time - twoDaysInSeconds * 1000)
        val result = LastUpdateRepository.hasExpired(twoDaysOld, currentDate, oneDayInSeconds)
        assertEquals(true, result)
    }

    @Test
    fun `with 2 hrs difference with OneDay cacheTime should return false`() {
        val twoHoursInSeconds = oneHourInSeconds * 2
        val oldDate = Date(currentDate.time - twoHoursInSeconds * 1000)
        val result = LastUpdateRepository.hasExpired(oldDate, currentDate, oneDayInSeconds)
        assertEquals(false, result)
    }

    @Test
    fun `with 1 day difference with OneDay cacheTime should return true`() {
        val oldDate = Date(currentDate.time - oneDayInSeconds * 1000)
        val result = LastUpdateRepository.hasExpired(oldDate, currentDate, oneDayInSeconds)
        assertEquals(true, result)
    }

    @Test
    fun `with 23 hrs 59 seconds difference with OneDay cacheTime should return false`() {
        val custom = oneDayInSeconds -1
        val oldDate = Date(currentDate.time - custom * 1000 )
        val result = LastUpdateRepository.hasExpired(oldDate, currentDate, oneDayInSeconds)
        assertEquals(false, result)
    }

    @Test
    fun `with 10 minutes difference with 1 hr cacheTime should return false`() {
        val tenMinutesInMillis = oneMinuteInSeconds * 10 * 1000
        val oldDate = Date(currentDate.time - tenMinutesInMillis )
        val result = LastUpdateRepository.hasExpired(oldDate, currentDate, oneHourInSeconds)
        assertEquals(false, result)
    }

    @Test
    fun `with 10 minutes difference with 5 minutes cacheTime should return true`() {
        val tenMinutesInMillis = oneMinuteInSeconds * 10 * 1000
        val fiveMinutesInSeconds = oneMinuteInSeconds * 5
        val oldDate = Date(currentDate.time - tenMinutesInMillis )
        val result = LastUpdateRepository.hasExpired(oldDate, currentDate, fiveMinutesInSeconds)
        assertEquals(true, result)
    }

}