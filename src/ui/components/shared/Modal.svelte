<script>
  import { onMount, createEventDispatcher } from 'svelte';

  export let show = false;
  export let title = "";

  const dispatch = createEventDispatcher();

  function closeModal() {
    show = false;
    dispatch('close');
  }

  function handleKeydown(event) {
    if (event.key === 'Escape' && show) {
      closeModal();
    }
  }

  // focus trap implementation
  let modalElement;

  onMount(() => {
    if (show) {
      // set focus to the modal when it appears
      modalElement?.focus();
    }

    // add event listener for keydown events
    window.addEventListener('keydown', handleKeydown);

    // cleanup
    return () => {
      window.removeEventListener('keydown', handleKeydown);
    };
  });
</script>

{#if show}
  <div
    class="fixed inset-0 flex items-center justify-center z-50"
    aria-labelledby="modal-title"
    role="dialog"
    aria-modal="true"
  >
    <!-- Use a button element for the backdrop to fix accessibility warnings -->
    <button
      class="fixed inset-0 bg-gray-900 bg-opacity-50 transition-opacity cursor-default"
      on:click={closeModal}
      on:keydown={handleKeydown}
      aria-label="Close modal"
      tabindex="-1"
    ></button>

    <!-- Modal panel -->
    <div
      class="bg-white rounded-lg shadow-xl overflow-hidden w-full max-w-xl mx-4 z-10 transform transition-all"
      bind:this={modalElement}
      tabindex="-1"
      >
      <div class="bg-white px-4 py-5 border-b border-gray-200 sm:px-6">
        <div class="flex items-center justify-between">
          <h3 id="modal-title" class="text-lg font-medium text-gray-900">{title}</h3>
          <button
            type="button"
            class="text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-baymax-primary"
            on:click={closeModal}
            aria-label="Close modal"
          >
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>

      <div class="px-4 py-5 sm:p-6">
        <slot></slot>
      </div>

      <div class="px-4 py-4 sm:px-6 bg-gray-50 border-t border-gray-200 flex justify-end">
        <slot name="footer">
          <button
            type="button"
            class="bg-baymax-primary text-white px-4 py-2 rounded hover:bg-blue-600 transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-baymax-primary"
            on:click={closeModal}
          >
            Close
          </button>
        </slot>
      </div>
    </div>
  </div>
{/if}
