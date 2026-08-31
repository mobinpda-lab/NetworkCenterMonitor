package com.mobinpdalab.networkcentermonitor.domain.camera

import com.mobinpdalab.networkcentermonitor.domain.model.DeviceId
import com.mobinpdalab.networkcentermonitor.domain.model.DeviceProfile
import com.mobinpdalab.networkcentermonitor.domain.model.NetworkPort
import com.mobinpdalab.networkcentermonitor.domain.model.SourcedValue

@JvmInline
value class CameraProfileId(val value: String) {
    init { require(value.isNotBlank()) }
}

@JvmInline
value class RecorderProfileId(val value: String) {
    init { require(value.isNotBlank()) }
}

enum class OnvifProfile {
    T,
    S,
    G,
    M,
}

enum class CameraType {
    DOME,
    BULLET,
    PTZ,
    BOX,
    FISHEYE,
    THERMAL,
    OTHER,
    UNKNOWN,
}

enum class CameraHealthMode {
    HEALTH_CHECK,
    SNAPSHOT,
    LIVE,
}

enum class StreamKind {
    SUB_STREAM,
    MAIN_STREAM,
}

enum class BandwidthProfile {
    LOW,
    NORMAL,
    HIGH,
}

data class CameraCapabilities(
    val onvifProfiles: Set<OnvifProfile> = emptySet(),
    val rtsp: Boolean = false,
    val http: Boolean = false,
    val https: Boolean = false,
    val mainStream: Boolean = false,
    val subStream: Boolean = false,
    val ptz: Boolean = false,
    val audio: Boolean = false,
    val events: Boolean = false,
    val recording: Boolean = false,
    val hddHealth: Boolean = false,
    val vendorApi: Boolean = false,
)

data class CameraProfile(
    val id: CameraProfileId,
    override val deviceId: DeviceId,
    val environmentName: String? = null,
    val installationLocation: String? = null,
    val cameraType: CameraType = CameraType.UNKNOWN,
    val recorderDeviceId: DeviceId? = null,
    val channelNumber: Int? = null,
    val httpPort: NetworkPort? = null,
    val httpsPort: NetworkPort? = null,
    val rtspPort: NetworkPort? = null,
    val onvifPort: NetworkPort? = null,
    val codec: SourcedValue<String>? = null,
    val resolution: SourcedValue<String>? = null,
    val fps: SourcedValue<Int>? = null,
    val bitrateKbps: SourcedValue<Int>? = null,
    val capabilities: CameraCapabilities = CameraCapabilities(),
    val notes: String? = null,
) : DeviceProfile {
    init {
        require(environmentName == null || environmentName.isNotBlank())
        require(installationLocation == null || installationLocation.isNotBlank())
        require(channelNumber == null || channelNumber > 0)
        require(fps == null || fps.value > 0)
        require(bitrateKbps == null || bitrateKbps.value > 0)
        require(recorderDeviceId != deviceId) { "Camera cannot record itself" }
    }
}

data class RecorderProfile(
    val id: RecorderProfileId,
    override val deviceId: DeviceId,
    val channelCount: SourcedValue<Int>? = null,
    val recordingSupported: Boolean = false,
    val hddHealthSupported: Boolean = false,
    val onvifProfiles: Set<OnvifProfile> = emptySet(),
    val notes: String? = null,
) : DeviceProfile {
    init { require(channelCount == null || channelCount.value > 0) }
}

data class CameraBandwidthPolicy(
    val profile: BandwidthProfile = BandwidthProfile.LOW,
    val maxConcurrentStreamsPerCenter: Int = 1,
) {
    init { require(maxConcurrentStreamsPerCenter > 0) }

    val persistentLiveStreamingAllowed: Boolean get() = false
    val stopStreamImmediatelyOnViewExit: Boolean get() = true

    fun chooseStream(mainStreamExplicitlyRequested: Boolean): StreamKind =
        if (mainStreamExplicitlyRequested) StreamKind.MAIN_STREAM else StreamKind.SUB_STREAM

    fun canOpenLive(activeStreamsInCenter: Int): Boolean {
        require(activeStreamsInCenter >= 0)
        return activeStreamsInCenter < maxConcurrentStreamsPerCenter
    }
}

/**
 * Vendor-neutral capability order. Vendor adapters extend gaps only after ONVIF/RTSP/HTTP checks.
 */
object CameraDiscoveryPriority {
    val onvifProfiles: List<OnvifProfile> = listOf(
        OnvifProfile.T,
        OnvifProfile.S,
        OnvifProfile.G,
        OnvifProfile.M,
    )
}
