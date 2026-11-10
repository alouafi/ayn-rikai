package io.github.alouafi.aynrikai.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.alouafi.aynrikai.R
import io.github.alouafi.aynrikai.analysis.MalformedAnalysisException
import io.github.alouafi.aynrikai.analysis.MissingConfigurationException
import io.github.alouafi.aynrikai.analysis.MissingSetting
import io.github.alouafi.aynrikai.capture.CaptureStoppedException
import io.github.alouafi.aynrikai.capture.NoFrameException
import io.github.alouafi.aynrikai.llm.TruncatedAnswerException
import io.github.alouafi.aynrikai.net.ApiException
import java.io.IOException

/** Why a scan or a connection check failed, in a form the UI can explain. */
sealed interface ScanError {
    data object NoCapture : ScanError
    data object NoFrame : ScanError
    data object Unreadable : ScanError
    data object TooLong : ScanError
    data class MissingConfiguration(val setting: MissingSetting) : ScanError
    data class Api(val status: Int, val message: String) : ScanError
    data class Network(val message: String) : ScanError
    data class Unexpected(val message: String) : ScanError

    /** The fix is in the settings: a missing key, a wrong key or an unknown model. */
    val concernsSettings: Boolean
        get() = this is MissingConfiguration || this is Api && status in setOf(401, 403, 404)
}

fun Throwable.toScanError(): ScanError = when (this) {
    is MissingConfigurationException -> ScanError.MissingConfiguration(setting)
    is ApiException -> ScanError.Api(status, message.orEmpty())
    is MalformedAnalysisException -> ScanError.Unreadable
    is TruncatedAnswerException -> ScanError.TooLong
    is NoFrameException -> ScanError.NoFrame
    is CaptureStoppedException -> ScanError.NoCapture
    is IOException -> ScanError.Network(message ?: javaClass.simpleName)
    else -> ScanError.Unexpected(message ?: javaClass.simpleName)
}

@Composable
fun ScanError.text(): String = when (this) {
    ScanError.NoCapture -> stringResource(R.string.error_no_capture)
    ScanError.NoFrame -> stringResource(R.string.error_no_frame)
    ScanError.Unreadable -> stringResource(R.string.error_unreadable)
    ScanError.TooLong -> stringResource(R.string.error_too_long)
    is ScanError.MissingConfiguration -> stringResource(
        when (setting) {
            MissingSetting.MISTRAL_API_KEY -> R.string.error_missing_mistral_key
            MissingSetting.OPENAI_BASE_URL -> R.string.error_missing_base_url
            MissingSetting.OPENAI_MODEL -> R.string.error_missing_model
        },
    )
    is ScanError.Api -> stringResource(R.string.error_api, status, message)
    is ScanError.Network -> stringResource(R.string.error_network, message)
    is ScanError.Unexpected -> stringResource(R.string.error_unexpected, message)
}
