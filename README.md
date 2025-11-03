# 🎸 Scale Finder

A modern Android application for guitarists to visualize scales, build chord progressions, and discover which scales work with their chords. Built with Jetpack Compose and Material Design 3.

## 📱 Features

### 🎯 Core Functionality
- **Interactive Fretboard Visualization**: Visualize scales and chords on a customizable guitar fretboard
- **Chord Progression Builder**: Create and manage chord progressions with an intuitive interface
- **Intelligent Scale Suggestions**: Automatically suggests compatible scales based on your chord progressions
- **Multiple Tunings**: Support for Standard, Drop D, and other guitar tunings
- **Note Names Display**: Toggle to show/hide note names (C, D, E, etc.) on the fretboard for learning

### 🎵 Audio Features
- **Chord Playback**: Listen to chords you build
- **Arpeggio Player**: Play chord arpeggios with smooth, click-free audio
- **Metronome**: Built-in metronome with adjustable BPM and time signature
- **Note Preview**: Tap any note on the fretboard to hear it

### 🎨 Customization
- **Dark/Light Theme**: Switch between themes with smooth animations
- **High Contrast Mode**: Enhanced visibility for better accessibility
- **Fretboard Inversion**: Flip the fretboard orientation to match your preference
- **Fret Range Control**: Adjust start fret and fret count for focused practice

### 📚 Educational Tools
- **Scale Types**: Support for major scales, minor scales, pentatonic scales, modes (Dorian, Phrygian, Lydian, Mixolydian, Aeolian, Locrian), and blues scales
- **Chord Qualities**: Major, minor, 7th chords (dominant, major, minor), diminished, and half-diminished
- **Quick Presets**: One-tap access to common progressions (I–V–vi–IV, ii–V–I, 12-bar blues) and fret positions

## 🛠️ Technology Stack

### Core Technologies
- **Kotlin**: Modern, concise programming language
- **Jetpack Compose**: Declarative UI framework
- **Material Design 3**: Modern design system with dynamic theming
- **MVVM Architecture**: Clean separation of concerns with ViewModel pattern

### Key Libraries
- **Compose Animation**: Smooth animations throughout the UI
- **AudioTrack**: High-quality audio playback for chords and notes
- **Coroutines**: Asynchronous operations and state management

## 📦 Building the Project

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 11 or higher
- Android SDK with API level 24+ (Android 7.0 Nougat)

### Build Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/ScaleFinder.git
   cd ScaleFinder
   ```

2. Open the project in Android Studio

3. Sync Gradle files (Android Studio will do this automatically)

4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```
   Or use Android Studio's Run button

### Build Variants
- **Debug**: Development build with debug symbols and `.debug` suffix
- **Release**: Optimized build with ProGuard/R8 for production

## 🏗️ Project Structure

```
app/src/main/
├── java/com/lddev/scalefinder/
│   ├── MainActivity.kt              # Application entry point
│   ├── model/
│   │   └── Theory.kt                # Music theory models (Note, Chord, Scale, Tuning)
│   ├── ui/
│   │   ├── HomeViewModel.kt         # Business logic and state management
│   │   ├── screens/
│   │   │   └── HomeScreen.kt        # Main UI screen
│   │   └── components/
│   │       └── Fretboard.kt         # Fretboard visualization component
│   └── audio/
│       ├── ChordPlayer.kt           # Chord and arpeggio playback
│       ├── NotePlayer.kt            # Individual note playback
│       └── Metronome.kt             # Metronome functionality
└── res/
    └── values/
        └── strings.xml              # Localized strings
```

## 🎯 Usage Guide

### Creating a Chord Progression
1. Use the chord picker to select a root note and chord quality
2. Tap "Add Chord" to add it to your progression
3. Reorder chords by using the left/right arrow buttons
4. Select a chord to highlight its notes on the fretboard

### Finding Compatible Scales
1. Build a chord progression (add 2+ chords)
2. View the "Scale Suggestions" section
3. Tap "Show on Neck" to visualize a suggested scale on the fretboard
4. Scales are ranked by compatibility with your progression

### Using the Metronome
1. Tap the play button to start/stop
2. Expand the metronome to adjust BPM and time signature
3. Use the stepper controls to fine-tune timing

### Customizing the Fretboard
- Access settings via the menu (⋮) in the top-right
- Toggle note names on/off for learning
- Adjust start fret and fret count for focused practice
- Switch between tunings (Standard, Drop D, etc.)
- Enable high contrast mode for better visibility

## 🎨 Design Philosophy

The app follows Material Design 3 principles with:
- **Smooth Animations**: Spring-based animations for natural feel
- **Accessibility**: Content descriptions, high contrast mode, and clear visual hierarchy
- **Modern UI**: Clean, icon-based interface with intuitive gestures
- **Responsive**: Adapts to different screen sizes and orientations

## 📱 Screenshots

*Add screenshots of your app here to showcase the UI*

## 🔧 Development

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Keep functions focused and single-purpose
- Document complex algorithms

### Adding New Features
1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes and test thoroughly
3. Ensure all strings are in `strings.xml` for localization
4. Submit a pull request with clear description

## 📄 License

*Add your license here (MIT, Apache 2.0, etc.)*

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 👨‍💻 Author

*Add your name and contact information*

## 🙏 Acknowledgments

- Built with love for the guitar community
- Inspired by the need for better music theory visualization tools

---

**Made with ❤️ using Jetpack Compose**

