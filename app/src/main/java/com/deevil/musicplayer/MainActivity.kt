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
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import com.deevil.mymusicplayer.SwipeListener
import kotlinx.android.synthetic.main.player_layout.*
import kotlinx.android.synthetic.main.select_layout.*
import java.util.*


class MainActivity : AppCompatActivity() {


    private val TAG = "DBG-ACT"

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var sessionToken: MediaSessionCompat.Token

    private var updSeek: Boolean = true

    private val formatBuilder = StringBuilder()
    private val formatter = Formatter(formatBuilder, Locale.getDefault())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val intent = Intent(this, AudioPlayerService::class.java)
        Util.startForegroundService(this, intent)

        btn_select.setOnClickListener {
            selectDir()
        }

        exo_play.setOnClickListener {
            Log.i(TAG, "play")
            mediaController.transportControls.play()
        }

        exo_pause.setOnClickListener {
            Log.i(TAG, "pause")
            mediaController.transportControls.pause()
        }

        exo_prev.setOnClickListener {
            Log.i(TAG, "skipToPrevious")
            mediaController.transportControls.skipToPrevious()
        }


        exo_next.setOnClickListener {
            Log.i(TAG, "skipToNext")
            mediaController.transportControls.skipToNext()
        }

        btn_shuffle.setOnClickListener {
            if (mediaController.shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_NONE) {
                Log.i(TAG, "setShuffleMode SHUFFLE_MODE_ALL")
                mediaController.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                setButtonEnabled(btn_shuffle, true, changeAlpha = false, changeSelected = true)
            } else {
                Log.i(TAG, "setShuffleMode SHUFFLE_MODE_NONE")
                mediaController.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                setButtonEnabled(btn_shuffle, false, changeAlpha = false, changeSelected = true)
            }
        }


        btn_repeat.setOnClickListener {

            if (mediaController.repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE) {
                Log.i(TAG, "setRepeatMode REPEAT_MODE_ALL")
                mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                setButtonEnabled(btn_repeat, true, changeAlpha = false, changeSelected = true)
            } else {
                Log.i(TAG, "setRepeatMode REPEAT_MODE_NONE")
                mediaController.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                setButtonEnabled(btn_repeat, false, changeAlpha = false, changeSelected = true)
            }
        }


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


        play_lay.setOnTouchListener(object : SwipeListener(this) {

            override fun onSwipeTop() {
                Log.i(TAG, "onSwipeTop")
            }

            override fun onSwipeRight() {
                Log.i(TAG, "onSwipeRight")
                mediaController.transportControls.skipToPrevious()
            }

            override fun onSwipeLeft() {
                Log.i(TAG, "onSwipeLeft")
                mediaController.transportControls.skipToNext()
            }

            override fun onSwipeBottom() {
                Log.i(TAG, "onSwipeBottom")
                // Hide app
                val startMain = Intent(Intent.ACTION_MAIN)
                startMain.addCategory(Intent.CATEGORY_HOME)
                startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(startMain)
            }

        })

    }

    /**
     * Change orientation
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setImageByOrientation()
    }

    private fun setImageByOrientation() {
        when (this.resources.configuration.orientation) {

            Configuration.ORIENTATION_PORTRAIT -> {
                play_lay_main.orientation = LinearLayout.VERTICAL
                controls_lay.gravity = Gravity.BOTTOM
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                play_lay_main.orientation = LinearLayout.HORIZONTAL
                controls_lay.gravity = Gravity.CENTER
            }

        }
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
                C.PERMISSION_REQUEST_CODE
            )
        } else {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(Intent.createChooser(i, "Choose directory"), C.DIRECTORY_REQUEST_CODE)
        }

    }

    /**
     * Ask permissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == C.PERMISSION_REQUEST_CODE) {
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

        if (requestCode == C.DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            Log.i(TAG, "Result URI " + data.data)

            val treeUri = data.data ?: return
            mediaController.transportControls.prepareFromUri(treeUri, null)

        }

    }


    private val mMessageReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(C.TAG, "onReceive")
            val b = intent!!.getBundleExtra(C.KEY_SESSION_TOKEN)
            val tmp: MediaSessionCompat.Token? = b.getParcelable(C.KEY_SESSION_TOKEN)
            if (tmp != null) {
                sessionToken = tmp
                connectToSession(sessionToken)
            }
        }
    }

    private fun connectToSession(token: MediaSessionCompat.Token) {
        mediaController = MediaControllerCompat(this, token)
        MediaControllerCompat.setMediaController(this, mediaController)

        mediaController.registerCallback(mMediaControllerCallback)

        mMediaControllerCallback.onPlaybackStateChanged(mediaController.playbackState)
        fillMetadata(mediaController.metadata)

    }

    private val mMediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            Log.i(TAG, "onPlaybackStateChanged " + state.state.toString() + " - " + state.position)

            if (state.state == PlaybackStateCompat.STATE_NONE) {
                sel_lay.visibility = View.VISIBLE
                play_lay.visibility = View.GONE
                setImageByOrientation()
            } else {
                sel_lay.visibility = View.GONE
                play_lay.visibility = View.VISIBLE
                setImageByOrientation()
            }

            val playing = (state.state == PlaybackStateCompat.STATE_PLAYING)
            if (state.state != PlaybackStateCompat.STATE_BUFFERING) {
                if (playing) {
                    exo_play.visibility = View.GONE
                    exo_pause.visibility = View.VISIBLE
                } else {
                    exo_play.visibility = View.VISIBLE
                    exo_pause.visibility = View.GONE
                }
            }
            exo_position.text = getTimeString(state.position)
            exo_progress.progress = state.position.toInt()

            changeSeekBar()
        }


        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.i(TAG, "onMetadataChanged")

            fillMetadata(metadata)
        }
    }

    fun fillMetadata(metadata: MediaMetadataCompat?) {

        Log.i(TAG, "fillMetadata")

        if (metadata!!.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) != null) {
            Log.i("MTD", "METADATA_KEY_ART")
            exo_artwork.setImageDrawable(
                BitmapDrawable(
                    resources, metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
                )
            )
        } else {
            exo_artwork.setImageDrawable(getDrawable(R.drawable.default_img))
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
            Log.i(
                "MTD",
                "METADATA_KEY_DURATION - " + metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toString()
            )
            exo_duration.text = getTimeString(metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
            exo_progress.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
        }

        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE) != null) {
            Log.i(
                "MTD",
                "METADATA_KEY_DISPLAY_SUBTITLE - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE)
            )

            val str = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE).split(",")
            setButtonEnabled(exo_next, str[1] == "true")
            setButtonEnabled(exo_prev, str[2] == "true")
            setButtonEnabled(btn_shuffle, str[3] == "true", changeAlpha = false, changeSelected = true)
            setButtonEnabled(btn_repeat, str[4] == "true", changeAlpha = false, changeSelected = true)
        }

    }

    private fun setButtonEnabled(
        view: View,
        enabled: Boolean,
        changeAlpha: Boolean = true,
        changeSelected: Boolean = false
    ) {

        if (changeAlpha) {
            view.alpha = if (enabled) 1f else 0.3f
        }
        if (changeSelected) {
            view.isSelected = enabled
        } else {
            view.isEnabled = enabled
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(C.TAG, "onResume")
        val filter = IntentFilter(C.INTENT_SESSION_TOKEN)
        registerReceiver(mMessageReceiver, filter)
    }

    private fun changeSeekBar(cur: Long? = null) {

        if (updSeek) {
            if (cur == null) {
                exo_progress.progress = mediaController.playbackState.position.toInt()
                exo_position.text = getTimeString(mediaController.playbackState.position)
            } else {
                exo_progress.progress = cur.toInt()
                exo_position.text = getTimeString(cur)
            }
        }

        if (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
            mHandler.postDelayed({
                changeSeekBar()
            }, 200)
        }
    }

    fun getTimeString(time: Long): String {
        return Util.getStringForTime(formatBuilder, formatter, time)
    }

    private val mHandler = Handler()

}
