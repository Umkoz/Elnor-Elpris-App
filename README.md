# Elpris Compose Android App

## Förutsättningar

Innan du öppnar projektet i Android Studio, säkerställ följande:

### 1. Android Studio
- Android Studio Hedgehog (2023.1.1) eller nyare
- JDK 17 (kommer med Android Studio)

### 2. SDK-krav
- Android SDK 34 (compileSdk)
- Android SDK 24 eller högre (minSdk)

### 3. Filer som skapas automatiskt
När du öppnar projektet i Android Studio kommer följande att skapas automatiskt:

- `local.properties` - Sökväg till Android SDK
- `gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper JAR-fil
- `.gradle/` - Gradle cache
- `.idea/` - Android Studio projektfiler
- `app/build/` - Byggda filer

## Kom igång

1. Öppna Android Studio
2. Välj "Open" och navigera till `android_app`-mappen
3. Vänta medan Android Studio synkroniserar Gradle
4. Om du får fel om saknad SDK, gå till File > Project Structure > SDK Location
5. Kör appen på en emulator eller fysisk enhet

## Vanliga problem och lösningar

### "SDK location not found"
Android Studio skapar `local.properties` automatiskt. Om det inte fungerar, skapa filen manuellt:
```properties
sdk.dir=/path/to/your/Android/Sdk
```

### "Gradle wrapper not found"
Android Studio laddar ner gradle-wrapper.jar automatiskt vid första synkroniseringen.

### "Could not resolve compose-bom"
Kontrollera din internetanslutning och att Google Maven repository är tillgängligt.

## Projektstruktur

```
android_app/
├── gradle.properties          # Gradle-inställningar
├── gradle/wrapper/
│   └── gradle-wrapper.properties  # Gradle version
├── gradlew                    # Unix build-script
├── gradlew.bat               # Windows build-script
├── build.gradle              # Root build-konfiguration
├── settings.gradle           # Projektinställningar
└── app/
    ├── build.gradle          # App build-konfiguration
    ├── proguard-rules.pro    # ProGuard-regler
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/...          # Kotlin-källkod
        └── res/              # Resurser (ikoner, teman, strängar)
```

## Versioner

- Android Gradle Plugin: 8.2.0
- Kotlin: 1.9.21
- Compose BOM: 2025.12.00
- Gradle: 8.2
