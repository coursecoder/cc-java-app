# cc-java-app

[![Live Demo](https://img.shields.io/badge/Live%20Demo-Visit%20App-brightgreen?style=for-the-badge)](https://currencyconverter-java-app.onrender.com)

A Java currency converter web app built with the Spark micro framework. Fetches live exchange rates from the ExchangeRate-API on startup and gracefully falls back to cached rates if the API is unreachable.

> **Note:** Hosted on Render's free tier — first load may take 30–60 seconds to wake up.

## Features

- **Live exchange rates** via [ExchangeRate-API](https://api.exchangerate-api.com) (no API key required)
- **10 supported currencies** — USD, EUR, GBP, CHF, CNY, JPY, CAD, AUD, INR, MXN
- **Graceful fallback** to hardcoded rates if the API is unreachable
- **Live/Cached badge** in the UI so users always know the rate source
- **Last updated timestamp** displayed on every page
- Built with **Spark + Thymeleaf** for routing and templating

## Tech Stack

- **Java 17**
- **Spark Java** — lightweight web framework
- **Thymeleaf** — HTML templating
- **Maven** — build and dependency management
- **ExchangeRate-API** — live currency data

## Project Structure

```
src/
└── main/
    ├── java/com/coursecoder/training/
    │   └── CurrencyConverter.java
    └── resources/
        └── templates/
            ├── template.html          # conversion form
            └── currencyConversion.html # results page
pom.xml
run-app.sh
```

## Getting Started

**Prerequisites:** Java 17+, Maven

**1. Clone the repo**
```bash
git clone https://github.com/coursecoder/cc-java-app.git
cd cc-java-app
```

**2. Build**
```bash
mvn clean package
```

**3. Run**
```bash
./run-app.sh
```
Or manually:
```bash
java -jar target/cc-java-app-1.0-SNAPSHOT.jar
```

**4. Open in browser**
```
http://localhost:4567/currency-converter
```

## How It Works

On startup the app makes a single GET request to:
```
https://api.exchangerate-api.com/v4/latest/USD
```

All rates are stored relative to USD. Any currency pair conversion normalizes through USD:
```
EUR → JPY = (amount / USD→EUR rate) × USD→JPY rate
```

If the API call fails, the app logs a warning and serves hardcoded fallback rates so the app remains functional offline.

## Supported Currencies

| Currency | Code |
|----------|------|
| US Dollar | USD |
| Euro | EUR |
| British Pound | GBP |
| Swiss Franc | CHF |
| Chinese Yuan Renminbi | CNY |
| Japanese Yen | JPY |
| Canadian Dollar | CAD |
| Australian Dollar | AUD |
| Indian Rupee | INR |
| Mexican Peso | MXN |
