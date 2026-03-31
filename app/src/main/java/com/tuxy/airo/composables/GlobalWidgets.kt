package com.tuxy.airo.composables

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata_rework.FlightData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Used in Airport, Aircraft, Ticket and Flight information screens
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SmallAppBar(text: String, paneNavigator: ThreePaneScaffoldNavigator<String>) {
    val scope = rememberCoroutineScope()

    TopAppBar(
        title = { Text(text) },
        navigationIcon = {
            if (paneNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                IconButton(onClick = {
                    scope.launch { paneNavigator.navigateBack() }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallAppBarLegacy(text: String, navController: NavController) {
    TopAppBar(
        title = { Text(text) },
        navigationIcon = {
            IconButton(onClick = {
                navController.navigateUp()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}

// Used in main flight screen and settings
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun LargeAppBar(
    text: String,
    paneNavigator: ThreePaneScaffoldNavigator<String>? = null
) {
    val scope = rememberCoroutineScope()

    LargeTopAppBar(
        title = { Text(text) },
        navigationIcon = {
            if (paneNavigator != null && paneNavigator.scaffoldDirective.maxHorizontalPartitions == 1) {
                IconButton(onClick = {
                    scope.launch { paneNavigator.navigateBack() }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            }
        }
    )
}

// Used in Flight, Ticket and main screen
@Composable
fun RouteBar(flightData: FlightData) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoldDepartureAndDestinationText(
            icao = flightData.from,
            countryCode = flightData.fromCountryCode,
            fullName = flightData.fromName,
            time = "",
            alignment = Alignment.Start
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = stringResource(R.string.to)
        )
        BoldDepartureAndDestinationText(
            icao = flightData.to,
            countryCode = flightData.toCountryCode,
            fullName = flightData.toName,
            time = "",
            alignment = Alignment.End
        )
    }
}

// Used in main flight and ticket screen
@Composable
fun LargeTopSmallBottom(top: String, bottom: String) {
    Column {
        Text(
            top,
            fontSize = 12.sp,
            lineHeight = 2.sp
        )
        Text(
            bottom,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DepartureAndDestinationText(icao: String, fullName: String) {
    Column {
        Text(
            fullName,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        )
        Text(icao)
    }
}

@Composable
fun BoldDepartureAndDestinationText(
    icao: String,
    countryCode: String,
    fullName: String,
    time: String,
    alignment: Alignment.Horizontal,
) {
    Column(
        modifier = Modifier.defaultMinSize(minWidth = 64.dp),
        horizontalAlignment = alignment
    ) {
        if (alignment == Alignment.Start) {
            Text(
                "${countryCodeToEmoji(countryCode)}  ${fullName.wrap()}",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
            )
        } else {
            Text(
                "${fullName.wrap()}  ${countryCodeToEmoji(countryCode)}",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        if (alignment == Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    icao,
                    fontWeight = FontWeight.W500,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    time,
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    time,
                    fontWeight = FontWeight.W400,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    icao,
                    fontWeight = FontWeight.W500,
                    fontSize = 24.sp,
                )
            }
        }
    }
}

fun String.wrap(): String {
    if (this.length > 15) {
        return "${this.substring(0, 14)}..."
    } else {
        return this
    }
}

@Composable
@Preview(showBackground = true)
fun TextPreview() {
    BoldDepartureAndDestinationText(
        icao = "FCO",
        countryCode = "IT",
        fullName = "Leonardo Da Vinci-Fiumicino",
        time = "5:00",
        alignment = Alignment.Start
    )
}

fun countryCodeToEmoji(countryCode: String): String {
    if (countryCode == "") {
        return countryCode
    }
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}

@SuppressLint("LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScannerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onResult: (String?) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val barCodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onResult(result.contents)
            scope.launch { onDismiss() }
        }
    }

    val cameraPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.CAMERA,
        onPermissionResult = { granted ->
            if (granted) {
                launchBarcodeScanner(barCodeLauncher)
            } else {
                Toast.makeText(context, context.resources.getString(R.string.allow_camera_toast), Toast.LENGTH_LONG).show()
            }
        }
    )

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    decodeBarcodeFromUri(context, uri)
                }
                onResult(result)
                if (result != null) {
                    onDismiss()
                } else {
                    Toast.makeText(
                        context,
                        context.resources.getString(R.string.invalid_pass),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            onDismiss()
        },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.add_ticket),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = {
                    if (cameraPermissionState.status.isGranted) {
                        launchBarcodeScanner(barCodeLauncher)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.scan_barcode))
            }

            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 16.dp)
                    .fillMaxWidth(0.5f)
            )

            Button(
                onClick = {
                    filePickerLauncher.launch(arrayOf("image/*", "application/pdf"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.select_file))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun launchBarcodeScanner(
    barCodeLauncher: androidx.activity.result.ActivityResultLauncher<ScanOptions>
) {
    val options = ScanOptions()
    options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
    options.setCameraId(0)
    options.setBeepEnabled(false)
    options.setOrientationLocked(false)
    barCodeLauncher.launch(options)
}

private fun decodeBarcodeFromUri(context: Context, uri: android.net.Uri): String? {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)

    val bitmap: Bitmap? = if (mimeType == "application/pdf") {
        renderPdfPageToBitmap(context, uri)
    } else {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }

    if (bitmap == null) return null

    return decodeBarcodeFromBitmap(bitmap)
}

private fun renderPdfPageToBitmap(context: Context, uri: android.net.Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        val fileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
        val pdfRenderer = PdfRenderer(fileDescriptor)
        if (pdfRenderer.pageCount == 0) {
            pdfRenderer.close()
            fileDescriptor.close()
            return null
        }
        val page = pdfRenderer.openPage(0)
        val bitmap = Bitmap.createBitmap(
            page.width * 2,
            page.height * 2,
            Bitmap.Config.ARGB_8888
        )
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        pdfRenderer.close()
        fileDescriptor.close()
        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun decodeBarcodeFromBitmap(bitmap: Bitmap): String? {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    val reader = MultiFormatReader()
    return try {
        val result = reader.decode(binaryBitmap)
        result.text
    } catch (e: Exception) {
        null
    } finally {
        reader.reset()
    }
}
