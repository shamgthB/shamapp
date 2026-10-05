package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

object ImageUtil {
    private const val TAG = "ImageUtil"

    fun compressUriToJpegBytes(context: Context, uri: Uri, maxDimension: Int = 400): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            val ratio = minOf(
                maxDimension.toFloat() / originalBitmap.width,
                maxDimension.toFloat() / originalBitmap.height,
                1.0f
            )
            val targetWidth = (originalBitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetHeight = (originalBitmap.height * ratio).toInt().coerceAtLeast(1)
            val resized = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)

            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image Uri", e)
            null
        }
    }

    fun convertUriToBase64(context: Context, uri: Uri, maxDimension: Int = 240): String? {
        val byteArray = compressUriToJpegBytes(context, uri, maxDimension) ?: return null
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
