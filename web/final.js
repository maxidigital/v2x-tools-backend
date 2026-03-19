// Solución simple final

// 1. Arreglar el botón clear con margen
document.addEventListener('DOMContentLoaded', function() {
  // Agregar margen al botón clear
  setTimeout(function() {
    const clearButton = document.getElementById('clearButton');
    if (clearButton) {
      clearButton.classList.add('mr-2');
    }
  }, 500);
  
  // 2. Reemplazar la función viewRawContent para crear una ventana simple sin botón share
  setTimeout(function() {
    window.viewRawContent = function() {
      const outputContent = document.getElementById('outputTextArea').value;
      
      if (!outputContent) return;
      
      // Obtener el contenido de entrada
      const inputs = ['pasteTextArea', 'generateTextArea', 'minimalTextArea', 'randomTextArea'];
      let inputContent = '';
      
      for (const id of inputs) {
        const textarea = document.getElementById(id);
        if (textarea && !textarea.classList.contains('hidden') && textarea.value) {
          inputContent = textarea.value;
          break;
        }
      }
      
      // Formato de salida
      const outputFormat = document.getElementById('outputFormatSelect').value || 'Unknown';
      
      // HTML simplificado al máximo
      const htmlContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Expanded View</title>
        <style>
          body { font-family: monospace; padding: 20px; }
          h2 { margin: 10px 0; }
          pre { 
            background-color: #f5f5f5; 
            padding: 15px; 
            border: 1px solid #ddd; 
            border-radius: 5px;
            white-space: pre-wrap;
            overflow-x: auto;
          }
          button { cursor: pointer; padding: 5px 10px; margin: 5px 0; }
          .container { max-width: 1200px; margin: 0 auto; }
        </style>
      </head>
      <body>
        <div class="container">
          <button onclick="document.getElementById('inputContainer').style.display = 
                          document.getElementById('inputContainer').style.display === 'none' ? 'block' : 'none'">
            Toggle Input View
          </button>
          
          <div id="inputContainer" style="display: block;">
            <h2>Input Payload</h2>
            <pre>${inputContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
          </div>
          
          <h2>Output Payload (${outputFormat})</h2>
          <pre>${outputContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
        </div>
      </body>
      </html>
      `;
      
      // Crear la ventana
      const blob = new Blob([htmlContent], {type: 'text/html'});
      const url = URL.createObjectURL(blob);
      window.open(url, '_blank');
      
      // Limpiar URL
      setTimeout(() => URL.revokeObjectURL(url), 30000);
    };
  }, 500);
});