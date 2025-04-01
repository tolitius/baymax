import { writable } from 'svelte/store';
import type { DashboardIntel } from '../types/dashboard';

const defaultIntel: DashboardIntel = {
  uptime: '',
  collectors: { count: 0, items: [] },
  sources: { count: 0, items: [] },
  publishers: { count: 0, items: [] }
};

export const dashboardStore = writable<DashboardIntel>(defaultIntel);
