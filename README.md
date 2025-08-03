# BuzzBoard

BuzzBoard is a modern Android news application that provides users with up-to-date news articles from various categories using the NewsData.io API.

## Features

- Clean and intuitive Material Design interface
- Category-based news browsing through navigation drawer
- Infinite scroll pagination (6 articles per load)
- Article preview with images
- In-app article viewing with WebView
- Edge-to-edge design for modern Android devices

## Setup

### Prerequisites

- Android Studio Meerkat or newer
- Android SDK with minimum API level 29 (Android 10.0)

### API Key Configuration

1. Sign up at [NewsData.io](https://newsdata.io) to get your API key
2. Create or modify the `local.properties` file in the project root
3. Add your API key:
   ```properties
   NEWS_DATA_API_KEY=your_api_key_here
   ```

### Building the Project

1. Clone the repository
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Build and run the application

## Architecture & Libraries

- **Language**: Kotlin
- **UI Components**: Material Design, ConstraintLayout, RecyclerView
- **Networking**: Retrofit with Gson converter
- **Image Loading**: Glide
- **Concurrency**: Kotlin Coroutines
- **Layout**: DrawerLayout for navigation, CardView for articles

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- [NewsData.io](https://newsdata.io) for providing the news API
- Material Design components for the modern UI
- Android Jetpack libraries for enhanced development
