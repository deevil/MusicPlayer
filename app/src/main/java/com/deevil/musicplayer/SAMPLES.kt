package com.deevil.musicplayer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.DrawableRes


object Samples {

    val SAMPLES = arrayOf(
        Sample(
            "https://storage.googleapis.com/automotive-media/Jazz_In_Paris.mp3",
            "audio_1",
            "Jazz in Paris",
            "Jazz for the masses",
            R.drawable.album_art_1
        ), Sample(
            "https://storage.googleapis.com/automotive-media/The_Messenger.mp3",
            "audio_2",
            "The messenger",
            "Hipster guide to London",
            R.drawable.album_art_2
        ), Sample(
            "https://storage.googleapis.com/automotive-media/Talkies.mp3",
            "audio_3",
            "Talkies",
            "If it talks like a duck and walks like a duck.",
            R.drawable.album_art_3
        )
    )

    class Sample(
        uri: String, val mediaId: String, val title: String, val description: String, val bitmapResource: Int
    ) {
        val uri: Uri

        init {
            this.uri = Uri.parse(uri)
        }

        override fun toString(): String {
            return title
        }
    }

    fun getMediaDescription(context: Context, sample: Sample): MediaDescriptionCompat {
        val extras = Bundle()
        val bitmap = getBitmap(context, sample.bitmapResource)
        extras.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
        extras.putParcelable(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
        return MediaDescriptionCompat.Builder()
            .setMediaId(sample.mediaId)
            .setIconBitmap(bitmap)
            .setTitle(sample.title)
            .setDescription(sample.description)
            .setExtras(extras)
            .build()
    }

    fun getBitmap(context: Context, @DrawableRes bitmapResource: Int): Bitmap {
        return (context.resources.getDrawable(bitmapResource) as BitmapDrawable).bitmap
    }

}
