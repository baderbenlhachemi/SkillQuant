/**
 * SkillQuant - Scoring and normalization math utilities
 */

/**
 * Normalize a value to a 0-100 scale given known min/max bounds
 */
export function normalize(value: number, min: number, max: number): number {
  if (max === min) return 50;
  const normalized = ((value - min) / (max - min)) * 100;
  return Math.max(0, Math.min(100, normalized));
}

/**
 * Calculate arbitrage score from demand and supply scores.
 * High arbitrage = high demand + low supply.
 *
 * Formula: arbitrageScore = (demandScore * 0.6 + (100 - supplyScore) * 0.3 + salaryGrowthBonus * 0.1)
 */
export function calculateArbitrage(
  demandScore: number,
  supplyScore: number,
  salaryGrowthPercent: number = 0
): number {
  const demandComponent = demandScore * 0.6;
  const supplyGap = (100 - supplyScore) * 0.3;
  const growthBonus = Math.max(0, Math.min(100, salaryGrowthPercent * 2)) * 0.1;

  return Math.max(0, Math.min(100, demandComponent + supplyGap + growthBonus));
}

/**
 * Calculate percent change between two values
 */
export function percentChange(current: number, previous: number): number {
  if (previous === 0) return current > 0 ? 100 : 0;
  return ((current - previous) / Math.abs(previous)) * 100;
}

/**
 * Compute average of an array of numbers
 */
export function average(values: number[]): number {
  if (values.length === 0) return 0;
  return values.reduce((sum, v) => sum + v, 0) / values.length;
}

/**
 * Compute median of an array of numbers
 */
export function median(values: number[]): number {
  if (values.length === 0) return 0;
  const sorted = [...values].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 !== 0
    ? sorted[mid]
    : (sorted[mid - 1] + sorted[mid]) / 2;
}

/**
 * Generate a human-readable summary sentence for an arbitrage opportunity
 */
export function generateSummary(
  skillName: string,
  arbitrageScore: number,
  demandScore: number,
  changePercent: number,
  avgSalary: number
): string {
  const salaryStr = avgSalary >= 1000
    ? `$${Math.round(avgSalary / 1000)}K`
    : `$${avgSalary}`;

  if (arbitrageScore >= 75) {
    return `${skillName} shows exceptional opportunity: demand at ${Math.round(demandScore)}/100 with avg ${salaryStr}/yr salary. ` +
           `${Math.abs(changePercent).toFixed(1)}% ${changePercent >= 0 ? "growth" : "shift"} this period.`;
  } else if (arbitrageScore >= 50) {
    return `${skillName} has strong demand-supply gap. Avg compensation: ${salaryStr}/yr. ` +
           `Consider upskilling for ${Math.abs(changePercent).toFixed(1)}% market movement.`;
  } else {
    return `${skillName} market is moderately active with avg ${salaryStr}/yr. ` +
           `Score: ${Math.round(arbitrageScore)}/100.`;
  }
}

