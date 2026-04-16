package com.mazzika.lyrics.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.components.PrimaryGoldButton
import com.mazzika.lyrics.ui.components.paletteFor
import com.mazzika.lyrics.ui.theme.Inter
import com.mazzika.lyrics.ui.theme.LocalStudioTokens

/**
 * Standalone "Create Session" dialog that can be invoked from anywhere in the app.
 *
 * Supports two modes:
 *  - **Full mode** (3 steps): Session name → Document picker → Confirm.
 *    Triggered when [preSelectedDocument] is `null`.
 *  - **Quick mode** (2 steps): Session name → Confirm.
 *    Triggered when [preSelectedDocument] is provided — the document picker step is
 *    skipped and the document is auto-selected.
 *
 * @param sessionName current session name (usually pre-filled by ViewModel).
 * @param onSessionNameChange callback when the user edits the session name.
 * @param allDocuments full catalogue, shown in the picker step (ignored in quick mode).
 * @param preSelectedDocument if non-null, the document is auto-selected and the picker
 *        step is skipped entirely.
 * @param onDismiss called when the user cancels.
 * @param onStart called with the final [PdfDocumentEntity] when the user confirms.
 */
@Composable
fun CreateSessionDialog(
    sessionName: String,
    onSessionNameChange: (String) -> Unit,
    allDocuments: List<PdfDocumentEntity>,
    preSelectedDocument: PdfDocumentEntity? = null,
    onDismiss: () -> Unit,
    onStart: (PdfDocumentEntity) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    val isQuickMode = preSelectedDocument != null
    val totalSteps = if (isQuickMode) 2 else 3

    var currentStep by remember { mutableIntStateOf(0) }
    var selectedDoc by remember { mutableStateOf(preSelectedDocument) }

    // Map logical step → content step.
    // Quick mode: 0=Name, 1=Confirm.
    // Full mode:  0=Name, 1=Picker, 2=Confirm.
    val isNameStep = currentStep == 0
    val isPickerStep = !isQuickMode && currentStep == 1
    val isConfirmStep = (isQuickMode && currentStep == 1) || (!isQuickMode && currentStep == 2)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(vertical = 24.dp),
        containerColor = tokens.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Créer une session",
                    color = tokens.text,
                    fontFamily = Inter,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.height(10.dp))
                StepIndicator(currentStep = currentStep, totalSteps = totalSteps)
            }
        },
        text = {
            when {
                isNameStep -> SessionNameStep(
                    sessionName = sessionName,
                    onSessionNameChange = onSessionNameChange,
                )
                isPickerStep -> DocumentPickerStep(
                    documents = allDocuments,
                    selectedDoc = selectedDoc,
                    onSelect = { selectedDoc = it },
                )
                isConfirmStep -> ConfirmStep(
                    sessionName = sessionName,
                    selectedDoc = selectedDoc,
                )
            }
        },
        confirmButton = {
            val canNext = when {
                isNameStep -> sessionName.isNotBlank()
                isPickerStep -> selectedDoc != null
                isConfirmStep -> selectedDoc != null
                else -> false
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                PrimaryGoldButton(
                    text = if (isConfirmStep) "Démarrer la session" else "Suivant",
                    onClick = {
                        if (isConfirmStep) selectedDoc?.let { onStart(it) }
                        else currentStep++
                    },
                    enabled = canNext,
                    leadingContent = if (isConfirmStep) {
                        { Icon(Icons.Filled.CellTower, null, Modifier.size(16.dp)) }
                    } else null,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStep > 0) {
                        TextButton(onClick = { currentStep-- }) {
                            Text("Précédent", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = tokens.textMid, fontFamily = Inter, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = null,
    )
}

// ═════════════════════════════════════════════════════════════════
// INTERNAL SUB-STEPS
// ═════════════════════════════════════════════════════════════════

@Composable
internal fun StepIndicator(currentStep: Int, totalSteps: Int) {
    val tokens = LocalStudioTokens.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { i ->
            Box(
                modifier = Modifier
                    .size(width = if (i == currentStep) 22.dp else 6.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (i <= currentStep) Brush.linearGradient(listOf(tokens.goldLight, tokens.gold))
                        else Brush.linearGradient(listOf(tokens.pillBg, tokens.pillBg)),
                    ),
            )
        }
    }
}

@Composable
private fun SessionNameStep(
    sessionName: String,
    onSessionNameChange: (String) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    Column {
        Text(
            "Nom de la session",
            color = tokens.textMid,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sessionName,
            onValueChange = onSessionNameChange,
            label = { Text("Ex. Répétition Hadhra", fontFamily = Inter) },
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
    }
}

@Composable
internal fun DocumentPickerStep(
    documents: List<PdfDocumentEntity>,
    selectedDoc: PdfDocumentEntity?,
    onSelect: (PdfDocumentEntity) -> Unit,
) {
    val tokens = LocalStudioTokens.current
    Column {
        Text(
            "Choisissez une partition",
            color = tokens.textMid,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(10.dp))
        if (documents.isEmpty()) {
            Text(
                "Aucun document disponible.\nImportez un PDF depuis le Catalogue.",
                color = tokens.textDim,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(documents) { doc ->
                    val isSelected = selectedDoc?.id == doc.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) tokens.gold.copy(alpha = 0.14f)
                                else if (tokens.isDark) tokens.surfaceHi else tokens.bg,
                            )
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) tokens.gold else Color.Transparent,
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelect(doc) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val palette = paletteFor(doc.id)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(palette.a, palette.b))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = doc.title.firstOrNull()?.uppercase() ?: "?",
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title,
                                color = tokens.text,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${doc.pageCount} page${if (doc.pageCount > 1) "s" else ""}",
                                color = tokens.textMid,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmStep(sessionName: String, selectedDoc: PdfDocumentEntity?) {
    val tokens = LocalStudioTokens.current
    Column {
        ConfirmRow("Session", sessionName)
        Spacer(Modifier.height(10.dp))
        ConfirmRow("Partition", selectedDoc?.title ?: "—")
        Spacer(Modifier.height(10.dp))
        ConfirmRow("Pages", selectedDoc?.pageCount?.toString() ?: "—")
        Spacer(Modifier.height(16.dp))
        Text(
            "Votre appareil deviendra pilote de la session. Les autres musiciens pourront se connecter à proximité.",
            color = tokens.textMid,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    val tokens = LocalStudioTokens.current
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label.uppercase(),
            color = tokens.textDim,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            modifier = Modifier.width(90.dp),
        )
        Text(
            text = value,
            color = tokens.text,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}
