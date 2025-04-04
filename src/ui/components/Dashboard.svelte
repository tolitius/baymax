<script>
  import { buildUrl } from '../lib/utils';
  import StatusOverview from './dashboard/StatusOverview.svelte';
  import NavigationCards from './dashboard/NavigationCards.svelte';
  import CollectorsTable from './dashboard/CollectorsTable.svelte';
  import SourcesTable from './dashboard/SourcesTable.svelte';

  const rootUri = window.BAYMAX_CONFIG?.rootUri || '/';
  const currentYear = new Date().getFullYear();
</script>

<div class="min-h-screen flex flex-col bg-baymax">
  <header class="bg-white shadow-md">
    <div class="container mx-auto px-4 py-3 flex justify-between items-center">
      <div class="flex items-center space-x-2">
        <img src={buildUrl("img/baymax-logo.png")} alt="baymax logo" class="w-10 h-10">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">baymax</h1>
          <p class="text-sm text-gray-600">your visibility and insights loyal companion</p>
        </div>
      </div>
    </div>
  </header>

  <main class="flex-grow container mx-auto px-4 py-8">
    <StatusOverview />
    <NavigationCards />
    <CollectorsTable />
    <SourcesTable />
  </main>

  <footer class="bg-white shadow-md mt-8 py-6">
    <div class="container mx-auto px-4">
      <div class="flex flex-col md:flex-row justify-between items-center">
        <div class="mb-4 md:mb-0">
          <p class="text-sm text-gray-600">tolitius © {currentYear}</p>
        </div>
      </div>
    </div>
  </footer>
</div>
