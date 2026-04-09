package com.mazzika.lyrics.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.preferences.UserPreferences
import com.mazzika.lyrics.ui.theme.CormorantGaramond
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkSurface
import com.mazzika.lyrics.ui.theme.DarkSurfaceElevated
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.DarkTextSecondary
import com.mazzika.lyrics.ui.theme.Gold

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
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
        ) {

            // Affichage section
            item {
                SectionLabel(title = "AFFICHAGE")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Th\u00E8me",
                                color = DarkTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Apparence de l'application",
                                color = DarkTextMuted,
                                fontSize = 11.sp,
                            )
                        }
                        ThemePills(
                            selectedTheme = theme,
                            onThemeSelected = { viewModel.setTheme(it) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // Synchronisation section
            item {
                SectionLabel(title = "SYNCHRONISATION")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        // Auto-save toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sauvegarde auto",
                                    color = DarkTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Fichiers re\u00E7us ajout\u00E9s au catalogue",
                                    color = DarkTextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            CustomToggle(
                                checked = autoSaveSync,
                                onCheckedChange = { viewModel.setAutoSaveSync(it) },
                            )
                        }

                        // Separator
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.03f),
                            thickness = 1.dp,
                        )

                        // Device name row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeviceNameDialog = true }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = "Nom de l'appareil",
                                    color = DarkTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Affich\u00E9 lors de la d\u00E9couverte",
                                    color = DarkTextMuted,
                                    fontSize = 11.sp,
                                )
                            }
                            Text(
                                text = "$deviceName \u203A",
                                color = Gold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }

            // A propos section
            item {
                SectionLabel(title = "\u00C0 PROPOS")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Version",
                            color = DarkTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "1.0.0",
                            color = DarkTextMuted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }

            // Footer
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Mazzika Lyrics",
                        fontFamily = CormorantGaramond,
                        color = DarkTextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Fait avec \u2665 pour les musiciens",
                        color = DarkTextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(Gold),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = DarkTextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.1.sp,
        )
    }
}

@Composable
private fun ThemePills(
    selectedTheme: UserPreferences.ThemeMode,
    onThemeSelected: (UserPreferences.ThemeMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        listOf(
            UserPreferences.ThemeMode.LIGHT to "Clair",
            UserPreferences.ThemeMode.DARK to "Sombre",
            UserPreferences.ThemeMode.SYSTEM to "Auto",
        ).forEach { (mode, label) ->
            val selected = selectedTheme == mode
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (selected) Modifier.background(Gold) else Modifier
                    )
                    .clickable { onThemeSelected(mode) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    color = if (selected) Color(0xFF0A0800) else DarkTextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun CustomToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) Gold else DarkSurfaceElevated)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .padding(3.dp)
                .size(18.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White),
        )
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
