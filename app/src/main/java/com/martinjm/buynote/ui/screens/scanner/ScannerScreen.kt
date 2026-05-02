package com.martinjm.buynote.ui.screens.scanner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController

private enum class CameraPermissionState {
    Checking,
    Granted,
    Rationale,
    PermanentlyDenied,
}

private fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No Activity found in context chain")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(navController: NavHostController) {
    val context = LocalContext.current
    var permissionState by remember { mutableStateOf(CameraPermissionState.Checking) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionState = when {
            isGranted -> CameraPermissionState.Granted
            ActivityCompat.shouldShowRequestPermissionRationale(
                context.findActivity(), Manifest.permission.CAMERA
            ) -> CameraPermissionState.Rationale
            else -> CameraPermissionState.PermanentlyDenied
        }
    }

    LaunchedEffect(Unit) {
        permissionState = when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> CameraPermissionState.Granted

            ActivityCompat.shouldShowRequestPermissionRationale(
                context.findActivity(), Manifest.permission.CAMERA
            ) -> CameraPermissionState.Rationale

            else -> {
                launcher.launch(Manifest.permission.CAMERA)
                CameraPermissionState.Checking
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Escáner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (permissionState) {
                CameraPermissionState.Checking -> CircularProgressIndicator()
                CameraPermissionState.Granted -> CameraReady()
                CameraPermissionState.Rationale -> CameraPermissionRationale(
                    onRequest = { launcher.launch(Manifest.permission.CAMERA) }
                )
                CameraPermissionState.PermanentlyDenied -> CameraPermissionDenied(
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraReady() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Cámara lista",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "El escáner se implementa en la siguiente historia",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Permiso de cámara requerido",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "BuyNote necesita acceso a la cámara para escanear códigos de barras de productos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("Conceder permiso")
        }
    }
}

@Composable
private fun CameraPermissionDenied(onOpenSettings: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Permiso denegado",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "El permiso de cámara fue denegado permanentemente. Para usar el escáner, habilitalo desde los ajustes del sistema.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text("Ir a Ajustes")
        }
    }
}
