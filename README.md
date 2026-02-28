# SkillQuant — Skill Market Arbitrage Detector

> **Discover where to invest your learning effort.** SkillQuant scans job boards and freelance markets daily, detects emerging skill gaps, and alerts you when skills are underserved with high demand — so you can stay ahead of the market.

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    FIREBASE CLOUD FUNCTIONS                     │
│                     (Scheduled — Daily)                         │
│                                                                 │
│  02:00 UTC   scrapeJobBoards()       → rawJobPostings          │
│  02:30 UTC   scrapeFreelanceSites()  → rawFreelanceGigs        │
│  03:00 UTC   aggregateSkillMetrics() → skillMetrics + trending │
│  03:30 UTC   calculateArbitrageScore()→ arbitrageOpportunities │
│  04:00 UTC   sendAlerts()            → alerts + FCM push       │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                     ┌──────▼──────┐
                     │  FIRESTORE  │
                     │  (Database) │
                     └──────┬──────┘
                            │
              ┌─────────────┼─────────────┐
              │             │             │
        ┌─────▼─────┐ ┌────▼────┐ ┌──────▼──────┐
        │  Android   │ │  iOS    │ │   Desktop   │
        │  (KMP)     │ │  (KMP)  │ │   (JVM)     │
        └───────────┘ └─────────┘ └─────────────┘
                    Compose Multiplatform
```

**Key principle:** The mobile app does NOT compute market intelligence. It **consumes processed insights** from Firestore.

---

## 📱 MVP Screens (3 Screens)

### 1. Dashboard
- Top arbitrage opportunities (horizontal card carousel)
- Trending skills list (7-day change)
- Recent alerts summary
- Badge on bell icon for unread count

### 2. Skill Detail
- Arbitrage score gauge (0-100, color-coded arc)
- Demand vs Supply comparison bars
- Trend chart (demand or salary, toggleable)
- Salary breakdown (avg, median, freelance rate)
- Top employers hiring
- Learning resources (courses, tutorials, certs)
- "Watch" FAB to add to watchlist

### 3. Alerts & Settings
- Push notification toggle
- Watchlist management (view, remove skills)
- Alert history (with unread indicators)
- "Upgrade to Pro" banner

---

## 🧱 KMP Module Structure

```
composeApp/src/commonMain/kotlin/com/badereddine/skillquant/
├── App.kt                          # Entry: Koin + Voyager Navigator
├── di/
│   └── AppModule.kt                # Koin DI wiring
├── domain/
│   ├── model/
│   │   ├── Alert.kt
│   │   ├── ArbitrageOpportunity.kt
│   │   ├── LearningResource.kt
│   │   ├── Skill.kt
│   │   ├── SkillMetrics.kt
│   │   ├── TrendingSkill.kt
│   │   ├── TrendPoint.kt
│   │   └── UserProfile.kt
│   └── repository/
│       ├── AlertRepository.kt       # Interface
│       ├── SkillRepository.kt       # Interface
│       └── UserRepository.kt        # Interface
├── data/
│   └── repository/
│       ├── FirestoreAlertRepository.kt
│       ├── FirestoreSkillRepository.kt
│       └── FirestoreUserRepository.kt
├── ui/
│   ├── theme/
│   │   └── Theme.kt                 # SkillQuantTheme (dark-first, fintech palette)
│   ├── components/
│   │   ├── ArbitrageCard.kt         # Opportunity card with score badge
│   │   ├── LoadingState.kt          # Shimmer loading placeholders
│   │   ├── ScoreGauge.kt            # Circular arc gauge (0-100)
│   │   ├── SkillChip.kt             # Category-colored tag chip
│   │   └── TrendChart.kt            # Canvas line chart
│   ├── dashboard/
│   │   ├── DashboardScreen.kt       # Voyager Screen
│   │   └── DashboardViewModel.kt    # ScreenModel
│   ├── detail/
│   │   ├── SkillDetailScreen.kt
│   │   └── SkillDetailViewModel.kt
│   └── settings/
│       ├── AlertsSettingsScreen.kt
│       └── AlertsSettingsViewModel.kt
└── util/
    ├── Constants.kt                  # Collection names, tier limits
    └── DateTimeUtil.kt               # Formatting helpers
```

---

## 🔥 Firestore Schema

| Collection | Doc ID | Key Fields |
|---|---|---|
| `skills` | auto | `name`, `category`, `tags[]` |
| `skillMetrics` | `{skillId}` | `demandScore`, `supplyScore`, `arbitrageScore`, `avgSalary`, `medianSalary`, `freelanceHourlyRate`, `jobPostCount`, `freelanceGigCount`, `demandTrend[]`, `salaryTrend[]`, `topEmployers[]`, `learningResources[]`, `updatedAt` |
| `skillMetrics/{id}/history` | `YYYY-MM-DD` | Daily snapshots for trend charts |
| `arbitrageOpportunities` | auto | `skillId`, `skillName`, `arbitrageScore`, `demandScore`, `supplyScore`, `avgSalary`, `changePercent`, `direction`, `summary`, `updatedAt` |
| `trendingSkills` | auto | `skillId`, `skillName`, `trendDirection`, `changePercent`, `period`, `updatedAt` |
| `alerts` | auto | `userId`, `skillId`, `skillName`, `type`, `title`, `message`, `read`, `createdAt` |
| `userProfiles` | `{userId}` | `email`, `tier`, `watchlist[]`, `notificationsEnabled`, `fcmToken`, `createdAt` |
| `appConfig` | `"monetization"` | `freeWatchlistLimit`, `freeHistoryDays`, `proPrice` |

---

## ☁️ Cloud Functions

All in `firebase/functions/src/index.ts`:

| Function | Schedule | Purpose |
|---|---|---|
| `scrapeJobBoards` | 02:00 UTC daily | Scrapes Remotive API (+ Adzuna when configured) |
| `scrapeFreelanceSites` | 02:30 UTC daily | Scrapes Freelancer.com public API |
| `aggregateSkillMetrics` | 03:00 UTC daily | Computes demand/supply scores per skill |
| `calculateArbitrageScore` | 03:30 UTC daily | Ranks top 20 opportunities |
| `sendAlerts` | 04:00 UTC daily | Creates alerts + sends FCM push to watchers |

**Utility scripts:**
- `utils/skillParser.ts` — Normalizes skill names (e.g., "K8s" → "Kubernetes")
- `utils/scoring.ts` — Arbitrage formula, normalization math
- `utils/seedData.ts` — Seeds ~50 skills + mock metrics for development

---

## 💰 Monetization

| Feature | Free | Pro ($4.99/mo) |
|---|---|---|
| Top opportunities | 5 | 20 |
| Trend history | 7 days | 90 days |
| Watchlist | 5 skills | Unlimited |
| Push alerts | Daily digest | Real-time per-skill |
| Salary data | Average only | Avg + Median + Percentiles |
| Learning resources | Top 3 | All |
| Ads | Banner on dashboard | Ad-free |

Tier is enforced client-side via `UserProfile.tier` + Firestore rules as secondary guard.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (with KMP plugin)
- Node.js 18+ (for Cloud Functions)
- Firebase CLI (`npm install -g firebase-tools`)
- A Firebase project with Firestore, Auth (Anonymous), and Cloud Messaging enabled

### 1. Firebase Setup
```bash
# Login and select your project
firebase login
cd firebase
firebase use --add

# Replace placeholder in .firebaserc with your project ID
```

### 2. Replace `google-services.json`
Download the real `google-services.json` from Firebase Console → Project Settings → Android app (package: `com.badereddine.skillquant`) and replace `androidApp/google-services.json`.

### 3. Seed Development Data
```bash
cd firebase/functions
npm install
npx ts-node src/utils/seedData.ts
```

### 4. Deploy Cloud Functions
```bash
cd firebase
firebase deploy --only functions
firebase deploy --only firestore:rules,firestore:indexes
```

### 5. Build & Run the App
Open the project in Android Studio, sync Gradle, and run the `androidApp` configuration.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Shared UI | Compose Multiplatform 1.10.0 |
| Shared Logic | Kotlin Multiplatform (Kotlin 2.3.0) |
| Navigation | Voyager 1.1.0-beta03 |
| DI | Koin 4.0.4 |
| Firebase SDK | GitLive Firebase KMP 2.1.0 |
| Serialization | kotlinx-serialization 1.8.1 |
| Date/Time | kotlinx-datetime 0.6.2 |
| Backend | Firebase Cloud Functions v2 (TypeScript) |
| Database | Cloud Firestore |
| Auth | Firebase Anonymous Auth |
| Push | Firebase Cloud Messaging |

---

## 📋 Development Roadmap

### MVP (Current)
- [x] Project structure & dependencies
- [x] Domain models & repository interfaces
- [x] Firestore repository implementations
- [x] DI module (Koin)
- [x] 3 screens: Dashboard, Skill Detail, Alerts/Settings
- [x] Custom theme (fintech dark palette)
- [x] Reusable components (ArbitrageCard, ScoreGauge, TrendChart, etc.)
- [x] Cloud Functions (5 scheduled functions)
- [x] Firestore security rules & indexes
- [x] Seed data script for development
- [ ] Replace placeholder google-services.json with real one
- [ ] Deploy Cloud Functions
- [ ] Test end-to-end with real Firebase project

### Post-MVP
- [ ] iOS target
- [ ] In-App Purchase / Subscription (Pro tier)
- [ ] More data sources (LinkedIn, Indeed, Upwork APIs)
- [ ] Full-text skill search with Algolia or Typesense
- [ ] Skill comparison screen (side-by-side metrics)
- [ ] "Learning Path" recommendations
- [ ] Offline caching (Firestore persistence)
- [ ] Analytics dashboard for skill portfolio tracking
