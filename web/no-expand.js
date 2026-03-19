// Script para eliminar el botón de vista expandida y agregar un botón de compartir
document.addEventListener('DOMContentLoaded', function() {
  // 1. Eliminar el botón de vista expandida
  const viewRawButton = document.getElementById('viewRawButton');
  if (viewRawButton) {
    // Ocultar el botón de expandir
    viewRawButton.style.display = 'none';
    console.log('Expand view button hidden');
  }
  
  // 2. Agregar el botón de compartir
  const rightButtons = document.querySelector('.right-buttons');
  
  if (rightButtons) {
    // Crear botón de compartir
    const shareButton = document.createElement('button');
    
    // Configurar el botón
    shareButton.id = 'mainShareButton';
    shareButton.className = 'p-1 bg-primary hover:bg-primary-hover text-white text-xs rounded';
    
    // Agregar icono y texto
    shareButton.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 inline-block" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <circle cx="18" cy="5" r="3"></circle>
        <circle cx="6" cy="12" r="3"></circle>
        <circle cx="18" cy="19" r="3"></circle>
        <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
        <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
      </svg>
      <span>Share</span>
    `;
    
    // Agregar evento de clic para abrir el modal
    shareButton.addEventListener('click', function() {
      const shareModal = document.getElementById('shareModal');
      if (shareModal) {
        shareModal.classList.remove('hidden');
        const emailInput = document.getElementById('emailInput');
        if (emailInput) {
          emailInput.focus();
        }
      }
    });
    
    // Añadir al contenedor
    rightButtons.appendChild(shareButton);
    console.log('Share button added');
  }
  
  // 3. Desactivar completamente la función viewRawContent
  if (typeof window.viewRawContent === 'function') {
    window.viewRawContent = function() {
      console.log('Expand view is disabled');
      return false;
    };
    console.log('Expand view function disabled');
  }
});