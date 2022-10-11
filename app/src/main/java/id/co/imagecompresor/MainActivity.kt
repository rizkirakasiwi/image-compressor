package id.co.imagecompresor

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import id.co.imagecompresor.ui.theme.ImageCompresorTheme
import id.co.imagecompresor.ui.compressor.Compressor
import id.co.imagecompresor.ui.compressor.constraint.format
import id.co.imagecompresor.ui.compressor.constraint.size
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCompresorTheme {
                // A surface container using the 'background' color from the theme
                ImageScreen()
            }
        }
    }
}

@Composable
fun ImageScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        ImageRequest
            .Builder(LocalContext.current)
            .data(data = imageUri)
            .build()
    )
    var compressedImage by remember{ mutableStateOf<File?>(null) }
    val compressedPainter = rememberAsyncImagePainter(
        ImageRequest
            .Builder(context)
            .data(compressedImage)
            .build()
    )

    LaunchedEffect(key1 = imageUri, block = {
        if (imageUri != null) {
            val imageFile = context.createFileFromUri("imageFile",imageUri!!)
            if(imageFile != null) {
                compressedImage = Compressor.compress(context, imageFile) {
                    format(Bitmap.CompressFormat.JPEG)
                    size(3_145_728) // max size 3 mb
                }
            }
        }
    })
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            imageUri = it
        }
    )

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .then(
                            (painter.state as? AsyncImagePainter.State.Success)
                                ?.painter
                                ?.intrinsicSize
                                ?.let { intrinsicSize ->
                                    Modifier.aspectRatio(intrinsicSize.width / intrinsicSize.height)
                                } ?: Modifier.aspectRatio(1f)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Before: ${getImageSize(imageUri, context = context)} kb")
            }
            Column(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)) {
                Image(
                    painter = compressedPainter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.LightGray)
                        .then(
                            (painter.state as? AsyncImagePainter.State.Success)
                                ?.painter
                                ?.intrinsicSize
                                ?.let { intrinsicSize ->
                                    Modifier.aspectRatio(intrinsicSize.width / intrinsicSize.height)
                                } ?: Modifier.aspectRatio(1f)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "After: ${getImageSize(compressedImage)} kb")
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { scope.launch { launcher.launch("image/*") } }, modifier = Modifier
                .height(56.dp)
        ) {
            Text(text = "Pilih Foto")
        }
    }
}

private fun getImageSize(uri: Uri?, context: Context): Long {
    val cursor: Cursor? = uri?.let {
        context.contentResolver!!.query(
            it, null, null, null, null
        )
    }

    cursor?.use {
        val sizeColumn =
            it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        if (it.moveToNext()) {
            return it.getLong(sizeColumn) / 1024
        }
    }
    return 0L
}

private fun getImageSize(file:File?):Long{
    return if(file == null) 0L else {
        val sizeInBytes = file.length()
        sizeInBytes / 1024
    }
}

private fun Context.createFileFromUri(name: String, uri: Uri): File? {
    return try {
        val stream = contentResolver.openInputStream(uri)
        val file =
            File.createTempFile(
                "${name}_${System.currentTimeMillis()}",
                ".jpeg",
                cacheDir
            )
        FileUtils.copyInputStreamToFile(stream, file)  // Use this one import org.apache.commons.io.FileUtils
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

