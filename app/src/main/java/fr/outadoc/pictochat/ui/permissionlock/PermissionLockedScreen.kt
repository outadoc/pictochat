package fr.outadoc.pictochat.ui.permissionlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PermissionLocked(
    multiplePermissionsState: MultiplePermissionsState,
    body: @Composable () -> Unit,
) {
    if (multiplePermissionsState.allPermissionsGranted) {
        body()
    } else {
        Scaffold { insets ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = "Permissions required",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    text = buildString {
                        appendLine(
                            "This app needs permissions to discover the devices near you and chat with them."
                        )
                        if (multiplePermissionsState.shouldShowRationale) {
                            appendLine("Some permissions were denied. The app cannot function without them.")
                        }
                    }
                )

                Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Request permissions")
                }
            }
        }
    }
}
