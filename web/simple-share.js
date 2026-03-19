// Script simple para agregar un botón de compartir en la página principal
document.addEventListener('DOMContentLoaded', function() {
  // Obtenemos referencias a los elementos necesarios
  const rightButtons = document.querySelector('.right-buttons');
  const viewRawButton = document.getElementById('viewRawButton');
  
  // Solo procedemos si encontramos el contenedor y el botón de vista expandida
  if (rightButtons && viewRawButton) {
    // Creamos un nuevo botón
    const shareButton = document.createElement('button');
    
    // Configuramos el botón con las mismas clases que el botón de vista expandida
    shareButton.id = 'simpleShareButton';
    shareButton.className = viewRawButton.className; // Copiamos las clases del botón de vista expandida
    shareButton.classList.remove('hidden'); // Aseguramos que no está oculto
    
    // Establecemos el contenido del botón con un ícono SVG
    shareButton.innerHTML = `
      <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <circle cx="18" cy="5" r="3"></circle>
        <circle cx="6" cy="12" r="3"></circle>
        <circle cx="18" cy="19" r="3"></circle>
        <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"></line>
        <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"></line>
      </svg>
      Share
    `;
    
    // Agregamos un evento de clic simple que abre el modal de compartir
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
    
    // Insertamos el botón antes del botón de vista expandida
    rightButtons.insertBefore(shareButton, viewRawButton);
    
    console.log('Share button added to main page');
  } else {
    console.error('Could not find right-buttons container or viewRawButton');
  }
});