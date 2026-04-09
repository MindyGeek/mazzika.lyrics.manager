package com.mazzika.lyrics.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkTextPrimary
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.PlayfairDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MazzikaTopBar(
    title: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    folderName: String? = null,
    folderIcon: String? = null,
) {
    TopAppBar(
        title = {
            when {
                folderName != null -> {
                    val prefix = if (folderIcon != null) "$folderIcon " else ""
                    Text(
                        text = "$prefix$folderName",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = DarkTextPrimary,
                    )
                }
                title == "Mazzika" -> {
                    Text(
                        text = title,
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Gold,
                    )
                }
                else -> {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = DarkTextPrimary,
                    )
                }
            }
        },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Gold,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground,
        ),
    )
}
