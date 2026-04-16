# Mazzika Lyrics — Refonte UX (Sync, Navigation, Import, Lecteur)

Refonte ergonomique de l'application pour améliorer la navigation, le flux de synchronisation, l'import de fichiers et l'expérience du lecteur PDF.

## 1. Principes généraux

### 1.1 Règle d'import universelle
Tout fichier importé depuis n'importe quelle section de l'app est **automatiquement ajouté au catalogue**. Si un fichier avec le même hash SHA-256 existe déjà dans le catalogue, l'import est ignoré silencieusement.

### 1.2 Comparaison de fichiers
La comparaison entre fichiers se base **exclusivement sur le hash SHA-256 du contenu**, jamais sur le nom du fichier.

## 2. Header fixe (Top App Bar Material 3)

### Comportement
- **Toutes les pages** ont un header fixe en haut de l'écran
- **Tabs principaux** (Accueil, Catalogue, Sync, Paramètres) : affichent le titre de la page
- **Écrans de détail** (Dossier, sous-dossier) : affichent un bouton retour (←) + titre de la page
- Le header reste visible même au scroll du contenu

### Bandeau de session persistant
- Affiché **juste sous le header**, sur **toutes les pages** tant qu'une session est active (pilote ou suiveur)
- Contenu : icône statut + nom de la session + nombre de connectés
- **Couleur verte** : session active et connectée
- **Couleur rouge** : connexion perdue (problème Wi-Fi, Bluetooth) — l'utilisateur est averti visuellement
- **Au tap** : redirige vers le lecteur du document de la session en cours
- **Disparaît** automatiquement quand la session se termine

## 3. Page Sync — Refonte complète

### 3.1 État initial (aucune session)
Afficher uniquement 2 boutons :
- **"Créer une session"** — icône soignée (la même que sur l'accueil)
- **"Rejoindre une session"** — icône soignée (la même que sur l'accueil)

Rien d'autre. Pas de liste de fichiers, pas de picker.

### 3.2 Création de session — Popup stepper (Pilote)

Accessible depuis **n'importe quelle page** (accueil, sync, etc.). La popup s'ouvre par-dessus la page courante.

**Étape 1 — Nom de la session :**
- Input texte avec un nom auto-généré par défaut (ex: "Session de [Nom appareil]")
- L'utilisateur peut le modifier
- Ce nom sera visible par les suiveurs lors de la découverte

**Étape 2 — Choix du fichier :**
- 2 onglets :
  - **Catalogue** : liste de tous les fichiers du catalogue
  - **Dossiers** : arborescence des dossiers pour naviguer et choisir un fichier

**Étape 3 — Démarrage :**
- Bouton "Démarrer la session" **désactivé** tant que les étapes 1 et 2 ne sont pas complétées
- Au clic :
  - La popup se ferme
  - La session démarre (advertising Nearby Connections)
  - L'utilisateur **reste sur la page** depuis laquelle il a lancé la session
  - Le bandeau de session apparaît sous le header

### 3.3 Page Sync — Pilote (session en cours)

Affiche :
1. Statut de la session (indicateur visuel vert)
2. Nom du fichier diffusé
3. Liste et nombre de périphériques connectés
4. Bouton **"Ouvrir le lecteur"**
5. Bouton **"Arrêter la diffusion"**

**Arrêt de la diffusion :**
- Les suiveurs sont **avertis** que la session est terminée
- Les suiveurs qui ont encore le lecteur ouvert voient un **dialog modal** avec :
  - **"Sauvegarder le fichier"** (visible uniquement si le fichier n'existe pas dans le catalogue du suiveur)
  - **"Fermer"** (bouton X)

### 3.4 Page Sync — Suiveur (session en cours)

Affiche :
1. Nom et statut de la session
2. Nom du fichier diffusé
3. Nombre de périphériques connectés
4. Bouton **"Ouvrir le lecteur"**
5. Bouton **"Quitter la session"**

**Quitter la session :**
- Le suiveur quitte proprement
- Les informations côté pilote et autres suiveurs sont actualisées (nom, nombre de connectés)

### 3.5 Rejoindre une session — Popup (Suiveur)

Au clic sur "Rejoindre une session" :

1. **Popup s'ouvre** avec un bloc de chargement (recherche en cours)
2. La recherche dure **15 secondes maximum**
3. **Si aucune session trouvée** après 15 secondes :
   - Stopper la recherche
   - Afficher message "Aucune session trouvée"
   - Afficher bouton **"Relancer la recherche"**
4. **Bouton "Relancer"** :
   - **Visible mais désactivé (grisé)** pendant la recherche
   - **Activé** quand le timeout expire ou quand au moins une session est trouvée
   - Au clic : réinitialise la liste des sessions, relance la recherche, le bouton redevient désactivé
5. **Session trouvée** : affichée dans la liste avec un bouton "Rejoindre"
6. **Au clic "Rejoindre"** : affichage des étapes d'adhésion :
   - **Étape 1** : Barre de progression dynamique du transfert de fichier (progression en %)
   - **Étape 2** : Transfert terminé → le fichier s'ouvre automatiquement dans le lecteur

## 4. Lecteur PDF — Modifications

### 4.1 Bouton fermer
- **Supprimer** le bouton retour (←) en haut à gauche de la toolbar
- **Ajouter** un pill button **"Fermer"** (avec icône X intégrée) centré horizontalement, positionné en bas de l'écran (avec espacement par rapport au bord de l'appareil)
- Visible quand la toolbar est affichée (tap simple sur l'écran)
- Au clic : ferme le lecteur et redirige vers la page précédente

### 4.2 Toolbar — Informations de session
Quand une session est en cours, la toolbar (affichée au tap) montre :
- **Pilote** : icône colorée du statut de connexion + nombre d'utilisateurs connectés
- **Suiveur** : icône colorée du statut de connexion + nombre d'utilisateurs connectés

Couleurs de l'icône statut :
- **Vert** : connecté
- **Orange** : connexion instable
- **Rouge** : connexion perdue

## 5. Page d'accueil — Modifications

### 5.1 Cartes d'action rapide
Remplacer les chips actuels par 3 **cartes avec icônes** :
- **"Créer une session"** — même icône que sur la page Sync
- **"Rejoindre"** — même icône que sur la page Sync
- **"Importer"** — icône d'import

Au clic sur "Créer une session" ou "Rejoindre" : ouvre la même popup stepper / popup de recherche que depuis la page Sync.

### 5.2 Modale d'import
Au clic sur "Importer" :
- **Popup modale** avec 2 cartes :
  - **"Importer dans le catalogue"** : ouvre le file picker, fichier importé dans le catalogue uniquement
  - **"Importer dans un dossier"** : ouvre le file picker, puis sélecteur de dossier

### 5.3 Sélecteur de dossier (grille)
Utilisé lors de l'import dans un dossier :
- Affiche les dossiers en **grille**
- **Tap sur un dossier sans sous-dossiers** → sélectionné directement
- **Tap sur un dossier avec sous-dossiers** :
  - Les sous-dossiers s'affichent
  - Le dossier parent reste sélectionné par défaut
  - Si on tape un sous-dossier, le même principe se poursuit récursivement
- **Fil d'Ariane** en haut pour naviguer rapidement dans l'arborescence
- Bouton de confirmation pour valider le dossier sélectionné

### 5.4 Section "Mes Dossiers"
- **Supprimer le FAB "+"** de la page d'accueil
- **Ajouter une carte "Nouveau dossier"** en **première position permanente** dans la liste/grille des dossiers
- La carte a le **même style visuel** que les cartes de dossier (même taille, même forme, même bordure)
- Contenu : icône "+" (ou icône dossier+) — avec ou sans texte "Nouveau" selon l'espace disponible
- Cette carte est **toujours visible** en première position, même quand il y a beaucoup de dossiers
- Au clic : ouvre la popup de création de dossier (nom + sélecteur d'icône emoji)

## 6. Page Dossier — FAB Menu expandable

Remplacer le FAB simple "+" par un **FAB expandable** avec 3 options :

| Option | Icône | Comportement |
|--------|-------|-------------|
| Nouveau dossier | Icône dossier+ | Ouvre la popup de création de sous-dossier |
| Importer fichier | Icône upload/import | Ouvre le file picker. Le fichier est importé dans le catalogue ET ajouté au dossier courant |
| Copier depuis le catalogue | Icône copie/lien | Affiche la liste des fichiers du catalogue pour en sélectionner un à ajouter au dossier courant |

## 7. Protocole de messages — Ajouts

Nouveaux messages pour la gestion de session :

| Type | Payload | Direction |
|------|---------|-----------|
| `SESSION_END` | `{ reason: String }` | Pilote → Suiveurs |
| `FOLLOWER_LEFT` | `{ endpointId: String }` | Système (via déconnexion Nearby) |
| `TRANSFER_PROGRESS` | `{ bytesTransferred: Long, totalBytes: Long }` | Pilote → Suiveur (via PayloadTransferUpdate) |

## 8. Éléments visuels

### Icônes
- Les icônes "Créer session" et "Rejoindre" sont **identiques** entre la page d'accueil et la page Sync
- Les icônes doivent être soignées et élégantes (Material Icons Extended ou custom SVG)
- Chaque option du FAB menu a une icône distincte et explicite

### Bandeau de session
- Hauteur compacte (~48dp)
- Fond coloré selon le statut (vert/rouge)
- Texte blanc pour lisibilité
- Icône statut à gauche + texte au centre + indicateur connectés à droite
- Animation d'entrée/sortie (slide down/up)

### Pill button "Fermer" (lecteur)
- Forme capsule avec coin arrondis
- Icône X + texte "Fermer"
- Fond semi-transparent
- Centré horizontalement, positionné ~60dp au-dessus du bord inférieur
