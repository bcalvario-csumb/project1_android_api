# Project 1 Android API

## CST438 Section 1 - Group 2

This is an Android card collection game built using Kotlin. Users are able to create accounts, log in, open card packs using data from the external API, collect cards, trade cards, and view all the cards in their collection.

### Features

- User registration and login
- Local Room database for users and cards
- External API using OkHttp
- Opening card packs
- Offline API caching using SharedPreferences
- Admin panel with users and API information
- Automated database and UI tests
- Static code analysis using Detekt, PMD, and Error Prone

### Technologies

- Kotlin
- Room Database
- OkHttp
- SharedPreferences

## How to Run

1. Clone the repository.

```bash
git clone https://github.com/bcalvario-csumb/project1_android_api.git
```

2. Open the project in Android Studio.

3. Allow Gradle to finish syncing.

4. Run the application on an Android emulator or physical Android device.

The project does require Internet access to retrieve card data from the external API. Once products are cached, the data can be loaded locally when the API is unavailable.

### Testing

#### Tests

- User database operations
- Card database operations
- User-card relationships
- Login and sign-up screens
- Opening a card pack
- Adding a card to the user's collection

## Team
### This project was developed by:

- Brandon Calvario
- Carlos Solian
- Alexander Trujillo
- Austin Phipps