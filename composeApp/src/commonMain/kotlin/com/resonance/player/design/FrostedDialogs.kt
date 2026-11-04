package com.resonance.player.design

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Keep Material's native dialog actions/semantics with a window-local frosted surface. */
@Composable
fun FrostedAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = ResonanceColors.Glass.copy(alpha = .82f),
    iconContentColor: Color = MaterialTheme.colorScheme.primary,
    titleContentColor: Color = MaterialTheme.colorScheme.onSurface,
    textContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tonalElevation: Dp = 0.dp,
    properties: DialogProperties = DialogProperties(),
) {
    // Dialog owns a separate native window. Neither its surface nor its content may sample
    // the host window's Haze source. Resolve resonanceGlass inside this provider so it uses
    // its static gradient fallback, and leave the host's state untouched.
    CompositionLocalProvider(LocalGlassState provides null) {
        AlertDialog(
            onDismissRequest = onDismissRequest, confirmButton = confirmButton,
            modifier = modifier.resonanceGlass(shape = shape, backgroundColor = containerColor, shadowElevation = 6.dp),
            dismissButton = dismissButton, icon = icon, title = title, text = text,
            shape = shape, containerColor = Color.Transparent,
            iconContentColor = iconContentColor, titleContentColor = titleContentColor,
            textContentColor = textContentColor, tonalElevation = tonalElevation, properties = properties,
        )
    }
}

