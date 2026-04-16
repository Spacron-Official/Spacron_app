# Spacron Migration Notes

This workspace now includes two new targets:

- `springboot-api/`: Spring Boot + Kotlin backend that mirrors the current Node marketplace API, including auth, task lifecycle, admin finance/config, local uploads, and a simple WebSocket topic.
- `android-app/`: Android Kotlin app using Jetpack Compose and Retrofit with screens for auth, user tasks, seller task management, and admin controls.

Suggested next steps:

1. Replace the Node app once the new API is validated against your existing data rules.
2. Split the large Kotlin source files into feature packages after the first successful build.
3. Add tests, password hashing, and a real token strategy before production release.
