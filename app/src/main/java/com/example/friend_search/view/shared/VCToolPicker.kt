package com.example.friend_search.view.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.friend_search.R
import com.example.friend_search.model.VCTool
import com.example.friend_search.viewmodel.shared.displayName

/** VC ツールの選択（行をタップするとメニューを出す）。unspecifiedLabel を渡すと「指定なし」（null）も選べる */
@Composable
fun VCToolPicker(label: String, selection: VCTool?, unspecifiedLabel: String? = null, onSelect: (VCTool?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label)
            Spacer(Modifier.weight(1f))
            Text(selection?.displayName ?: unspecifiedLabel.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(painterResource(R.drawable.unfold_more_24px), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (unspecifiedLabel != null) {
                DropdownMenuItem(text = { Text(unspecifiedLabel) }, onClick = { onSelect(null); expanded = false })
            }
            VCTool.entries.forEach { tool ->
                DropdownMenuItem(text = { Text(tool.displayName) }, onClick = { onSelect(tool); expanded = false })
            }
        }
    }
}
