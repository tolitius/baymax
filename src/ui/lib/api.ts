import type { DashboardIntel } from '../types/dashboard';
import { buildUrl } from './utils';

/**
 * dashboard intel data from the baymax backend
 */
export async function fetchDashboardIntel(): Promise<DashboardIntel> {
  try {
    const response = await fetch(buildUrl('/dashboard'));
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Could not fetch dashboard intel:', error);
    throw error;
  }
}

/**
 * intel for a specific collector
 */
export async function fetchCollectorIntel(collectorId: string): Promise<any> {
  try {
    const response = await fetch(buildUrl(`/intel/${collectorId}`));
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Could not fetch intel for collector ${collectorId}:`, error);
    throw error;
  }
}

/**
 * all intel data
 */
export async function fetchAllIntel(): Promise<any> {
  try {
    const response = await fetch(buildUrl('/intel-all'));
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Could not fetch all intel:', error);
    throw error;
  }
}

/**
 * schedule status data
 */
export async function fetchScheduleStatus(): Promise<any> {
  try {
    const response = await fetch(buildUrl('/schedule/status'));
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Could not fetch schedule status:', error);
    throw error;
  }
}

/**
 * system health data
 */
export async function fetchSystemHealth(): Promise<any> {
  try {
    const response = await fetch(buildUrl('/schedule/health'));
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Could not fetch system health:', error);
    throw error;
  }
}
