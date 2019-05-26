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
import kotlinx.android.synthetic.main.activity_main.*
import android.content.IntentFilter
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.os.SystemClock
import android.widget.SeekBar


class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 9998
    private val DIRECTORY_REQUEST_CODE = 9999
    private val TAG = "DBG-ACT"

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var serviceConnection: ServiceConnection
    private lateinit var sessionToken: MediaSessionCompat.Token

    private var updSeek: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val intent = Intent(this, AudioPlayerService::class.java)
        Util.startForegroundService(this, intent)

        button.setOnClickListener {

            selectDir()
        }

        button2.setOnClickListener {
            Log.i("TST", "play")
            mediaController.transportControls.play()
        }

        button3.setOnClickListener {
            Log.i("TST", "pause")
            mediaController.transportControls.pause()
        }

        button4.setOnClickListener {
            Log.i("TST", "skipToPrevious")
            mediaController.transportControls.skipToPrevious()
        }


        button5.setOnClickListener {
            Log.i("TST", "skipToNext")
            mediaController.transportControls.skipToNext()
        }
        //mediaController.


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the current progress of SeekBar
                //text_view.text = "Progress : $i"
                //mediaController.transportControls.seekTo(i.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Do something
                updSeek = false
                //Toast.makeText(applicationContext, "start tracking", Toast.LENGTH_SHORT).show()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Do something
                //Toast.makeText(applicationContext, "stop tracking", Toast.LENGTH_SHORT).show()
                mediaController.transportControls.seekTo(seekBar.progress.toLong())
                updSeek = true
            }
        })
//        seekBar.upda

        //MediaControllerCompat.getMediaController(this)


        // mediaController = MediaControllerCompat
//        val listView = findViewById(R.id.list_view)
//        listView.setAdapter(
//            ArrayAdapter(this, android.R.layout.simple_list_item_1, SAMPLES)
//        )
//        listView.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
//            val action = ProgressiveDownloadAction(
//                SAMPLES[position].uri, false, null, null
//            )
//            AudioDownloadService.startWithAction(
//                this@MainActivity,
//                AudioDownloadService::class.java,
//                action,
//                false
//            )
//        })


//        serviceConnection = object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName, service: IBinder) {
//                playerServiceBinder = service as AudioPlayerService.AudioPlayerServiceBinder
//                try {
//                    mediaController =
//                        MediaControllerCompat(this@MainActivity, playerServiceBinder.mediaSessionToken)
//                    mediaController.registerCallback(callback)
//                    callback.onPlaybackStateChanged(mediaController.playbackState)
//                } catch (e: RemoteException) {
//                    //mediaController = null
//                }
//
//            }
//
//            override fun onServiceDisconnected(name: ComponentName) {
//                //playerServiceBinder = null
//                mediaController.unregisterCallback(callback)
////                if (mediaController != null) {
////                    mediaController.unregisterCallback(callback)
////                    mediaController = null
////                }
//            }
//        }


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

            val playing = state.state == PlaybackStateCompat.STATE_PLAYING
            textView6.text = state.position.toString()
            seekBar.progress = state.position.toInt()


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
            textView5.text = playing.toString()
            changeSeekBar()

            super.onPlaybackStateChanged(state)
        }


        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.w("TST", "onMetadataChanged")

            fillMetadata(metadata)

            super.onMetadataChanged(metadata)
        }

        override fun onSessionReady() {
            Log.w("TST", "onSessionReady")
            super.onSessionReady()
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            Log.w("TST", "onSessionEvent")
            super.onSessionEvent(event, extras)
        }

        override fun onAudioInfoChanged(info: MediaControllerCompat.PlaybackInfo?) {
            Log.w("TST", "onAudioInfoChanged")
            super.onAudioInfoChanged(info)
        }

        override fun onExtrasChanged(extras: Bundle?) {
            Log.w("TST", "onExtrasChanged")
            super.onExtrasChanged(extras)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            Log.w("TST", "onQueueChanged")
            super.onQueueChanged(queue)
        }

        override fun onQueueTitleChanged(title: CharSequence?) {
            Log.w("TST", "onQueueTitleChanged")
            super.onQueueTitleChanged(title)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            Log.w("TST", "onRepeatModeChanged")
            super.onRepeatModeChanged(repeatMode)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            Log.w("TST", "onShuffleModeChanged")
            super.onShuffleModeChanged(shuffleMode)
        }
    }

    fun fillMetadata(metadata: MediaMetadataCompat?) {

        Log.i("MTD", "METADATA_KEY_ART")

        if (metadata!!.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) != null) {
            Log.i("MTD", "METADATA_KEY_ART")
            image.setImageDrawable(
                BitmapDrawable(resources, metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
                )
            )
        }

        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE) != null) {
            Log.i("MTD", "METADATA_KEY_TITLE - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            textView2.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        }


        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) != null) {
            Log.i("MTD", "METADATA_KEY_ARTIST - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            textView3.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        }

        if (metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) != null) {
            Log.i("MTD", "METADATA_KEY_DURATION - " + metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toString())
            textView4.text = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toString()
            seekBar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
        }

        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE) != null) {
            Log.i("MTD", "METADATA_KEY_TRACK_NUMBER - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE))
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
                seekBar.progress = mediaController.playbackState.position.toInt()
                textView6.text = mediaController.playbackState.position.toString()
            } else {
                seekBar.progress = cur.toInt()
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
