package com.mazzika.lyrics.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.preferences.UserPreferences
import com.mazzika.lyrics.ui.components.GroupLabel
import com.mazzika.lyrics.ui.components.SettingsCard
import com.mazzika.lyrics.ui.components.SettingsRow
import com.mazzika.lyrics.ui.components.SettingsValueText
import com.mazzika.lyrics.ui.components.StudioToggle
import com.mazzika.lyrics.ui.components.ThemeChoice
import com.mazzika.lyrics.ui.components.ThemePicker
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val tokens = LocalStudioTokens.current
    val theme by viewModel.theme.collectAsState()
    val autoSaveSync by viewModel.autoSaveSync.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()

    var autoAcceptSessions by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(true) }
    var showDeviceNameDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // ── Affichage
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp)) {
                    GroupLabel(text = "Affichage")
                    SettingsCard {
                        SettingsRow(
                            title = "Thème",
                            description = "Apparence de l'application",
                            isLast = true,
                            trailing = {
                                ThemePicker(
                                    selected = when (theme) {
                                        UserPreferences.ThemeMode.LIGHT -> ThemeChoice.LIGHT
                                        UserPreferences.ThemeMode.DARK -> ThemeChoice.DARK
                                        UserPreferences.ThemeMode.SYSTEM -> ThemeChoice.SYSTEM
                                    },
                                    onSelect = {
                                        viewModel.setTheme(
                                            when (it) {
                                                ThemeChoice.LIGHT -> UserPreferences.ThemeMode.LIGHT
                                                ThemeChoice.DARK -> UserPreferences.ThemeMode.DARK
                                                ThemeChoice.SYSTEM -> UserPreferences.ThemeMode.SYSTEM
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    }
                }
            }

            // ── Synchronisation
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GroupLabel(text = "Synchronisation")
                    SettingsCard {
                        SettingsRow(
                            title = "Sauvegarde automatique",
                            description = "Fichiers reçus ajoutés au catalogue",
                            trailing = {
                                StudioToggle(
                                    checked = autoSaveSync,
                                    onCheckedChange = { viewModel.setAutoSaveSync(it) },
                                )
                            },
                        )
                        SettingsRow(
                            title = "Nom de l'appareil",
                            description = "Affiché lors de la découverte",
                            onClick = { showDeviceNameDialog = true },
                            trailing = { SettingsValueText("$deviceName ›") },
                        )
                        SettingsRow(
                            title = "Accepter automatiquement",
                            description = "Rejoindre les sessions du même groupe",
                            isLast = true,
                            trailing = {
                                StudioToggle(
                                    checked = autoAcceptSessions,
                                    onCheckedChange = { autoAcceptSessions = it },
                                )
                            },
                        )
                    }
                }
            }

            // ── Lecteur
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GroupLabel(text = "Lecteur")
                    SettingsCard {
                        SettingsRow(
                            title = "Maintenir écran allumé",
                            description = "Pendant la lecture d'une partition",
                            isLast = true,
                            trailing = {
                                StudioToggle(
                                    checked = keepScreenOn,
                                    onCheckedChange = { keepScreenOn = it },
                                )
                            },
                        )
                    }
                }
            }

            // ── À propos
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GroupLabel(text = "À propos")
                    SettingsCard {
                        SettingsRow(
                            title = "Version",
                            trailing = { SettingsValueText(text = "1.0.0", isGold = false) },
                        )
                        SettingsRow(
                            title = "Conditions d'utilisation",
                            isLast = true,
                            trailing = { SettingsValueText("›") },
                            onClick = {},
                        )
                    }
                }
            }

            // ── Footer
            item {
                Spacer(Modifier.height(40.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "MAZZIKA LYRICS",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = tokens.textDim,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Fait avec ♥ pour les musiciens",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = tokens.textMid,
                        textAlign = TextAlign.Center,
                    )
                }
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
private fun DeviceNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = tokens.surface,
        title = {
            Text(
                "Nom de l'appareil",
                fontFamily = Inter,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = tokens.text,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom", fontFamily = Inter) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tokens.gold,
                    unfocusedBorderColor = tokens.border,
                    focusedLabelColor = tokens.gold,
                    unfocusedLabelColor = tokens.textMid,
                    focusedTextColor = tokens.text,
                    unfocusedTextColor = tokens.text,
                    cursorColor = tokens.gold,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) {
                Text("Enregistrer", color = tokens.gold, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
