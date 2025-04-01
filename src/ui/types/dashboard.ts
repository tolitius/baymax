export interface SourceHealth {
  healthy: boolean;
  status: string;
  message?: string;
}

export interface CollectorItem {
  id: string;
  running?: boolean;
  source: string;
  schedule: string;
}

export interface SourceItem {
  id: string;
  type: string;
  health: SourceHealth;
}

export interface PublisherItem {
  type: string;
  collectors: string[];
}

export interface DashboardIntel {
  uptime: string;
  collectors: {
    count: number;
    items: CollectorItem[];
  };
  sources: {
    count: number;
    items: SourceItem[];
  };
  publishers: {
    count: number;
    items: PublisherItem[];
  };
}
