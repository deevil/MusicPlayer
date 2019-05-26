package com.deevil.musicplayer

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.util.Util
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.player_layout.*
import kotlinx.android.synthetic.main.select_layout.*


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 9998
    private val DIRECTORY_REQUEST_CODE = 9999
    private val TAG = "DBG-ACT"

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var sessionToken: MediaSessionCompat.Token

    private var updSeek: Boolean = true
    private var hasNext: Boolean = false
    private var hasPrev: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val intent = Intent(this, AudioPlayerService::class.java)
        Util.startForegroundService(this, intent)

        btn_select.setOnClickListener {
            selectDir()
        }

        exo_play.setOnClickListener {
            Log.i("TST", "play")
            mediaController.transportControls.play()
        }

        exo_pause.setOnClickListener {
            Log.i("TST", "pause")
            mediaController.transportControls.pause()
        }

        exo_prev.setOnClickListener {
            Log.i("TST", "skipToPrevious")
            mediaController.transportControls.skipToPrevious()
        }


        exo_next.setOnClickListener {
            Log.i("TST", "skipToNext")
            mediaController.transportControls.skipToNext()
        }

        btn_shuffle.setOnClickListener {
            if (mediaController.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
                Log.i("TST", "setShuffleMode SHUFFLE_MODE_ALL")
                mediaController.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
            } else {
                Log.i("TST", "setShuffleMode SHUFFLE_MODE_NONE")
                mediaController.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
            }
        }

        btn_repeat.setOnClickListener {

            if (mediaController.repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                Log.i("TST", "setRepeatMode REPEAT_MODE_ALL")
                mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
            } else {
                Log.i("TST", "setRepeatMode REPEAT_MODE_NONE")
                mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
            }
        }
        //mediaController.


        exo_progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                updSeek = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mediaController.transportControls.seekTo(seekBar.progress.toLong())
                updSeek = true
            }
        })

    }


    private fun selectDir() {

        // Check permissions
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            //i.setType("audio/*");
            startActivityForResult(Intent.createChooser(i, "Choose directory"), DIRECTORY_REQUEST_CODE)
        }

    }

    /**
     * Ask permissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted!")
                selectDir()
            } else {
                Log.i(TAG, "Permission denied")
                Toast.makeText(this, "Нет прав на чтение файловой системы", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * When receive answer from other activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            Log.i(TAG, "Result URI " + data.data)

            val treeUri = data.data ?: return
            mediaController.transportControls.prepareFromUri(treeUri, null)

        }

    }


    private val mMessageReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.w(C.TAG, "onReceive")
            val b = intent!!.getBundleExtra(C.KEY_SESSION_TOKEN)
            val tmp: MediaSessionCompat.Token? = b.getParcelable(C.KEY_SESSION_TOKEN)
            if (tmp != null) {
                sessionToken = tmp
                connectToSession(sessionToken)
                //                LogHelper.e(FragmentActivity.TAG, "on sessionToken receive")
//                try {
//                    connectToSession(sessionToken)
//                } catch (re: RemoteException) {
//                    LogHelper.e(FragmentActivity.TAG, re, "could not connect media controller")
//                    hidePlaybackControls()
//                }

            }
        }
    }

    private fun connectToSession(token: MediaSessionCompat.Token) {
        //LogHelper.e(FragmentActivity.TAG, "connectToSession")
        mediaController = MediaControllerCompat(this, token)
        MediaControllerCompat.setMediaController(this, mediaController)

        mediaController.registerCallback(mMediaControllerCallback)

        mMediaControllerCallback.onPlaybackStateChanged(mediaController.playbackState)
        //mMediaControllerCallback.onMetadataChanged(mediaController.metadata)
        fillMetadata(mediaController.metadata)


//        if (shouldShowControls()) {
//            showPlaybackControls()
//        } else {
//            LogHelper.e(
//                FragmentActivity.TAG,
//                "connectionCallback.onConnected: hiding controls because metadata is null"
//            )
//            hidePlaybackControls()
//        }
//
//        if (mControlsFragment != null) {
//            mControlsFragment.onConnected()
//        }

        //onMediaControllerConnected()
    }

    private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            Log.w("TST", "onPlaybackStateChanged " + state.state.toString() + " - " + state.position)

            if (state.state == PlaybackStateCompat.STATE_NONE) {
                sel_lay.visibility = View.VISIBLE
                play_lay.visibility = View.GONE
                //Log.w("DDD", "ALL HIDDEN")
            } else {
                //Log.w("DDD", "ALL NOT HIDDEN")
                sel_lay.visibility = View.GONE
                play_lay.visibility = View.VISIBLE
            }
            val playing = state.state == PlaybackStateCompat.STATE_PLAYING
            exo_position.text = state.position.toString()
            exo_progress.progress = state.position.toInt()
            //textView7.text = state.state.toString()


            //val playing = state.state == PlaybackStateCompat.STATE_PLAYING
//            LogHelper.e(FragmentActivity.TAG, "onPlaybackStateChanged")
//            if (shouldShowControls()) {
//                showPlaybackControls()
//            } else {
//                LogHelper.e(
//                    FragmentActivity.TAG,
//                    "mediaControllerCallback.onPlaybackStateChanged: hiding controls because " + "state is ",
//                    state.state
//                )
//                hidePlaybackControls()
//            }
            //textView5.text = playing.toString()
            changeSeekBar()

            //super.onPlaybackStateChanged(state)
        }


        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.w("TST", "onMetadataChanged")

            fillMetadata(metadata)

            //super.onMetadataChanged(metadata)
        }

        override fun onSessionReady() {
            Log.w("TST", "onSessionReady")
            //super.onSessionReady()
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            Log.w("TST", "onSessionEvent")
            //super.onSessionEvent(event, extras)
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            Log.w("TST", "onAudioInfoChanged")
            //super.onAudioInfoChanged(info)
        }

        override fun onExtrasChanged(extras: Bundle?) {
            Log.w("TST", "onExtrasChanged")
            //super.onExtrasChanged(extras)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            //mediaController.queueTitle
            //queue.
            Log.w("TST", "onQueueChanged")
            //super.onQueueChanged(queue)
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            Log.w("TST", "onQueueTitleChanged")
            //super.onQueueTitleChanged(title)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.w("TST", "onRepeatModeChanged")
            //super.onRepeatModeChanged(repeatMode)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            Log.w("TST", "onShuffleModeChanged")
            //super.onShuffleModeChanged(shuffleMode)
        }
    }

    fun fillMetadata(metadata: MediaMetadataCompat?) {

        Log.i("MTD", "fillMetadata")

        if (metadata!!.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) != null) {
            Log.i("MTD", "METADATA_KEY_ART")
            exo_artwork.setImageDrawable(
                BitmapDrawable(resources, metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
                )
            )
        }

        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) != null) {
            Log.i("MTD", "METADATA_KEY_TITLE - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            Title.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        }


        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) != null) {
            Log.i("MTD", "METADATA_KEY_ARTIST - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            Artist.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        }

        if (metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) != null) {
            Log.i("MTD", "METADATA_KEY_DURATION - " + metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toString())
            exo_duration.text = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toString()
            exo_progress.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
        }

        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE) != null) {
            Log.i("MTD", "METADATA_KEY_TRACK_NUMBER - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
            val str = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE).split(",")


            hasNext = str[1] == "true"
            hasPrev = str[2] == "true"
            exo_next.isEnabled = hasNext
            exo_prev.isEnabled = hasPrev

        }

    }

    override fun onResume() {
        super.onResume()
        Log.w(C.TAG, "onResume")
        val filter = IntentFilter(C.INTENT_SESSION_TOKEN)
        registerReceiver(mMessageReceiver, filter)

//        if (sessionToken != null) {
//            try {
//                connectToSession(sessionToken)
//                if (lastTransaction != null) {
//                    lastTransaction.commit()
//                    lastTransaction = null
//                }
//            } catch (re: RemoteException) {
//                LogHelper.e(FragmentActivity.TAG, re, "could not connect media controller")
//                hidePlaybackControls()
//            }
//
//        }
    }

    private fun changeSeekBar(cur: Long? = null) {

        if (updSeek) {
            if (cur == null) {
                exo_progress.progress = mediaController.playbackState.position.toInt()
                exo_position.text = mediaController.playbackState.position.toString()
            } else {
                exo_progress.progress = cur.toInt()
            }
        }

        if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            mHandler.postDelayed(Runnable {
                changeSeekBar()
                //mHandler.postDelayed(this, 200)
            }, 200)
        }
    }

    private val mHandler = Handler()

}
