package com.example.platform.ui.appwidgets.glance.layout.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Compose UI for displaying a form for selecting a data source. */
// TODO: Align to UX design once available.
@Composable
fun DataSourceSelectionUI(
  dataSources: List<String>,
  onConfirm: (selectedDataSource: String) -> Unit,
) {
  val (selectedDataSource, onDataSourceSelected) = remember { mutableStateOf("") }

  Row(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
        .wrapContentSize()
    ) {
      // Note that Modifier.selectableGroup() is essential to ensure correct accessibility
      // behavior
      Column(Modifier.selectableGroup()) {
        dataSources.forEach { source ->
          Row(
            Modifier
              .background(
                MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
              )
              .fillMaxWidth()
              .height(56.dp)
              .selectable(
                selected = (source == selectedDataSource),
                onClick = { onDataSourceSelected(source) },
                role = Role.RadioButton
              )
              .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = (source == selectedDataSource),
              onClick = null // null recommended for accessibility with screen-readers
            )
            Text(
              text = source
                .replaceFirstChar { it.uppercase() }
                .replace("-", " "),
              style = MaterialTheme.typography.bodyLarge,
              modifier = Modifier.padding(start = 16.dp)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
        }
      }
      Spacer(Modifier.size(18.dp))
      Button(
        modifier = Modifier.padding(horizontal = 16.dp),
        enabled = selectedDataSource != "",
        onClick = { onConfirm(selectedDataSource) }) {
        Text("Confirm")
      }
    }
  }
}