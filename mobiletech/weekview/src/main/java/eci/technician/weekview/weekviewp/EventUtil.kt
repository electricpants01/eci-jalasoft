package eci.technician.weekview.weekviewp

object EventUtil {

    /**
     * Split the list of events with Normal Events and Clock Events
     */
    fun splitEvents(list: List<WeekViewEvent>): Pair<List< WeekViewEvent>, List<WeekViewEvent>> {
        val clockEvents = mutableListOf<WeekViewEvent>()
        val events = mutableListOf<WeekViewEvent>()
        list.forEach {
            when(it.eventType){
                EventType.CLOCK_OUT, EventType.CLOCK_IN -> clockEvents.add(it)
                else -> events.add(it)
            }
        }
        return Pair(events, clockEvents)

    }
}