package com.deevil.musicplayer

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.*
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.android.exoplayer2.metadata.id3.Id3Frame
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray


class AudioPlayerService : Service() {

    private lateinit var player: ExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private val mServiceHandler = ServiceHandler()

    override fun onCreate() {

        super.onCreate()
        val context = this

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())

        mediaSession = MediaSessionCompat(context, C.MEDIA_SESSION_TAG)
        mediaSession.isActive = true
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)


        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            C.PLAYBACK_CHANNEL_ID,
            R.string.playback_channel_name,
            C.PLAYBACK_NOTIFICATION_ID,
            MyMediaDescriptionAdapter(context),
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                    Log.i("NOT", "onNotificationStarted")
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int) {
                    Log.i("NOT", "onNotificationCancelled")
                    stopSelf()
                }
            }
        )
        playerNotificationManager.setUseChronometer(true)
        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)
        playerNotificationManager.setPlayer(player)
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)


        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(player)
        mediaSessionConnector.setPlaybackPreparer(AudioPlayerPreparer(player, this))
        mediaSessionConnector.setMediaMetadataProvider(MyMediaMetadataProvider(this))
        mediaSessionConnector.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                return MediaDescriptionCompat.Builder().setTitle(windowIndex.toString())
                    .setMediaId(windowIndex.toString()).build()
            }
        })

        player.addListener(MyPlayerEventListener(this, player, mediaSession))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(C.PLAYBACK_CHANNEL_ID, C.PLAYBACK_CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = C.PLAYBACK_CHANNEL_ID
            notificationChannel.setSound(null, null)
            val notification = Notification.Builder(this, C.PLAYBACK_CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(com.google.android.exoplayer2.ui.R.drawable.exo_notification_small_icon)
                .build()
            startForeground(C.PLAYBACK_NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
        playerNotificationManager.setPlayer(null)
        player.release()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i(C.TAG, "onBind")
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(C.TAG, "onStartCommand")
        mServiceHandler.removeCallbacksAndMessages(null)
        mServiceHandler.sendEmptyMessage(0)
        return START_STICKY
    }


    @SuppressLint("HandlerLeak")
    private inner class ServiceHandler: Handler() {
        override fun handleMessage(message: Message) {
            Log.i(C.TAG, "handleMessage")
            val intent = Intent(C.INTENT_SESSION_TOKEN)
            val b = Bundle()
            b.putParcelable(C.KEY_SESSION_TOKEN, mediaSession.sessionToken)
            intent.putExtra(C.KEY_SESSION_TOKEN, b)
            sendBroadcast(intent)
        }
    }


    class MyPlayerEventListener(private val context: Context, private val player: ExoPlayer, private val mediaSession: MediaSessionCompat) :
        Player.EventListener {
        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            Log.i("PEL", "onTracksChanged")
            var art: String? = null
            var tit: String? = null
            var bitmap: Bitmap? = null

            for (i in 0 until (trackSelections?.length ?: 0)) {
                val selection = trackSelections?.get(i)
                for (j in 0 until (selection?.length() ?: 0)) {
                    val metadata: Metadata? = selection?.getFormat(j)?.metadata
                    for (z in 0 until (metadata?.length() ?: 0)) {
                        val metadataEntry = metadata?.get(z)
                        if (metadataEntry is Id3Frame) {
                            when (metadataEntry.id) {
                                "TPE1" -> art = (metadataEntry as TextInformationFrame).value
                                "TIT2" -> tit = (metadataEntry as TextInformationFrame).value
                            }
                        }
                        if (metadataEntry is ApicFrame) {
                            val bitmapData = metadataEntry.pictureData
                            bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
                        }
                    }
                }
            }

            val metadataBuilder = MediaMetadataCompat.Builder()
            val mediaController = mediaSession.controller
            //if (Art != null && Tit != null) {

            if (bitmap != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
            } else if ((tit != null && tit != "") || (art != null && art != "")) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, (context.resources.getDrawable(R.drawable.default_img) as BitmapDrawable).bitmap)
            } else {
                metadataBuilder.putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ART,
                    mediaController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
                )
            }

            if (tit != null && tit != "") {
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, tit)
            } else {
                metadataBuilder.putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                )
            }

            if (art != null && art != "") {
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, art)
            } else {
                metadataBuilder.putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                )
            }

            val dur = player.duration
            if (dur > 0) {
                metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, dur)
            } else {
                metadataBuilder.putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    mediaController.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                )
            }

            var str = "true"
            str += "," + if (player.hasNext()) "true" else "false"
            str += "," + if (player.hasPrevious()) "true" else "false"
            str += "," + if (player.shuffleModeEnabled) "true" else "false"
            str += "," + if (player.repeatMode == Player.REPEAT_MODE_ALL) "true" else "false"

            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, str)

            mediaSession.setMetadata(metadataBuilder.build())
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.i("PEL", "onRepeatModeChanged")
            val metadataBuilder = MediaMetadataCompat.Builder()
            val mediaController = mediaSession.controller

            metadataBuilder.putBitmap(
                MediaMetadataCompat.METADATA_KEY_ART,
                mediaController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
            )

            metadataBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            )

            metadataBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            )

            metadataBuilder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                mediaController.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )

            var str = "true"
            str += "," + if (player.hasNext()) "true" else "false"
            str += "," + if (player.hasPrevious()) "true" else "false"
            str += "," + if (player.shuffleModeEnabled) "true" else "false"
            str += "," + if (player.repeatMode == Player.REPEAT_MODE_ALL) "true" else "false"

            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, str)

            mediaSession.setMetadata(metadataBuilder.build())
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            Log.i("PEL", "onShuffleModeEnabledChanged")
            val metadataBuilder = MediaMetadataCompat.Builder()
            val mediaController = mediaSession.controller

            metadataBuilder.putBitmap(
                MediaMetadataCompat.METADATA_KEY_ART,
                mediaController.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
            )

            metadataBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_TITLE,
                mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            )

            metadataBuilder.putString(
                MediaMetadataCompat.METADATA_KEY_ARTIST,
                mediaController.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            )

            metadataBuilder.putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION,
                mediaController.metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
            )

            var str = "true"
            str += "," + if (player.hasNext()) "true" else "false"
            str += "," + if (player.hasPrevious()) "true" else "false"
            str += "," + if (player.shuffleModeEnabled) "true" else "false"
            str += "," + if (player.repeatMode == Player.REPEAT_MODE_ALL) "true" else "false"

            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, str)

            mediaSession.setMetadata(metadataBuilder.build())
        }

    }

    class MyMediaDescriptionAdapter(private val context: Context) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        override fun getCurrentContentTitle(player: Player): String {

            for (i in 0 until (player.currentTrackSelections.length)) {
                val selection = player.currentTrackSelections.get(i)
                for (j in 0 until (selection?.length() ?: 0)) {
                    val metadata: Metadata? = selection?.getFormat(j)?.metadata
                    for (z in 0 until (metadata?.length() ?: 0)) {
                        val metadataEntry = metadata?.get(z)
                        if (metadataEntry is Id3Frame) {
                            if (metadataEntry.id == "TPE1") {
                                return (metadataEntry as TextInformationFrame).value
                            }

                        }
                    }
                }
            }
            return ""
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return null
        }

        override fun getCurrentContentText(player: Player): String {
            for (i in 0 until (player.currentTrackSelections.length)) {
                val selection = player.currentTrackSelections.get(i)
                for (j in 0 until (selection?.length() ?: 0)) {
                    val metadata: Metadata? = selection?.getFormat(j)?.metadata
                    for (z in 0 until (metadata?.length() ?: 0)) {
                        val metadataEntry = metadata?.get(z)
                        if (metadataEntry is Id3Frame) {
                            if (metadataEntry.id == "TIT2") {
                                return (metadataEntry as TextInformationFrame).value
                            }

                        }
                    }
                }
            }
            return ""
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap {
            for (i in 0 until (player.currentTrackSelections.length)) {
                val selection = player.currentTrackSelections.get(i)
                for (j in 0 until (selection?.length() ?: 0)) {
                    val metadata: Metadata? = selection?.getFormat(j)?.metadata
                    for (z in 0 until (metadata?.length() ?: 0)) {
                        val metadataEntry = metadata?.get(z)
                        if (metadataEntry is ApicFrame) {
                            val bitmapData = metadataEntry.pictureData
                            return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
                        }
                    }
                }
            }
            return (context.resources.getDrawable(R.drawable.default_img) as BitmapDrawable).bitmap
        }
    }

    class MyMediaMetadataProvider(var context: Context): MediaMetadataProvider {

        override fun getMetadata(player: Player?): MediaMetadataCompat {

            Log.i("DBG", "MyMediaMetadataProvider")
            var art: String? = null
            var tit: String? = null
            var bitmap: Bitmap? = null
            val trackSelections = player!!.currentTrackSelections
            val metadataBuilder = MediaMetadataCompat.Builder()

            for (i in 0 until (trackSelections?.length ?: 0)) {
                val selection = trackSelections?.get(i)
                for (j in 0 until (selection?.length() ?: 0)) {
                    val metadata: Metadata? = selection?.getFormat(j)?.metadata
                    for (z in 0 until (metadata?.length() ?: 0)) {
                        val metadataEntry = metadata?.get(z)
                        if (metadataEntry is Id3Frame) {
                            when (metadataEntry.id) {
                                "TPE1" -> art = (metadataEntry as TextInformationFrame).value
                                "TIT2" -> tit = (metadataEntry as TextInformationFrame).value
                            }
                        }
                        if (metadataEntry is ApicFrame) {
                            val bitmapData = metadataEntry.pictureData
                            bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
                        }
                    }
                }
            }


            if (bitmap != null) {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
            } else {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, (context.resources.getDrawable(R.drawable.default_img) as BitmapDrawable).bitmap)
            }

            if (tit != null && tit != "") {
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, tit)
            }

            if (art != null && art != "") {
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, art)
            }

            val dur = player.duration
            if (dur > 0) {
                metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, dur)
            }

            var str = "true"
            str += "," + if (player.hasNext()) "true" else "false"
            str += "," + if (player.hasPrevious()) "true" else "false"
            str += "," + if (player.shuffleModeEnabled) "true" else "false"
            str += "," + if (player.repeatMode == Player.REPEAT_MODE_ALL) "true" else "false"

            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, str)

            return metadataBuilder.build()
        }

    }


}
