package com.deevil.musicplayer

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.deevil.musicplayer.Samples.SAMPLES
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import android.content.IntentFilter
import android.os.Parcelable
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.NonNull




class MainActivity : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 9998
    private val DIRECTORY_REQUEST_CODE = 9999
    private val TAG = "DBG-ACT"

    private lateinit var mediaController: MediaControllerCompat
    private lateinit var serviceConnection: ServiceConnection
    private lateinit var sessionToken:MediaSessionCompat.Token
    private lateinit var callback: MediaControllerCompat.Callback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val intent = Intent(this, AudioPlayerService::class.java)
        val a = Util.startForegroundService(this, intent)
        button.setOnClickListener {

            selectDir()
        }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
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
     * When receive answer from other activity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            Log.i(TAG, "Result URI " + data.data)

            var treeUri = data.data ?: return

                mediaController.transportControls.prepareFromUri(treeUri, null)

        }

    }


    private val mMessageReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.w(C.TAG, "onReceive")
            val b = intent!!.getBundleExtra(C.KEY_SESSION_TOKEN)
            sessionToken = b.getParcelable(C.KEY_SESSION_TOKEN)
            if (sessionToken != null) {
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
            Log.w("TST", "onPlaybackStateChanged")
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
            super.onPlaybackStateChanged(state)
        }


        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.w("TST", "onMetadataChanged")
//            LogHelper.e(FragmentActivity.TAG, "onMetadataChanged")
//            if (shouldShowControls()) {
//                showPlaybackControls()
//            } else {
//                LogHelper.e(
//                    FragmentActivity.TAG,
//                    "mediaControllerCallback.onMetadataChanged: hiding controls because " + "metadata is null"
//                )
//                hidePlaybackControls()
//            }
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
}
