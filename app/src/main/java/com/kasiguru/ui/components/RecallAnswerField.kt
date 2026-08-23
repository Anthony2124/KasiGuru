package com.kasiguru.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import com.kasiguru.ui.theme.Faint
import com.kasiguru.ui.theme.Space

/**
 * The typed-recall answer field, shared by Lesson Player and the Word Recall mini-game — the two
 * places in the app that ask the learner to produce a word rather than choose one.
 *
 * Autocorrect and capitalisation are off: a keyboard trained on English or Tagalog will happily
 * rewrite a correctly recalled Kasiguranin word into a word it recognises, marking the learner wrong
 * for something they did not type, and suggestions would hand them the answer outright.
 */
@Composable
fun RecallAnswerField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Type the word") },
            textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(
                autoCorrect = false,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (value.isNotBlank()) onSubmit()
                }
            )
        )

        Spacer(Modifier.height(Space.xs))

        // Said before the answer, not after it. A fifth of the corpus is spelled with ë, no standard
        // phone keyboard produces one, and a learner who believes they cannot type the word will not
        // try. The matcher accepts a plain e, so the only thing missing was saying so.
        Text(
            text = "Type e where the word has ë — accents are optional too.",
            style = MaterialTheme.typography.bodySmall,
            color = Faint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
