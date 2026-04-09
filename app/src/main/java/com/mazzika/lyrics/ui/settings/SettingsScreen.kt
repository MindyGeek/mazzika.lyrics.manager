package com.mazzika.lyrics.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.preferences.UserPreferences
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDeep

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val theme by viewModel.theme.collectAsState()
    val autoSaveSync by viewModel.autoSaveSync.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()

    var showDeviceNameDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            // Affichage section
            item {
                SectionLabel(title = "Affichage")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Thème",
                            color = DarkTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemePicker(
                            selectedTheme = theme,
                            onThemeSelected = { viewModel.setTheme(it) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Synchronisation section
            item {
                SectionLabel(title = "Synchronisation")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column {
                        // Auto-save toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sauvegarde automatique",
                                    color = DarkTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Sauvegarder les fichiers reçus automatiquement",
                                    color = DarkTextMuted,
                                    fontSize = 12.sp,
                                )
                            }
                            Switch(
                                checked = autoSaveSync,
                                onCheckedChange = { viewModel.setAutoSaveSync(it) },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Gold,
                                    checkedThumbColor = DarkBackground,
                                    uncheckedTrackColor = DarkSurfaceElevated,
                                    uncheckedThumbColor = DarkTextMuted,
                                ),
                            )
                        }

                        // Divider
                        androidx.compose.material3.HorizontalDivider(
                            color = DarkSurfaceElevated,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        // Device name card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { showDeviceNameDialog = true },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "Nom de l'appareil",
                                        color = DarkTextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Text(
                                        text = deviceName,
                                        color = DarkTextSecondary,
                                        fontSize = 13.sp,
                                    )
                                }
                                Text(
                                    text = "Modifier",
                                    color = Gold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // À propos section
            item {
                SectionLabel(title = "À propos")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Version",
                            color = DarkTextPrimary,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = "1.0.0",
                            color = DarkTextMuted,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }

            // Footer
            item {
                Text(
                    text = "Mazzika Lyrics",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    color = DarkTextMuted,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    if (showDeviceNameDialog) {
        DeviceNameDialog(
            currentName = deviceName,
            onDismiss = { showDeviceNameDialog = false },
            onConfirm = { newName ->
                viewModel.setDeviceName(newName)
                showDeviceNameDialog = false
            },
        )
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        color = DarkTextSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(
    selectedTheme: UserPreferences.ThemeMode,
    onThemeSelected: (UserPreferences.ThemeMode) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            UserPreferences.ThemeMode.LIGHT to "Clair",
            UserPreferences.ThemeMode.DARK to "Sombre",
            UserPreferences.ThemeMode.SYSTEM to "Auto",
        ).forEach { (mode, label) ->
            val selected = selectedTheme == mode
            FilterChip(
                selected = selected,
                onClick = { onThemeSelected(mode) },
                label = {
                    Text(
                        text = label,
                        color = if (selected) DarkBackground else DarkTextPrimary,
                        fontSize = 13.sp,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkSurfaceElevated,
                    selectedContainerColor = Gold,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = DarkSurfaceElevated,
                    selectedBorderColor = Gold,
                ),
            )
        }
    }
}

@Composable
private fun DeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = { Text("Nom de l'appareil", color = DarkTextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold,
                    unfocusedBorderColor = DarkTextMuted,
                    focusedLabelColor = Gold,
                    unfocusedLabelColor = DarkTextMuted,
                    focusedTextColor = DarkTextPrimary,
                    unfocusedTextColor = DarkTextPrimary,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Enregistrer", color = Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = DarkTextSecondary)
            }
        },
    )
}
