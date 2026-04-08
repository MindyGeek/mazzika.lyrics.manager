# Mazzika Lyrics — Specification

Application Android pour la troupe Mazzika Band. Permet d'importer, organiser et lire des partitions/paroles en PDF, avec synchronisation en temps réel entre appareils via Nearby Connections.

## 1. Cibles et contraintes

- **Plateformes** : smartphones Android (Galaxy S24, etc.) et tablettes Android
- **minSdk** : 24 — **targetSdk** : 36
- **Framework UI** : Jetpack Compose (Material 3)
- **Architecture** : MVVM, module unique (`app`)
- **Package** : `com.mazzika.lyrics`
- **Dépendance critique** : Google Play Services (Nearby Connections)

## 2. Fonctionnalites

### 2.1 Import de fichiers

- Import via le file picker Android natif (Storage Access Framework)
- Formats supportés : **PDF** (principal), **ChordPro** (.cho/.chopro) (secondaire)
- Sources : stockage local, Google Drive, Dropbox (automatique via SAF si les apps sont installées)
- Réception depuis d'autres apps via Intent "Partager vers Mazzika Lyrics"
- A l'import : le fichier est copié dans le stockage interne (`filesDir/pdfs/`), une miniature est générée depuis la première page

### 2.2 Catalogue

- Liste centrale de tous les fichiers importés
- Chaque entrée affiche : miniature, titre, nombre de pages, date d'import
- Recherche par titre
- Filtres : Récents, A-Z
- Actions contextuelles (long press / menu 3 points) : supprimer, ajouter à un dossier

### 2.3 Dossiers

- Système de dossiers imbriqués (sous-dossiers illimités)
- Les fichiers sont référencés (pas copiés) — un même fichier peut apparaître dans plusieurs dossiers
- Opérations : créer, renommer, supprimer un dossier
- Icône personnalisable par dossier (optionnel) : l'utilisateur peut choisir une icône parmi une sélection prédéfinie (emoji/icônes Material). Si non définie, une icône dossier par défaut est utilisée
- Ordonnancement des fichiers dans un dossier via `sortOrder`
- Ecran d'accueil : dossiers en scroll horizontal (smartphone) ou grille (tablette)

### 2.4 Lecteur PDF

- **Plein écran immersif** : barres système masquées, bottom nav masquée
- **Navigation** : swipe horizontal avec animation page flip (curl effect, style livre)
- **Zoom** : pinch-to-zoom libre (pan + zoom via `TransformableState`), double-tap pour reset 1x
- **Conflit gestes** : quand zoomé, le swipe de page est désactivé — dézoomer d'abord pour changer de page
- **Cache** : 3 pages en mémoire (courante + précédente + suivante)
- **Rendu** : `PdfRenderer` natif Android
- **Barre d'outils** : tap au centre pour afficher/masquer — titre du document, bouton fermer, bouton session sync, indicateur de page

### 2.5 Synchronisation (Nearby Connections)

#### Technologie

- **Google Nearby Connections API** (Play Services)
- Communication P2P : Bluetooth + Wi-Fi Direct (automatique, transparent)
- Aucun réseau internet ou routeur requis
- Supporte 2-10 appareils simultanés

#### Rôles

- **Pilote (Advertiser)** : crée une session, partage un fichier, contrôle la navigation
- **Suiveur (Discoverer)** : découvre les sessions, rejoint, reçoit le fichier et les changements de page

#### Protocole de messages

| Type | Payload | Direction |
|------|---------|-----------|
| `SESSION_INFO` | `{ title, pageCount, fileHash }` | Pilote → Suiveurs |
| `ALREADY_HAVE` | `{ fileHash }` | Suiveur → Pilote |
| `NEED_FILE` | `{ fileHash }` | Suiveur → Pilote |
| `FILE_TRANSFER` | Fichier PDF (bytes) | Pilote → Suiveurs |
| `PAGE_CHANGE` | `{ page: Int }` | Pilote → Suiveurs |

#### Transfert intelligent

- A la connexion, le pilote envoie `SESSION_INFO` avec le hash SHA-256 du fichier
- Le suiveur vérifie si le hash existe dans son catalogue
- Si oui : pas de transfert, utilise le fichier local
- Si non : le pilote envoie le fichier complet

#### Fichiers reçus

- Par défaut : stockés en tant que fichier **temporaire** (pas dans le catalogue)
- Bouton "Sauvegarder dans mon catalogue" dans le lecteur
- Paramètre configurable : "Sauvegarde auto des fichiers partagés" (défaut : désactivé)

#### Synchronisation

- Seul le **numéro de page** est synchronisé (pas le zoom ni la position dans la page)
- Chaque suiveur contrôle son propre zoom indépendamment

#### Mode détaché

- Le suiveur peut activer "Navigation libre" : ignore les `PAGE_CHANGE` du pilote
- Bouton "Re-synchroniser" pour revenir à la page du pilote
- La connexion Nearby reste active dans les deux cas

#### Service Android

- `Foreground Service` pour maintenir la session active en arrière-plan
- Notification persistante : "Session Mazzika active — X appareils connectés"

## 3. Modèle de données (Room)

### PdfDocument
| Champ | Type | Description |
|-------|------|-------------|
| id | Long (PK, auto) | Identifiant unique |
| title | String | Titre du document |
| fileName | String | Nom du fichier original |
| filePath | String | Chemin dans le stockage interne |
| fileHash | String | SHA-256 du fichier (pour sync) |
| pageCount | Int | Nombre de pages |
| importedAt | Long | Timestamp d'import |
| thumbnailPath | String | Chemin de la miniature |

### Folder
| Champ | Type | Description |
|-------|------|-------------|
| id | Long (PK, auto) | Identifiant unique |
| name | String | Nom du dossier |
| icon | String? | Emoji ou identifiant d'icône (null = icône par défaut) |
| parentFolderId | Long? | Dossier parent (null = racine) |
| createdAt | Long | Timestamp de création |

### FolderDocumentRef
| Champ | Type | Description |
|-------|------|-------------|
| folderId | Long (FK) | Référence au dossier |
| documentId | Long (FK) | Référence au document |
| sortOrder | Int | Ordre dans le dossier |

Clé primaire composite : (folderId, documentId)

## 4. Structure du projet

```
com.mazzika.lyrics/
├── data/
│   ├── db/              → Room (entities, DAOs, MazzikaDatabase)
│   ├── file/            → FileManager (import, copie, miniature, hash)
│   └── nearby/          → NearbySessionManager, NearbyService (Foreground)
├── ui/
│   ├── catalog/         → CatalogScreen, CatalogViewModel
│   ├── folders/         → FoldersScreen, FolderDetailScreen, FoldersViewModel
│   ├── reader/          → ReaderScreen, ReaderViewModel, PageFlipAnimation
│   ├── sync/            → SyncScreen, SyncViewModel
│   ├── settings/        → SettingsScreen, SettingsViewModel
│   ├── navigation/      → NavGraph, BottomNavBar
│   └── theme/           → Color, Theme, Type (charte Mazzika)
├── di/                  → Dépendances (si Hilt utilisé)
└── MainActivity.kt
```

## 5. Navigation

```
BottomNav:
├── Accueil (dossiers + récents)
├── Catalogue (tous les fichiers)
├── Session (sync Nearby)
└── Paramètres

Hors BottomNav (plein écran) :
├── Lecteur PDF
└── Lecteur PDF (mode sync)
```

- Navigation via Compose Navigation Component
- Le lecteur PDF masque la bottom nav et les barres système

## 6. Thème visuel

Inspiré du logo Mazzika Band (noir et or, style art déco raffiné).

### Thème sombre
| Élément | Couleur |
|---------|---------|
| Fond principal | `#060606` |
| Surface/cartes | `#141414` |
| Accent primaire (or) | `#C5A028` |
| Accent clair | `#E8D48B` |
| Accent foncé | `#8B7620` |
| Texte principal | `#F2EFE6` |
| Texte secondaire | `#8A8478` |
| Texte muted | `#5A5650` |

### Thème clair
| Élément | Couleur |
|---------|---------|
| Fond principal | `#FAFAF5` |
| Surface/cartes | `#FFFFFF` |
| Accent primaire (or) | `#C5A028` |
| Accent foncé | `#8B7620` |
| Texte principal | `#1A1510` |
| Texte secondaire | `#6B6560` |

### Typographie
- **Titres** : Playfair Display (serif, bold) — ou équivalent serif sur Android
- **Sous-titres** : Cormorant Garamond (serif, élégant)
- **Corps** : Outfit (sans-serif, léger)

### Composants
- Boutons principaux : dégradé or, texte noir, border-radius 16px
- Cartes : fond surface, bordure fine dorée subtile, hover avec accent
- FAB : dégradé or, border-radius 16px (Material 3), ombre dorée
- Bottom nav : fond semi-transparent avec backdrop blur, icônes + labels
- Toggles : piste or quand activé

## 7. Responsive (smartphone / tablette)

| Élément | Smartphone (< 600dp) | Tablette (>= 600dp) |
|---------|----------------------|---------------------|
| Dossiers | Scroll horizontal | Grille |
| Catalogue | Liste simple | Grille 2 colonnes |
| Appareils sync | Liste | Grille 2 colonnes |
| PDF page max-width | 460px | 560px |
| Bottom nav | Pleine largeur | Centrée max 800px |

## 8. Paramètres

Stockage : **DataStore Preferences**

| Paramètre | Type | Défaut | Description |
|-----------|------|--------|-------------|
| Thème | Enum (Clair/Sombre/Système) | Sombre | Apparence de l'app |
| Sauvegarde auto sync | Boolean | false | Auto-ajout au catalogue des fichiers reçus |
| Nom de l'appareil | String | Nom du device Android | Affiché lors de la découverte Nearby |

## 9. Dépendances principales

| Dépendance | Usage |
|------------|-------|
| Jetpack Compose + Material 3 | UI |
| Compose Navigation | Navigation |
| Room | Base de données locale |
| DataStore Preferences | Paramètres |
| Google Play Services Nearby | Communication P2P |
| Kotlin Coroutines + Flow | Async + reactive |
| PdfRenderer (android.graphics.pdf) | Rendu PDF (natif, pas de lib externe) |

## 10. Maquettes

Les maquettes interactives validées sont disponibles dans :
`.superpowers/brainstorm/mockups/index.html`

Elles incluent les 6 écrans (Accueil, Catalogue, Lecteur, Lecteur Sync, Session, Paramètres) en mode smartphone et tablette.

## 11. Pistes d'amélioration futures

Ces fonctionnalités sont hors scope V1 mais documentées pour référence :

- **Support ChordPro avancé** : rendu natif des fichiers .cho avec transposition automatique des accords (changement de tonalité)
- **Setlists** : listes ordonnées de morceaux avec enchaînement automatique (le lecteur passe au fichier suivant de la setlist)
- **Annotations** : possibilité de dessiner/écrire sur le PDF (surligner, annoter des notes personnelles) avec sauvegarde par couche
- **Metronome intégré** : métronome visuel/sonore synchronisé avec la session, le pilote définit le tempo
- **Auto-scroll** : défilement automatique à vitesse réglable pour les fichiers longs (utile pour les tablatures)
- **Export/backup** : export du catalogue complet (fichiers + structure dossiers) pour migration vers un autre appareil
- **Support multi-format** : MusicXML, Guitar Pro (.gp), images de partitions (JPG/PNG)
- **Mode performance** : écran simplifié avec contraste maximal, gros texte, navigation minimale — optimisé pour la scène
- **Historique de sessions** : log des sessions passées (date, fichiers partagés, participants)
- **Transfert du rôle pilote** : le pilote peut passer le contrôle à un autre appareil en cours de session
