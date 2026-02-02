package com.tuxy.airo.data.flightdata_rework

import org.openapitools.client.apis.FlightAPIApi

class FlightRequest {
    public fun test() {
        val api = FlightAPIApi(basePath = "https://airoapi.tuxy.stream")

        api.getFlightFlightOnSpecificDateWithHttpInfo(
            searchParam = TODO(),
            dateLocal = TODO(),
            searchBy = TODO(),
            dateLocalRole = TODO(),
            withAircraftImage = TODO(),
            withLocation = TODO(),
            withFlightPlan = TODO()
        )
    }
}