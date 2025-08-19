package eci.technician.tools

object ConstantsKotlin {

    enum class TechnicianServiceCallLaborStatus {
        PENDING, DISPATCHED, ARRIVED, COMPLETED
    }

    /**
     * Strings in Repositories
     * Migrate these Strings with a better approach
     * Suggestion-> Use Dependency Injection to retrieve the context/resources
     */

    const val ERROR_STRING = "Error"
    const val INVALID_DATA = "Invalid data received."
    const val CAN_NOT_SAVE_FILE = "Can not save file to downloads folder."
}