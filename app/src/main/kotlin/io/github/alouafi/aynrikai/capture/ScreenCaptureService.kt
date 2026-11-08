package io.github.alouafi.aynrikai.capture

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.IntentCompat
import io.github.alouafi.aynrikai.R
import io.github.alouafi.aynrikai.appContainer
import io.github.alouafi.aynrikai.ui.LauncherActivity

/** Holds the screen capture permission while the companion runs. Android requires a foreground service for it. */
class ScreenCaptureService : Service() {

    private val captureRepository: CaptureRepository
        get() = appContainer.captureRepository

    private var projection: MediaProjection? = null
    private var capturer: ScreenCapturer? = null

    /** The user or the system ended the capture. */
    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val consent = intent?.let { IntentCompat.getParcelableExtra(it, EXTRA_CONSENT, Intent::class.java) }
        if (consent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        // The foreground type must be active before the projection is created.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
        )
        release()
        try {
            startCapture(resultCode, consent)
        } catch (_: RuntimeException) {
            release()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, consent: Intent) {
        val manager = getSystemService(MediaProjectionManager::class.java)
        val projection = manager.getMediaProjection(resultCode, consent) ?: throw CaptureStoppedException()
        this.projection = projection
        // Android 14 requires the callback before the virtual display is created.
        projection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))
        capturer = ScreenCapturer(this, projection).also(captureRepository::publish)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        captureRepository.publish(null)
        capturer?.close()
        capturer = null
        // Unregister first: a restart releases the old projection, and its onStop must not end the new one.
        projection?.run {
            unregisterCallback(projectionCallback)
            stop()
        }
        projection = null
    }

    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.capture_channel), NotificationManager.IMPORTANCE_LOW),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, LauncherActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenCaptureService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, getString(R.string.capture_stop), stop).build())
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val ACTION_STOP = "io.github.alouafi.aynrikai.action.STOP_CAPTURE"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_CONSENT = "consent"
        private const val CHANNEL_ID = "capture"
        private const val NOTIFICATION_ID = 1

        /** Starts capturing with the result of the screen capture consent dialog. */
        fun start(context: Context, resultCode: Int, consent: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_CONSENT, consent)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
