# Airo - A flight management application

![Banner](resources/banner.png)

## Screenshots

| ![screenshot-1](resources/screenshot-1.png) | ![screenshot-2](resources/screenshot-2.png) | ![screenshot-3](resources/screenshot-3.png) | ![screenshot-4](resources/screenshot-4.png) | ![screenshot-5](resources/screenshot-5.png) |
|---------------------------------------------|---------------------------------------------|---------------------------------------------|---------------------------------------------|---------------------------------------------|

**Now with public AiroAPI server at `https://airoapi.tuxy.stream/flights`!**

## Current features

- Adding, removing flights using [aerodatabox's](https://aerodatabox.com/)
  api [link](https://aerodatabox.com/)
    - Adding flights with schedules 8 months in advance
- Usage with AiroAPI caching server
- Route map and progress
- Aircraft information with photos
- Flights organised by date
- Scanning boarding passes for information and boarding (QR ONLY for now...)

## TODO before release / Roadmap

- Notification integration
- Periodic alerts and status for flights
- Regional Locale settings
- Bugfixes

## Libraries used/credits

- MapCompose https://github.com/p-lr/MapCompose
- ComposedBarcodes https://github.com/simonsickle/ComposedBarcodes
- Coil Image loading https://coil-kt.github.io/coil/
- Square libraries https://square.github.io/
    - OkHttp
- Zxing embedded https://github.com/journeyapps/zxing-android-embedded
- Klaxon JSON Parsing https://github.com/cbeust/klaxon
