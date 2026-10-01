package com.kasiguru.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kasiguru.ui.theme.*

/** A persistent label above an input; the example stays separate from its identity. */
@Composable
fun KasiGuruTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalContentColor provides if (isError) RedText else Muted,
                    content = label
                )
            }
        }
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(), enabled = enabled, readOnly = readOnly,
            textStyle = textStyle, placeholder = placeholder,
            leadingIcon = leadingIcon, trailingIcon = trailingIcon,
            supportingText = supportingText, isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            singleLine = singleLine, maxLines = maxLines, minLines = minLines,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Ink, unfocusedTextColor = Ink,
                focusedContainerColor = SurfaceSunken, unfocusedContainerColor = SurfaceSunken,
                disabledContainerColor = SurfaceSunken,
                focusedBorderColor = Lime, unfocusedBorderColor = BorderHairline,
                focusedPlaceholderColor = Faint, unfocusedPlaceholderColor = Faint,
                cursorColor = Lime
            )
        )
    }
}
