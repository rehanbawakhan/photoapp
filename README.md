# PhotoApp 📸

[![Platform](https://img.shields.io/badge/platform-Android-3DDC84.svg?style=flat-square&logo=android)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.0-7F52FF.svg?style=flat-square&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-BOM-F25F5C.svg?style=flat-square&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Licence](https://img.shields.io/badge/licence-MIT-blue.svg?style=flat-square)](LICENSE)

A sleek, state-of-the-art, AMOLED-optimized gallery application for Android devices. PhotoApp combines raw speed, beautiful layouts, and offline AI processing to provide a modern, premium experience for organizing and editing device media.

---

## ✨ Features

- 🧠 **On-Device AI Photo Editor**: Built with Google ML Kit. Perform fully offline, private image segmentations:
  - **Portrait Mode**: Isolate subjects and add custom Gaussian blur to backgrounds.
  - **Background Eraser**: Cut out subjects and export transparent PNGs directly to the device.
  - **Color Pop**: Keep subjects in vivid color while converting the background to black-and-white.
  - **Auto-Enhance**: One-tap histogram adjustment for brightness, contrast, and levels.
- ⚡ **Dynamic Grid Zooming**: Pinch-to-zoom gestures dynamically scale the photos grid from **2** up to **8** columns with persistent state preservation.
- 📁 **Smart Folder & Album Manager**: Easily organize, browse, and edit local device folders (Downloads, Camera, Screenshots). Includes a robust soft-delete **Trash** system.
- 🔍 **Google Lens Integration**: Instantly extract text or scan visual elements from images in the full-screen photo viewer.
- 🏷️ **Advanced Media Badging**: Under-the-hood `MediaFormatAnalyzer` probes file headers asynchronously to show precise parameters:
  - HDR classification (Dolby Vision, HDR10, HLG).
  - High resolution badges (8K Ultra, 4K UHD, 1080p Full HD, etc.).
  - Video framerate parsing and Megapixel counts.
- 🎬 **Integrated Video Player**: Seamless ExoPlayer playback wrapper with swiping constraints that filter out photos when entering from the video feed.
- 🎨 **Accent & AMOLED Theme Engine**: Configurable system, light, dark, and pure AMOLED Black themes, supporting customizable accents (Red, Purple, Sky Blue, Deep Blue, White).

---

## 🏗️ Architecture & Folder Structure

PhotoApp follows clean architecture principles utilizing MVVM, reactive programming with flows, and dependency injection:

```text
com.photoapp/
├── PhotoApp.kt                # Application class initializing Hilt and repositories
├── MainActivity.kt            # Core Activity managing edge-to-edge layout & reactive permission states
├── data/
│   ├── local/                 # Room database cache (SQLite configuration, entities, and DAOs)
│   ├── media/                 # MediaStoreManager scanning and observing Android MediaStore API
│   ├── repository/            # Unified data mapping and file operations (PhotoRepository)
│   └── settings/              # ThemeSettingsManager managing configurations via SharedPreferences
├── di/
│   └── AppModule.kt           # Dagger Hilt dependency injection modules
├── navigation/
│   └── AppNavigation.kt       # NavHost configuration, transitions, and query arguments routing
├── ui/                        # Jetpack Compose UI layout layers (Gallery, Videos, Albums, Settings, Viewer, Editor)
│   └── theme/                 # HSL tailored theme palettes, AMOLED presets, and typography tokens
└── util/                      # Offline AI editor engine (ML Kit), Date/Image formats, and file header parsers
```

---

## 🛠️ Tech Stack & Dependencies

- **Jetpack Compose**: Declarative UI layout library.
- **Kotlin Coroutines & Flow**: Thread-safe asynchronous event streaming.
- **Dagger Hilt**: Injecting modular repositories and ViewModels.
- **Room database**: Offline SQLite database engine mapping local sync files.
- **Coil (with Video Extension)**: High-efficiency image and video frame decoding.
- **Jetpack Media3 (ExoPlayer)**: Custom video player control and playback buffer setup.
- **Google ML Kit (Subject Segmentation)**: AI segmentation maps processed strictly on-device.

---

## 🚀 Getting Started

### Prerequisites
- JDK 17
- Android Studio Koala (or newer)
- Android device or emulator running SDK level 26 (Android 8.0) or higher

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/rehanbawakhan/photoapp.git
   ```
2. Open the project in Android Studio.
3. Build the project using Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
4. Run the application on an active emulator or connected device.

---

## 👤 Developer

**Rehan Bawakhan**
- GitHub: [@rehanbawakhan](https://github.com/rehanbawakhan)
- LinkedIn: [Rehan Bawakhan](https://www.linkedin.com/in/rehan-bawakhan-249149306/)

---

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
