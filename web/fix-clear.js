// Solución simple para el botón Clear
// Al cargar la página, esperar que todo esté cargado
window.addEventListener('load', function() {
  // Crear un nuevo botón para reemplazar el actual
  const newClearBtn = document.createElement('button');
  newClearBtn.id = 'clearButton2';
  newClearBtn.className = 'w-16 p-1 bg-error hover:bg-red-600 text-white text-xs rounded';
  newClearBtn.textContent = 'Clear';
  
  // Agregar el event listener que SOLO limpia el textarea
  newClearBtn.addEventListener('click', function() {
    document.getElementById('outputTextArea').value = '';
    document.getElementById('conversion-status').innerHTML = '';
  });
  
  // Reemplazar el botón original con el nuevo botón
  setTimeout(function() {
    const originalBtn = document.getElementById('clearButton');
    if (originalBtn && originalBtn.parentNode) {
      originalBtn.parentNode.replaceChild(newClearBtn, originalBtn);
    }
  }, 500);
});