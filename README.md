# 🏦 FinTrack Frontend (Flutter)

FinTrack is a modern personal finance tracking application built with **Flutter**. It provides a sleek, interactive interface for managing transactions, visualizing statistics, and setting budgets.

## 🎨 Key Features
- **Interactive Dashboard**: Visual breakdown of income vs expenses.
- **Dynamic Theming**: Custom CRM-inspired dark/light aesthetics.
- **Provider State Management**: Fast, reactive UI updates.
- **Robust Validation**: Zod-inspired form validation for secure data entry.
- **Security**: Secure JWT storage and automatic session restoration.

## 🛠️ Tech Stack
- **Framework**: Flutter (Dart)
- **State Management**: Provider
- **Networking**: Dio / Http (JWT-based)
- **Architecture**: Modular View/Service/Model structure

## 📦 Getting Started

### Prerequisites
- Flutter SDK (Latest Stable)
- Android Studio / VS Code with Flutter extension

### Installation
1. Clone the repository.
2. Install dependencies:
```bash
flutter pub get
```
3. Run the application:
```bash
flutter run
```

## 🔌 API Integration
The frontend is designed to work with the [FinTrack Backend](https://github.com/user/fintrack-backend).
Ensure the backend is running and update the `API_URL` in the environment configuration if necessary.

## 🧪 Testing
```bash
flutter test
```