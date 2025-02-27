package com.mapbox.maps.mapbox_maps

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxDelicateApi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.PlatformEventInfo
import com.mapbox.maps.PlatformEventType
import com.mapbox.maps.TileCacheBudget
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.interactions.FeatureStateKey
import com.mapbox.maps.mapbox_maps.pigeons.CanonicalTileID
import com.mapbox.maps.mapbox_maps.pigeons.ConstrainMode
import com.mapbox.maps.mapbox_maps.pigeons.FeatureExtensionValue
import com.mapbox.maps.mapbox_maps.pigeons.FeaturesetDescriptor
import com.mapbox.maps.mapbox_maps.pigeons.FeaturesetFeature
import com.mapbox.maps.mapbox_maps.pigeons.FeaturesetFeatureId
import com.mapbox.maps.mapbox_maps.pigeons.MapDebugOptions
import com.mapbox.maps.mapbox_maps.pigeons.MapOptions
import com.mapbox.maps.mapbox_maps.pigeons.NorthOrientation
import com.mapbox.maps.mapbox_maps.pigeons.QueriedRenderedFeature
import com.mapbox.maps.mapbox_maps.pigeons.QueriedSourceFeature
import com.mapbox.maps.mapbox_maps.pigeons.RenderedQueryOptions
import com.mapbox.maps.mapbox_maps.pigeons.Size
import com.mapbox.maps.mapbox_maps.pigeons.SourceQueryOptions
import com.mapbox.maps.mapbox_maps.pigeons.TileCacheBudgetInMegabytes
import com.mapbox.maps.mapbox_maps.pigeons.TileCacheBudgetInTiles
import com.mapbox.maps.mapbox_maps.pigeons.TileCoverOptions
import com.mapbox.maps.mapbox_maps.pigeons.ViewportMode
import com.mapbox.maps.mapbox_maps.pigeons._MapInterface
import com.mapbox.maps.mapbox_maps.pigeons._MapWidgetDebugOptions
import com.mapbox.maps.mapbox_maps.pigeons._RenderedQueryGeometry
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import org.json.JSONObject

class MapInterfaceController(
  private val mapboxMap: MapboxMap,
  private val mapView: MapView,
  private val context: Context
) : _MapInterface {

  override fun setSnapshotLegacyMode(enabled: Boolean, callback: (Result<Unit>) -> Unit) {
    mapView.setSnapshotLegacyMode(enabled)
    callback(Result.success(Unit))
  }

  override fun styleGlyphURL(): String = mapboxMap.getStyleGlyphURL()

  override fun setStyleGlyphURL(glyphURL: String) = mapboxMap.setStyleGlyphURL(glyphURL)

  override fun loadStyleURI(styleURI: String, callback: (Result<Unit>) -> Unit) {
    mapboxMap.loadStyleUri(
      styleURI,
      { callback(Result.success(Unit)) },
      object : OnMapLoadErrorListener {
        override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
          callback(Result.failure(Throwable(eventData.message)))
        }
      }
    )
  }

  override fun loadStyleJson(
    styleJson: String,
    callback: (Result<Unit>) -> Unit
  ) {
    mapboxMap.loadStyleJson(
      styleJson,
      { callback(Result.success(Unit)) },
      object : OnMapLoadErrorListener {
        override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
          callback(Result.failure(Throwable(eventData.message)))
        }
      }
    )
  }

  override fun clearData(callback: (Result<Unit>) -> Unit) {
    MapboxMap.clearData {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(Result.success(Unit))
      }
    }
  }

  override fun setTileCacheBudget(
    tileCacheBudgetInMegabytes: TileCacheBudgetInMegabytes?,
    tileCacheBudgetInTiles: TileCacheBudgetInTiles?
  ) {
    if (tileCacheBudgetInMegabytes != null) {
      mapboxMap.setTileCacheBudget(TileCacheBudget.valueOf(tileCacheBudgetInMegabytes.toMapMemoryBudgetInMegabytes()))
    } else if (tileCacheBudgetInTiles != null) {
      mapboxMap.setTileCacheBudget(TileCacheBudget.valueOf(tileCacheBudgetInTiles.toMapMemoryBudgetInTiles()))
    }
  }

  override fun getSize(): Size {
    return mapboxMap.getSize().toFLTSize(context)
  }

  override fun triggerRepaint() {
    mapboxMap.triggerRepaint()
  }

  override fun isGestureInProgress(): Boolean {
    return mapboxMap.isGestureInProgress()
  }

  override fun isUserAnimationInProgress(): Boolean {
    return mapboxMap.isUserAnimationInProgress()
  }

  override fun getPrefetchZoomDelta(): Long {
    return mapboxMap.getPrefetchZoomDelta().toLong()
  }

  override fun setNorthOrientation(orientation: NorthOrientation) {
    mapboxMap.setNorthOrientation(com.mapbox.maps.NorthOrientation.values()[orientation.ordinal])
  }

  override fun setConstrainMode(mode: ConstrainMode) {
    mapboxMap.setConstrainMode(com.mapbox.maps.ConstrainMode.values()[mode.ordinal])
  }

  override fun setViewportMode(mode: ViewportMode) {
    mapboxMap.setViewportMode(com.mapbox.maps.ViewportMode.values()[mode.ordinal])
  }

  override fun getMapOptions(): MapOptions {
    return mapboxMap.getMapOptions().toFLTMapOptions(context)
  }

  override fun getDebugOptions(): List<_MapWidgetDebugOptions> {
    return mapView.debugOptions.mapNotNull { nativeOption ->
      nativeOption.toFLTDebugOptions()
    }
  }

  override fun setDebugOptions(debugOptions: List<_MapWidgetDebugOptions>) {
    mapView.debugOptions = debugOptions.map { it.toMapViewDebugOptions() }.toSet()
  }

  override fun getDebug(): List<MapDebugOptions> {
    return mapboxMap.getDebug().map { it.toFLTMapDebugOptions() }.toMutableList()
  }

  override fun setDebug(debugOptions: List<MapDebugOptions?>, value: Boolean) {
    mapboxMap.setDebug(debugOptions.map { it!!.toMapDebugOptions() }, value)
  }

  override fun queryRenderedFeatures(
    geometry: _RenderedQueryGeometry,
    options: RenderedQueryOptions,
    callback: (Result<List<QueriedRenderedFeature?>>) -> Unit
  ) {
    mapboxMap.queryRenderedFeatures(
      geometry.toRenderedQueryGeometry(context),
      options.toRenderedQueryOptions()
    ) {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(
          Result.success(
            it.value!!.map { feature -> feature.toFLTQueriedRenderedFeature() }.toMutableList()
          )
        )
      }
    }
  }

  @OptIn(MapboxExperimental::class)
  override fun queryRenderedFeaturesForFeatureset(
    featureset: FeaturesetDescriptor,
    geometry: _RenderedQueryGeometry?,
    filter: String?,
    callback: (Result<List<FeaturesetFeature>>) -> Unit
  ) {
    featureset.toTypedFeaturesetDescriptor()?.let { typedFeaturesetDescriptor ->
      mapboxMap.queryRenderedFeatures(
        typedFeaturesetDescriptor,
        geometry?.toRenderedQueryGeometry(context),
        filter?.let { Expression.fromRaw(filter) }
      ) {
        callback(Result.success(it.map { feature -> feature.toFLTFeaturesetFeature() }.toMutableList()))
      }
    } ?: {
      callback(
        Result.failure(
          Throwable("Error querying rendered features for featureset: {featureId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}}")
        )
      )
    }
  }

  override fun querySourceFeatures(
    sourceId: String,
    options: SourceQueryOptions,
    callback: (Result<List<QueriedSourceFeature?>>) -> Unit
  ) {
    mapboxMap.querySourceFeatures(sourceId, options.toSourceQueryOptions()) {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(
          Result.success(
            it.value!!.map { feature -> feature.toFLTQueriedSourceFeature() }.toMutableList()
          )
        )
      }
    }
  }

  override fun getGeoJsonClusterLeaves(
    sourceIdentifier: String,
    cluster: Map<String?, Any?>,
    limit: Long?,
    offset: Long?,
    callback: (Result<FeatureExtensionValue>) -> Unit
  ) {
    mapboxMap.getGeoJsonClusterLeaves(
      sourceIdentifier, Feature.fromJson(Gson().toJson(cluster)),
      limit ?: 10, offset ?: 0
    ) {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(Result.success(it.value!!.toFLTFeatureExtensionValue()))
      }
    }
  }

  override fun getGeoJsonClusterChildren(
    sourceIdentifier: String,
    cluster: Map<String?, Any?>,
    callback: (Result<FeatureExtensionValue>) -> Unit
  ) {
    mapboxMap.getGeoJsonClusterChildren(
      sourceIdentifier,
      Feature.fromJson(Gson().toJson(cluster))
    ) {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(Result.success(it.value!!.toFLTFeatureExtensionValue()))
      }
    }
  }

  override fun getGeoJsonClusterExpansionZoom(
    sourceIdentifier: String,
    cluster: Map<String?, Any?>,
    callback: (Result<FeatureExtensionValue>) -> Unit
  ) {
    mapboxMap.getGeoJsonClusterExpansionZoom(
      sourceIdentifier,
      Feature.fromJson(Gson().toJson(cluster))
    ) {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(Result.success(it.value!!.toFLTFeatureExtensionValue()))
      }
    }
  }

  override fun setFeatureState(
    sourceId: String,
    sourceLayerId: String?,
    featureId: String,
    state: String,
    callback: (Result<Unit>) -> Unit
  ) {
    mapboxMap.setFeatureState(sourceId, sourceLayerId, featureId, state.toValue()) {
      if (it.isError) {
        callback(Result.failure(Throwable(it.error)))
      } else {
        callback(Result.success(Unit))
      }
    }
  }

  @OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
  override fun setFeatureStateForFeaturesetDescriptor(
    featureset: FeaturesetDescriptor,
    featureId: FeaturesetFeatureId,
    state: Map<String, Any?>,
    callback: (Result<Unit>) -> Unit
  ) {
    featureset.toTypedFeaturesetDescriptor()?.let { typedFeaturesetDescriptor ->
      mapboxMap.setFeatureState(
        typedFeaturesetDescriptor,
        featureId.toFeaturesetFeatureId(),
        state.toFeatureState()
      ) { callback(Result.success(Unit)) }
    } ?: {
      callback(Result.failure(Throwable("Error setting feature state for feature $featureId from featureset {featuresetId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}.")))
    }
  }

  @OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
  override fun setFeatureStateForFeaturesetFeature(
    feature: FeaturesetFeature,
    state: Map<String, Any?>,
    callback: (Result<Unit>) -> Unit
  ) {
    feature.id?.let {
      setFeatureStateForFeaturesetDescriptor(
        feature.featureset,
        it,
        state,
        callback
      )
    } ?: {
      callback(Result.failure(Throwable("Invalid feature id for the requested feature: $feature")))
    }
  }

  override fun getFeatureState(
    sourceId: String,
    sourceLayerId: String?,
    featureId: String,
    callback: (Result<String>) -> Unit
  ) {
    mapboxMap.getFeatureState(sourceId, sourceLayerId, featureId) { expected ->
      callback.let {
        if (expected.isError) {
          it(Result.failure(Throwable(expected.error)))
        } else {
          it(Result.success(expected.value!!.toJson()))
        }
      }
    }
  }

  @OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
  override fun getFeatureStateForFeaturesetDescriptor(
    featureset: FeaturesetDescriptor,
    featureId: FeaturesetFeatureId,
    callback: (Result<Map<String, Any?>>) -> Unit
  ) {
    featureset.toTypedFeaturesetDescriptor()?.also { featuresetDescriptor ->
      mapboxMap.getFeatureState(
        featuresetDescriptor,
        featureId.toFeaturesetFeatureId()
      ) {
        callback(Result.success(JSONObject(it.asJsonString()).toFilteredMap()))
      }
    } ?: {
      callback(Result.failure(Throwable("Error getting feature state for feature $featureId from featureset: {featureId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}}.")))
    }
  }

  @OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
  override fun getFeatureStateForFeaturesetFeature(
    feature: FeaturesetFeature,
    callback: (Result<Map<String, Any?>>) -> Unit
  ) {
    feature.id?.let {
      getFeatureStateForFeaturesetDescriptor(
        feature.featureset,
        it,
        callback
      )
    } ?: {
      callback(Result.failure(Throwable("Invalid feature id for the requested feature: $feature")))
    }
  }

  override fun removeFeatureState(
    sourceId: String,
    sourceLayerId: String?,
    featureId: String,
    stateKey: String?,
    callback: (Result<Unit>) -> Unit
  ) {
    mapboxMap.removeFeatureState(sourceId, sourceLayerId, featureId, stateKey) {
      if (it.isError) {
        callback(Result.failure(Throwable("Cannot remove feature state for the requested feature: $featureId")))
      } else {
        callback(Result.success(Unit))
      }
    }
  }

  @OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
  override fun removeFeatureStateForFeaturesetDescriptor(
    featureset: FeaturesetDescriptor,
    featureId: FeaturesetFeatureId,
    stateKey: String?,
    callback: (Result<Unit>) -> Unit
  ) {
    featureset.toTypedFeaturesetDescriptor()?.let { typedFeaturesetDescriptor ->
      mapboxMap.removeFeatureState(
        typedFeaturesetDescriptor,
        featureId.toFeaturesetFeatureId(),
        stateKey?.let { FeatureStateKey.create(it) }
      ) {
        if (it.isError) {
          callback(Result.failure(Throwable("Cannot remove feature state for the requested featureset: {featureId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}} and featureID: $featureId.")))
        } else {
          callback(Result.success(Unit))
        }
      }
    } ?: {
      callback(Result.failure(Throwable("Failed to convert requested featureset: {featureId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}}.")))
    }
  }

  @OptIn(MapboxExperimental::class, MapboxDelicateApi::class)
  override fun removeFeatureStateForFeaturesetFeature(
    feature: FeaturesetFeature,
    stateKey: String?,
    callback: (Result<Unit>) -> Unit
  ) {
    feature.id?.let {
      removeFeatureStateForFeaturesetDescriptor(
        feature.featureset,
        it,
        stateKey,
        callback
      )
    } ?: {
      callback(Result.failure(Throwable("Invalid feature id for the requested feature: $feature")))
    }
  }

  @OptIn(MapboxExperimental::class)
  override fun resetFeatureStatesForFeatureset(
    featureset: FeaturesetDescriptor,
    callback: (Result<Unit>) -> Unit
  ) {
    featureset.toTypedFeaturesetDescriptor()?.let { typedFeaturesetDescriptor ->
      mapboxMap.resetFeatureStates(
        typedFeaturesetDescriptor
      ) {
        if (it.isError) {
          callback(Result.failure(Throwable("Error resetting feature states for the requested featureset: {featureId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}}.")))
        } else {
          callback(Result.success(Unit))
        }
      }
    } ?: {
      callback(Result.failure(Throwable("Failed to convert requested featureset: {featureId: ${featureset.featuresetId}, importId: ${featureset.importId}, layerId: ${featureset.layerId}}.")))
    }
  }

  override fun reduceMemoryUse() {
    mapboxMap.reduceMemoryUse()
  }

  override fun getElevation(coordinate: Point): Double? {
    return mapboxMap.getElevation(coordinate)
  }

  override fun tileCover(options: TileCoverOptions): List<CanonicalTileID> {
    return mapboxMap.tileCover(options.toTileCoverOptions(), null)
      .map { it.toFLTCanonicalTileID() }
  }

  override fun setPrefetchZoomDelta(delta: Long) {
    mapboxMap.setPrefetchZoomDelta(delta.toByte())
  }

  override fun setUserAnimationInProgress(inProgress: Boolean) {
    mapboxMap.setUserAnimationInProgress(inProgress)
  }

  override fun setGestureInProgress(inProgress: Boolean) {
    mapboxMap.setGestureInProgress(inProgress)
  }

  @OptIn(MapboxExperimental::class)
  @SuppressLint("RestrictedApi")
  override fun dispatch(gesture: String, screenCoordinate: com.mapbox.maps.mapbox_maps.pigeons.ScreenCoordinate) {
    val eventType: PlatformEventType = when (gesture) {
      "click" -> PlatformEventType.CLICK
      "longClick" -> PlatformEventType.LONG_CLICK
      "drag" -> PlatformEventType.DRAG
      "dragBegin" -> PlatformEventType.DRAG_BEGIN
      "dragEnd" -> PlatformEventType.DRAG_END
      else -> throw IllegalArgumentException("Invalid gesture type: $gesture")
    }
    mapboxMap.dispatch(
      platformEventInfo = PlatformEventInfo(
        eventType,
        screenCoordinate.toScreenCoordinate(context)
      )
    )
  }
}