# Mazzika Lyrics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app for Mazzika Band to import, organize, read PDF lyrics/tablatures with synchronized page-turning across devices via Nearby Connections.

**Architecture:** Single-module MVVM with Jetpack Compose (Material 3). Data layer uses Room for catalog/folders, DataStore for preferences, and PdfRenderer for rendering. Nearby Connections API handles P2P sync. Navigation via Compose Navigation with a bottom nav bar.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, DataStore Preferences, Google Nearby Connections, PdfRenderer, Coroutines + Flow

**Spec:** `docs/superpowers/specs/2026-04-08-mazzika-lyrics-design.md`

**Mockups:** `.superpowers/brainstorm/mockups/index.html`

---

## File Structure

```
app/src/main/java/com/mazzika/lyrics/
├── MainActivity.kt                          (modify — single activity, hosts NavGraph)
├── MazzikaApplication.kt                    (create — Application class, init database)
├── data/
│   ├── db/
│   │   ├── MazzikaDatabase.kt               (create — Room database, version 1)
│   │   ├── entity/
│   │   │   ├── PdfDocumentEntity.kt          (create — @Entity for PDF catalog)
│   │   │   ├── FolderEntity.kt               (create — @Entity for folders)
│   │   │   └── FolderDocumentRefEntity.kt    (create — @Entity junction table)
│   │   └── dao/
│   │       ├── PdfDocumentDao.kt             (create — CRUD + search queries)
│   │       ├── FolderDao.kt                  (create — CRUD + nested folder queries)
│   │       └── FolderDocumentRefDao.kt       (create — add/remove/reorder refs)
│   ├── file/
│   │   └── FileManager.kt                   (create — import, copy, hash, thumbnail)
│   ├── preferences/
│   │   └── UserPreferences.kt               (create — DataStore wrapper)
│   └── nearby/
│       ├── NearbySessionManager.kt           (create — advertise/discover/messaging)
│       ├── NearbyService.kt                  (create — foreground service)
│       └── SyncMessage.kt                    (create — protocol message types)
├── ui/
│   ├── theme/
│   │   ├── Color.kt                          (modify — Mazzika gold/black palette)
│   │   ├── Theme.kt                          (modify — dark/light schemes, no dynamic)
│   │   └── Type.kt                           (modify — Playfair Display, Cormorant, Outfit)
│   ├── navigation/
│   │   ├── NavGraph.kt                       (create — all routes + navigation)
│   │   ├── BottomNavBar.kt                   (create — Material 3 bottom nav)
│   │   └── Screen.kt                         (create — sealed class for routes)
│   ├── home/
│   │   ├── HomeScreen.kt                     (create — folders + recent files)
│   │   └── HomeViewModel.kt                  (create — state management)
│   ├── catalog/
│   │   ├── CatalogScreen.kt                  (create — file list + search + import)
│   │   └── CatalogViewModel.kt               (create — state management)
│   ├── folders/
│   │   ├── FolderDetailScreen.kt             (create — folder contents + sub-folders)
│   │   └── FolderDetailViewModel.kt          (create — state management)
│   ├── reader/
│   │   ├── ReaderScreen.kt                   (create — full-screen PDF viewer)
│   │   ├── ReaderViewModel.kt                (create — page state, zoom)
│   │   └── PageFlipPager.kt                  (create — curl animation composable)
│   ├── sync/
│   │   ├── SyncScreen.kt                     (create — create/join session UI)
│   │   └── SyncViewModel.kt                  (create — Nearby state management)
│   └── settings/
│       ├── SettingsScreen.kt                 (create — theme, auto-save, device name)
│       └── SettingsViewModel.kt              (create — preferences management)
└── res/
    └── font/
        ├── playfair_display_bold.ttf         (create — download from Google Fonts)
        ├── cormorant_garamond_regular.ttf    (create — download from Google Fonts)
        ├── cormorant_garamond_semibold.ttf   (create — download from Google Fonts)
        ├── cormorant_garamond_italic.ttf     (create — download from Google Fonts)
        ├── outfit_light.ttf                  (create — download from Google Fonts)
        ├── outfit_regular.ttf                (create — download from Google Fonts)
        └── outfit_medium.ttf                 (create — download from Google Fonts)
```

---

### Task 1: Dependencies & Project Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add dependency versions and libraries to version catalog**

In `gradle/libs.versions.toml`, replace the entire contents with:

```toml
[versions]
agp = "9.1.0"
kotlin = "2.2.10"
coreKtx = "1.15.0"
lifecycleRuntimeKtx = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2026.02.01"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
room = "2.6.1"
navigation = "2.8.5"
datastore = "1.1.1"
playServicesNearby = "19.3.0"
coroutines = "1.9.0"
ksp = "2.2.10-1.0.30"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
play-services-nearby = { group = "com.google.android.gms", name = "play-services-nearby", version.ref = "playServicesNearby" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Add KSP plugin to root build.gradle.kts**

In `build.gradle.kts` (root), replace contents with:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Update app/build.gradle.kts with all dependencies**

Replace `app/build.gradle.kts` contents with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.mazzika.lyrics"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.mazzika.lyrics"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Nearby Connections
    implementation(libs.play.services.nearby)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 4: Update AndroidManifest.xml with permissions and intent filters**

Replace `app/src/main/AndroidManifest.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Nearby Connections permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
        android:usesPermissionFlags="neverForLocation" />

    <!-- Foreground service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".MazzikaApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.MazzikaLyrics">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@style/Theme.MazzikaLyrics"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Receive PDF shares from other apps -->
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="application/pdf" />
            </intent-filter>
        </activity>

        <service
            android:name=".data.nearby.NearbyService"
            android:foregroundServiceType="connectedDevice"
            android:exported="false" />

    </application>

</manifest>
```

- [ ] **Step 5: Sync Gradle and verify build**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "chore: add dependencies for Room, Navigation, DataStore, Nearby Connections"
```

---

### Task 2: Mazzika Theme (Colors, Typography, Theme)

**Files:**
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/theme/Type.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/theme/Theme.kt`
- Create: `app/src/main/res/font/playfair_display_bold.ttf`
- Create: `app/src/main/res/font/cormorant_garamond_regular.ttf`
- Create: `app/src/main/res/font/cormorant_garamond_semibold.ttf`
- Create: `app/src/main/res/font/cormorant_garamond_italic.ttf`
- Create: `app/src/main/res/font/outfit_light.ttf`
- Create: `app/src/main/res/font/outfit_regular.ttf`
- Create: `app/src/main/res/font/outfit_medium.ttf`

- [ ] **Step 1: Download font files**

Download from Google Fonts and place in `app/src/main/res/font/`:
- Playfair Display Bold → `playfair_display_bold.ttf`
- Cormorant Garamond Regular → `cormorant_garamond_regular.ttf`
- Cormorant Garamond SemiBold → `cormorant_garamond_semibold.ttf`
- Cormorant Garamond Italic → `cormorant_garamond_italic.ttf`
- Outfit Light → `outfit_light.ttf`
- Outfit Regular → `outfit_regular.ttf`
- Outfit Medium → `outfit_medium.ttf`

Verify directory: `ls app/src/main/res/font/`
Expected: all 7 .ttf files listed

- [ ] **Step 2: Replace Color.kt with Mazzika palette**

Replace `app/src/main/java/com/mazzika/lyrics/ui/theme/Color.kt` with:

```kotlin
package com.mazzika.lyrics.ui.theme

import androidx.compose.ui.graphics.Color

// Mazzika Gold palette
val Gold = Color(0xFFC5A028)
val GoldLight = Color(0xFFE8D48B)
val GoldBright = Color(0xFFF0E2A0)
val GoldDark = Color(0xFF8B7620)
val GoldDeep = Color(0xFF6B5A18)

// Dark theme
val DarkBackground = Color(0xFF060606)
val DarkSurface = Color(0xFF141414)
val DarkSurfaceElevated = Color(0xFF1A1A1A)
val DarkTextPrimary = Color(0xFFF2EFE6)
val DarkTextSecondary = Color(0xFF8A8478)
val DarkTextMuted = Color(0xFF5A5650)

// Light theme
val LightBackground = Color(0xFFFAFAF5)
val LightSurface = Color(0xFFFFFFFF)
val LightTextPrimary = Color(0xFF1A1510)
val LightTextSecondary = Color(0xFF6B6560)
val LightTextMuted = Color(0xFF9E9890)

// Semantic
val Success = Color(0xFF6BBF6A)
val Error = Color(0xFFCF6679)
```

- [ ] **Step 3: Replace Type.kt with Mazzika typography**

Replace `app/src/main/java/com/mazzika/lyrics/ui/theme/Type.kt` with:

```kotlin
package com.mazzika.lyrics.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mazzika.lyrics.R

val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_bold, FontWeight.Bold),
)

val CormorantGaramond = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_semibold, FontWeight.SemiBold),
    Font(R.font.cormorant_garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

val Outfit = FontFamily(
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        letterSpacing = 0.5.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = CormorantGaramond,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Light,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Outfit,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
    ),
)
```

- [ ] **Step 4: Replace Theme.kt with Mazzika dark/light themes**

Replace `app/src/main/java/com/mazzika/lyrics/ui/theme/Theme.kt` with:

```kotlin
package com.mazzika.lyrics.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MazzikaDarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = DarkBackground,
    primaryContainer = GoldDark,
    onPrimaryContainer = GoldLight,
    secondary = GoldLight,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    outline = GoldDeep,
    outlineVariant = DarkTextMuted,
    error = Error,
)

private val MazzikaLightColorScheme = lightColorScheme(
    primary = Gold,
    onPrimary = LightBackground,
    primaryContainer = GoldLight,
    onPrimaryContainer = GoldDeep,
    secondary = GoldDark,
    onSecondary = LightBackground,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightTextSecondary,
    outline = GoldDark,
    outlineVariant = LightTextMuted,
    error = Error,
)

@Composable
fun MazzikaLyricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) MazzikaDarkColorScheme else MazzikaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
```

- [ ] **Step 5: Build to verify fonts and theme compile**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/font/ app/src/main/java/com/mazzika/lyrics/ui/theme/
git commit -m "feat: add Mazzika theme with gold/black palette and custom typography"
```

---

### Task 3: Room Database — Entities & DAOs

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/entity/PdfDocumentEntity.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/entity/FolderEntity.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/entity/FolderDocumentRefEntity.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/dao/PdfDocumentDao.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/dao/FolderDao.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/dao/FolderDocumentRefDao.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/db/MazzikaDatabase.kt`

- [ ] **Step 1: Create PdfDocumentEntity**

Create `app/src/main/java/com/mazzika/lyrics/data/db/entity/PdfDocumentEntity.kt`:

```kotlin
package com.mazzika.lyrics.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileHash: String,
    val pageCount: Int,
    val importedAt: Long,
    val thumbnailPath: String,
)
```

- [ ] **Step 2: Create FolderEntity**

Create `app/src/main/java/com/mazzika/lyrics/data/db/entity/FolderEntity.kt`:

```kotlin
package com.mazzika.lyrics.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentFolderId")],
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val parentFolderId: Long? = null,
    val createdAt: Long,
)
```

- [ ] **Step 3: Create FolderDocumentRefEntity**

Create `app/src/main/java/com/mazzika/lyrics/data/db/entity/FolderDocumentRefEntity.kt`:

```kotlin
package com.mazzika.lyrics.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "folder_document_refs",
    primaryKeys = ["folderId", "documentId"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = PdfDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId")],
)
data class FolderDocumentRefEntity(
    val folderId: Long,
    val documentId: Long,
    val sortOrder: Int,
)
```

- [ ] **Step 4: Create PdfDocumentDao**

Create `app/src/main/java/com/mazzika/lyrics/data/db/dao/PdfDocumentDao.kt`:

```kotlin
package com.mazzika.lyrics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {

    @Query("SELECT * FROM pdf_documents ORDER BY importedAt DESC")
    fun getAll(): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id")
    suspend fun getById(id: Long): PdfDocumentEntity?

    @Query("SELECT * FROM pdf_documents WHERE fileHash = :hash LIMIT 1")
    suspend fun getByHash(hash: String): PdfDocumentEntity?

    @Query(
        "SELECT * FROM pdf_documents WHERE title LIKE '%' || :query || '%' ORDER BY importedAt DESC"
    )
    fun search(query: String): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY importedAt DESC LIMIT :limit")
    fun getRecent(limit: Int = 10): Flow<List<PdfDocumentEntity>>

    @Query("SELECT * FROM pdf_documents ORDER BY title ASC")
    fun getAllSortedByTitle(): Flow<List<PdfDocumentEntity>>

    @Insert
    suspend fun insert(document: PdfDocumentEntity): Long

    @Update
    suspend fun update(document: PdfDocumentEntity)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 5: Create FolderDao**

Create `app/src/main/java/com/mazzika/lyrics/data/db/dao/FolderDao.kt`:

```kotlin
package com.mazzika.lyrics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mazzika.lyrics.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY name ASC")
    fun getSubFolders(parentId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 6: Create FolderDocumentRefDao**

Create `app/src/main/java/com/mazzika/lyrics/data/db/dao/FolderDocumentRefDao.kt`:

```kotlin
package com.mazzika.lyrics.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDocumentRefDao {

    @Query(
        """
        SELECT d.* FROM pdf_documents d
        INNER JOIN folder_document_refs r ON d.id = r.documentId
        WHERE r.folderId = :folderId
        ORDER BY r.sortOrder ASC
        """
    )
    fun getDocumentsInFolder(folderId: Long): Flow<List<PdfDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ref: FolderDocumentRefEntity)

    @Query("DELETE FROM folder_document_refs WHERE folderId = :folderId AND documentId = :documentId")
    suspend fun delete(folderId: Long, documentId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM folder_document_refs WHERE folderId = :folderId")
    suspend fun getNextSortOrder(folderId: Long): Int
}
```

- [ ] **Step 7: Create MazzikaDatabase**

Create `app/src/main/java/com/mazzika/lyrics/data/db/MazzikaDatabase.kt`:

```kotlin
package com.mazzika.lyrics.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.mazzika.lyrics.data.db.dao.FolderDao
import com.mazzika.lyrics.data.db.dao.FolderDocumentRefDao
import com.mazzika.lyrics.data.db.dao.PdfDocumentDao
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity

@Database(
    entities = [
        PdfDocumentEntity::class,
        FolderEntity::class,
        FolderDocumentRefEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MazzikaDatabase : RoomDatabase() {
    abstract fun pdfDocumentDao(): PdfDocumentDao
    abstract fun folderDao(): FolderDao
    abstract fun folderDocumentRefDao(): FolderDocumentRefDao

    companion object {
        @Volatile
        private var INSTANCE: MazzikaDatabase? = null

        fun getInstance(context: Context): MazzikaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MazzikaDatabase::class.java,
                    "mazzika_lyrics.db",
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

- [ ] **Step 8: Build to verify Room compiles**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/data/db/
git commit -m "feat: add Room database with entities and DAOs for catalog and folders"
```

---

### Task 4: FileManager & UserPreferences

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/data/file/FileManager.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/preferences/UserPreferences.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/MazzikaApplication.kt`

- [ ] **Step 1: Create FileManager**

Create `app/src/main/java/com/mazzika/lyrics/data/file/FileManager.kt`:

```kotlin
package com.mazzika.lyrics.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class FileManager(private val context: Context) {

    private val pdfsDir: File
        get() = File(context.filesDir, "pdfs").also { it.mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").also { it.mkdirs() }

    private val tempDir: File
        get() = File(context.filesDir, "temp").also { it.mkdirs() }

    suspend fun importPdf(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val fileName = getFileName(uri)
        val destFile = File(pdfsDir, "${System.currentTimeMillis()}_$fileName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Cannot open file: $uri")

        val hash = computeHash(destFile)
        val pageCount = getPageCount(destFile)
        val thumbnailPath = generateThumbnail(destFile, hash)

        ImportResult(
            fileName = fileName,
            filePath = destFile.absolutePath,
            fileHash = hash,
            pageCount = pageCount,
            thumbnailPath = thumbnailPath,
        )
    }

    suspend fun saveTempFile(bytes: ByteArray, fileName: String): String = withContext(Dispatchers.IO) {
        val destFile = File(tempDir, "${System.currentTimeMillis()}_$fileName")
        destFile.writeBytes(bytes)
        destFile.absolutePath
    }

    suspend fun moveTempToCatalog(tempPath: String): String = withContext(Dispatchers.IO) {
        val tempFile = File(tempPath)
        val destFile = File(pdfsDir, tempFile.name)
        tempFile.renameTo(destFile)
        destFile.absolutePath
    }

    suspend fun computeHash(file: File): String = withContext(Dispatchers.IO) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun getPageCount(file: File): Int = withContext(Dispatchers.IO) {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val count = renderer.pageCount
        renderer.close()
        fd.close()
        count
    }

    suspend fun generateThumbnail(file: File, hash: String): String = withContext(Dispatchers.IO) {
        val thumbFile = File(thumbnailsDir, "$hash.png")
        if (thumbFile.exists()) return@withContext thumbFile.absolutePath

        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val page = renderer.openPage(0)

        val width = 200
        val height = (width.toFloat() / page.width * page.height).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        FileOutputStream(thumbFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
        }

        page.close()
        renderer.close()
        fd.close()
        bitmap.recycle()

        thumbFile.absolutePath
    }

    fun deleteFile(path: String) {
        File(path).delete()
    }

    fun readFileBytes(path: String): ByteArray = File(path).readBytes()

    private fun getFileName(uri: Uri): String {
        var name = "document.pdf"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    data class ImportResult(
        val fileName: String,
        val filePath: String,
        val fileHash: String,
        val pageCount: Int,
        val thumbnailPath: String,
    )
}
```

- [ ] **Step 2: Create UserPreferences**

Create `app/src/main/java/com/mazzika/lyrics/data/preferences/UserPreferences.kt`:

```kotlin
package com.mazzika.lyrics.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val autoSaveSyncKey = booleanPreferencesKey("auto_save_sync")
    private val deviceNameKey = stringPreferencesKey("device_name")

    val theme: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            "light" -> ThemeMode.LIGHT
            "dark" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val autoSaveSync: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[autoSaveSyncKey] ?: false
    }

    val deviceName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[deviceNameKey] ?: android.os.Build.MODEL
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
        }
    }

    suspend fun setAutoSaveSync(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[autoSaveSyncKey] = enabled }
    }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { prefs -> prefs[deviceNameKey] = name }
    }

    enum class ThemeMode { LIGHT, DARK, SYSTEM }
}
```

- [ ] **Step 3: Create MazzikaApplication**

Create `app/src/main/java/com/mazzika/lyrics/MazzikaApplication.kt`:

```kotlin
package com.mazzika.lyrics

import android.app.Application
import com.mazzika.lyrics.data.db.MazzikaDatabase
import com.mazzika.lyrics.data.file.FileManager
import com.mazzika.lyrics.data.preferences.UserPreferences

class MazzikaApplication : Application() {
    val database: MazzikaDatabase by lazy { MazzikaDatabase.getInstance(this) }
    val fileManager: FileManager by lazy { FileManager(this) }
    val userPreferences: UserPreferences by lazy { UserPreferences(this) }
}
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/data/file/ app/src/main/java/com/mazzika/lyrics/data/preferences/ app/src/main/java/com/mazzika/lyrics/MazzikaApplication.kt
git commit -m "feat: add FileManager for PDF import/hash/thumbnails and UserPreferences with DataStore"
```

---

### Task 5: Navigation — Routes, Bottom Nav, NavGraph

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/navigation/Screen.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/navigation/BottomNavBar.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/MainActivity.kt`

- [ ] **Step 1: Create Screen routes**

Create `app/src/main/java/com/mazzika/lyrics/ui/navigation/Screen.kt`:

```kotlin
package com.mazzika.lyrics.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Catalog : Screen("catalog")
    data object Sync : Screen("sync")
    data object Settings : Screen("settings")
    data object FolderDetail : Screen("folder/{folderId}") {
        fun createRoute(folderId: Long) = "folder/$folderId"
    }
    data object Reader : Screen("reader/{documentId}") {
        fun createRoute(documentId: Long) = "reader/$documentId"
    }
    data object ReaderSync : Screen("reader_sync/{filePath}") {
        fun createRoute(filePath: String) = "reader_sync/${java.net.URLEncoder.encode(filePath, "UTF-8")}"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Accueil", Icons.Default.Home),
    BottomNavItem(Screen.Catalog, "Catalogue", Icons.Default.LibraryMusic),
    BottomNavItem(Screen.Sync, "Session", Icons.Default.SyncAlt),
    BottomNavItem(Screen.Settings, "Param.", Icons.Default.Settings),
)
```

- [ ] **Step 2: Create BottomNavBar**

Create `app/src/main/java/com/mazzika/lyrics/ui/navigation/BottomNavBar.kt`:

```kotlin
package com.mazzika.lyrics.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mazzika.lyrics.ui.theme.DarkBackground
import com.mazzika.lyrics.ui.theme.DarkTextMuted
import com.mazzika.lyrics.ui.theme.Gold

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = modifier,
        containerColor = DarkBackground.copy(alpha = 0.92f),
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.screen.route,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Gold,
                    selectedTextColor = Gold,
                    unselectedIconColor = DarkTextMuted,
                    unselectedTextColor = DarkTextMuted,
                    indicatorColor = Gold.copy(alpha = 0.12f),
                ),
            )
        }
    }
}
```

- [ ] **Step 3: Create NavGraph with placeholder screens**

Create `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`:

```kotlin
package com.mazzika.lyrics.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            PlaceholderScreen("Accueil")
        }
        composable(Screen.Catalog.route) {
            PlaceholderScreen("Catalogue")
        }
        composable(Screen.Sync.route) {
            PlaceholderScreen("Session")
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen("Paramètres")
        }
        composable(
            route = Screen.FolderDetail.route,
            arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
        ) {
            PlaceholderScreen("Dossier")
        }
        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
        ) {
            PlaceholderScreen("Lecteur PDF")
        }
        composable(
            route = Screen.ReaderSync.route,
            arguments = listOf(navArgument("filePath") { type = NavType.StringType }),
        ) {
            PlaceholderScreen("Lecteur Sync")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name)
    }
}
```

- [ ] **Step 4: Update MainActivity to use NavGraph + BottomNavBar**

Replace `app/src/main/java/com/mazzika/lyrics/MainActivity.kt` with:

```kotlin
package com.mazzika.lyrics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mazzika.lyrics.ui.navigation.BottomNavBar
import com.mazzika.lyrics.ui.navigation.NavGraph
import com.mazzika.lyrics.ui.navigation.Screen
import com.mazzika.lyrics.ui.theme.MazzikaLyricsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MazzikaLyricsTheme(darkTheme = true) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Catalog.route,
                    Screen.Sync.route,
                    Screen.Settings.route,
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                        ) {
                            BottomNavBar(navController)
                        }
                    },
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 5: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/navigation/ app/src/main/java/com/mazzika/lyrics/MainActivity.kt
git commit -m "feat: add bottom navigation with 4 tabs and NavGraph with placeholder screens"
```

---

### Task 6: Home Screen (Folders + Recent Files)

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/home/HomeViewModel.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create HomeViewModel**

Create `app/src/main/java/com/mazzika/lyrics/ui/home/HomeViewModel.kt`:

```kotlin
package com.mazzika.lyrics.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as MazzikaApplication).database

    val rootFolders: StateFlow<List<FolderEntity>> = db.folderDao().getRootFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentDocuments: StateFlow<List<PdfDocumentEntity>> = db.pdfDocumentDao().getRecent(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String, icon: String?) {
        viewModelScope.launch {
            db.folderDao().insert(
                FolderEntity(
                    name = name,
                    icon = icon,
                    parentFolderId = null,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            db.folderDao().deleteById(folderId)
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            db.folderDao().getById(folderId)?.let { folder ->
                db.folderDao().update(folder.copy(name = newName))
            }
        }
    }
}
```

- [ ] **Step 2: Create HomeScreen**

Create `app/src/main/java/com/mazzika/lyrics/ui/home/HomeScreen.kt`:

```kotlin
package com.mazzika.lyrics.ui.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark

@Composable
fun HomeScreen(
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToReader: (Long) -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val folders by viewModel.rootFolders.collectAsState()
    val recentDocs by viewModel.recentDocuments.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Gold,
                contentColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, "Nouveau dossier")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Mazzika",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Gold,
                        )
                        Text(
                            "BAND LYRICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(onClick = onNavigateToSync) {
                            Icon(Icons.Default.SyncAlt, "Session", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, "Paramètres", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Folders section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Mes Dossiers", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Voir tout",
                        style = MaterialTheme.typography.labelMedium,
                        color = Gold,
                    )
                }
            }

            item {
                if (folders.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Aucun dossier. Appuyez sur + pour en créer.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(folders) { folder ->
                            FolderCard(
                                folder = folder,
                                onClick = { onNavigateToFolder(folder.id) },
                            )
                        }
                    }
                }
            }

            // Recent section
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Récemment ouverts",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            if (recentDocs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Aucun fichier récent.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(recentDocs) { doc ->
                    RecentFileItem(
                        document = doc,
                        onClick = { onNavigateToReader(doc.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, icon ->
                viewModel.createFolder(name, icon)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun FolderCard(folder: FolderEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp, 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    folder.icon ?: "\uD83D\uDCC1",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                folder.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentFileItem(document: PdfDocumentEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp, 52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "PDF",
                style = MaterialTheme.typography.labelSmall,
                color = GoldDark,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                document.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${document.pageCount} pages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { /* context menu */ }) {
            Icon(
                Icons.Default.MoreVert,
                "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    val iconOptions = listOf("\uD83D\uDCC1", "\uD83C\uDFB6", "\uD83C\uDFB8", "\u2B50", "\uD83C\uDFBC", "\u2728", "\uD83D\uDCE6", "\uD83C\uDFA4")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau dossier") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom du dossier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("Icône (optionnel)", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    iconOptions.forEach { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedIcon == icon) Gold.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable {
                                    selectedIcon = if (selectedIcon == icon) null else icon
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(icon)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), selectedIcon) },
            ) {
                Text("Créer", color = Gold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}
```

- [ ] **Step 3: Wire HomeScreen into NavGraph**

In `NavGraph.kt`, replace the `composable(Screen.Home.route)` block:

```kotlin
composable(Screen.Home.route) {
    HomeScreen(
        onNavigateToFolder = { folderId ->
            navController.navigate(Screen.FolderDetail.createRoute(folderId))
        },
        onNavigateToReader = { docId ->
            navController.navigate(Screen.Reader.createRoute(docId))
        },
        onNavigateToSync = {
            navController.navigate(Screen.Sync.route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateToSettings = {
            navController.navigate(Screen.Settings.route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    )
}
```

Add import at top of NavGraph.kt:
```kotlin
import com.mazzika.lyrics.ui.home.HomeScreen
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/home/ app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt
git commit -m "feat: add Home screen with folder cards, recent files, and create folder dialog"
```

---

### Task 7: Catalog Screen (File List + Import + Search)

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/catalog/CatalogViewModel.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/catalog/CatalogScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create CatalogViewModel**

Create `app/src/main/java/com/mazzika/lyrics/ui/catalog/CatalogViewModel.kt`:

```kotlin
package com.mazzika.lyrics.ui.catalog

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortMode { RECENT, TITLE }

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MazzikaApplication
    private val docDao = app.database.pdfDocumentDao()
    private val refDao = app.database.folderDocumentRefDao()
    private val folderDao = app.database.folderDao()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.RECENT)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val documents: StateFlow<List<PdfDocumentEntity>> = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            when (_sortMode.value) {
                SortMode.RECENT -> docDao.getAll()
                SortMode.TITLE -> docDao.getAllSortedByTitle()
            }
        } else {
            docDao.search(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
        _searchQuery.value = _searchQuery.value // retrigger
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = app.fileManager.importPdf(uri)
                val title = result.fileName.removeSuffix(".pdf").removeSuffix(".PDF")
                docDao.insert(
                    PdfDocumentEntity(
                        title = title,
                        fileName = result.fileName,
                        filePath = result.filePath,
                        fileHash = result.fileHash,
                        pageCount = result.pageCount,
                        importedAt = System.currentTimeMillis(),
                        thumbnailPath = result.thumbnailPath,
                    )
                )
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch {
            docDao.getById(id)?.let { doc ->
                app.fileManager.deleteFile(doc.filePath)
                app.fileManager.deleteFile(doc.thumbnailPath)
                docDao.deleteById(id)
            }
        }
    }

    fun addToFolder(documentId: Long, folderId: Long) {
        viewModelScope.launch {
            val sortOrder = refDao.getNextSortOrder(folderId)
            refDao.insert(FolderDocumentRefEntity(folderId, documentId, sortOrder))
        }
    }
}
```

- [ ] **Step 2: Create CatalogScreen**

Create `app/src/main/java/com/mazzika/lyrics/ui/catalog/CatalogScreen.kt`:

```kotlin
package com.mazzika.lyrics.ui.catalog

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CatalogScreen(
    onNavigateToReader: (Long) -> Unit,
    viewModel: CatalogViewModel = viewModel(),
) {
    val documents by viewModel.documents.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importPdf(it) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                containerColor = Gold,
                contentColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.background,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Add, "Importer")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 20.dp, 20.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Catalogue", style = MaterialTheme.typography.headlineLarge)
                }
                Text(
                    "${documents.size} partitions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 2.dp),
                )
            }

            // Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Rechercher un titre, artiste...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp, 14.dp),
                )
            }

            // Filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        FilterChip(
                            selected = sortMode == SortMode.RECENT,
                            onClick = { viewModel.setSortMode(SortMode.RECENT) },
                            label = { Text("Récents") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                    item {
                        FilterChip(
                            selected = sortMode == SortMode.TITLE,
                            onClick = { viewModel.setSortMode(SortMode.TITLE) },
                            label = { Text("A - Z") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // File list
            if (documents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (searchQuery.isBlank()) "Aucun fichier. Appuyez sur + pour importer."
                            else "Aucun résultat pour \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(documents) { doc ->
                    FileCard(
                        document = doc,
                        onClick = { onNavigateToReader(doc.id) },
                        onDelete = { viewModel.deleteDocument(doc.id) },
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun FileCard(
    document: PdfDocumentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thumbnail
            val thumbFile = remember(document.thumbnailPath) { File(document.thumbnailPath) }
            if (thumbFile.exists()) {
                val bitmap = remember(document.thumbnailPath) {
                    BitmapFactory.decodeFile(document.thumbnailPath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp, 64.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp, 64.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("PDF", style = MaterialTheme.typography.labelSmall, color = GoldDark)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    document.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${document.pageCount} pages · ${formatDate(document.importedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.MoreVert,
                    "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
```

- [ ] **Step 3: Wire CatalogScreen into NavGraph**

In `NavGraph.kt`, replace the `composable(Screen.Catalog.route)` block:

```kotlin
composable(Screen.Catalog.route) {
    CatalogScreen(
        onNavigateToReader = { docId ->
            navController.navigate(Screen.Reader.createRoute(docId))
        },
    )
}
```

Add import:
```kotlin
import com.mazzika.lyrics.ui.catalog.CatalogScreen
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/catalog/ app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt
git commit -m "feat: add Catalog screen with PDF import, search, sort, and file cards"
```

---

### Task 8: Folder Detail Screen

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/folders/FolderDetailViewModel.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/folders/FolderDetailScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create FolderDetailViewModel**

Create `app/src/main/java/com/mazzika/lyrics/ui/folders/FolderDetailViewModel.kt`:

```kotlin
package com.mazzika.lyrics.ui.folders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.FolderDocumentRefEntity
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val app = application as MazzikaApplication
    private val folderId: Long = savedStateHandle["folderId"] ?: 0L

    private val _folder = MutableStateFlow<FolderEntity?>(null)
    val folder: StateFlow<FolderEntity?> = _folder.asStateFlow()

    val subFolders: StateFlow<List<FolderEntity>> =
        app.database.folderDao().getSubFolders(folderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<PdfDocumentEntity>> =
        app.database.folderDocumentRefDao().getDocumentsInFolder(folderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _folder.value = app.database.folderDao().getById(folderId)
        }
    }

    fun createSubFolder(name: String, icon: String?) {
        viewModelScope.launch {
            app.database.folderDao().insert(
                FolderEntity(
                    name = name,
                    icon = icon,
                    parentFolderId = folderId,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    fun removeDocumentFromFolder(documentId: Long) {
        viewModelScope.launch {
            app.database.folderDocumentRefDao().delete(folderId, documentId)
        }
    }

    fun addDocumentToFolder(documentId: Long) {
        viewModelScope.launch {
            val sortOrder = app.database.folderDocumentRefDao().getNextSortOrder(folderId)
            app.database.folderDocumentRefDao().insert(
                FolderDocumentRefEntity(folderId, documentId, sortOrder)
            )
        }
    }
}
```

- [ ] **Step 2: Create FolderDetailScreen**

Create `app/src/main/java/com/mazzika/lyrics/ui/folders/FolderDetailScreen.kt`:

```kotlin
package com.mazzika.lyrics.ui.folders

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.FolderEntity
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (Long) -> Unit,
    onNavigateToReader: (Long) -> Unit,
    viewModel: FolderDetailViewModel = viewModel(),
) {
    val folder by viewModel.folder.collectAsState()
    val subFolders by viewModel.subFolders.collectAsState()
    val documents by viewModel.documents.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        folder?.icon?.let { icon ->
                            Text(icon, modifier = Modifier.padding(end = 8.dp))
                        }
                        Text(
                            folder?.name ?: "",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Gold,
                contentColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Add, "Ajouter")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Sub-folders
            if (subFolders.isNotEmpty()) {
                item {
                    Text(
                        "Sous-dossiers",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 8.dp),
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(subFolders) { sub ->
                            Card(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { onNavigateToFolder(sub.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Gold.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(sub.icon ?: "\uD83D\uDCC1")
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        sub.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Documents
            if (documents.isNotEmpty()) {
                item {
                    Text(
                        "Fichiers",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(20.dp, 12.dp, 20.dp, 8.dp),
                    )
                }
                items(documents) { doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToReader(doc.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp, 52.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("PDF", style = MaterialTheme.typography.labelSmall, color = GoldDark)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(doc.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${doc.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.removeDocumentFromFolder(doc.id) }) {
                            Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (subFolders.isEmpty() && documents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Ce dossier est vide.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var selectedIcon by remember { mutableStateOf<String?>(null) }
        val iconOptions = listOf("\uD83D\uDCC1", "\uD83C\uDFB6", "\uD83C\uDFB8", "\u2B50", "\uD83C\uDFBC", "\u2728", "\uD83D\uDCE6", "\uD83C\uDFA4")

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nouveau sous-dossier") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Icône (optionnel)", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        iconOptions.forEach { icon ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selectedIcon == icon) Gold.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface,
                                    )
                                    .clickable { selectedIcon = if (selectedIcon == icon) null else icon },
                                contentAlignment = Alignment.Center,
                            ) { Text(icon) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createSubFolder(name.trim(), selectedIcon)
                        showCreateDialog = false
                    }
                }) { Text("Créer", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Annuler") }
            },
        )
    }
}
```

- [ ] **Step 3: Wire FolderDetailScreen into NavGraph**

In `NavGraph.kt`, replace the `composable(Screen.FolderDetail.route)` block:

```kotlin
composable(
    route = Screen.FolderDetail.route,
    arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
) {
    FolderDetailScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToFolder = { folderId ->
            navController.navigate(Screen.FolderDetail.createRoute(folderId))
        },
        onNavigateToReader = { docId ->
            navController.navigate(Screen.Reader.createRoute(docId))
        },
    )
}
```

Add import:
```kotlin
import com.mazzika.lyrics.ui.folders.FolderDetailScreen
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/folders/ app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt
git commit -m "feat: add FolderDetail screen with sub-folders and document list"
```

---

### Task 9: PDF Reader (Full Screen + Page Flip + Zoom)

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/reader/ReaderViewModel.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/reader/PageFlipPager.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/reader/ReaderScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create ReaderViewModel**

Create `app/src/main/java/com/mazzika/lyrics/ui/reader/ReaderViewModel.kt`:

```kotlin
package com.mazzika.lyrics.ui.reader

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val app = application as MazzikaApplication
    private val documentId: Long = savedStateHandle["documentId"] ?: 0L

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _showToolbar = MutableStateFlow(true)
    val showToolbar: StateFlow<Boolean> = _showToolbar.asStateFlow()

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private val pageCache = mutableMapOf<Int, Bitmap>()

    // For sync mode — external page control
    private val _filePath = MutableStateFlow("")
    val filePath: StateFlow<String> = _filePath.asStateFlow()

    init {
        viewModelScope.launch {
            val doc = app.database.pdfDocumentDao().getById(documentId) ?: return@launch
            _title.value = doc.title
            _filePath.value = doc.filePath
            openRenderer(doc.filePath)
        }
    }

    fun initWithFilePath(path: String) {
        viewModelScope.launch {
            val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
            _filePath.value = decodedPath
            _title.value = File(decodedPath).nameWithoutExtension
            openRenderer(decodedPath)
        }
    }

    private suspend fun openRenderer(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(fileDescriptor!!)
        _pageCount.value = renderer!!.pageCount
    }

    suspend fun renderPage(pageIndex: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        pageCache[pageIndex]?.let { return@withContext it }

        val r = renderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= r.pageCount) return@withContext null

        val page = r.openPage(pageIndex)
        val scale = width.toFloat() / page.width
        val height = (page.height * scale).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        // Keep cache to 3 pages
        if (pageCache.size >= 3) {
            val toRemove = pageCache.keys.filter { kotlin.math.abs(it - pageIndex) > 1 }
            toRemove.forEach { key ->
                pageCache.remove(key)?.recycle()
            }
        }
        pageCache[pageIndex] = bitmap
        bitmap
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun toggleToolbar() {
        _showToolbar.value = !_showToolbar.value
    }

    override fun onCleared() {
        super.onCleared()
        pageCache.values.forEach { it.recycle() }
        pageCache.clear()
        renderer?.close()
        fileDescriptor?.close()
    }
}
```

- [ ] **Step 2: Create PageFlipPager composable**

Create `app/src/main/java/com/mazzika/lyrics/ui/reader/PageFlipPager.kt`:

```kotlin
package com.mazzika.lyrics.ui.reader

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs

@Composable
fun PageFlipPager(
    pageCount: Int,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    renderPage: suspend (Int, Int) -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx().toInt() }

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var targetPage by remember { mutableIntStateOf(currentPage) }

    // Zoom state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val isZoomed = scale > 1.05f

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }

    // Load current page bitmap
    LaunchedEffect(currentPage, screenWidthPx) {
        bitmap = renderPage(currentPage, screenWidthPx)
    }

    // Animate page transition
    val animatedOffset by animateFloatAsState(
        targetValue = if (targetPage != currentPage) {
            if (targetPage > currentPage) -screenWidthPx.toFloat() else screenWidthPx.toFloat()
        } else 0f,
        animationSpec = tween(300),
        finishedListener = {
            if (targetPage != currentPage) {
                onPageChanged(targetPage)
                dragOffset = 0f
            }
        },
        label = "pageFlip",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .transformable(transformState)
            .pointerInput(isZoomed, pageCount) {
                if (!isZoomed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val threshold = size.width * 0.25f
                            when {
                                dragOffset < -threshold && currentPage < pageCount - 1 -> {
                                    targetPage = currentPage + 1
                                }
                                dragOffset > threshold && currentPage > 0 -> {
                                    targetPage = currentPage - 1
                                }
                                else -> dragOffset = 0f
                            }
                        },
                        onDragCancel = { dragOffset = 0f },
                    ) { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reset zoom
                        scale = 1f
                        offset = Offset.Zero
                    },
                    onTap = { tapOffset ->
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val isCenter = abs(tapOffset.x - centerX) < size.width * 0.25f &&
                            abs(tapOffset.y - centerY) < size.height * 0.25f
                        if (isCenter) onCenterTap()
                    },
                )
            },
    ) {
        bitmap?.let { bmp ->
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x + dragOffset + animatedOffset,
                        translationY = offset.y,
                    ),
            ) {
                val imageBitmap = bmp.asImageBitmap()
                val canvasWidth = size.width
                val canvasHeight = size.height
                val imgAspect = bmp.width.toFloat() / bmp.height
                val canvasAspect = canvasWidth / canvasHeight

                val (drawWidth, drawHeight) = if (imgAspect > canvasAspect) {
                    canvasWidth to canvasWidth / imgAspect
                } else {
                    canvasHeight * imgAspect to canvasHeight
                }

                val left = (canvasWidth - drawWidth) / 2f
                val top = (canvasHeight - drawHeight) / 2f

                drawImage(
                    image = imageBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt()),
                )
            }
        }
    }
}
```

- [ ] **Step 3: Create ReaderScreen**

Create `app/src/main/java/com/mazzika/lyrics/ui/reader/ReaderScreen.kt`:

```kotlin
package com.mazzika.lyrics.ui.reader

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark

@Composable
fun ReaderScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSync: () -> Unit,
    viewModel: ReaderViewModel = viewModel(),
) {
    val title by viewModel.title.collectAsState()
    val pageCount by viewModel.pageCount.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val showToolbar by viewModel.showToolbar.collectAsState()

    // Immersive mode
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (pageCount > 0) {
            PageFlipPager(
                pageCount = pageCount,
                currentPage = currentPage,
                onPageChanged = { viewModel.setCurrentPage(it) },
                onCenterTap = { viewModel.toggleToolbar() },
                renderPage = { page, width -> viewModel.renderPage(page, width) },
            )
        }

        // Top toolbar
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent),
                        )
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.Close, "Fermer", tint = Color.White.copy(alpha = 0.7f))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                IconButton(
                    onClick = onNavigateToSync,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.06f),
                    ),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Default.SyncAlt, "Session", tint = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // Page indicator
        AnimatedVisibility(
            visible = showToolbar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pageCount.coerceAtMost(10)) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) width = 18.dp else 6.dp, 6.dp)
                            .background(
                                if (index == currentPage) Gold else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(3.dp),
                            ),
                    )
                }
                Text(
                    "${currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
```

- [ ] **Step 4: Wire ReaderScreen into NavGraph**

In `NavGraph.kt`, replace the `composable(Screen.Reader.route)` block:

```kotlin
composable(
    route = Screen.Reader.route,
    arguments = listOf(navArgument("documentId") { type = NavType.LongType }),
) {
    ReaderScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToSync = { navController.navigate(Screen.Sync.route) },
    )
}
```

Add import:
```kotlin
import com.mazzika.lyrics.ui.reader.ReaderScreen
```

- [ ] **Step 5: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/reader/ app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt
git commit -m "feat: add PDF reader with full-screen immersive mode, page flip, and pinch-to-zoom"
```

---

### Task 10: Settings Screen

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create SettingsViewModel**

Create `app/src/main/java/com/mazzika/lyrics/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.mazzika.lyrics.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = (application as MazzikaApplication).userPreferences

    val theme: StateFlow<UserPreferences.ThemeMode> = prefs.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences.ThemeMode.DARK)

    val autoSaveSync: StateFlow<Boolean> = prefs.autoSaveSync
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val deviceName: StateFlow<String> = prefs.deviceName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setTheme(mode: UserPreferences.ThemeMode) {
        viewModelScope.launch { prefs.setTheme(mode) }
    }

    fun setAutoSaveSync(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSaveSync(enabled) }
    }

    fun setDeviceName(name: String) {
        viewModelScope.launch { prefs.setDeviceName(name) }
    }
}
```

- [ ] **Step 2: Create SettingsScreen**

Create `app/src/main/java/com/mazzika/lyrics/ui/settings/SettingsScreen.kt`:

```kotlin
package com.mazzika.lyrics.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.preferences.UserPreferences
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val theme by viewModel.theme.collectAsState()
    val autoSave by viewModel.autoSaveSync.collectAsState()
    val deviceName by viewModel.deviceName.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Paramètres", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        // Affichage section
        SectionTitle("Affichage")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Thème", style = MaterialTheme.typography.bodyMedium)
                    Text("Apparence de l'application", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeChip("Clair", theme == UserPreferences.ThemeMode.LIGHT) {
                        viewModel.setTheme(UserPreferences.ThemeMode.LIGHT)
                    }
                    ThemeChip("Sombre", theme == UserPreferences.ThemeMode.DARK) {
                        viewModel.setTheme(UserPreferences.ThemeMode.DARK)
                    }
                    ThemeChip("Auto", theme == UserPreferences.ThemeMode.SYSTEM) {
                        viewModel.setTheme(UserPreferences.ThemeMode.SYSTEM)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Sync section
        SectionTitle("Synchronisation")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sauvegarde auto", style = MaterialTheme.typography.bodyMedium)
                    Text("Fichiers reçus ajoutés au catalogue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = autoSave,
                    onCheckedChange = { viewModel.setAutoSaveSync(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Gold),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsCard(onClick = { showNameDialog = true }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Nom de l'appareil", style = MaterialTheme.typography.bodyMedium)
                    Text("Affiché lors de la découverte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(deviceName, style = MaterialTheme.typography.bodyMedium, color = Gold)
            }
        }

        Spacer(Modifier.height(24.dp))

        // About
        SectionTitle("À propos")
        SettingsCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Version", style = MaterialTheme.typography.bodyMedium)
                Text("1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(40.dp))
        Text(
            "Mazzika Lyrics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }

    if (showNameDialog) {
        var name by remember { mutableStateOf(deviceName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Nom de l'appareil") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.setDeviceName(name.trim())
                        showNameDialog = false
                    }
                }) { Text("OK", color = Gold) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Annuler") }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = GoldDark,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        onClick = onClick ?: {},
        enabled = onClick != null,
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Gold,
            selectedLabelColor = MaterialTheme.colorScheme.background,
        ),
    )
}
```

- [ ] **Step 3: Wire SettingsScreen into NavGraph**

In `NavGraph.kt`, replace the `composable(Screen.Settings.route)` block:

```kotlin
composable(Screen.Settings.route) {
    SettingsScreen()
}
```

Add import:
```kotlin
import com.mazzika.lyrics.ui.settings.SettingsScreen
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/settings/ app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt
git commit -m "feat: add Settings screen with theme, auto-save sync, and device name"
```

---

### Task 11: Nearby Connections — Service & Message Protocol

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/data/nearby/SyncMessage.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/nearby/NearbySessionManager.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/data/nearby/NearbyService.kt`

- [ ] **Step 1: Create SyncMessage protocol**

Create `app/src/main/java/com/mazzika/lyrics/data/nearby/SyncMessage.kt`:

```kotlin
package com.mazzika.lyrics.data.nearby

import org.json.JSONObject

sealed class SyncMessage {
    data class SessionInfo(val title: String, val pageCount: Int, val fileHash: String) : SyncMessage()
    data class AlreadyHave(val fileHash: String) : SyncMessage()
    data class NeedFile(val fileHash: String) : SyncMessage()
    data class PageChange(val page: Int) : SyncMessage()

    fun toBytes(): ByteArray {
        val json = JSONObject()
        when (this) {
            is SessionInfo -> {
                json.put("type", "SESSION_INFO")
                json.put("title", title)
                json.put("pageCount", pageCount)
                json.put("fileHash", fileHash)
            }
            is AlreadyHave -> {
                json.put("type", "ALREADY_HAVE")
                json.put("fileHash", fileHash)
            }
            is NeedFile -> {
                json.put("type", "NEED_FILE")
                json.put("fileHash", fileHash)
            }
            is PageChange -> {
                json.put("type", "PAGE_CHANGE")
                json.put("page", page)
            }
        }
        return json.toString().toByteArray()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): SyncMessage? {
            return try {
                val json = JSONObject(String(bytes))
                when (json.getString("type")) {
                    "SESSION_INFO" -> SessionInfo(
                        title = json.getString("title"),
                        pageCount = json.getInt("pageCount"),
                        fileHash = json.getString("fileHash"),
                    )
                    "ALREADY_HAVE" -> AlreadyHave(json.getString("fileHash"))
                    "NEED_FILE" -> NeedFile(json.getString("fileHash"))
                    "PAGE_CHANGE" -> PageChange(json.getInt("page"))
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
```

- [ ] **Step 2: Create NearbySessionManager**

Create `app/src/main/java/com/mazzika/lyrics/data/nearby/NearbySessionManager.kt`:

```kotlin
package com.mazzika.lyrics.data.nearby

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbySessionManager(private val context: Context) {

    companion object {
        private const val SERVICE_ID = "com.mazzika.lyrics.sync"
    }

    private val connectionsClient by lazy { Nearby.getConnectionsClient(context) }

    data class EndpointInfo(val id: String, val name: String)

    // State
    private val _connectedEndpoints = MutableStateFlow<List<EndpointInfo>>(emptyList())
    val connectedEndpoints: StateFlow<List<EndpointInfo>> = _connectedEndpoints.asStateFlow()

    private val _discoveredEndpoints = MutableStateFlow<List<EndpointInfo>>(emptyList())
    val discoveredEndpoints: StateFlow<List<EndpointInfo>> = _discoveredEndpoints.asStateFlow()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _incomingMessages = MutableStateFlow<Pair<String, SyncMessage>?>(null)
    val incomingMessages: StateFlow<Pair<String, SyncMessage>?> = _incomingMessages.asStateFlow()

    private val _incomingFile = MutableStateFlow<ByteArray?>(null)
    val incomingFile: StateFlow<ByteArray?> = _incomingFile.asStateFlow()

    private var onConnectionRequest: ((String, String) -> Boolean)? = null

    fun setConnectionRequestHandler(handler: (endpointId: String, endpointName: String) -> Boolean) {
        onConnectionRequest = handler
    }

    // Advertise (Pilot)
    fun startAdvertising(deviceName: String) {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startAdvertising(
            deviceName, SERVICE_ID, connectionLifecycleCallback, options,
        ).addOnSuccessListener { _isAdvertising.value = true }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
    }

    // Discover (Follower)
    fun startDiscovery() {
        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        connectionsClient.startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, options,
        ).addOnSuccessListener { _isDiscovering.value = true }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
        _discoveredEndpoints.value = emptyList()
    }

    fun requestConnection(deviceName: String, endpointId: String) {
        connectionsClient.requestConnection(deviceName, endpointId, connectionLifecycleCallback)
    }

    // Messaging
    fun sendMessage(endpointId: String, message: SyncMessage) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(message.toBytes()))
    }

    fun broadcastMessage(message: SyncMessage) {
        val payload = Payload.fromBytes(message.toBytes())
        _connectedEndpoints.value.forEach { endpoint ->
            connectionsClient.sendPayload(endpoint.id, payload)
        }
    }

    fun sendFile(endpointId: String, fileBytes: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(fileBytes))
    }

    fun disconnect() {
        _connectedEndpoints.value.forEach { connectionsClient.disconnectFromEndpoint(it.id) }
        _connectedEndpoints.value = emptyList()
        stopAdvertising()
        stopDiscovery()
    }

    // Callbacks
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val accept = onConnectionRequest?.invoke(endpointId, info.endpointName) ?: true
            if (accept) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            } else {
                connectionsClient.rejectConnection(endpointId)
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                val name = _discoveredEndpoints.value.find { it.id == endpointId }?.name ?: endpointId
                _connectedEndpoints.value = _connectedEndpoints.value + EndpointInfo(endpointId, name)
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedEndpoints.value = _connectedEndpoints.value.filter { it.id != endpointId }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _discoveredEndpoints.value = _discoveredEndpoints.value + EndpointInfo(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {
            _discoveredEndpoints.value = _discoveredEndpoints.value.filter { it.id != endpointId }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val message = SyncMessage.fromBytes(bytes)
            if (message != null) {
                _incomingMessages.value = endpointId to message
            } else {
                // It's a file transfer
                _incomingFile.value = bytes
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Could track progress here for large files
        }
    }
}
```

- [ ] **Step 3: Create NearbyService (Foreground Service)**

Create `app/src/main/java/com/mazzika/lyrics/data/nearby/NearbyService.kt`:

```kotlin
package com.mazzika.lyrics.data.nearby

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mazzika.lyrics.R

class NearbyService : Service() {

    companion object {
        const val CHANNEL_ID = "mazzika_sync"
        const val NOTIFICATION_ID = 1
    }

    private val binder = LocalBinder()
    var sessionManager: NearbySessionManager? = null
        private set

    inner class LocalBinder : Binder() {
        fun getService(): NearbyService = this@NearbyService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        sessionManager = NearbySessionManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(0))
    }

    fun updateNotification(connectedCount: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(connectedCount))
    }

    private fun buildNotification(connectedCount: Int): Notification {
        val text = if (connectedCount > 0) {
            "Session active — $connectedCount appareil(s) connecté(s)"
        } else {
            "Session Mazzika en attente..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mazzika Lyrics")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Session de synchronisation",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        sessionManager?.disconnect()
        super.onDestroy()
    }
}
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/data/nearby/
git commit -m "feat: add Nearby Connections service, session manager, and sync protocol"
```

---

### Task 12: Sync Screen (Create/Join Session UI)

**Files:**
- Create: `app/src/main/java/com/mazzika/lyrics/ui/sync/SyncViewModel.kt`
- Create: `app/src/main/java/com/mazzika/lyrics/ui/sync/SyncScreen.kt`
- Modify: `app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt`

- [ ] **Step 1: Create SyncViewModel**

Create `app/src/main/java/com/mazzika/lyrics/ui/sync/SyncViewModel.kt`:

```kotlin
package com.mazzika.lyrics.ui.sync

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazzika.lyrics.MazzikaApplication
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.nearby.NearbyService
import com.mazzika.lyrics.data.nearby.NearbySessionManager
import com.mazzika.lyrics.data.nearby.SyncMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SyncRole { NONE, PILOT, FOLLOWER }

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MazzikaApplication
    private var sessionManager: NearbySessionManager? = null

    private val _role = MutableStateFlow(SyncRole.NONE)
    val role: StateFlow<SyncRole> = _role.asStateFlow()

    private val _selectedDocument = MutableStateFlow<PdfDocumentEntity?>(null)
    val selectedDocument: StateFlow<PdfDocumentEntity?> = _selectedDocument.asStateFlow()

    private val _syncFilePath = MutableStateFlow<String?>(null)
    val syncFilePath: StateFlow<String?> = _syncFilePath.asStateFlow()

    val allDocuments: StateFlow<List<PdfDocumentEntity>> = app.database.pdfDocumentDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredEndpoints: StateFlow<List<NearbySessionManager.EndpointInfo>>
        get() = sessionManager?.discoveredEndpoints
            ?: MutableStateFlow(emptyList())

    val connectedEndpoints: StateFlow<List<NearbySessionManager.EndpointInfo>>
        get() = sessionManager?.connectedEndpoints
            ?: MutableStateFlow(emptyList())

    private var serviceBound = false
    private var nearbyService: NearbyService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            nearbyService = (binder as NearbyService.LocalBinder).getService()
            sessionManager = nearbyService?.sessionManager
            setupMessageHandler()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            nearbyService = null
            sessionManager = null
        }
    }

    init {
        val intent = Intent(application, NearbyService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        serviceBound = true
    }

    private fun setupMessageHandler() {
        sessionManager?.setConnectionRequestHandler { _, _ -> true } // auto-accept

        viewModelScope.launch {
            sessionManager?.incomingMessages?.collect { pair ->
                if (pair == null) return@collect
                val (endpointId, message) = pair
                handleMessage(endpointId, message)
            }
        }

        viewModelScope.launch {
            sessionManager?.incomingFile?.collect { bytes ->
                if (bytes == null) return@collect
                val path = app.fileManager.saveTempFile(bytes, "sync_${System.currentTimeMillis()}.pdf")

                // Check auto-save preference
                val autoSave = app.userPreferences.autoSaveSync.first()
                if (autoSave) {
                    val catalogPath = app.fileManager.moveTempToCatalog(path)
                    val hash = app.fileManager.computeHash(java.io.File(catalogPath))
                    val pageCount = app.fileManager.getPageCount(java.io.File(catalogPath))
                    val thumbnail = app.fileManager.generateThumbnail(java.io.File(catalogPath), hash)
                    app.database.pdfDocumentDao().insert(
                        PdfDocumentEntity(
                            title = java.io.File(catalogPath).nameWithoutExtension,
                            fileName = java.io.File(catalogPath).name,
                            filePath = catalogPath,
                            fileHash = hash,
                            pageCount = pageCount,
                            importedAt = System.currentTimeMillis(),
                            thumbnailPath = thumbnail,
                        )
                    )
                    _syncFilePath.value = catalogPath
                } else {
                    _syncFilePath.value = path
                }
            }
        }
    }

    private suspend fun handleMessage(endpointId: String, message: SyncMessage) {
        when (message) {
            is SyncMessage.SessionInfo -> {
                // Follower received session info — check if we have the file
                val existing = app.database.pdfDocumentDao().getByHash(message.fileHash)
                if (existing != null) {
                    sessionManager?.sendMessage(endpointId, SyncMessage.AlreadyHave(message.fileHash))
                    _syncFilePath.value = existing.filePath
                } else {
                    sessionManager?.sendMessage(endpointId, SyncMessage.NeedFile(message.fileHash))
                }
            }
            is SyncMessage.NeedFile -> {
                // Pilot: send the file
                _selectedDocument.value?.let { doc ->
                    val bytes = app.fileManager.readFileBytes(doc.filePath)
                    sessionManager?.sendFile(endpointId, bytes)
                }
            }
            is SyncMessage.AlreadyHave -> {
                // Nothing to do, follower has the file
            }
            is SyncMessage.PageChange -> {
                // Handled by reader screen
            }
        }
    }

    fun startAsPilot(document: PdfDocumentEntity) {
        _role.value = SyncRole.PILOT
        _selectedDocument.value = document

        viewModelScope.launch {
            val deviceName = app.userPreferences.deviceName.first()
            sessionManager?.startAdvertising(deviceName)

            // When endpoints connect, send session info
            sessionManager?.connectedEndpoints?.collect { endpoints ->
                nearbyService?.updateNotification(endpoints.size)
                endpoints.forEach { endpoint ->
                    sessionManager?.sendMessage(
                        endpoint.id,
                        SyncMessage.SessionInfo(
                            title = document.title,
                            pageCount = document.pageCount,
                            fileHash = document.fileHash,
                        ),
                    )
                }
            }
        }
    }

    fun startAsFollower() {
        _role.value = SyncRole.FOLLOWER
        sessionManager?.startDiscovery()
    }

    fun connectToEndpoint(endpointId: String) {
        viewModelScope.launch {
            val deviceName = app.userPreferences.deviceName.first()
            sessionManager?.requestConnection(deviceName, endpointId)
        }
    }

    fun broadcastPageChange(page: Int) {
        sessionManager?.broadcastMessage(SyncMessage.PageChange(page))
    }

    fun stopSession() {
        sessionManager?.disconnect()
        _role.value = SyncRole.NONE
        _selectedDocument.value = null
        _syncFilePath.value = null
    }

    override fun onCleared() {
        if (serviceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            serviceBound = false
        }
        super.onCleared()
    }
}
```

- [ ] **Step 2: Create SyncScreen**

Create `app/src/main/java/com/mazzika/lyrics/ui/sync/SyncScreen.kt`:

```kotlin
package com.mazzika.lyrics.ui.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Broadcast
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.ui.theme.Gold
import com.mazzika.lyrics.ui.theme.GoldDark
import com.mazzika.lyrics.ui.theme.Success

@Composable
fun SyncScreen(
    onNavigateToReaderSync: (String) -> Unit,
    viewModel: SyncViewModel = viewModel(),
) {
    val role by viewModel.role.collectAsState()
    val allDocs by viewModel.allDocuments.collectAsState()
    val discovered by viewModel.discoveredEndpoints.collectAsState()
    val connected by viewModel.connectedEndpoints.collectAsState()
    val syncFilePath by viewModel.syncFilePath.collectAsState()

    // Navigate to reader when file is ready
    LaunchedEffect(syncFilePath) {
        syncFilePath?.let { path ->
            onNavigateToReaderSync(path)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("⌖", style = MaterialTheme.typography.displayLarge.copy(fontSize = MaterialTheme.typography.displayLarge.fontSize))
                Spacer(Modifier.height(12.dp))
                Text("Session de partage", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Partagez vos partitions en temps réel avec les appareils à proximité",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        when (role) {
            SyncRole.NONE -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card(
                            modifier = Modifier.weight(1f).clickable { /* show picker */ },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Default.Broadcast, null, tint = Gold, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Créer", style = MaterialTheme.typography.titleMedium)
                                Text("Devenez pilote", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f).clickable { viewModel.startAsFollower() },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Default.PhoneAndroid, null, tint = Gold, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(12.dp))
                                Text("Rejoindre", style = MaterialTheme.typography.titleMedium)
                                Text("Sessions à proximité", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                // Doc picker for pilot
                item {
                    Text("Choisir un fichier à partager", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                }
                items(allDocs) { doc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { viewModel.startAsPilot(doc) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, style = MaterialTheme.typography.bodyMedium)
                                Text("${doc.pageCount} pages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            SyncRole.PILOT -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Success.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Success, strokeWidth = 2.dp)
                            Text(
                                "  Session active — ${connected.size} appareil(s)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Success,
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.stopSession() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Stop, null)
                        Text("  Arrêter la session")
                    }
                }
            }

            SyncRole.FOLLOWER -> {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Gold, strokeWidth = 2.dp)
                        Text("  Recherche de sessions...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                items(discovered) { endpoint ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { viewModel.connectToEndpoint(endpoint.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).then(Modifier.padding(end = 12.dp)))
                            Text(endpoint.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            OutlinedButton(
                                onClick = { viewModel.connectToEndpoint(endpoint.id) },
                                shape = RoundedCornerShape(100),
                            ) { Text("Rejoindre", color = Gold) }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.stopSession() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Annuler") }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}
```

- [ ] **Step 3: Wire SyncScreen into NavGraph and add ReaderSync route**

In `NavGraph.kt`, replace the `composable(Screen.Sync.route)` block:

```kotlin
composable(Screen.Sync.route) {
    SyncScreen(
        onNavigateToReaderSync = { filePath ->
            navController.navigate(Screen.ReaderSync.createRoute(filePath))
        },
    )
}
```

Replace the `composable(Screen.ReaderSync.route)` block:

```kotlin
composable(
    route = Screen.ReaderSync.route,
    arguments = listOf(navArgument("filePath") { type = NavType.StringType }),
) { backStackEntry ->
    val filePath = backStackEntry.arguments?.getString("filePath") ?: ""
    ReaderScreen(
        onNavigateBack = { navController.popBackStack() },
        onNavigateToSync = {},
        viewModel = viewModel<ReaderViewModel>().also { vm ->
            LaunchedEffect(filePath) { vm.initWithFilePath(filePath) }
        },
    )
}
```

Add imports:
```kotlin
import com.mazzika.lyrics.ui.sync.SyncScreen
import com.mazzika.lyrics.ui.reader.ReaderViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
```

- [ ] **Step 4: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/ui/sync/ app/src/main/java/com/mazzika/lyrics/ui/navigation/NavGraph.kt
git commit -m "feat: add Sync screen with create/join session, device discovery, and file sharing"
```

---

### Task 13: Theme Integration with Preferences & Intent Handling

**Files:**
- Modify: `app/src/main/java/com/mazzika/lyrics/MainActivity.kt`

- [ ] **Step 1: Update MainActivity to use theme preference and handle PDF intents**

Replace `app/src/main/java/com/mazzika/lyrics/MainActivity.kt` with:

```kotlin
package com.mazzika.lyrics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mazzika.lyrics.data.db.entity.PdfDocumentEntity
import com.mazzika.lyrics.data.preferences.UserPreferences
import com.mazzika.lyrics.ui.navigation.BottomNavBar
import com.mazzika.lyrics.ui.navigation.NavGraph
import com.mazzika.lyrics.ui.navigation.Screen
import com.mazzika.lyrics.ui.theme.MazzikaLyricsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val app by lazy { application as MazzikaApplication }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val themeMode by app.userPreferences.theme.collectAsState(initial = UserPreferences.ThemeMode.DARK)
            val isDark = when (themeMode) {
                UserPreferences.ThemeMode.DARK -> true
                UserPreferences.ThemeMode.LIGHT -> false
                UserPreferences.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MazzikaLyricsTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Catalog.route,
                    Screen.Sync.route,
                    Screen.Settings.route,
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = slideInVertically { it },
                            exit = slideOutVertically { it },
                        ) {
                            BottomNavBar(navController)
                        }
                    },
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "application/pdf") {
            intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                lifecycleScope.launch {
                    val result = app.fileManager.importPdf(uri)
                    val title = result.fileName.removeSuffix(".pdf").removeSuffix(".PDF")
                    app.database.pdfDocumentDao().insert(
                        PdfDocumentEntity(
                            title = title,
                            fileName = result.fileName,
                            filePath = result.filePath,
                            fileHash = result.fileHash,
                            pageCount = result.pageCount,
                            importedAt = System.currentTimeMillis(),
                            thumbnailPath = result.thumbnailPath,
                        )
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/mazzika/lyrics/MainActivity.kt
git commit -m "feat: integrate theme preferences and handle incoming PDF share intents"
```

---

### Task 14: Final — .gitignore update & cleanup

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: Update .gitignore to exclude superpowers brainstorm files**

Append to `.gitignore`:

```
# Superpowers brainstorm mockups
.superpowers/
```

- [ ] **Step 2: Verify full build**

Run: `cd /Users/mac/Desktop/Dev/Mazzika/Lyrics && ./gradlew clean assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add .gitignore
git commit -m "chore: add .superpowers to gitignore"
```
