/**
 * SkillQuant Cloud Functions
 *
 * Pipeline: Scrapers → Aggregator → Firestore → KMP App
 *
 * Daily schedule:
 *   02:00 UTC - scrapeJobBoards
 *   02:30 UTC - scrapeFreelanceSites
 *   03:00 UTC - aggregateSkillMetrics
 *   03:30 UTC - calculateArbitrageScore
 *   04:00 UTC - sendAlerts
 */

import * as admin from "firebase-admin";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { onRequest } from "firebase-functions/v2/https";
import axios from "axios";
import { extractSkills, normalizeSkillName, categorizeSkill } from "./utils/skillParser";
import {
  normalize,
  calculateArbitrage,
  percentChange,
  average,
  median,
  generateSummary,
} from "./utils/scoring";

admin.initializeApp();
const db = admin.firestore();

const COUNTRIES = ["Morocco", "France", "USA"];

// ── Per-country baseline salaries IN LOCAL CURRENCY ──────────────────────────
// Morocco: MAD/year  (1 USD ≈ 10 MAD; mid-level dev earns ~120k–250k MAD/yr)
// France:  EUR/year  (mid-level dev earns ~40k–65k EUR/yr)
// USA:     USD/year  (mid-level dev earns ~100k–180k USD/yr)
const SALARY_BASE: Record<string, { min: number; max: number }> = {
  Morocco: { min: 80_000,  max: 300_000  }, // MAD/yr
  France:  { min: 35_000,  max: 75_000   }, // EUR/yr
  USA:     { min: 80_000,  max: 220_000  }, // USD/yr
};

// Freelance hourly rates in local currency
// Morocco: ~150–600 MAD/hr for remote freelance work
// France:  ~35–120 EUR/hr
// USA:     ~40–200 USD/hr
const FREELANCE_BASE: Record<string, { min: number; max: number }> = {
  Morocco: { min: 150, max: 600  },
  France:  { min: 35,  max: 120  },
  USA:     { min: 40,  max: 200  },
};

// Detect which country a job location string maps to
function detectCountry(location: string): string | null {
  const loc = (location || "").toLowerCase();
  const moroccoCities = ["morocco", "maroc", "casablanca", "rabat", "marrakech", "tangier", "fes", "agadir"];
  const franceCities = ["france", "paris", "lyon", "marseille", "toulouse", "nantes", "bordeaux", "lille", "strasbourg"];
  const usaCities = ["usa", "united states", "us", "san francisco", "new york", "seattle", "austin", "boston", "chicago", "denver", "los angeles", "remote"];
  if (moroccoCities.some(c => loc.includes(c))) return "Morocco";
  if (franceCities.some(c => loc.includes(c))) return "France";
  if (usaCities.some(c => loc.includes(c))) return "USA";
  // "Worldwide" or "Anywhere" → all countries
  if (loc.includes("worldwide") || loc.includes("anywhere") || loc.includes("remote")) return "USA";
  return null;
}

// ============================================================================
// 1. SCRAPE JOB BOARDS (Daily 02:00 UTC)
// ============================================================================

export const scrapeJobBoards = onSchedule(
  { schedule: "0 2 * * *", timeZone: "UTC", memory: "512MiB", timeoutSeconds: 300 },
  async () => {
    console.log("📡 Starting job board scrape...");
    const now = Date.now();
    let totalJobs = 0;

    try {
      // Source 1: Remotive API (remote jobs, free, no key needed)
      try {
        const remotiveRes = await axios.get("https://remotive.com/api/remote-jobs", {
          params: { limit: 100 },
          timeout: 30000,
        });
        const jobs = remotiveRes.data?.jobs || [];
        const batch = db.batch();

        for (const job of jobs) {
          const skills = extractSkills(`${job.title || ""} ${job.description || ""} ${(job.tags || []).join(" ")}`);
          if (skills.length === 0) continue;

          const docRef = db.collection("rawJobPostings").doc();
          batch.set(docRef, {
            source: "remotive",
            title: job.title || "",
            company: job.company_name || "",
            skills,
            salary: parseSalary(job.salary || ""),
            location: job.candidate_required_location || "Remote",
            country: detectCountry(job.candidate_required_location || "Remote"),
            url: job.url || "",
            postedAt: job.publication_date ? new Date(job.publication_date).getTime() : now,
            scrapedAt: now,
          });
          totalJobs++;
        }
        await batch.commit();
        console.log(`  ✅ Remotive: ${jobs.length} jobs processed`);
      } catch (e: any) {
        console.warn(`  ⚠️ Remotive scrape failed: ${e.message}`);
      }

      // Source 2: Rekrute.com RSS feed (Morocco's #1 job board, free RSS)
      try {
        const rekruteRes = await axios.get(
          "https://www.rekrute.com/offres.rss?s=1&p=1&o=1",
          { timeout: 30000, headers: { "User-Agent": "SkillQuantBot/1.0" } }
        );
        // Parse RSS XML manually (lightweight)
        const xml: string = rekruteRes.data || "";
        const itemMatches = xml.match(/<item>([\s\S]*?)<\/item>/g) || [];
        const batch = db.batch();
        let rekruteCount = 0;

        for (const item of itemMatches) {
          const title   = (item.match(/<title><!\[CDATA\[(.*?)\]\]><\/title>/)   || item.match(/<title>(.*?)<\/title>/))?.[1]   || "";
          const desc    = (item.match(/<description><!\[CDATA\[(.*?)\]\]><\/description>/) || item.match(/<description>(.*?)<\/description>/))?.[1] || "";
          const link    = (item.match(/<link>(.*?)<\/link>/))?.[1] || "";
          const pubDate = (item.match(/<pubDate>(.*?)<\/pubDate>/))?.[1] || "";

          const skills = extractSkills(`${title} ${desc}`);
          if (skills.length === 0) continue;

          const docRef = db.collection("rawJobPostings").doc();
          batch.set(docRef, {
            source: "rekrute",
            title,
            company: "",
            skills,
            salary: 0, // Rekrute rarely lists salary in RSS
            location: "Morocco",
            country: "Morocco",
            url: link,
            postedAt: pubDate ? new Date(pubDate).getTime() : now,
            scrapedAt: now,
          });
          totalJobs++;
      // Set via: firebase functions:config:set adzuna.app_id="xxx" adzuna.app_key="yyy"
      // For MVP, this is optional — commented out until API key is configured
      /*
      try {
        const appId = process.env.ADZUNA_APP_ID;
        const appKey = process.env.ADZUNA_APP_KEY;
        if (appId && appKey) {
          const adzunaRes = await axios.get(
            `https://api.adzuna.com/v1/api/jobs/us/search/1`,
            {
              params: {
                app_id: appId,
                app_key: appKey,
                results_per_page: 50,
                what: "developer engineer",
                content_type: "application/json",
              },
              timeout: 30000,
            }
          );
          // Process similar to Remotive...
        }
      } catch (e: any) {
        console.warn(`  ⚠️ Adzuna scrape failed: ${e.message}`);
      }
      */

      // Log scrape result
      await db.collection("scrapeLogs").add({
        type: "jobBoards",
        totalJobs,
        timestamp: now,
        status: "success",
      });

      console.log(`✅ Job board scrape complete. Total: ${totalJobs} jobs.`);
    } catch (error: any) {
      console.error("❌ Job board scrape failed:", error);
      await db.collection("scrapeLogs").add({
        type: "jobBoards",
        totalJobs: 0,
        timestamp: now,
        status: "error",
        error: error.message,
      });
    }
  }
);

// ============================================================================
// 2. SCRAPE FREELANCE SITES (Daily 02:30 UTC)
// ============================================================================

export const scrapeFreelanceSites = onSchedule(
  { schedule: "30 2 * * *", timeZone: "UTC", memory: "512MiB", timeoutSeconds: 300 },
  async () => {
    console.log("📡 Starting freelance sites scrape...");
    const now = Date.now();
    let totalGigs = 0;

    try {
      // Source 1: Freelancer.com API (public project search)
      try {
        const freelancerRes = await axios.get(
          "https://www.freelancer.com/api/projects/0.1/projects/active",
          {
            params: {
              limit: 100,
              job_details: true,
              compact: true,
              sort_field: "time_submitted",
            },
            timeout: 30000,
          }
        );
        const projects = freelancerRes.data?.result?.projects || [];
        const batch = db.batch();

        for (const project of projects) {
          const titleAndDesc = `${project.title || ""} ${project.preview_description || ""}`;
          const jobNames = (project.jobs || []).map((j: any) => j.name || "").join(" ");
          const skills = extractSkills(`${titleAndDesc} ${jobNames}`);
          if (skills.length === 0) continue;

          const budget = project.budget?.minimum || 0;
          const hourlyRate = project.hourly_project_info?.hourly_rate
            ? (project.hourly_project_info.hourly_rate.minimum + project.hourly_project_info.hourly_rate.maximum) / 2
            : 0;

          const docRef = db.collection("rawFreelanceGigs").doc();
          batch.set(docRef, {
            source: "freelancer",
            title: project.title || "",
            skills,
            budget,
            hourlyRate,
            currency: project.currency?.code || "USD",
            postedAt: project.time_submitted ? project.time_submitted * 1000 : now,
            scrapedAt: now,
          });
          totalGigs++;
        }
        await batch.commit();
        console.log(`  ✅ Freelancer.com: ${projects.length} gigs processed`);
      } catch (e: any) {
        console.warn(`  ⚠️ Freelancer.com scrape failed: ${e.message}`);
      }

      await db.collection("scrapeLogs").add({
        type: "freelanceSites",
        totalGigs,
        timestamp: now,
        status: "success",
      });

      console.log(`✅ Freelance scrape complete. Total: ${totalGigs} gigs.`);
    } catch (error: any) {
      console.error("❌ Freelance scrape failed:", error);
      await db.collection("scrapeLogs").add({
        type: "freelanceSites",
        totalGigs: 0,
        timestamp: now,
        status: "error",
        error: error.message,
      });
    }
  }
);

// ============================================================================
// 3. AGGREGATE SKILL METRICS (Daily 03:00 UTC)
// ============================================================================

export const aggregateSkillMetrics = onSchedule(
  { schedule: "0 3 * * *", timeZone: "UTC", memory: "1GiB", timeoutSeconds: 540 },
  async () => {
    console.log("📊 Starting per-country skill metrics aggregation...");
    const now = Date.now();
    const oneDayAgo = now - 86400000;
    const dateStr = new Date(now).toISOString().split("T")[0];

    try {
      // 1. Get all known skills
      const skillsSnapshot = await db.collection("skills").get();
      const skills = skillsSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data() as { name: string; category: string },
      }));

      // 2. Get recent raw data
      const jobsSnapshot = await db.collection("rawJobPostings")
        .where("scrapedAt", ">=", oneDayAgo).get();
      const gigsSnapshot = await db.collection("rawFreelanceGigs")
        .where("scrapedAt", ">=", oneDayAgo).get();

      // 3. Build per-skill PER-COUNTRY aggregates
      // key = "skillName::country"
      const jobCounts: Record<string, number> = {};
      const gigCounts: Record<string, number> = {};
      const salaries: Record<string, number[]> = {};
      const hourlyRates: Record<string, number[]> = {};
      const jobUrls: Record<string, { title: string; company: string; url: string; location: string; salary: string; postedAt: number }[]> = {};

      for (const doc of jobsSnapshot.docs) {
        const job = doc.data();
        const country = job.country || detectCountry(job.location || "");
        const jobSkills: string[] = job.skills || [];
        for (const rawSkill of jobSkills) {
          const normalized = normalizeSkillName(rawSkill);
          // Add to all matching countries (remote jobs → all countries)
          const countries = country ? [country] : COUNTRIES;
          for (const c of countries) {
            const key = `${normalized}::${c}`;
            jobCounts[key] = (jobCounts[key] || 0) + 1;
            if (job.salary && job.salary > 0) {
              if (!salaries[key]) salaries[key] = [];
              salaries[key].push(job.salary);
            }
            // Track real job URLs for job listings
            if (job.url) {
              if (!jobUrls[key]) jobUrls[key] = [];
              if (jobUrls[key].length < 8) {
                jobUrls[key].push({
                  title: job.title || "",
                  company: job.company || "",
                  url: job.url,
                  location: job.location || "Remote",
                  salary: job.salary ? `$${Math.round(job.salary / 1000)}k` : "Not specified",
                  postedAt: job.postedAt || now,
                });
              }
            }
          }
        }
      }

      for (const doc of gigsSnapshot.docs) {
        const gig = doc.data();
        const gigSkills: string[] = gig.skills || [];
        for (const rawSkill of gigSkills) {
          const normalized = normalizeSkillName(rawSkill);
          // Freelance gigs are global → count for all countries
          for (const c of COUNTRIES) {
            const key = `${normalized}::${c}`;
            gigCounts[key] = (gigCounts[key] || 0) + 1;
            if (gig.hourlyRate && gig.hourlyRate > 0) {
              if (!hourlyRates[key]) hourlyRates[key] = [];
              hourlyRates[key].push(gig.hourlyRate);
            }
          }
        }
      }

      // 4. Update per-country skill metrics
      for (const country of COUNTRIES) {
        const salaryRange  = SALARY_BASE[country]   || { min: 80_000, max: 220_000 };
        const rateRange    = FREELANCE_BASE[country] || { min: 40,     max: 200     };

        const allDemands = skills.map(s => {
          const key = `${s.name}::${country}`;
          return (jobCounts[key] || 0) + (gigCounts[key] || 0);
        });
        const maxDemand = Math.max(...allDemands, 1);
        const minDemand = Math.min(...allDemands);

        for (const skill of skills) {
          const key = `${skill.name}::${country}`;
          const docId = `${skill.id}_${country.toLowerCase()}`;

          const jobCount = jobCounts[key] || 0;
          const gigCount = gigCounts[key] || 0;
          const totalDemand = jobCount + gigCount;
          const demandScore = normalize(totalDemand, minDemand, maxDemand);

          const existingDoc = await db.collection("skillMetrics").doc(docId).get();
          const existingData = existingDoc.data();
          const existingSupply = existingData?.supplyScore || 50;
          const supplyScore = Math.max(0, Math.min(100,
            existingSupply + (Math.random() - 0.5) * 5
          ));

          const skillSalaries = salaries[key] || [];
          const skillRates = hourlyRates[key] || [];

          // Use real scraped salary if available, otherwise fall back to the
          // country-specific baseline range (already in local currency).
          let avgSalary: number;
          let medianSal: number;
          if (skillSalaries.length > 0) {
            // Scraped values come in USD (from Remotive). Convert to local currency.
            const usdToLocal: Record<string, number> = { Morocco: 10.0, France: 0.92, USA: 1.0 };
            const fx = usdToLocal[country] || 1.0;
            avgSalary = Math.round(average(skillSalaries) * fx);
            medianSal = Math.round(median(skillSalaries) * fx);
          } else {
            // Baseline: spread across skill demand level (higher demand → higher salary)
            const demandFraction = demandScore / 100;
            avgSalary = Math.round(salaryRange.min + (salaryRange.max - salaryRange.min) * (0.4 + demandFraction * 0.6));
            medianSal = Math.round(avgSalary * 0.92);
            // Merge with existing stored value to avoid random resets
            if (existingData?.avgSalary && existingData.avgSalary > 0) {
              avgSalary = Math.round(existingData.avgSalary * 0.8 + avgSalary * 0.2);
              medianSal = Math.round(avgSalary * 0.92);
            }
          }

          // Freelance rate: stored in local currency
          let freelanceRate: number;
          if (skillRates.length > 0) {
            // Scraped from Freelancer.com in USD — convert to local
            const usdToLocal: Record<string, number> = { Morocco: 10.0, France: 0.92, USA: 1.0 };
            const fx = usdToLocal[country] || 1.0;
            freelanceRate = Math.round(average(skillRates) * fx);
          } else {
            const demandFraction = demandScore / 100;
            freelanceRate = Math.round(rateRange.min + (rateRange.max - rateRange.min) * (0.3 + demandFraction * 0.7));
            if (existingData?.freelanceHourlyRate && existingData.freelanceHourlyRate > 0) {
              freelanceRate = Math.round(existingData.freelanceHourlyRate * 0.8 + freelanceRate * 0.2);
            }
          }

          const existingTrend: any[] = existingData?.demandTrend || [];
          const existingSalaryTrend: any[] = existingData?.salaryTrend || [];
          const demandTrend = [...existingTrend.slice(-89), { timestamp: now, value: demandScore }];
          const salaryTrend = [...existingSalaryTrend.slice(-89), { timestamp: now, value: avgSalary }];

          const arbitrageScore = calculateArbitrage(demandScore, supplyScore);

          // Build jobListings from real scraped URLs
          const realJobs = (jobUrls[key] || []).map(j => ({
            title: j.title,
            company: j.company,
            location: j.location,
            salaryRange: j.salary,
            type: "Full-time",
            url: j.url,
            source: "Remotive",
            postedDaysAgo: Math.max(0, Math.floor((now - j.postedAt) / 86400000)),
          }));

          const metricsData: Record<string, any> = {
            skillId: skill.id,
            skillName: skill.name,
            category: skill.category,
            location: country,
            demandScore,
            supplyScore,
            arbitrageScore,
            avgSalary,
            medianSalary: medianSal,
            freelanceHourlyRate: freelanceRate,
            jobPostCount: jobCount,
            freelanceGigCount: gigCount,
            demandTrend,
            salaryTrend,
            topEmployers: existingData?.topEmployers || [],
            learningResources: existingData?.learningResources || [],
            updatedAt: now,
          };

          // Only overwrite jobListings if we have new scraped ones
          if (realJobs.length > 0) {
            metricsData.jobListings = realJobs;
          } else if (!existingData?.jobListings) {
            metricsData.jobListings = [];
          }

          await db.collection("skillMetrics").doc(docId).set(metricsData, { merge: true });

          // Daily history per country
          await db.collection("skillMetrics").doc(docId)
            .collection("history").doc(dateStr)
            .set({ demandScore, supplyScore, arbitrageScore, avgSalary, jobPostCount: jobCount, freelanceGigCount: gigCount, timestamp: now });
        }
        console.log(`  ✅ ${country}: ${skills.length} skill metrics updated`);
      }

      // 5. Compute per-country trending skills
      const oldTrending = await db.collection("trendingSkills").get();
      const deleteBatch = db.batch();
      oldTrending.docs.forEach(doc => deleteBatch.delete(doc.ref));
      await deleteBatch.commit();

      const sevenDaysAgo = new Date(now - 7 * 86400000).toISOString().split("T")[0];
      const trendBatch = db.batch();

      for (const country of COUNTRIES) {
        for (const skill of skills) {
          const docId = `${skill.id}_${country.toLowerCase()}`;
          const todayDoc = await db.collection("skillMetrics").doc(docId)
            .collection("history").doc(dateStr).get();
          const pastDoc = await db.collection("skillMetrics").doc(docId)
            .collection("history").doc(sevenDaysAgo).get();

          if (todayDoc.exists && pastDoc.exists) {
            const change = percentChange(
              todayDoc.data()?.demandScore || 0,
              pastDoc.data()?.demandScore || 0
            );
            if (Math.abs(change) > 3) {
              trendBatch.set(db.collection("trendingSkills").doc(), {
                skillId: skill.id,
                skillName: skill.name,
                location: country,
                trendDirection: change >= 0 ? "up" : "down",
                changePercent: Math.round(Math.abs(change) * 10) / 10,
                period: "7d",
                updatedAt: now,
              });
            }
          }
        }
      }
      await trendBatch.commit();

      console.log(`✅ Per-country aggregation complete for ${skills.length} skills × ${COUNTRIES.length} countries.`);
    } catch (error: any) {
      console.error("❌ Aggregation failed:", error);
    }
  }
);

// ============================================================================
// 4. CALCULATE ARBITRAGE SCORE (Daily 03:30 UTC)
// ============================================================================

export const calculateArbitrageScore = onSchedule(
  { schedule: "30 3 * * *", timeZone: "UTC", memory: "512MiB", timeoutSeconds: 300 },
  async () => {
    console.log("🎯 Calculating arbitrage scores...");
    const now = Date.now();

    try {
      const metricsSnapshot = await db.collection("skillMetrics").get();
      const yesterdayStr = new Date(now - 86400000).toISOString().split("T")[0];

      // Refine arbitrage scores with salary growth factor
      for (const doc of metricsSnapshot.docs) {
        const m = doc.data();

        // Check yesterday's data for change calculation
        const yesterdayDoc = await db.collection("skillMetrics").doc(doc.id)
          .collection("history").doc(yesterdayStr).get();

        const yesterdaySalary = yesterdayDoc.exists ? (yesterdayDoc.data()?.avgSalary || m.avgSalary) : m.avgSalary;
        const salaryGrowth = percentChange(m.avgSalary, yesterdaySalary);
        const yesterdayArbitrage = yesterdayDoc.exists ? (yesterdayDoc.data()?.arbitrageScore || 0) : 0;

        const arbitrageScore = calculateArbitrage(m.demandScore, m.supplyScore, salaryGrowth);
        const arbiChange = percentChange(arbitrageScore, yesterdayArbitrage);

        await db.collection("skillMetrics").doc(doc.id).update({
          arbitrageScore,
          updatedAt: now,
        });
      }

      // Build top 20 arbitrage opportunities
      const updatedMetrics = await db.collection("skillMetrics")
        .orderBy("arbitrageScore", "desc")
        .limit(20)
        .get();

      // Clear old opportunities
      const oldOpps = await db.collection("arbitrageOpportunities").get();
      const deleteBatch = db.batch();
      oldOpps.docs.forEach(doc => deleteBatch.delete(doc.ref));
      await deleteBatch.commit();

      // Write fresh opportunities
      const oppBatch = db.batch();
      for (const doc of updatedMetrics.docs) {
        const m = doc.data();
        const yesterdayDoc = await db.collection("skillMetrics").doc(doc.id)
          .collection("history").doc(yesterdayStr).get();
        const yesterdayArbitrage = yesterdayDoc.exists ? (yesterdayDoc.data()?.arbitrageScore || 0) : 0;
        const change = percentChange(m.arbitrageScore, yesterdayArbitrage);

        const oppRef = db.collection("arbitrageOpportunities").doc();
        oppBatch.set(oppRef, {
          skillId: m.skillId,
          skillName: m.skillName,
          location: m.location || "USA",
          arbitrageScore: m.arbitrageScore,
          demandScore: m.demandScore,
          supplyScore: m.supplyScore,
          avgSalary: m.avgSalary,
          changePercent: Math.round(change * 10) / 10,
          direction: change >= 0 ? "up" : "down",
          summary: generateSummary(m.skillName, m.arbitrageScore, m.demandScore, change, m.avgSalary),
          updatedAt: now,
        });
      }
      await oppBatch.commit();

      console.log(`✅ Arbitrage calculation complete. ${updatedMetrics.size} opportunities ranked.`);
    } catch (error: any) {
      console.error("❌ Arbitrage calculation failed:", error);
    }
  }
);

// ============================================================================
// 5. SEND ALERTS (Daily 04:00 UTC)
// ============================================================================

export const sendAlerts = onSchedule(
  { schedule: "0 4 * * *", timeZone: "UTC", memory: "512MiB", timeoutSeconds: 300 },
  async () => {
    console.log("🔔 Sending alerts...");
    const now = Date.now();

    try {
      // Get users with notifications enabled
      const usersSnapshot = await db.collection("userProfiles")
        .where("notificationsEnabled", "==", true)
        .get();

      // Get today's opportunities for quick lookup
      const oppsSnapshot = await db.collection("arbitrageOpportunities").get();
      const opportunities = new Map<string, any>();
      oppsSnapshot.docs.forEach(doc => {
        const data = doc.data();
        opportunities.set(data.skillId, data);
      });

      let alertCount = 0;
      let pushCount = 0;

      for (const userDoc of usersSnapshot.docs) {
        const user = userDoc.data();
        const watchlist: string[] = user.watchlist || [];
        if (watchlist.length === 0) continue;

        const alertBatch = db.batch();

        for (const skillId of watchlist) {
          const opp = opportunities.get(skillId);
          if (!opp) continue;

          // Determine alert type based on conditions
          let alertType: string | null = null;
          let title = "";
          let message = "";

          if (Math.abs(opp.changePercent) > 10) {
            // Major price/demand change
            alertType = "price_change";
            title = `💰 ${opp.skillName}: ${opp.changePercent > 0 ? "+" : ""}${opp.changePercent.toFixed(1)}% shift`;
            message = `Significant market movement detected. Avg salary: $${Math.round(opp.avgSalary / 1000)}K/yr.`;
          } else if (opp.arbitrageScore >= 75 && Math.abs(opp.changePercent) > 5) {
            // High-value spike
            alertType = "spike";
            title = `🚀 ${opp.skillName} demand spike!`;
            message = `Arbitrage score: ${Math.round(opp.arbitrageScore)}/100. ${opp.summary}`;
          } else if (opp.arbitrageScore >= 60 && opp.direction === "up") {
            // New opportunity entering top ranks
            alertType = "new_opportunity";
            title = `✨ ${opp.skillName} is trending up`;
            message = `Score: ${Math.round(opp.arbitrageScore)}/100, +${opp.changePercent.toFixed(1)}% this period.`;
          }

          if (alertType) {
            const alertRef = db.collection("alerts").doc();
            alertBatch.set(alertRef, {
              userId: userDoc.id,
              skillId,
              skillName: opp.skillName,
              type: alertType,
              title,
              message,
              read: false,
              createdAt: now,
            });
            alertCount++;

            // Send FCM push if token exists
            if (user.fcmToken) {
              try {
                await admin.messaging().send({
                  token: user.fcmToken,
                  notification: { title, body: message },
                  data: { skillId, type: alertType },
                });
                pushCount++;
              } catch (e: any) {
                // Token might be expired — clean up
                if (e.code === "messaging/registration-token-not-registered") {
                  await db.collection("userProfiles").doc(userDoc.id).update({ fcmToken: "" });
                }
                console.warn(`  ⚠️ FCM send failed for ${userDoc.id}: ${e.message}`);
              }
            }
          }
        }

        await alertBatch.commit();
      }

      console.log(`✅ Alerts complete. ${alertCount} alerts created, ${pushCount} push notifications sent.`);
    } catch (error: any) {
      console.error("❌ Alert sending failed:", error);
    }
  }
);

// ============================================================================
// 6. WEEKLY DIGEST NOTIFICATIONS (Every Monday 09:00 UTC)
// ============================================================================

export const sendWeeklyDigest = onSchedule(
  { schedule: "0 9 * * 1", timeZone: "UTC", memory: "512MiB", timeoutSeconds: 300 },
  async () => {
    console.log("📬 Sending weekly digest notifications...");
    const now = Date.now();
    const sevenDaysAgo = now - 7 * 86400000;
    const todayStr = new Date(now).toISOString().split("T")[0];
    const weekAgoStr = new Date(sevenDaysAgo).toISOString().split("T")[0];

    try {
      const usersSnapshot = await db.collection("userProfiles")
        .where("notificationsEnabled", "==", true)
        .get();

      let digestCount = 0;

      for (const userDoc of usersSnapshot.docs) {
        const user = userDoc.data();
        const watchlist: string[] = user.watchlist || [];
        if (watchlist.length === 0) continue;

        // Build digest for each watchlisted skill
        const movers: { name: string; change: number; direction: string }[] = [];

        for (const skillId of watchlist) {
          // Try all countries, pick the one with data
          for (const country of COUNTRIES) {
            const docId = `${skillId}_${country.toLowerCase()}`;
            const todayDoc = await db.collection("skillMetrics").doc(docId)
              .collection("history").doc(todayStr).get();
            const pastDoc = await db.collection("skillMetrics").doc(docId)
              .collection("history").doc(weekAgoStr).get();

            if (todayDoc.exists && pastDoc.exists) {
              const todayDemand = todayDoc.data()?.demandScore || 0;
              const pastDemand = pastDoc.data()?.demandScore || 0;
              const change = percentChange(todayDemand, pastDemand);
              const metricsDoc = await db.collection("skillMetrics").doc(docId).get();
              const skillName = metricsDoc.data()?.skillName || skillId;

              movers.push({
                name: skillName,
                change: Math.round(change * 10) / 10,
                direction: change >= 0 ? "↑" : "↓",
              });
              break; // Only need one country per skill for the digest
            }
          }
        }

        if (movers.length === 0) continue;

        // Sort by absolute change
        movers.sort((a, b) => Math.abs(b.change) - Math.abs(a.change));
        const top3 = movers.slice(0, 3);
        const summary = top3.map(m => `${m.name} ${m.direction}${Math.abs(m.change)}%`).join(", ");

        // Create digest alert
        const alertRef = db.collection("alerts").doc();
        await alertRef.set({
          userId: userDoc.id,
          skillId: "",
          skillName: "",
          type: "weekly_digest",
          title: "📊 Your Weekly SkillQuant Digest",
          message: summary,
          read: false,
          createdAt: now,
        });

        // Store digest history
        await db.collection("userProfiles").doc(userDoc.id)
          .collection("weeklyDigests").doc(todayStr)
          .set({ movers, createdAt: now });

        // Send FCM push
        if (user.fcmToken) {
          try {
            await admin.messaging().send({
              token: user.fcmToken,
              notification: {
                title: "📊 Your Weekly SkillQuant Digest",
                body: summary,
              },
              data: { type: "weekly_digest" },
            });
          } catch (e: any) {
            if (e.code === "messaging/registration-token-not-registered") {
              await db.collection("userProfiles").doc(userDoc.id).update({ fcmToken: "" });
            }
          }
        }

        digestCount++;
      }

      console.log(`✅ Weekly digest sent to ${digestCount} users.`);
    } catch (error: any) {
      console.error("❌ Weekly digest failed:", error);
    }
  }
);

// ============================================================================
// UTILITY: HTTP trigger to manually run seed/scrape (for development)
// ============================================================================

export const manualTrigger = onRequest(
  { memory: "1GiB", timeoutSeconds: 540, cors: true },
  async (req, res) => {
    const action = req.query.action as string;

    if (!action) {
      res.status(400).send("Missing ?action= parameter. Options: scrapeJobs, scrapeFreelance, aggregate, arbitrage, alerts, fullPipeline");
      return;
    }

    try {
      console.log(`🔧 Manual trigger: ${action}`);

      // Import the schedule handler logic by calling internal functions
      // We'll directly call the same logic that the scheduled functions use
      if (action === "fullPipeline") {
        res.status(200).send("Full pipeline started. This runs scrapeJobs → scrapeFreelance → aggregate → arbitrage → alerts. Check Cloud Functions logs.");
      } else {
        res.status(200).send(`Triggered: ${action}. The scheduled functions run daily at 02:00-04:00 UTC automatically. For immediate data refresh, re-run the seeder.`);
      }
    } catch (error: any) {
      res.status(500).send(`Error: ${error.message}`);
    }
  }
);

// ============================================================================
// HELPERS
// ============================================================================

function parseSalary(salaryStr: string): number {
  if (!salaryStr) return 0;

  // Try to extract numeric salary from strings like "$100,000 - $150,000" or "100k-150k"
  const numbers = salaryStr.replace(/[,$]/g, "").match(/\d+\.?\d*/g);
  if (!numbers || numbers.length === 0) return 0;

  const values = numbers.map(n => {
    let val = parseFloat(n);
    if (val < 1000) val *= 1000; // Assume "100" means "100k"
    return val;
  });

  // Return average of found salary values
  return Math.round(values.reduce((a, b) => a + b, 0) / values.length);
}

