# 🔐 Production Signing Setup

## Security Notice
**This configuration is designed to be secure and generic - no sensitive information is stored in the repository.**

## Setup for Production Release

### Option 1: gradle.properties (Recommended)
1. Copy `signing.properties.template` to `gradle.properties`
2. Configure with your keystore details:
```properties
RELEASE_STORE_FILE=path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

### Option 2: Environment Variables
```bash
export RELEASE_STORE_FILE="/path/to/keystore.jks"
export RELEASE_STORE_PASSWORD="your_password"
export RELEASE_KEY_ALIAS="your_alias"
export RELEASE_KEY_PASSWORD="your_key_password"
```

## Build Commands
```bash
# Debug build (uses debug keystore)
./gradlew assembleDebug

# Release build (uses production keystore if configured)
./gradlew assembleRelease
./gradlew bundleRelease
```

## Security Features
- ✅ No sensitive data in repository
- ✅ Generic configuration template
- ✅ Multiple secure setup options
- ✅ Automatic fallback to debug signing for development