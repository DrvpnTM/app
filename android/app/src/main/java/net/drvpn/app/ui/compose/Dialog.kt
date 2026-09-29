package net.drvpn.app.ui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.font.FontWeight
import net.drvpn.app.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.RectangleShape

@Composable
fun ConfirmDialog(
    title: String? = null,
    message: String,
    confirmText: String = stringResource(R.string.action_ok),
    dismissText: String? = stringResource(R.string.action_cancel),
    confirmIcon: @Composable (() -> Unit)? = null,
    messageTextAlign: TextAlign = TextAlign.Start,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val confirmFocusRequester = remember { FocusRequester() }
    val dismissFocusRequester = remember { FocusRequester() }

    LaunchedEffect(dismissText) {
        if (dismissText != null) dismissFocusRequester.requestFocus()
        else confirmFocusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it) } },
        text = {
            Text(
                text = message,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = messageTextAlign
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                modifier = Modifier.focusRequester(confirmFocusRequester)
            ) {
                confirmIcon?.invoke()
                if (confirmIcon != null) Spacer(Modifier.width(8.dp))
                Text(confirmText)
            }
        },
        dismissButton = dismissText?.let { text ->
            {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.focusRequester(dismissFocusRequester)
                ) {
                    Text(text)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.cell
    )
}

@Composable
fun DeleteConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemName: String? = null,
) {
    ConfirmDialog(
        message = if (itemName.isNullOrBlank()) message else "$message\n\n$itemName",
        messageTextAlign = TextAlign.Center,
        confirmText = stringResource(R.string.action_delete),
        confirmIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_delete_24dp),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

data class InputField(
    val label: String,
    val value: String,
    val singleLine: Boolean = true,
    val visualTransformation: VisualTransformation = VisualTransformation.None
)

@Composable
fun InputDialog(
    title: String,
    fields: List<InputField>,
    onFieldChange: (Int, String) -> Unit,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fields.forEachIndexed { index, field ->
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { onFieldChange(index, it) },
                        label = { Text(field.label) },
                        singleLine = false,
                        maxLines = 5,
                        visualTransformation = field.visualTransformation,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            selectionColors = TextSelectionColors(
                                handleColor = MaterialTheme.colorScheme.secondary,
                                backgroundColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
        containerColor = MaterialTheme.colorScheme.cell
    )
}

@Composable
fun QRCodeDialog(
    bitmap: Bitmap?,
    onDismiss: () -> Unit,
    title: String? = null,
    hint: String? = null
) {
    if (bitmap == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        text = {
            // Keep the code small enough for landscape screens (TVs) so the hint stays visible.
            val maxQr = (LocalConfiguration.current.screenHeightDp * 0.45f).coerceIn(160f, 320f).dp
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.acc_qr_code),
                    modifier = Modifier
                        .widthIn(max = maxQr)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color.White)
                        .padding(8.dp)
                )
                if (hint != null) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
        containerColor = MaterialTheme.colorScheme.cell
    )
}

/**
 * When showRadio is true, displays RadioButton (single selection mode);
 * otherwise, plain clickable list mode.
 * The selectedOption parameter is used to highlight the selected item only when showRadio is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectListDialog(
    options: List<T>,
    optionText: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    selectedOption: T? = null,
    showRadio: Boolean = false
) {
    // iOS action sheet: options in a rounded card, a separate Cancel button underneath.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
            SettingsGroup {
                options.forEach { option ->
                    val isSelected = showRadio && option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusHighlight(RectangleShape)
                            .then(
                                if (showRadio) Modifier.selectable(
                                    selected = isSelected,
                                    onClick = { onSelected(option) },
                                    role = Role.RadioButton
                                ) else Modifier.clickable { onSelected(option) }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = optionText(option),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (showRadio) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                            textAlign = if (showRadio) TextAlign.Start else TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_done),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SettingsGroup {
                Text(
                    text = stringResource(R.string.action_cancel),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusHighlight(RectangleShape)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp)
                )
            }
        }
    }
}
