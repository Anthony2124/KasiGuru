package com.kasiguru.ui.components.clay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kasiguru.data.local.entity.VocabularyEntity
import com.kasiguru.ui.theme.Iconsax
import com.kasiguru.ui.theme.Ink
import com.kasiguru.ui.theme.Muted
import com.kasiguru.ui.theme.Shapes
import com.kasiguru.ui.theme.Space
import com.kasiguru.ui.theme.Surface
import com.kasiguru.ui.theme.SurfaceSunken
import com.kasiguru.ui.theme.Violet

/**
 * A dictionary search bar that sits on top of a screen's content rather than pushing it - the
 * caller places this last inside a [androidx.compose.foundation.layout.Box] over the normal
 * scrolling content so it draws as an overlay.
 *
 * Distinct from the in-place filters VocabularyScreen and CategoryDetailScreen already had: this
 * queries [com.kasiguru.data.local.dao.VocabularyDao.searchVocabulary] directly, so it finds a
 * word in any category rather than only filtering an already-loaded list.
 *
 * [results] is expected to already be debounced by the caller's ViewModel - this composable does
 * not debounce itself, so it can be reused against any already-debounced result flow.
 */
@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<VocabularyEntity>,
    onResultClick: (VocabularyEntity) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search the dictionary…"
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(painter = painterResource(id = Iconsax.Search), contentDescription = null, tint = Violet)
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            painter = painterResource(id = Iconsax.CloseCircle),
                            contentDescription = "Clear search",
                            tint = Muted
                        )
                    }
                }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 6.dp, shape = Shapes.tile),
            shape = Shapes.tile,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                focusedBorderColor = Violet,
                unfocusedBorderColor = SurfaceSunken
            ),
            singleLine = true
        )

        if (query.isNotBlank()) {
            Spacer(Modifier.height(Space.xxs))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = Shapes.panel)
                    .clip(Shapes.panel)
                    .background(Surface)
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (results.isEmpty()) {
                    Text(
                        text = "No words match \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier.padding(Space.md)
                    )
                } else {
                    results.forEach { word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onResultClick(word) }
                                .padding(horizontal = Space.md, vertical = Space.sm),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = word.kasiguranin,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Ink
                                )
                                Text(
                                    text = listOf(word.tagalog, word.english)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
