package com.deevil.musicplayer

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.provider.DocumentsContract
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


class AudioPlayerPreparer(private val exoPlayer: ExoPlayer, private val context: Context) : MediaSessionConnector.PlaybackPreparer {

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {}

    override fun onCommand(
        player: Player?,
        controlDispatcher: ControlDispatcher?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = false

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
        if (uri!= null) {
            val lst = getAllAudioFromTree(uri)

            val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, context.getString(R.string.app_name)))
            //lst.get()

            if (lst.size > 0) {
                val concatenatedSource = ConcatenatingMediaSource()
                for ((int, i) in lst.withIndex()) {
                    concatenatedSource.addMediaSource(ExtractorMediaSource.Factory(dataSourceFactory).setTag(i).createMediaSource(i))
                }
                exoPlayer.prepare(concatenatedSource)
                //exoPlayer.playWhenReady = true
            } else {
                //Toast.makeText(this, "В выбранной директории нет аудио файлов", Toast.LENGTH_LONG).show()
            }
            //context.li.fileList()
        }

    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
    }

    override fun getSupportedPrepareActions(): Long = PlaybackStateCompat.ACTION_PREPARE_FROM_URI


    override fun onPrepare() {
    }



    private fun getAllAudioFromTree(treeUri: Uri, inpParentDocumentId: String? = null): ArrayList<Uri> {

        val parentDocumentId = inpParentDocumentId ?: DocumentsContract.getTreeDocumentId(treeUri)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        var children: Cursor? = null

        val res: ArrayList<Uri> = ArrayList<Uri>()

        try {
            children = context.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null,
                null,
                null
            )
        } catch (e: NullPointerException) {
            Log.e(TAG, "Error reading $childrenUri", e)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Error reading $childrenUri", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error reading $childrenUri", e)
        }

        if (children == null) {
            return res
        }


        while (children.moveToNext()) {
            val documentId = children.getString(children.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID))
            val mimeType = children.getString(children.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE))

            if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                for (i in getAllAudioFromTree(treeUri, documentId)) {
                    res.add(i)
                }
            } else if (mimeType != null && mimeType.startsWith("audio/")) {
                res.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId))
            }
        }

        children.close()

        return res
    }

}

private const val TAG = "MediaSessionHelper"
