package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val photosDir: File
        get() = File(context.filesDir, "family_photos").apply { if (!exists()) mkdirs() }

    private val docsDir: File
        get() = File(context.filesDir, "family_docs").apply { if (!exists()) mkdirs() }

    private val cameraTempDir: File
        get() = File(context.cacheDir, "camera_temp").apply { if (!exists()) mkdirs() }

    fun createTempCameraUri(): Pair<Uri, File> {
        val file = File(cameraTempDir, "cam_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return uri to file
    }

    /**
     * Copies and optimizes an image from a content Uri into app-specific private storage.
     * Downscales images larger than maxDimension to keep memory usage low on all devices.
     */
    suspend fun saveImageFromUri(uri: Uri, maxDimension: Int = 1440): String? = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val srcWidth = boundsOptions.outWidth
            val srcHeight = boundsOptions.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) {
                return@runCatching copyRawFile(uri, photosDir, "img_${UUID.randomUUID()}.jpg")
            }

            var inSampleSize = 1
            while ((srcWidth / inSampleSize) > maxDimension * 2 || (srcHeight / inSampleSize) > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return@runCatching null

            val rotated = applyExifRotationIfNeeded(uri, decoded)
            val scaled = scaleDownIfNeeded(rotated, maxDimension)
            if (rotated !== decoded) decoded.recycle()
            if (scaled !== rotated) rotated.recycle()

            val outFile = File(photosDir, "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 86, fos)
            }
            scaled.recycle()
            outFile.absolutePath
        }.getOrNull()
    }

    suspend fun saveDocumentFromUri(uri: Uri, prefix: String = "doc"): String? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val ext = when {
                mime.contains("pdf") -> "pdf"
                mime.contains("png") -> "png"
                mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
                else -> "bin"
            }
            if (ext == "jpg" || ext == "png") {
                return@runCatching saveImageFromUri(uri, maxDimension = 1800)
            }
            copyRawFile(uri, docsDir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext")
        }.getOrNull()
    }

    private fun copyRawFile(uri: Uri, targetDir: File, fileName: String): String? {
        val outFile = File(targetDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
            return outFile.absolutePath
        }
        return null
    }

    private fun applyExifRotationIfNeeded(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxSide.toFloat()
        val newW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    fun deleteLocalFileIfOwned(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            val canonicalFilesDir = context.filesDir.canonicalPath
            if (file.exists() && file.canonicalPath.startsWith(canonicalFilesDir)) {
                file.delete()
            }
        }
    }

    fun encodeFileToBase64(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val file = File(path)
            if (!file.exists() || file.length() > 4 * 1024 * 1024) return null
            Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
        }.getOrNull()
    }

    fun decodeBase64ToInternalFile(base64: String, isDocument: Boolean = false): String? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val dir = if (isDocument) docsDir else photosDir
            val ext = if (isDocument && bytes.size >= 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte()) "pdf" else "jpg"
            val file = File(dir, "restored_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext")
            FileOutputStream(file).use { it.write(bytes) }
            file.absolutePath
        }.getOrNull()
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun clearAllManagedMedia() {
        runCatching { photosDir.deleteRecursively() }
        runCatching { docsDir.deleteRecursively() }
    }
}
