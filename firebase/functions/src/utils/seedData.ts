/**
 * SkillQuant - Seed data script
 *
 * Run once to populate the `skills` collection with initial skill entries.
 *
 * Usage (Firebase Emulator — recommended for local dev):
 *   1. Start emulator:  cd firebase && firebase emulators:start --only firestore
 *   2. Seed:            npx ts-node src/utils/seedData.ts
 *
 * Usage (Production Firestore):
 *   npx ts-node src/utils/seedData.ts --prod
 *   (Requires: firebase login + gcloud auth application-default login,
 *    OR set GOOGLE_APPLICATION_CREDENTIALS to a service account key file)
 */

import * as admin from "firebase-admin";
import { categorizeSkill } from "./skillParser";

const PROJECT_ID = process.env.GCLOUD_PROJECT || process.env.FIREBASE_PROJECT_ID || "skillquant-v1";
const USE_EMULATOR = !process.argv.includes("--prod");

// IMPORTANT: Set emulator env var BEFORE any Firebase initialization
if (USE_EMULATOR) {
  process.env.FIRESTORE_EMULATOR_HOST = process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080";
  console.log(`🔧 Using Firestore Emulator at ${process.env.FIRESTORE_EMULATOR_HOST}`);
  console.log(`   (pass --prod flag to seed production Firestore instead)\n`);
} else {
  console.log(`🔥 Seeding PRODUCTION Firestore for project: ${PROJECT_ID}\n`);
}

// Initialize Firebase Admin with explicit project ID
if (!admin.apps.length) {
  admin.initializeApp({ projectId: PROJECT_ID });
}

const db = admin.firestore();

interface SkillSeed {
  name: string;
  tags: string[];
}

const INITIAL_SKILLS: SkillSeed[] = [
  // Backend
  { name: "Spring Boot", tags: ["java", "backend", "microservices"] },
  { name: "Node.js", tags: ["javascript", "backend", "api"] },
  { name: "Go", tags: ["backend", "systems", "concurrency"] },
  { name: "Rust", tags: ["systems", "performance", "safety"] },
  { name: "Python", tags: ["scripting", "backend", "data"] },
  { name: "Django", tags: ["python", "backend", "web"] },
  { name: "FastAPI", tags: ["python", "backend", "api"] },
  { name: "GraphQL", tags: ["api", "query", "backend"] },

  // Frontend
  { name: "React", tags: ["frontend", "ui", "spa"] },
  { name: "Vue.js", tags: ["frontend", "ui", "progressive"] },
  { name: "Next.js", tags: ["react", "ssr", "fullstack"] },
  { name: "Angular", tags: ["frontend", "enterprise", "typescript"] },
  { name: "TypeScript", tags: ["typing", "frontend", "backend"] },
  { name: "Tailwind CSS", tags: ["css", "utility", "frontend"] },

  // AI/ML
  { name: "Machine Learning", tags: ["ai", "models", "data"] },
  { name: "LLM", tags: ["ai", "nlp", "generative"] },
  { name: "Generative AI", tags: ["ai", "creation", "models"] },
  { name: "Prompt Engineering", tags: ["ai", "llm", "optimization"] },
  { name: "LangChain", tags: ["ai", "orchestration", "llm"] },
  { name: "RAG", tags: ["ai", "retrieval", "generation"] },
  { name: "Data Science", tags: ["analytics", "ml", "statistics"] },
  { name: "Computer Vision", tags: ["ai", "image", "deep-learning"] },

  // DevOps / Cloud
  { name: "Kubernetes", tags: ["orchestration", "containers", "cloud"] },
  { name: "Docker", tags: ["containers", "devops", "deployment"] },
  { name: "Terraform", tags: ["iac", "cloud", "devops"] },
  { name: "AWS", tags: ["cloud", "infrastructure", "services"] },
  { name: "Google Cloud", tags: ["cloud", "gcp", "services"] },
  { name: "Azure", tags: ["cloud", "microsoft", "services"] },
  { name: "CI/CD", tags: ["automation", "devops", "pipeline"] },
  { name: "DevOps", tags: ["culture", "automation", "operations"] },

  // Mobile
  { name: "Kotlin", tags: ["android", "mobile", "jvm"] },
  { name: "Swift", tags: ["ios", "apple", "mobile"] },
  { name: "Flutter", tags: ["crossplatform", "dart", "mobile"] },
  { name: "React Native", tags: ["crossplatform", "javascript", "mobile"] },

  // Data
  { name: "Data Engineering", tags: ["pipeline", "etl", "big-data"] },
  { name: "PostgreSQL", tags: ["database", "sql", "relational"] },
  { name: "MongoDB", tags: ["database", "nosql", "document"] },
  { name: "Apache Kafka", tags: ["streaming", "messaging", "data"] },
  { name: "Redis", tags: ["cache", "in-memory", "database"] },

  // Web3
  { name: "Solidity", tags: ["blockchain", "ethereum", "smart-contracts"] },
  { name: "Web3", tags: ["blockchain", "decentralized", "crypto"] },

  // Security
  { name: "Cybersecurity", tags: ["security", "infosec", "protection"] },

  // Design
  { name: "Figma", tags: ["design", "ui", "prototyping"] },

  // Emerging
  { name: "WebAssembly", tags: ["performance", "web", "compilation"] },
  { name: "Deno", tags: ["runtime", "typescript", "backend"] },
  { name: "Bun", tags: ["runtime", "javascript", "performance"] },
  { name: "htmx", tags: ["frontend", "hypermedia", "simplicity"] },
  { name: "Astro", tags: ["frontend", "static", "islands"] },
  { name: "Svelte", tags: ["frontend", "compiler", "ui"] },
];

async function seedSkills() {
  console.log("🌱 Seeding skills collection...");

  const batch = db.batch();
  let count = 0;

  for (const skill of INITIAL_SKILLS) {
    const docRef = db.collection("skills").doc(); // auto-id
    batch.set(docRef, {
      name: skill.name,
      category: categorizeSkill(skill.name),
      tags: skill.tags,
    });
    count++;
  }

  await batch.commit();
  console.log(`✅ Seeded ${count} skills successfully.`);
}

// Also seed mock skillMetrics and arbitrageOpportunities for app development
async function seedMockMetrics() {
  console.log("📊 Seeding mock metrics...");

  const skillsSnapshot = await db.collection("skills").get();
  const now = Date.now();
  const DAY_MS = 86400000;

  const batch = db.batch();
  let count = 0;

  for (const doc of skillsSnapshot.docs) {
    const skill = doc.data();

    // Generate random but realistic metrics
    const demandScore = 30 + Math.random() * 65;
    const supplyScore = 20 + Math.random() * 60;
    const arbitrageScore = Math.max(0, Math.min(100,
      demandScore * 0.6 + (100 - supplyScore) * 0.3 + Math.random() * 10
    ));
    const avgSalary = 60000 + Math.floor(Math.random() * 140000);
    const medianSalary = Math.floor(avgSalary * (0.85 + Math.random() * 0.3));
    const freelanceRate = 40 + Math.floor(Math.random() * 160);

    // Generate 30-day trend data
    const demandTrend = [];
    const salaryTrend = [];
    let demandBase = demandScore - 10;
    let salaryBase = avgSalary - 5000;
    for (let i = 29; i >= 0; i--) {
      demandBase += (Math.random() - 0.4) * 3;
      salaryBase += (Math.random() - 0.4) * 2000;
      demandTrend.push({
        timestamp: now - i * DAY_MS,
        value: Math.max(0, Math.min(100, demandBase)),
      });
      salaryTrend.push({
        timestamp: now - i * DAY_MS,
        value: Math.max(30000, salaryBase),
      });
    }

    const metricsRef = db.collection("skillMetrics").doc(doc.id);
    batch.set(metricsRef, {
      skillId: doc.id,
      skillName: skill.name,
      category: skill.category,
      demandScore,
      supplyScore,
      arbitrageScore,
      avgSalary,
      medianSalary,
      freelanceHourlyRate: freelanceRate,
      jobPostCount: 50 + Math.floor(Math.random() * 500),
      freelanceGigCount: 10 + Math.floor(Math.random() * 200),
      demandTrend,
      salaryTrend,
      topEmployers: ["Google", "Meta", "Amazon", "Microsoft", "Apple"].slice(0, 2 + Math.floor(Math.random() * 3)),
      learningResources: [
        { title: `${skill.name} Masterclass`, url: "https://udemy.com", type: "course", platform: "Udemy" },
        { title: `Learn ${skill.name}`, url: "https://coursera.org", type: "course", platform: "Coursera" },
        { title: `${skill.name} in 30 Days`, url: "https://youtube.com", type: "tutorial", platform: "YouTube" },
        { title: `${skill.name} Certification`, url: "https://example.com", type: "certification", platform: "Official" },
      ],
      updatedAt: now,
    });
    count++;
  }

  await batch.commit();
  console.log(`✅ Seeded ${count} skill metrics.`);
}

async function seedArbitrageOpportunities() {
  console.log("🎯 Seeding arbitrage opportunities...");

  const metricsSnapshot = await db.collection("skillMetrics")
    .orderBy("arbitrageScore", "desc")
    .limit(20)
    .get();

  const batch = db.batch();
  let count = 0;
  const now = Date.now();

  for (const doc of metricsSnapshot.docs) {
    const m = doc.data();
    const changePercent = -5 + Math.random() * 20;
    const direction = changePercent >= 0 ? "up" : "down";

    const oppRef = db.collection("arbitrageOpportunities").doc();
    batch.set(oppRef, {
      skillId: m.skillId,
      skillName: m.skillName,
      arbitrageScore: m.arbitrageScore,
      demandScore: m.demandScore,
      supplyScore: m.supplyScore,
      avgSalary: m.avgSalary,
      changePercent: Math.round(changePercent * 10) / 10,
      direction,
      summary: `People with ${m.skillName} skills earn ${(1.5 + Math.random()).toFixed(1)}x more this quarter. ` +
               `${Math.round(m.demandScore)}/100 demand with ${m.jobPostCount || 0} open positions.`,
      updatedAt: now,
    });
    count++;
  }

  await batch.commit();
  console.log(`✅ Seeded ${count} arbitrage opportunities.`);
}

async function seedTrendingSkills() {
  console.log("📈 Seeding trending skills...");

  const metricsSnapshot = await db.collection("skillMetrics").get();
  const batch = db.batch();
  let count = 0;
  const now = Date.now();

  const docs = metricsSnapshot.docs
    .map(doc => ({ doc, score: Math.random() }))
    .sort((a, b) => b.score - a.score)
    .slice(0, 10);

  for (const { doc } of docs) {
    const m = doc.data();
    const changePercent = 2 + Math.random() * 25;
    const direction = Math.random() > 0.2 ? "up" : "down";

    const trendRef = db.collection("trendingSkills").doc();
    batch.set(trendRef, {
      skillId: m.skillId,
      skillName: m.skillName,
      trendDirection: direction,
      changePercent: Math.round(changePercent * 10) / 10,
      period: "7d",
      updatedAt: now,
    });
    count++;
  }

  await batch.commit();
  console.log(`✅ Seeded ${count} trending skills.`);
}

// Run all seed functions
async function main() {
  try {
    await seedSkills();
    await seedMockMetrics();
    await seedArbitrageOpportunities();
    await seedTrendingSkills();
    console.log("\n🎉 All seed data created successfully!");
    process.exit(0);
  } catch (error) {
    console.error("❌ Seeding failed:", error);
    process.exit(1);
  }
}

main();

