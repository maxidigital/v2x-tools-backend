// Script original que solo corrige el botón de Clear
document.addEventListener('DOMContentLoaded', function() {
  // Agregar margen al botón clear
  setTimeout(function() {
    const clearButton = document.getElementById('clearButton');
    if (clearButton) {
      clearButton.classList.add('mr-2');
    }
  }, 500);
});