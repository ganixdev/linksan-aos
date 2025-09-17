# LinkSan - URL Sanitizer

LinkSan is a privacy-focused Android app that helps you clean URLs by removing tracking parameters, affiliate codes, and unnecessary redirects. Share cleaner links and protect your privacy.

## Features

- **Remove tracking parameters**: UTM codes, Facebook tracking (fbclid), Google Analytics, and more
- **Clean affiliate codes**: Remove Amazon affiliate links and other marketing parameters  
- **Resolve redirects**: Get the final destination URL
- **Material Design 3**: Modern Android UI with dark mode support
- **Privacy-first**: Local processing, no data sent to external servers
- **Share integration**: Works with Android's share system
- **Bulk processing**: Clean multiple URLs at once
- **Offline operation**: Basic cleaning works without internet

## Privacy

LinkSan is designed with privacy as the top priority:
- URLs are processed locally on your device
- No analytics or tracking
- No personal data collection
- Network requests only made when resolving redirects
- Open source - you can verify the code yourself

## Download

### F-Droid (Recommended)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.ganixdev.linksan/)

### GitHub Releases
Download the latest APK from [GitHub Releases](https://github.com/ganixdev/linksan-aos/releases)

### Google Play Store
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.ganixdev.linksan)

## Building

### Requirements
- Android Studio Arctic Fox or later
- JDK 8 or later
- Android SDK 35

### Build Steps
```bash
git clone https://github.com/ganixdev/linksan-aos.git
cd linksan-aos
./gradlew assembleRelease
```

### F-Droid Build
F-Droid builds use a special configuration:
```bash
./gradlew -PFDROID_BUILD=true assembleRelease
```

The project includes F-Droid compliant metadata in the `metadata/en-US/` directory:
- `short_description.txt` - Brief app description
- `full_description.txt` - Detailed feature list
- `changelogs/4.txt` - Version-specific changelog
- `images/icon.png` - App icon
- `images/phoneScreenshots/` - Screenshots directory

## Supported Tracking Parameters

LinkSan removes these common tracking parameters:
- **UTM parameters**: utm_source, utm_medium, utm_campaign, utm_term, utm_content
- **Facebook**: fbclid, fb_action_ids, fb_action_types, fb_ref, fb_source
- **Google**: gclid, dclid, ga_source, ga_medium, ga_term, ga_content, ga_campaign
- **Amazon**: tag, linkCode, creative, creativeASIN
- **Microsoft**: msclkid, mc_cid, mc_eid
- **Twitter**: twclid
- **LinkedIn**: trk, trkCampaign
- **Adobe**: sc_cid, sc_lid
- **Mailchimp**: mc_cid, mc_eid
- **And many more...**

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## F-Droid Submission Requirements

This project follows F-Droid requirements:
- ✅ **Git Tags**: Each release has a corresponding git tag (e.g., `v1.0.4`)
- ✅ **Metadata**: Proper F-Droid metadata structure in `metadata/en-US/`
- ✅ **FOSS Dependencies**: All dependencies are Free and Open Source
- ✅ **Reproducible Builds**: Build configuration supports F-Droid builds
- ✅ **License**: MIT License (F-Droid compatible)

### Submit to F-Droid:
1. Fork the [fdroiddata repository](https://gitlab.com/fdroid/fdroiddata)
2. Create metadata file `metadata/com.ganixdev.linksan.yml`
3. Add screenshots to `metadata/en-US/images/phoneScreenshots/`
4. Submit merge request

## Disclaimer

LinkSan is provided as-is for educational and privacy purposes. While it removes common tracking parameters, it may not catch all tracking methods. Always review URLs before sharing sensitive information.

---

Made with ❤️ by [ganixdev](https://github.com/ganixdev)