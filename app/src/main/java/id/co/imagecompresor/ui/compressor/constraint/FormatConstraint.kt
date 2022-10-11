package id.co.imagecompresor.ui.compressor.constraint

import android.graphics.Bitmap
import id.co.imagecompresor.ui.compressor.compressFormat
import id.co.imagecompresor.ui.compressor.loadBitmap
import id.co.imagecompresor.ui.compressor.overWrite
import java.io.File

class FormatConstraint(private val format: Bitmap.CompressFormat) : Constraint {

    override fun isSatisfied(imageFile: File): Boolean {
        return format == imageFile.compressFormat()
    }

    override fun satisfy(imageFile: File): File {
        return overWrite(imageFile, loadBitmap(imageFile), format)
    }
}

fun Compression.format(format: Bitmap.CompressFormat) {
    constraint(FormatConstraint(format))
}