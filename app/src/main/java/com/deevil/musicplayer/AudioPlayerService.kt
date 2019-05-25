package com.deevil.musicplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.deevil.musicplayer.Samples.SAMPLES
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.id3.ApicFrame
import com.google.android.exoplayer2.metadata.id3.Id3Frame
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class AudioPlayerService : Service() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private var mediaSession: MediaSessionCompat? = null
    private var mediaSessionConnector: MediaSessionConnector? = null
    private val mServiceHandler = ServiceHandler(this)

    override fun onCreate() {
        super.onCreate()
        val context = this

        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())

        //val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, getString(R.string.app_name)))
//        //val cacheDataSourceFactory = CacheDataSourceFactory(DownloadUtil.getCache(context), dataSourceFactory, CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
//        val concatenatingMediaSource = ConcatenatingMediaSource()
//
//        for (sample in SAMPLES) {
//            val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(sample.uri)
//            concatenatingMediaSource.addMediaSource(mediaSource)
//        }
//        player.prepare(concatenatingMediaSource)
//        player.playWhenReady = true


        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context,
            C.PLAYBACK_CHANNEL_ID,
            R.string.playback_channel_name,
            C.PLAYBACK_NOTIFICATION_ID,
            object : PlayerNotificationManager.MediaDescriptionAdapter {
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
                   // return SAMPLES[player.currentWindowIndex].title
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
                           // return SAMPLES[player.currentWindowIndex].description
                }

                override fun getCurrentLargeIcon(player: Player, callback: PlayerNotificationManager.BitmapCallback): Bitmap {
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
            },
            object : PlayerNotificationManager.NotificationListener {
                override fun onNotificationStarted(notificationId: Int, notification: Notification) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int) {
                    stopSelf()
                }
            }
        )


        playerNotificationManager.setUseChronometer(true)
        playerNotificationManager.setFastForwardIncrementMs(0)
        playerNotificationManager.setRewindIncrementMs(0)

        playerNotificationManager.setPlayer(player)

        mediaSession = MediaSessionCompat(context, C.MEDIA_SESSION_TAG)
        mediaSession!!.isActive = true
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        playerNotificationManager.setMediaSessionToken(mediaSession!!.sessionToken)

        mediaSessionConnector = MediaSessionConnector(mediaSession)
//        mediaSessionConnector!!.setQueueNavigator(object : TimelineQueueNavigator(mediaSession) {
//            override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
//                return Samples.getMediaDescription(context, SAMPLES[windowIndex])
//            }
//        })


        mediaSessionConnector!!.setPlayer(player)
        mediaSessionConnector!!.setPlaybackPreparer(AudioPlayerPreparer(player, this))

        //mediaSession!!.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
        //mediaSessionConnector.
        //player.repeatMode = Player.REPEAT_MODE_ALL
        //player.
    }

    override fun onDestroy() {
        mediaSession!!.release()
        mediaSessionConnector!!.setPlayer(null)
        playerNotificationManager.setPlayer(null)
        player.release()

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        //Log.w(TAG, "onBind")
        return AudioPlayerServiceBinder()
    }

    inner class AudioPlayerServiceBinder : Binder() {
        val mediaSessionToken: MediaSessionCompat.Token
            get() = mediaSession!!.sessionToken
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.w(C.TAG, "onStartCommand")
        mServiceHandler.removeCallbacksAndMessages(null)
        mServiceHandler.sendEmptyMessage(0)
        return START_STICKY
    }


    // Define how the handler will process messages
    private inner class ServiceHandler(service: AudioPlayerService) : Handler() {


        // Define how to handle any incoming messages here
        override fun handleMessage(message: Message) {
            Log.w(C.TAG, "handleMessage")
            val intent = Intent(C.INTENT_SESSION_TOKEN)
            val b = Bundle()
            b.putParcelable(C.KEY_SESSION_TOKEN, mediaSession!!.sessionToken)
            intent.putExtra(C.KEY_SESSION_TOKEN, b)
            sendBroadcast(intent)

        }
    }
}
