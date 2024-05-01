package fr.outadoc.pictochat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionLocked(
    multiplePermissionsState: MultiplePermissionsState,
    body: @Composable () -> Unit,
) {
    if (multiplePermissionsState.allPermissionsGranted) {
        body()
    } else {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                getTextToShowGivenPermissions(
                    permissions = multiplePermissionsState.revokedPermissions,
                    shouldShowRationale = multiplePermissionsState.shouldShowRationale
                )
            )

            Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                Text("Request permissions")
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getTextToShowGivenPermissions(
    permissions: List<PermissionState>,
    shouldShowRationale: Boolean,
): String {
    val revokedPermissionsSize = permissions.size
    if (revokedPermissionsSize == 0) return ""

    return buildString {
        append("The ")

        permissions.indices.forEach { i ->
            append(permissions[i].permission)

            when {
                revokedPermissionsSize > 1 && i == revokedPermissionsSize - 2 -> {
                    append(", and ")
                }

                i == revokedPermissionsSize - 1 -> {
                    append(" ")
                }

                else -> {
                    append(", ")
                }
            }
        }

        append(if (revokedPermissionsSize == 1) "permission is" else "permissions are")
        append(
            if (shouldShowRationale) {
                " important. Please grant all of them for the app to function properly."
            } else {
                " denied. The app cannot function without them."
            }
        )
    }
}
