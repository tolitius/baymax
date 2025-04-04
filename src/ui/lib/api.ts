import type { DashboardIntel } from '../types/dashboard';

declare global {
  interface Window {
    BAYMAX_CONFIG: {
      rootUri: string;
    };
  }
}

// move to tools (once tools are a thing)
function buildUrl(path: string): string {
  const rootUri = window.BAYMAX_CONFIG?.rootUri || '/';

  // don't double up on slashes
  const normalizedRoot = rootUri.endsWith('/') ? rootUri.slice(0, -1) : rootUri;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${normalizedRoot}${normalizedPath}`;
}

export async function fetchDashboardIntel(): Promise<DashboardIntel> {
  try {
    const response = await fetch(buildUrl('/dashboard'));
    if (!response.ok) {
      throw new Error(`api error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('could not fetch dashboard intel:', error);
    throw error;
  }
}
