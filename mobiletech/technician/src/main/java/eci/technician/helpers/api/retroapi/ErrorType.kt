package eci.technician.helpers.api.retroapi

enum class ErrorType {
    CONNECTION_EXCEPTION,
    SOCKET_TIMEOUT_EXCEPTION,
    IO_EXCEPTION,
    HTTP_EXCEPTION,
    SOMETHING_WENT_WRONG,
    NOT_SUCCESSFUL,
    BACKEND_ERROR
}