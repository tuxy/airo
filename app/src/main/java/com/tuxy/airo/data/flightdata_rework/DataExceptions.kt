package com.tuxy.airo.data.flightdata_rework

class FlightNotFoundException(message: String?) : Exception(message)
class InvalidApiUrlException(message: String?) : Exception(message)
class UnexpectedResponseException(message: String?) : Exception(message)
class MissingCriticalDataException(message: String?) : Exception(message)
