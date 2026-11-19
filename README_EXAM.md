# Préparation DS — Évolution d’après un cahier des charges

Guide pour coder une évolution sur l’application **M2TmdbLuque** (Android / Kotlin) à partir d’un CdC donné.

---

## 1. Carte de l’application

| Zone | Fichiers clés | Rôle |
|------|---------------|------|
| Liste + recherche TMDB | `MainActivity`, `PersonAdapter`, `TmdbApi` | RecyclerView, pagination, offline banner |
| Détail personne | `PersonDetailActivity`, `KnownForAdapter` | Intent + extras, DataBinding |
| Social (Room) | `SocialBarFragment` → `ViewModel` → `Repository` → `Dao` → `Entity` | Like / favori persistés |
| Capteurs | `SensorDemoActivity`, `SensorListAdapter`, `SensorDemoView` | SensorManager + View custom |
| Views custom | `ScoreGaugeView`, `SensorDemoView` | Canvas / attrs XML |
| Config | `AndroidManifest`, `res/layout` (+ `layout-land`), `strings.xml` | Déclarations + UI + i18n |

**Principe exam :** tu ne réinventes pas — tu **branches** sur un de ces patterns existants.

---

## 2. Méthode en 5 minutes au début du sujet

1. **Lire le CdC en soulignant les verbes** : afficher, filtrer, persister, naviguer, écouter un capteur, custom view, landscape, strings…
2. **Traduire chaque exigence → type technique** (tableau ci-dessous).
3. **Lister les fichiers à toucher** avant de coder.
4. **Coder le squelette d’abord** (layout vide + Activity qui s’ouvre), puis remplir.
5. **Tester au fur et à mesure** : navigation → UI → logique → persistence.

### Traduction CdC → technique

| Phrase CdC typique | Où aller |
|--------------------|----------|
| « Nouvelle écran / activité » | `Activity` + layout + **Manifest** |
| « Liste d’éléments » | `RecyclerView` + Adapter (copie `PersonAdapter` / `SensorListAdapter`) |
| « Au clic ouvrir… » | `Intent` + extras (comme `PersonDetailActivity`) |
| « Données API » | `TmdbApi` + modèle Gson + callback Retrofit |
| « Mémoriser / favoris » | Room : Entity → Dao → Repo → ViewModel |
| « Jauge / cadran / dessin » | Custom `View` + `attrs_*.xml` |
| « Capteur / accéléro » | `SensorManager` + cycle `onResume` / `onPause` |
| « Portrait et paysage » | `layout/` **et** `layout-land/` |
| « Texte FR/EN » | `values/strings.xml` + `values-en/` |

---

## 3. Checklist « évolution » (ordre sûr)

Pour **toute** nouvelle feature :

```
1. strings.xml (+ en si demandé)
2. layout XML (+ land si demandé)
3. Kotlin (Activity / Fragment / Adapter / View)
4. AndroidManifest si nouvelle Activity
5. build.gradle seulement si nouvelle lib (rare en DS)
```

### Exemple mental

> « Ajouter un écran Favoris listant les personnes likées »

1. Room : déjà `SocialBarEntity` → lire via Dao les `isFavorite`
2. Nouvelle `FavoritesActivity` + `activity_favorites.xml`
3. Adapter (copie allégée de `PersonAdapter`)
4. Déclarer dans le Manifest, bouton depuis `MainActivity`
5. Intent au clic → `PersonDetailActivity` (déjà prêt)

Tu **réutilises** la chaîne Room existante ; tu n’ajoutes que l’UI + le lancement.

---

## 4. Patterns à maîtriser (les plus demandés)

### A. Ouvrir une Activity

Copie le schéma `MainActivity` → `SensorDemoActivity` / `PersonDetailActivity`  
(toolbar, binding, insets).

### B. RecyclerView

Liste + click listener (`OnSensorItemClickListener`) = modèle exam parfait.

### C. Cycle de vie capteurs

- `registerListener` dans `onResume`
- `unregisterListener` dans `onPause`

Classique noté — ne pas oublier le unregister.

### D. Room (favoris / likes)

Toujours respecter la chaîne :

```
Entity → Dao → Repository → ViewModel → Fragment / Activity
```

Ne saute pas d’étage.

### E. Custom View

Regarde `SensorDemoView` / `ScoreGaugeView` :

- constructeur `@JvmOverloads`
- attrs XML
- `onDraw`
- `invalidate()`

### F. Intent + extras

Comme `PERSON_ID_EXTRA_KEY` :

- clé constante
- `putExtra` / `getExtra`
- `Parcelable` si objet

---

## 5. Pièges fréquents en DS

- Oublier l’Activity dans le **Manifest**
- Hardcoder du texte → toujours `@string/...`
- Modifier seulement `layout/` et pas `layout-land/`
- Capteur sans `unregisterListener` → fuite / crash
- Room : oublier d’incrémenter la version DB si tu changes l’Entity
- Sortir des conventions du projet (ViewBinding / DataBinding déjà activés)

---

## 6. Entraînement sur ce repo

Prends 3 mini-CdC et chronomètre-toi (30–45 min) :

1. Filtrer les capteurs par type (accel / gyro) avec un Spinner  
2. Écran « Favoris » depuis Room  
3. Nouvelle jauge ou info affichée sur `PersonDetailActivity`

Pour chaque exercice :

1. Écrire la **liste de fichiers** à modifier  
2. Puis coder  

---

## Structure rapide des packages

```
com.example.m2tmdbluque/
├── MainActivity.kt
├── PersonDetailActivity.kt
├── SensorDemoActivity.kt
├── SensorListAdapter.kt
├── SensorDemoView.kt
├── TmdbApplication.kt
├── api/          → TmdbApi, TmdbClient
├── model/        → Person, KnownFor, PopularPeopleResponse
├── data/db/      → Room (Entity, Dao, Repository, Database)
├── ui/           → Adapters, Fragment, ViewModel, Views custom
└── util/         → NetworkMonitor
```

Ressources UI : `app/src/main/res/layout/`, `layout-land/`, `values/`, `values-en/`, `drawable/`.
