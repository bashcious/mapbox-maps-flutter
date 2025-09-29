// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "mapbox_maps_flutter",
    platforms: [
        .iOS("14.0"),
    ],
    products: [
        .library(name: "mapbox-maps-flutter", targets: ["mapbox_maps_flutter"])
    ],
    dependencies: [
<<<<<<< HEAD
        .package(url: "https://github.com/mapbox/mapbox-maps-ios.git", exact: "11.11.0"),
=======
        .package(url: "https://github.com/mapbox/mapbox-maps-ios.git", exact: "11.16.0-beta.1"),
>>>>>>> 9dca0f0d86afb36472e45225da006dc5a7f9f14a
    ],
    targets: [
        .target(
            name: "mapbox_maps_flutter",
            dependencies: [
                .product(name: "MapboxMaps", package: "mapbox-maps-ios"),
            ],
            resources: []
        )
    ]
)
