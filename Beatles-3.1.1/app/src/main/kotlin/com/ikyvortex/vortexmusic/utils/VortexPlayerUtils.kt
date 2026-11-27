package com.ikyvortex.vortexmusic.utils

import com.ikyvortex.vortexmusic.constants.VideoQuality
import com.ikyvortex.auravideo.VortexVideo
import com.ikyvortex.auravideo.VortexVideo.VideoStreamResult
import com.ikyvortex.auravideo.VortexVideo.VideoSearchResult
import com.ikyvortex.auravideo.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object VortexPlayerUtils {
    private const val logTag = "VortexPlayerUtils"

    /**
     * Set the preferred video quality before fetching video URLs
     */
    fun setPreferredVideoQuality(quality: VideoQuality) {
        // Map PreferenceKeys.VideoQuality to VortexVideo.VideoQuality
        val auraQuality = when (quality) {
            VideoQuality.QUALITY_360P -> VortexVideo.VideoQuality.QUALITY_360P
            VideoQuality.QUALITY_480P -> VortexVideo.VideoQuality.QUALITY_480P
            VideoQuality.QUALITY_720P -> VortexVideo.VideoQuality.QUALITY_720P
            VideoQuality.QUALITY_1080P -> VortexVideo.VideoQuality.QUALITY_1080P
        }
        VortexVideo.setPreferredVideoQuality(auraQuality)
    }

    suspend fun getVideoStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("VortexPlayerUtils: Fetching video stream URL for videoId: $videoId")

        VortexVideo.getVideoStreamUrl(videoId).map { result ->
            Timber.tag(logTag).d("VortexPlayerUtils: Successfully obtained video URL with mimeType: ${result.mimeType}")
            "${result.url}|${result.mimeType}"
        }.also { result ->
            result.onSuccess { _ ->
                Timber.tag(logTag).d("VortexPlayerUtils: Successfully obtained video URL")
            }.onFailure { e ->
                Timber.tag(logTag).e(e, "VortexPlayerUtils: Failed to get video URL")
            }
        }
    }

    /**
     * Resolve a video stream as either a single muxed URL or a pair of
     * video-only + audio-only streams that the player must merge.
     *
     * For quality > 720p, a single muxed URL is physically not available from
     * YouTube — the caller MUST handle the [VortexVideo.VideoStreamSource.Merged]
     * case to actually display 1080p video.
     */
    suspend fun getVideoStreamSource(videoId: String): Result<VortexVideo.VideoStreamSource> =
        withContext(Dispatchers.IO) {
            Timber.tag(logTag).d("VortexPlayerUtils: Resolving stream source for videoId: $videoId")
            VortexVideo.getVideoStreamSource(videoId).also { result ->
                result.onSuccess { source ->
                    when (source) {
                        is VortexVideo.VideoStreamSource.Single ->
                            Timber.tag(logTag).d("VortexPlayerUtils: Single source (${source.mimeType})")
                        is VortexVideo.VideoStreamSource.Merged ->
                            Timber.tag(logTag)
                                .d("VortexPlayerUtils: Merged source ${source.height}p video + audio")
                    }
                }.onFailure { e ->
                    Timber.tag(logTag).e(e, "VortexPlayerUtils: Failed to resolve stream source")
                }
            }
        }

    /**
     * Get video stream URL with automatic search fallback for regular songs
     * This searches for official music video if direct lookup fails
     */
    suspend fun getVideoStreamUrlWithFallback(songTitle: String, artistName: String, videoId: String, isVideoSong: Boolean = true): Result<VideoSearchResult> = withContext(Dispatchers.IO) {
        Timber.tag(logTag).d("VortexPlayerUtils: Trying to get video with fallback for: $songTitle by $artistName (isVideoSong=$isVideoSong)")

        try {
            val result = VortexVideo.getVideoStreamUrlWithFallback(songTitle, artistName, videoId, isVideoSong)
            result.onSuccess { searchResult ->
                Timber.tag(logTag).d("VortexPlayerUtils: Found video via fallback: ${searchResult.videoId}")
            }
            result
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "VortexPlayerUtils: Fallback search also failed")
            Result.failure(e)
        }
    }

    suspend fun hasVideoPlayback(videoId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            VortexVideo.hasVideoPlayback(videoId)
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "VortexPlayerUtils: Error checking video availability")
            false
        }
    }

    suspend fun getVideoDetails(videoId: String): Result<VideoItem> = withContext(Dispatchers.IO) {
        VortexVideo.getVideoDetails(videoId)
    }
}

/** Maps app-level video quality constants to the library enum, used by settings UI. */
fun appVideoQualityToLibrary(quality: VideoQuality): VortexVideo.VideoQuality = when (quality) {
    VideoQuality.QUALITY_360P -> VortexVideo.VideoQuality.QUALITY_360P
    VideoQuality.QUALITY_480P -> VortexVideo.VideoQuality.QUALITY_480P
    VideoQuality.QUALITY_720P -> VortexVideo.VideoQuality.QUALITY_720P
    VideoQuality.QUALITY_1080P -> VortexVideo.VideoQuality.QUALITY_1080P
}
