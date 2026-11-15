package com.rushgames.barrier.ui.screens.blocklists

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rushgames.barrier.data.model.BlocklistCategory
import com.rushgames.barrier.ui.theme.BarrierGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBlocklistScreen(
    onBack: () -> Unit,
    viewModel: BlocklistsViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(BlocklistCategory.CUSTOM) }
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("New Blocklist", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.background) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                    Button(
                        onClick = {
                            isSaving = true
                            // ViewModel creates it then back
                            onBack()
                        },
                        enabled = name.isNotBlank() && !isSaving,
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(containerColor = BarrierGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Create Blocklist", fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Social Media") }
            )
            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BlocklistCategory.entries.forEach { category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category.name.replace("_", " "), style = MaterialTheme.typography.bodyMedium)
                    RadioButton(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        colors = RadioButtonDefaults.colors(selectedColor = BarrierGreen)
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }
        }
    }
}
