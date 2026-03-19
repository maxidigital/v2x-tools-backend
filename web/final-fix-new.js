// Solución definitiva para arreglar el clear button y quitar el share button de la vista expandida
document.addEventListener('DOMContentLoaded', function() {
  // PARTE 1: Arreglar el botón clear
  setTimeout(function() {
    // Encontrar el botón clear original
    var oldButton = document.getElementById('clearButton');
    
    if (oldButton) {
      // Crear un elemento div para reemplazar el botón, con mr-1 para margen derecho
      var newButtonHTML = '<button id="newClearButton" class="w-16 p-1 bg-error hover:bg-red-600 text-white text-xs rounded mr-2">Clear</button>';
      
      // Reemplazar el botón antiguo con este HTML
      oldButton.outerHTML = newButtonHTML;
      
      // Agregar onclick directamente después de insertar el botón
      var newButton = document.getElementById('newClearButton');
      if (newButton) {
        newButton.onclick = function() {
          // Vaciar textarea directamente
          document.getElementById('outputTextArea').value = '';
          document.getElementById('conversion-status').innerHTML = '';
          
          // Activar convertButton
          var convertButton = document.getElementById('convertButton');
          var convertButtonDisabled = document.getElementById('convertButtonDisabled');
          if (convertButton && convertButtonDisabled) {
            convertButton.classList.remove('hidden');
            convertButtonDisabled.classList.add('hidden');
          }
        };
      }
    }
  }, 500);
  
  // PARTE 2: Reemplazar la función viewRawContent para quitar el botón share
  setTimeout(function() {
    // Guardar la referencia original si existe
    var originalViewRawContent = window.viewRawContent;
    
    // Reemplazar la función con una versión sin botón share
    window.viewRawContent = function() {
      const outputContent = document.getElementById('outputTextArea').value;
      
      // Intentar obtener el textarea activo
      let inputContent = "";
      const pasteTextArea = document.getElementById('pasteTextArea');
      const generateTextArea = document.getElementById('generateTextArea');
      
      if (pasteTextArea && !pasteTextArea.classList.contains('hidden')) {
        inputContent = pasteTextArea.value;
      } else if (generateTextArea && !generateTextArea.classList.contains('hidden')) {
        inputContent = generateTextArea.value;
      }
      
      console.log("Opening raw view without share button");
      
      if (outputContent) {
        try {
          // Detectar dark mode
          const isDarkMode = document.documentElement.classList.contains('dark');
          
          // Determinar formato de entrada
          let inputFormat = "Unknown";
          
          // Para la pestaña paste, usar el formato seleccionado
          const sendFormatRadios = document.querySelectorAll('input[name="sendFormat"]');
          for (const radio of sendFormatRadios) {
            if (radio.checked) {
              inputFormat = radio.value;
              break;
            }
          }
          
          // O usar el dropdown de generate
          const generateFormatSelect = document.getElementById('generateFormatSelect');
          if (generateFormatSelect && generateTextArea && !generateTextArea.classList.contains('hidden')) {
            inputFormat = generateFormatSelect.value;
          }
          
          // Determinar formato de salida
          const outputFormat = document.getElementById('outputFormatSelect').value;
          
          // Generar HTML para la nueva ventana (sin botón share)
          const htmlContent = `
          <!DOCTYPE html>
          <html lang="en" ${isDarkMode ? 'class="dark-mode"' : ''}>
          <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Expanded View</title>
              <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500&family=Roboto+Mono:wght@400;500&display=swap" rel="stylesheet">
              <style>
                  body {
                      font-family: 'Inter', sans-serif;
                      background-color: ${isDarkMode ? '#1e293b' : '#f8fafc'};
                      color: ${isDarkMode ? '#e5e7eb' : '#333'};
                      padding: 20px;
                      margin: 0;
                  }
                  
                  .container {
                      max-width: 1200px;
                      margin: 0 auto;
                      display: flex;
                      flex-direction: column;
                      gap: 20px;
                  }
                  
                  h2 {
                      font-size: 1.2rem;
                      margin: 0 0 10px 0;
                      font-weight: 500;
                      color: ${isDarkMode ? '#3498db' : '#3498db'};
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                  }
                  
                  pre {
                      font-family: 'Roboto Mono', monospace;
                      font-weight: 400;
                      background-color: ${isDarkMode ? '#111827' : '#fff'};
                      color: ${isDarkMode ? '#e5e7eb' : '#333'};
                      padding: 20px;
                      border-radius: 4px;
                      border: 1px solid ${isDarkMode ? '#475569' : '#ddd'};
                      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                      white-space: pre-wrap;
                      word-wrap: break-word;
                      text-indent: 0;
                      margin: 0;
                      height: auto;
                      overflow-y: visible;
                  }
                  
                  .theme-toggle {
                      position: fixed;
                      top: 15px;
                      right: 15px;
                      width: 40px;
                      height: 40px;
                      border-radius: 50%;
                      background-color: #3498db;
                      color: white;
                      border: none;
                      display: flex;
                      align-items: center;
                      justify-content: center;
                      cursor: pointer;
                      box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                      z-index: 1000;
                  }
                  
                  .theme-toggle:hover {
                      background-color: #2980b9;
                  }
                  
                  .icon-light, .icon-dark {
                      width: 24px;
                      height: 24px;
                  }
                  
                  .hidden {
                      display: none;
                  }
                  
                  .collapsible {
                      cursor: pointer;
                      user-select: none;
                  }
                  
                  .collapse-icon {
                      width: 16px;
                      height: 16px;
                      margin-left: 5px;
                  }
                  
                  .format-badge {
                      font-size: 0.7rem;
                      padding: 2px 6px;
                      border-radius: 4px;
                      background-color: ${isDarkMode ? '#4B5563' : '#E5E7EB'};
                      color: ${isDarkMode ? '#E5E7EB' : '#1F2937'};
                      margin-left: 8px;
                      font-weight: normal;
                  }
              </style>
          </head>
          <body>
              <div class="container">
                  <div>
                      <h2>
                          <span class="collapsible" onclick="toggleInput()">
                              Input Payload
                              <span class="format-badge">${inputFormat}</span>
                              <svg xmlns="http://www.w3.org/2000/svg" class="collapse-icon" viewBox="0 0 20 20" fill="currentColor">
                                  <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd" />
                              </svg>
                          </span>
                      </h2>
                      <div id="inputContainer" style="display: block;">
                          <pre>${inputContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
                      </div>
                  </div>
                  
                  <div>
                      <h2>Output Payload <span class="format-badge">${outputFormat}</span></h2>
                      <pre>${outputContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
                  </div>
              </div>
              
              <button class="theme-toggle" id="themeToggle" title="Toggle dark mode" onclick="toggleTheme()">
                  <svg xmlns="http://www.w3.org/2000/svg" class="icon-light ${isDarkMode ? '' : 'hidden'}" id="lightIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <circle cx="12" cy="12" r="5"></circle>
                      <line x1="12" y1="1" x2="12" y2="3"></line>
                      <line x1="12" y1="21" x2="12" y2="23"></line>
                      <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"></line>
                      <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"></line>
                      <line x1="1" y1="12" x2="3" y2="12"></line>
                      <line x1="21" y1="12" x2="23" y2="12"></line>
                      <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"></line>
                      <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"></line>
                  </svg>
                  <svg xmlns="http://www.w3.org/2000/svg" class="icon-dark ${isDarkMode ? 'hidden' : ''}" id="darkIcon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"></path>
                  </svg>
              </button>
              
              <script>
                  // Toggle input display (show/hide)
                  function toggleInput() {
                      const inputContainer = document.getElementById('inputContainer');
                      if (inputContainer.style.display === 'none') {
                          inputContainer.style.display = 'block';
                      } else {
                          inputContainer.style.display = 'none';
                      }
                  }
                  
                  // Simple dark mode toggle that works directly with DOM
                  function toggleTheme() {
                      const body = document.body;
                      const isDark = body.style.backgroundColor === 'rgb(30, 41, 59)' || body.style.backgroundColor === '#1e293b';
                      const lightIcon = document.getElementById('lightIcon');
                      const darkIcon = document.getElementById('darkIcon');
                      
                      // Toggle body background
                      if (!isDark) {
                          // Switch to dark mode
                          body.style.backgroundColor = '#1e293b';
                          body.style.color = '#e5e7eb';
                          
                          // Update pre elements
                          const preElements = document.querySelectorAll('pre');
                          for (let i = 0; i < preElements.length; i++) {
                              preElements[i].style.backgroundColor = '#111827';
                              preElements[i].style.color = '#e5e7eb';
                              preElements[i].style.borderColor = '#475569';
                          }
                          
                          // Update format badges
                          const badgeElements = document.querySelectorAll('.format-badge');
                          for (let i = 0; i < badgeElements.length; i++) {
                              badgeElements[i].style.backgroundColor = '#4B5563';
                              badgeElements[i].style.color = '#E5E7EB';
                          }
                          
                          // Show/hide icons
                          lightIcon.classList.remove('hidden');
                          darkIcon.classList.add('hidden');
                      } else {
                          // Switch to light mode
                          body.style.backgroundColor = '#f8fafc';
                          body.style.color = '#333';
                          
                          // Update pre elements
                          const preElements = document.querySelectorAll('pre');
                          for (let i = 0; i < preElements.length; i++) {
                              preElements[i].style.backgroundColor = '#fff';
                              preElements[i].style.color = '#333';
                              preElements[i].style.borderColor = '#ddd';
                          }
                          
                          // Update format badges
                          const badgeElements = document.querySelectorAll('.format-badge');
                          for (let i = 0; i < badgeElements.length; i++) {
                              badgeElements[i].style.backgroundColor = '#E5E7EB';
                              badgeElements[i].style.color = '#1F2937';
                          }
                          
                          // Show/hide icons
                          lightIcon.classList.add('hidden');
                          darkIcon.classList.remove('hidden');
                      }
                  }
              </script>
          </body>
          </html>
          `;
          
          // Crear un blob y abrir en una nueva ventana
          const blob = new Blob([htmlContent], { type: 'text/html' });
          const url = URL.createObjectURL(blob);
          window.open(url, '_blank');
          
          // Limpiar después de un minuto
          setTimeout(() => {
              URL.revokeObjectURL(url);
          }, 60000);
        } catch (error) {
          console.error("Error opening raw view:", error);
          alert("Error opening raw view: " + error.message);
        }
      }
    };
    
    console.log("Replaced viewRawContent function to remove share button");
  }, 1000);
});