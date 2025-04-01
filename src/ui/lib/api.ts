import type { DashboardIntel } from '../types/dashboard';

export async function fetchDashboardIntel(): Promise<DashboardIntel> {
  try {
    const response = await fetch('/dashboard');
    if (!response.ok) {
      throw new Error(`api error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('could not fetch dashboard intel:', error);
    throw error;
  }
}
