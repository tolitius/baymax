/**
 * build a url with the root uri from the app config
 */
export function buildUrl(path: string): string {
  const rootUri = window.BAYMAX_CONFIG?.rootUri || '/';

  const normalizedRoot = rootUri.endsWith('/') ? rootUri.slice(0, -1) : rootUri;
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;

  return `${normalizedRoot}${normalizedPath}`;
}

/**
 * source type => icon
 */
export function findSourceTypeIcon(type: string): { icon: string, color: string, label: string } {
  switch (type) {
    case 'postgres':
      return {
        icon: 'fas fa-database',
        color: 'text-indigo-500',
        label: 'PostgreSQL'
      };
    case 'http':
      return {
        icon: 'fas fa-globe',
        color: 'text-blue-500',
        label: 'HTTP'
      };
    case 'jmx':
      return {
        icon: 'fas fa-cogs',
        color: 'text-orange-500',
        label: 'JMX'
      };
    default:
      return {
        icon: 'fas fa-question-circle',
        color: 'text-gray-500',
        label: type
      };
  }
}

/**
 * publisher type => icon
 */
export function findPublisherTypeIcon(type: string): { icon: string, color: string, label: string } {
  switch (type) {
    case 'prometheus':
      return {
        icon: 'fas fa-chart-line',
        color: 'text-orange-500',
        label: 'Prometheus'
      };
    case 'elasticsearch':
    case 'elastic':
      return {
        icon: 'fas fa-search',
        color: 'text-yellow-500',
        label: 'Elasticsearch'
      };
    case 'stdout':
      return {
        icon: 'fas fa-terminal',
        color: 'text-gray-500',
        label: 'Stdout'
      };
    default:
      return {
        icon: 'fas fa-question-circle',
        color: 'text-gray-500',
        label: type
      };
  }
}

/**
 * format a value with its label
 */
export function formatMetricValue(value: number, total: number): string {
  return `${value} / ${total}`;
}
