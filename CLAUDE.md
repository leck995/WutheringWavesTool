# WutheringWavesTool - Codebase Analysis

## Project Overview

**WutheringWavesTool** (鸣潮助手) is a comprehensive JavaFX desktop application that serves as a third-party tool for the game "Wuthering Waves" (鸣潮). It acts as an enhanced launcher replacement with extensive game data management and analysis capabilities.

### Application Type
- **Desktop Application** built with JavaFX 25
- **Target Platform**: Windows (with WeGame and official server support)
- **Architecture**: MVVM pattern using MvvmFX framework
- **Language Support**: Chinese (zh_CN) and English (EN)

## Technology Stack

### Core Technologies
- **Java 25** (Latest LTS + Preview features)
- **JavaFX 25** (UI Framework)
- **Maven** (Build system)
- **SQLite** (Local database)
- **Jackson** (JSON processing)

### Key Libraries & Frameworks
- **MvvmFX 1.8.0** - MVVM framework for JavaFX
- **AtlantaFX 2.0.1** - Modern JavaFX theme library
- **Ikonli 12.3.1** - Icon libraries (Material Design, Ant Design, Fluent UI)
- **JNA Platform 5.14.0** - Native Windows API access
- **JNativeHook 2.2.2** - Global key/mouse listeners
- **ControlsFX 11.2.2** - Additional JavaFX controls
- **Apache Commons DBUtils** - Database utilities
- **Thumbnailator 0.4.20** - Image processing
- **Logback 1.5.17** - Logging framework

### Custom Dependencies
- **Teafx 1.2.0** - Custom JavaFX utilities (GitHub: leck995/Teafx)
- **Fx-plugin 1.0.2** - Plugin framework (GitHub: leck995/Fx-plugin)

## Project Structure

### Source Code Organization
```
src/main/java/
├── cn.tealc.wutheringwavestool/          # Main application package
│   ├── base/                             # Configuration and core services
│   ├── dao/                              # Data Access Objects (SQLite)
│   ├── jna/                              # Windows native integrations
│   ├── model/                            # Data models and POJOs
│   │   ├── analysis/                     # Gacha analysis models
│   │   ├── game/                         # Game-related models
│   │   ├── role/                         # Character/role models
│   │   └── ui/                           # UI-specific models
│   ├── plugin/                           # Plugin system
│   ├── theme/                            # UI theming
│   ├── thread/                           # Background tasks
│   ├── ui/                               # View controllers and ViewModels
│   │   ├── cardpool/                     # Gacha analysis views
│   │   ├── game/                         # Game launcher views
│   │   ├── kujiequ/                      # Kuro Games API integration
│   │   └── tray/                         # System tray functionality
│   └── util/                             # Utility classes
└── com.kuro/                             # Kuro Games API integration
    ├── game/                             # Game data models
    └── kujiequ/                          # Kuro Plaza API models
```

### Resources Structure
```
src/main/resources/
├── cn/tealc/wutheringwavestool/
│   ├── css/                              # Application stylesheets
│   ├── data/                             # Static data files
│   ├── font/                             # Custom fonts
│   ├── image/                            # Application icons and images
│   ├── language/                         # Internationalization files
│   └── ui/                               # FXML view definitions
└── module-info.java                      # Java modules configuration
```

### External Directories
- **assets/** - Game data, character information, weapon data (JSON format)
- **data/** - Runtime application data
- **docs/** - Documentation and screenshots
- **log/** - Application logs
- **target/** - Maven build output

## Key Features & Modules

### 1. Game Launcher
- **Advanced startup options** (DX11/DX12, FPS unlock, custom parameters)
- **Game process monitoring** via Windows API hooks
- **Custom game installation detection** (WeGame, Official launcher)

### 2. Gacha Analysis System
- **Detailed pull statistics** with multiple visualization modes
- **Pity tracking** and probability calculations
- **Historical data management** with SQLite storage

### 3. Kuro Plaza Integration (Chinese servers only)
- **Auto-sign in** for up to 9 accounts
- **Character and weapon viewing** with detailed stats
- **Material calculator** for character/weapon upgrades
- **Tower of Adversity** historical data

### 4. Game Data Tracking
- **Play time statistics** (daily and total)
- **Combat statistics** (battles, echoes collected, dodges)
- **Daily resource tracking** (stamina, tasks, chests)

### 5. System Integration
- **System tray support** with quick actions
- **Global hotkey support** via JNativeHook
- **Custom window styling** with modern UI themes
- **Multi-language support** (Chinese/English)

## Build System & Configuration

### Maven Configuration
```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>25</javafx.version>
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

### Key Maven Plugins
- **maven-compiler-plugin** - Java 25 compilation
- **maven-shade-plugin** - Fat JAR creation with all dependencies
- **javafx-maven-plugin** - JavaFX application packaging and jlink

### Build Commands
```bash
# Development run
mvn clean javafx:run

# Create executable JAR
mvn clean package

# Create native runtime with jlink
mvn clean javafx:jlink
```

### JVM Runtime Options
- **ZGC garbage collector** (`-XX:+UseZGC`)
- **High-DPI support** (`-Dprism.lcdtext=true`)
- **JavaFX preview features** (`-Djavafx.enablePreview=true`)
- **Native access permissions** for JNA and SQLite

## Database Schema

### SQLite Tables
- **GameTime** - Play session tracking
- **GameRecord** - Combat and activity statistics
- **GameRoleData** - Character progression data
- **GameTowerData** - Tower of Adversity records
- **SignHistory** - Auto-sign tracking
- **UserInfo** - Account management

## Development Workflow

### Development Environment
- **Java 25** (Amazon Corretto or equivalent)
- **Maven 3.9+**
- **JavaFX Scene Builder** (optional, for FXML editing)

### IDE Configuration
- **IntelliJ IDEA** project files included (`.idea/`)
- **Code style** configuration provided
- **UI Designer** settings for JavaFX

### Version Control
- **Git** repository with `.gitignore` for Java/Maven projects
- **GitLab CI/CD** configured for documentation deployment

### Internationalization
- **Resource bundles** for Chinese (`zh_CN`) and English (`en`)
- **Dynamic language switching** without restart
- **Custom language file backup** system

## Runtime Configuration

### Application Settings (`settings.json`)
Key configuration options:
- **Game installation path** detection and override
- **UI scaling** for high-DPI displays
- **Theme selection** (light/dark modes)
- **Feature toggles** (Kuro Plaza integration, auto-sign)
- **Window state** persistence
- **Logging level** configuration

### Security Features
- **Application lock** mechanism to prevent multiple instances
- **Native permission requests** for Windows API access
- **Secure credential storage** for Kuro Plaza accounts

## External Integrations

### Kuro Games APIs
- **Kuro Plaza** (库街区) - Official game data platform
- **Game launcher** API integration
- **Character and weapon** data synchronization

### Windows System APIs
- **Process monitoring** via SetWinEventHook
- **Global input hooks** for hotkey support
- **System tray** integration
- **High-DPI** display support

## Performance Optimizations

### Threading Model
- **Virtual threads** (Java 21+) for I/O operations
- **Background services** for data synchronization
- **UI thread** protection with proper dispatching

### Memory Management
- **ZGC** for low-latency garbage collection
- **Image caching** system for character portraits
- **Database connection pooling**
- **Resource cleanup** on application exit

## Notable Architecture Patterns

### MVVM Implementation
- **ViewModels** manage UI state and business logic
- **Views** are pure JavaFX FXML definitions
- **Models** represent data structures and API responses
- **Dependency injection** via MvvmFX framework

### Plugin System
- **Modular architecture** allowing feature extensions
- **Dynamic loading** of additional functionality
- **Clean separation** between core and plugin features

### Configuration Management
- **Centralized Config class** with property binding
- **Automatic persistence** of user preferences
- **Runtime configuration** updates without restart

This codebase represents a sophisticated desktop application with modern Java features, comprehensive game integration, and a well-structured architecture suitable for ongoing development and feature expansion.