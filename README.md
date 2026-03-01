# SkillQuant - Skill Market Arbitrage Detector

> **Discover where to invest your learning effort.** SkillQuant scans job boards and freelance markets daily, detects emerging skill gaps, and alerts you: *"People with X skill earn 2.4x more this quarter."* It predicts where to invest learning effort using real market data.

![KMP](https://img.shields.io/badge/Kotlin_Multiplatform-2.3.0-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Compose_Multiplatform-1.10.0-4285F4?logo=jetpackcompose&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-Cloud_Functions-FFCA28?logo=firebase&logoColor=black)
![Platform](https://img.shields.io/badge/Platform-Android_%7C_Desktop-green)

---

## Screenshots

| Dashboard | Skill Detail | Radar Chart | Settings |
|:---------:|:------------:|:-----------:|:--------:|
| Arbitrage opportunities, trending skills, search & country filter | Demand/Supply gauge, salary, employers, jobs, learning resources | Spider chart comparing watchlisted skills | Google account, theme, watchlist, alerts |

---

## Architecture

The backend is fully serverless on Firebase. Data flows:

**Cloud Functions** → **Firestore** → **KMP App (Android / Desktop)**

| Function | Schedule | What it does |
|---|---|---|
| scrapeJobBoards() | 02:00 UTC daily | Fetches remote job postings from Remotive API |
| scrapeFreelanceSites() | 02:30 UTC daily | Fetches freelance gig data |
| aggregateSkillMetrics() | 03:00 UTC daily | Computes demand/supply/salary per skill per country |
| calculateArbitrageScore() | 03:30 UTC daily | Ranks top opportunities + trending skills |
| sendAlerts() | 04:00 UTC daily | Creates alerts + sends FCM push notifications |

**Key principle:** The mobile app does **NOT** compute market intelligence. It consumes pre-processed insights from Firestore via real-time snapshot listeners.

---

## Features

### Core

- **Skill Search** - Search 210+ tech skills with real-time filtering
- **Multi-Country** - Toggle between Morocco, France, USA with localized data
- **Localized Currency** - MAD for Morocco, EUR for France, USD for USA
- **Arbitrage Detection** - Identifies underserved high-demand skills
- **90-Day Trend Charts** - Demand and salary trends over 3 months
- **Top Employers** - Country-specific employer rankings per skill
- **Job Listings** - Real LinkedIn/Indeed links + remote jobs from Remotive API
- **Learning Resources** - Curated courses with direct links (Udemy, Coursera, YouTube, etc.)
- **Watchlist** - Star skills to track them across the app

### Screens (10+)

| Screen | Description |
|--------|-------------|
| **Dashboard** | Top 15 arbitrage opportunities, trending skills, search bar + country dropdown |
| **Skill Detail** | Score gauge, demand/supply bars, salary card, employers, job listings, learning resources |
| **Alerts and Settings** | Google account, push notifications, theme toggle, watchlist management |
| **Skill Comparison** | Side-by-side compare 2 skills (demand, salary, supply) |
| **Salary Calculator** | Personalized ROI estimator: "If I learn X, my salary increases by Y%" |
| **Learning Path Builder** | Prioritized study plan based on watchlist, ranked by ROI |
| **Skill Radar Chart** | Spider/radar chart across demand/salary/supply/growth axes |
| **Market News Feed** | Curated tech news from Hacker News + Dev.to, filtered by watchlisted skills |
| **Onboarding** | First-launch wizard: pick your current skills and get personalized recommendations |
| **No Internet** | Graceful offline screen with retry button |

### Authentication

- **Google Sign-In** - via Android Credential Manager API
- **Anonymous Auth** - Works out of the box, no sign-up required
- **Account Linking** - Anonymous to Google without losing watchlist/preferences/data

### UX

- **Dark/Light/System Theme** - Fintech-inspired dark palette with teal/gold accents
- **Pull-to-Refresh** - SwipeRefresh on the dashboard
- **Shimmer Loading** - Skeleton placeholders while data loads
- **Offline Detection** - "Please connect to the internet" screen

---

## Scoring System

### Arbitrage Score (0-100)

```
arbitrageScore = demandScore * 0.6
               + (100 - supplyScore) * 0.3
               + salaryGrowthBonus * 0.1
```

| Component | Weight | Logic |
|-----------|--------|-------|
| **Demand** | 60% | Normalized job + gig count across all skills (0-100) |
| **Supply Gap** | 30% | 100 - supply: fewer people know it = bigger opportunity |
| **Salary Growth** | 10% | min(100, salaryGrowth% * 2) * 0.1, capped at 10 pts |

**Examples:**

- **Rust** - Demand: 82, Supply: 25 => score 71.7 => High opportunity
- **React** - Demand: 90, Supply: 75 => score 61.5 => Moderate (everyone knows it)

| Score Range | Meaning |
|-------------|---------|
| 75-100 | High opportunity - underserved + strong demand |
| 50-74 | Good - growing demand gap |
| 25-49 | Moderate - balanced market |
| 0-24 | Low - well-supplied or low demand |

---

## Project Structure

```
SkillQuant/
  androidApp/
    google-services.json
    src/main/kotlin/
      MainActivity.kt

  composeApp/
    src/
      commonMain/kotlin/
        App.kt
        auth/GoogleAuthHelper.kt
        di/AppModule.kt
        domain/
          model/
          repository/
        data/repository/
        ui/
          calculator/
          comparison/
          components/
          dashboard/
          detail/
          learningpath/
          news/
          onboarding/
          radar/
          settings/
          theme/
        util/
      androidMain/kotlin/
        auth/GoogleAuthHelper.android.kt
        di/PlatformModule.android.kt
      jvmMain/kotlin/
        auth/GoogleAuthHelper.jvm.kt
        di/PlatformModule.jvm.kt

  firebase/
    firebase.json
    firestore.rules
    firestore.indexes.json
    seeder.html
    functions/src/
      index.ts
      utils/
        scoring.ts
        skillParser.ts
        seedData.ts

  gradle/libs.versions.toml
```

---

## Firestore Schema

| Collection | Doc ID | Key Fields |
|---|---|---|
| skills | {skillId} | name, category, tags[] |
| skillMetrics | {skillId}_{country} | demandScore, supplyScore, arbitrageScore, avgSalary, medianSalary, freelanceHourlyRate, jobPostCount, demandTrend[], salaryTrend[], topEmployers[], jobListings[], learningResources[] |
| arbitrageOpportunities | {skillId}_{country} | skillId, skillName, arbitrageScore, demandScore, supplyScore, avgSalary, changePercent, direction, summary, location |
| trendingSkills | {skillId}_{country} | skillId, skillName, trendDirection, changePercent, period, location |
| alerts | auto | userId, skillId, skillName, type, title, message, read, createdAt |
| userProfiles | {userId} | email, displayName, tier, watchlist[], currentSkills[], notificationsEnabled, fcmToken, isAnonymous, onboardingComplete, darkThemeOverride |

---

## Monetization

| Feature | Free | Pro ($4.99/mo) |
|---|---|---|
| Top opportunities | 15 | 20 |
| Trend history | 90 days | 90 days |
| Watchlist | 5 skills | Unlimited |
| Push alerts | Daily digest | Real-time per-skill |
| Salary data | Average only | Avg + Median + Percentiles |
| Learning resources | Top 3 | All |

---

## Getting Started

### Prerequisites

- **Android Studio** (Ladybug+, with KMP plugin)
- **JDK 21**
- **Node.js 18+** (for Cloud Functions)
- **Firebase CLI**: npm install -g firebase-tools
- A **Firebase project** with Firestore, Auth (Anonymous + Google), and Cloud Messaging

### 1. Clone and Open

```bash
git clone https://github.com/your-username/SkillQuant.git
cd SkillQuant
```

Open in Android Studio and let Gradle sync.

### 2. Firebase Setup

```bash
firebase login
cd firebase
firebase use skillquant-v1
```

**Enable Auth Providers** in Firebase Console > Authentication > Sign-in method:
1. Enable **Anonymous**
2. Enable **Google** (set support email)

**Add SHA-1 fingerprint:**

```bash
./gradlew signingReport
```

Copy the debug SHA-1 and add it in Firebase Console > Project Settings > Android app > Add fingerprint.

**Configure google-services.json:**
1. Download it from Firebase Console > Project Settings > Android app
2. Place it in androidApp/google-services.json
3. Copy the Web client ID (oauth_client with client_type: 3)
4. Update Constants.kt: GOOGLE_WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"

### 3. Seed Data

Open firebase/seeder.html in a browser and click **"Seed All Collections"**.

### 4. Deploy

```bash
cd firebase
firebase deploy --only functions
firebase deploy --only firestore:rules,firestore:indexes
```

### 5. Build and Run

```bash
./gradlew :androidApp:assembleDebug
```

Or press the Run button in Android Studio on the androidApp run configuration.

> **Emulator tip:** Add a Google account in Settings > Accounts before testing Google Sign-In.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Shared UI | Compose Multiplatform 1.10.0 |
| Shared Logic | Kotlin Multiplatform (Kotlin 2.3.0) |
| Navigation | Voyager 1.1.0-beta03 |
| DI | Koin 4.0.4 |
| Firebase SDK | GitLive Firebase KMP 2.1.0 |
| Serialization | kotlinx-serialization 1.8.1 |
| Date/Time | kotlinx-datetime 0.6.2 |
| Auth (Android) | Credential Manager + Google Identity |
| Backend | Firebase Cloud Functions v2 (TypeScript) |
| Database | Cloud Firestore (real-time snapshots) |
| Push | Firebase Cloud Messaging |
| News | Hacker News Algolia API + Dev.to API |
| Jobs | Remotive API + LinkedIn/Indeed search URLs |

---

## Roadmap

### Completed

- [x] KMP project structure with Compose Multiplatform
- [x] 10+ screens: Dashboard, Detail, Comparison, Calculator, Radar, News, Learning Path, Onboarding, Settings, Offline
- [x] Firebase backend: 5 scheduled Cloud Functions
- [x] 210 skills across 7 categories, 3 countries (Morocco, France, USA)
- [x] Real job listings (Remotive + LinkedIn/Indeed search links)
- [x] Market news feed (Hacker News + Dev.to, filtered by watchlist)
- [x] Google Sign-In with account linking (anonymous to Google)
- [x] Dark/Light/System theme toggle
- [x] Pull-to-refresh, shimmer loading, offline detection
- [x] Country-specific currencies (MAD, EUR, USD)
- [x] Skill search + onboarding flow (210 skill picker with search)
- [x] Firestore security rules + browser-based seeder
- [x] Arbitrage scoring engine (demand 60% + supply gap 30% + growth 10%)

### Next

- [ ] iOS target
- [ ] In-App Purchase / Subscription (Google Play Billing)
- [ ] Weekly digest push notifications
- [ ] More scraping sources (LinkedIn, Indeed, Upwork APIs)
- [ ] Real supply data (LinkedIn talent pool, GitHub contributors)
- [ ] Offline caching with Firestore persistence
- [ ] Skill portfolio analytics and progress tracking
- [ ] Export insights as PDF

---

## License

MIT (c) Bader Eddine Ben-Lhachemi
