# Contributing

So you would like to contribute? Sure! (although it's not easy to understand...)

- Even if you don't know how to code, you can help with translations. (Right now, there isn't a
  website, you just have to research on how to...)
- Do make some suggestions! Bugs reports and new feature (reasonable) issues welcome!
- If you understand code, but not Android development, you can help with documentation in the
  codebase that I have not been diligent with.
- And of course for people that can help with the Android parts, please help me fix the bugs and
  terrible code semantics :)

## Code structure

This project never followed specific structure. Essentially:

- `data`: for data related to the management of the database and the fetching of flight information.
- `motion`: animations, movement and app flow. This is quite small and doesn't really need improving
- `screens`: main UI composables.
- `viewmodel`: UI logic and data handling outside of composable functions. This is the main
  interface between `data` and `screens`
- `Alarm.kt`: notifications
- `MainActivity.kt`: fairly self-explanatory
- `NavGraph.kt`: Navigation between screens
- `Screen`: Standardised names for screens
- `AiroTest`: Testing (0% coverage...)

## Main focus points for improvement (currently)

- `data/FlightRequest.kt`: Direct request from the APIs. Error handling and code semantics are still
  very unorganised.
- `data/FlightData.kt`: Large db may cause performance issues
- `viewmodel/FlightDetailsViewModel.kt`: Adding/Fixing refresh functionality (check
  refreshing-feature branch)
- `screens/MainFlightScreen.kt`: Up to spec, but could have more sophisticated grouping.
- Comments & documentation. A lot of the functions are "willy-nilly" and need some documentation.