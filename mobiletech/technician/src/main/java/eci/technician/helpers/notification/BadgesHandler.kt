package eci.technician.helpers.notification

import java.util.*
import kotlin.collections.HashMap

object BadgesHandler {
    private val badgesMenu = HashMap<Badges, Boolean>()

    init {
        badgesMenu[Badges.FIELD_TRANSFER] = false
        badgesMenu[Badges.UNSYNCED_DATA] = false
    }

    fun activateBadge(badge: Badges) {
        badgesMenu[badge] = true
    }

    fun deactivateBadge(badge: Badges) {
        badgesMenu[badge] = false
    }

    fun deactivateAll() {
        for ((key, value) in badgesMenu){
            badgesMenu[key] = false
        }
    }

    fun getActiveBadges(): Stack<Badges> {
        var activeBadges: Stack<Badges> = Stack()
        for ((key, value) in badgesMenu){
            if(value) {
                activeBadges.add(key)
            }
        }
        return activeBadges
    }

    fun hasActiveBadges(): Boolean {
        for ((key, value) in badgesMenu){
            if(value) {
                return true
            }
        }
        return false
    }
}