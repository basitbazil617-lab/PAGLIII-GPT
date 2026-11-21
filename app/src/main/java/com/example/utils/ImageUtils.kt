package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {

    fun createCameraImageUri(context: Context): Uri {
        val dir = File(context.cacheDir, "camera_images").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "paglii_photo_$stamp.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun decodeScaledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1440
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val origW = boundsOptions.outWidth
            val origH = boundsOptions.outHeight
            if (origW <= 0 || origH <= 0) return@withContext null

            var sampleSize = 1
            while (origW / sampleSize > maxDimension * 2 || origH / sampleSize > maxDimension * 2) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@withContext null

            val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = ExifInterface(exifStream)
                when (
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val rotated = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    .also { if (it != rawBitmap) rawBitmap.recycle() }
            } else {
                rawBitmap
            }

            val maxSide = maxOf(rotated.width, rotated.height)
            if (maxSide > maxDimension) {
                val scale = maxDimension.toFloat() / maxSide.toFloat()
                val newW = (rotated.width * scale).toInt().coerceAtLeast(1)
                val newH = (rotated.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rotated, newW, newH, true).also {
                    if (it != rotated) rotated.recycle()
                }
            } else {
                rotated
            }
        } catch (_: Exception) {
            null
        }
    }

    fun shareText(context: Context, title: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$text\n\n— Shared from PAGLII GPT 🤖❤️")
        }
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }
}
