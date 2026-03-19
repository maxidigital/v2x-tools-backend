// v2xConverter.js

document.addEventListener('DOMContentLoaded', function() {
    // Font size storage key
    const FONT_SIZE_STORAGE_KEY = 'outputTextAreaFontSize';
    // Min and max font size in pixels
    const MIN_FONT_SIZE = 6;
    const MAX_FONT_SIZE = 24;
    // Default font size if not stored (14px)
    const DEFAULT_FONT_SIZE = 14;
    
    // Tab management is now handled by TabManager module
    
    // Rotating headline setup
    const isMobile = window.innerWidth < 768;
    const headlines = isMobile ? [
        "Convert V2X Instantly",
        "XML/JSON Output",
        "No Setup Required",
        "Secure Conversion"
    ] : [
        "Convert V2X Payloads Instantly",
        "Supports XML/JSON input/output",
        "No setup required",
        "Fast & secure conversion"
    ];
    let currentHeadlineIndex = 0;
    const headlineElement = document.getElementById('rotatingHeadline');
    
    // Function to rotate headlines
    function rotateHeadline() {
        // Fade out
        headlineElement.style.opacity = '0';
        
        // Change text after fade out
        setTimeout(() => {
            currentHeadlineIndex = (currentHeadlineIndex + 1) % headlines.length;
            headlineElement.textContent = headlines[currentHeadlineIndex];
            // Fade in
            headlineElement.style.opacity = '1';
        }, 500);
    }
    
    // Start rotating headlines every 3 seconds
    setInterval(rotateHeadline, 3000);
    // Update tooltip for buttons
    document.getElementById('copyButton1').title = "Copy to Clipboard";
    document.getElementById('copyButton').title = "Copy to Clipboard";
    
    // Dark mode toggle
    const darkModeToggle = document.getElementById('darkModeToggle');
    const iconLight = darkModeToggle.querySelector('.icon-light');
    const iconDark = darkModeToggle.querySelector('.icon-dark');
    
    // Check for saved theme preference or use device preference
    const theme = localStorage.getItem('theme') || 
                 (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');
    
    // Apply the theme
    if (theme === 'dark') {
        document.documentElement.classList.add('dark');
        // Mostrar el sol en modo oscuro (para indicar que se puede volver al modo claro)
        iconLight.classList.remove('hidden');
        iconDark.classList.add('hidden');
    } else {
        document.documentElement.classList.remove('dark');
        // Mostrar la luna en modo claro (para indicar que se puede cambiar al modo oscuro)
        iconLight.classList.add('hidden');
        iconDark.classList.remove('hidden');
    }
    
    // Toggle theme when button is clicked
    darkModeToggle.addEventListener('click', () => {
        const isDark = document.documentElement.classList.toggle('dark');
        
        // Toggle icons - invertido para mostrar el sol en modo oscuro y la luna en modo claro
        iconLight.classList.toggle('hidden', !isDark);
        iconDark.classList.toggle('hidden', isDark);
        
        // Cambiar título del botón
        darkModeToggle.title = isDark ? "Switch to light mode" : "Switch to dark mode";
        
        // Save preference
        localStorage.setItem('theme', isDark ? 'dark' : 'light');
    });
    
    // Store the last selected format to detect changes
    var lastSelectedFormat;
    // DOM Elements
    const inputFormatSelect = document.getElementById('inputFormatSelect');
    
    // Textareas for each tab
    const pasteTextArea = document.getElementById('pasteTextArea');
    const generateTextArea = document.getElementById('generateTextArea');
    const minimalTextArea = document.getElementById('minimalTextArea');
    const randomTextArea = document.getElementById('randomTextArea');
    
    // Current active textarea (starts with paste)
    let activeTextArea = pasteTextArea;
    
    const bytesLen = document.getElementById('bytes-len');
    const convertButton = document.getElementById('convertButton');
    const convertButtonDisabled = document.getElementById('convertButtonDisabled');
    
    // El botón clearButton ha sido eliminado del HTML
    // Ahora usamos una referencia nula para mantener compatibilidad con el código existente
    const clearButton = null;
    
    const clearInputButton = document.getElementById('clearInputButton');
    const copyButton = document.getElementById('copyButton');
	const copyButton1 = document.getElementById('copyButton1');
    const outputTextArea = document.getElementById('outputTextArea');
    const messageTypeSelector = document.getElementById('messageTypeSelector');
    const outputFormatSelect = document.getElementById('outputFormatSelect');
    
    // Tab buttons
    const pasteTabButton = document.getElementById('pasteTabButton');
    const generateTabButton = document.getElementById('generateTabButton');
    
    // Tab content panels
    const pasteTabContent = document.getElementById('pasteTabContent');
    const generateTabContent = document.getElementById('generateTabContent');
    
    // Message type selector for generate tab
    const generateMessageTypeSelector = document.getElementById('generateMessageTypeSelector');
    
    // Radio button groups and new format dropdown
    const generateSendFormatRadios = document.querySelectorAll('input[name="generateSendFormat"]');
    const generateFormatSelect = document.getElementById('generateFormatSelect');
    
    // Minimal payload checkbox
    const minimalPayloadCheckbox = document.getElementById('minimalPayloadCheckbox');
    
    // Legacy controls (for compatibility)
    const minimalMessageTypeSelector = document.getElementById('minimalMessageTypeSelector');
    const randomMessageTypeSelector = document.getElementById('randomMessageTypeSelector');
    const minimalSendFormatRadios = document.querySelectorAll('input[name="minimalSendFormat"]');
    const randomSendFormatRadios = document.querySelectorAll('input[name="randomSendFormat"]');

    // Event listeners for each textarea
    pasteTextArea.addEventListener('input', handleTextAreaInput);
    generateTextArea.addEventListener('input', handleTextAreaInput);
    minimalTextArea.addEventListener('input', handleTextAreaInput);
    randomTextArea.addEventListener('input', handleTextAreaInput);
    outputTextArea.addEventListener('input', updateButtonStates);
    
    // Legacy selector event listener (can be removed if no longer needed)
    if (messageTypeSelector) {
        messageTypeSelector.addEventListener('change', handleMessageTypeChange);
    }
    
    // Add event listeners for the new message type selectors
    generateMessageTypeSelector.addEventListener('change', handleGenerateMessageTypeChange);
    
    // Add event listener for minimal checkbox and generate button
    minimalPayloadCheckbox.addEventListener('change', function() {
        // No generamos automáticamente cuando cambia el checkbox,
        // ahora esperamos que el usuario haga clic en el botón Generate
        
        // Cambiar el texto del botón según el estado del checkbox
        const generateButton = document.getElementById('generatePayloadButton');
        if (this.checked) {
            generateButton.textContent = 'Generate minimal payload';
        } else {
            generateButton.textContent = 'Generate random payload';
        }
    });
    
    // Generate Payload Button
    const generatePayloadButton = document.getElementById('generatePayloadButton');
    if (generatePayloadButton) {
        console.log('Setting up generatePayloadButton event listener');
        generatePayloadButton.addEventListener('click', function() {
            console.log('Generate payload button clicked');
            updateGeneratePayload();
        });
    } else {
        console.error('generatePayloadButton not found in the DOM');
    }
    
    // Legacy event listeners for compatibility
    if (minimalMessageTypeSelector) {
        minimalMessageTypeSelector.addEventListener('change', handleMinimalMessageTypeChange);
    }
    if (randomMessageTypeSelector) {
        randomMessageTypeSelector.addEventListener('change', handleRandomMessageTypeChange);
    }
    
    outputFormatSelect.addEventListener('change', () => {
        console.log('Output format changed to:', outputFormatSelect.value);
        // Re-enable convert button if there's input and format has changed
        updateButtonStates();
    });
    
    convertButton.addEventListener('click', convertPayload);
    // El botón clearButton ha sido eliminado del HTML, por lo que ya no necesitamos configurar event listeners
    console.log('El botón clearButton ha sido eliminado');
    clearInputButton.addEventListener('click', clearInput);
    copyButton.addEventListener('click', copyToClipboard);
    copyButton1.addEventListener('click', copyInputToClipboard);
    
    // Share button functionality
    const shareButton = document.getElementById('shareButton');
    const shareDropdown = document.getElementById('shareDropdown');
    const shareCurrent = document.getElementById('shareCurrent');
    const shareAll = document.getElementById('shareAll');
    
    shareButton.addEventListener('click', function(e) {
        e.stopPropagation();
        shareDropdown.classList.toggle('hidden');
    });
    
    shareCurrent.addEventListener('click', function() {
        shareDropdown.classList.add('hidden');
        shareCurrentTab();
    });
    
    shareAll.addEventListener('click', function() {
        shareDropdown.classList.add('hidden');
        shareAllTabs();
    });
    
    // Download button functionality
    const downloadButton = document.getElementById('downloadButton');
    const downloadDropdown = document.getElementById('downloadDropdown');
    const downloadCurrentHTML = document.getElementById('downloadCurrentHTML');
    const downloadCurrentCSV = document.getElementById('downloadCurrentCSV');
    const downloadAllHTML = document.getElementById('downloadAllHTML');
    const downloadAllCSV = document.getElementById('downloadAllCSV');
    
    downloadButton.addEventListener('click', function(e) {
        e.stopPropagation();
        downloadDropdown.classList.toggle('hidden');
    });
    
    downloadCurrentHTML.addEventListener('click', function() {
        downloadDropdown.classList.add('hidden');
        downloadCurrentTab('html');
    });
    
    downloadCurrentCSV.addEventListener('click', function() {
        downloadDropdown.classList.add('hidden');
        downloadCurrentTab('csv');
    });
    
    downloadAllHTML.addEventListener('click', function() {
        downloadDropdown.classList.add('hidden');
        downloadAllTabs('html');
    });
    
    downloadAllCSV.addEventListener('click', function() {
        downloadDropdown.classList.add('hidden');
        downloadAllTabs('csv');
    });
    
    // Close dropdowns when clicking outside
    document.addEventListener('click', function() {
        shareDropdown.classList.add('hidden');
        downloadDropdown.classList.add('hidden');
    });
    
    // Expand button functionality
    const expandButton = document.getElementById('expandButton');
    expandButton.addEventListener('click', expandOutput);
    
    // Tab button event listeners
    pasteTabButton.addEventListener('click', activatePasteTab);
    generateTabButton.addEventListener('click', activateGenerateTab);
    
    // Add event listeners for radio buttons in generate tab (legacy)
    generateSendFormatRadios.forEach(radio => {
        radio.addEventListener('change', updateGeneratePayload);
    });
    
    // Add event listener for the new format dropdown
    generateFormatSelect.addEventListener('change', function() {
        // When dropdown changes, update the hidden radio button with the same value
        const selectedFormat = generateFormatSelect.value;
        const radioToSelect = document.querySelector(`input[name="generateSendFormat"][value="${selectedFormat}"]`);
        if (radioToSelect) {
            radioToSelect.checked = true;
        }
        
        // Clear textarea content when input format changes
        if (generateTextArea.value.trim().length > 0) {
            generateTextArea.value = '';
            updateBytesLen();
        }
        
        // Update button states since input format changed
        updateButtonStates();
    });
    
    // Legacy event listeners for compatibility
    minimalSendFormatRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            // Clear textarea content when input format changes
            if (minimalTextArea.value.trim().length > 0) {
                minimalTextArea.value = '';
                updateBytesLen();
            }
            updateMinimalPayload();
        });
    });
    
    randomSendFormatRadios.forEach(radio => {
        radio.addEventListener('change', function() {
            // Clear textarea content when input format changes
            if (randomTextArea.value.trim().length > 0) {
                randomTextArea.value = '';
                updateBytesLen();
            }
            updateRandomPayload();
        });
    });

    // Add event listeners for paste tab input format radios
    document.querySelectorAll('input[name="sendFormat"]').forEach(radio => {
        radio.addEventListener('change', function() {
            // Clear textarea content when input format changes
            if (pasteTextArea.value.trim().length > 0) {
                pasteTextArea.value = '';
                updateBytesLen();
            }
            updateButtonStates();
        });
    });

    // Ensure all buttons are properly initialized
    // IMPORTANTE: Esta función proporciona una capa adicional de protección para asegurar
    // que el botón clearButton funcione correctamente, incluso si hay problemas con
    // los event listeners estándar. Se ejecuta después de un breve retraso para asegurar
    // que el DOM esté completamente cargado.
    function reinitializeButtons() {
        console.log('Reinitializing buttons');
        // Re-get all button elements to ensure we have the latest references
        const btns = {
            clearInputBtn: document.getElementById('clearInputButton'),
            copyBtn: document.getElementById('copyButton'),
            copyBtn1: document.getElementById('copyButton1'),
            fontSizeDecreaseBtn: document.getElementById('fontSizeDecreaseButton'),
            fontSizeIncreaseBtn: document.getElementById('fontSizeIncreaseButton')
        };
        
        console.log('Button references refreshed:', btns);
        
        // Clear all button functionality removed - now handled by tab context menu
        
    }
    
    // Initial setup
    lastSelectedFormat = outputFormatSelect.value;
    
    // Set the initial value of generateFormatSelect based on radio button
    const initialGenerateFormat = document.querySelector('input[name="generateSendFormat"]:checked')?.value || "UPER";
    if (generateFormatSelect) {
        generateFormatSelect.value = initialGenerateFormat;
    }
    
    // Load saved state early to set correct tab before other initializations
    loadSavedState();
    
    updateButtonStates();
    
    // New handling functions for Generate tab
    function handleGenerateMessageTypeChange() {
        updateGeneratePayload();
    }
    
    // Run button re-initialization after a short delay to ensure DOM is ready
    setTimeout(reinitializeButtons, 500);

        // Tab activation functions
    function activatePasteTab() {
        // Update tab button styles
        pasteTabButton.classList.add('active');
        generateTabButton.classList.remove('active');
        
        // Update tab content visibility
        pasteTabContent.classList.add('active');
        pasteTabContent.classList.remove('hidden');
        generateTabContent.classList.remove('active');
        generateTabContent.classList.add('hidden');
        
        // Show the paste textarea and hide others
        pasteTextArea.classList.remove('hidden');
        generateTextArea.classList.add('hidden');
        minimalTextArea.classList.add('hidden');
        randomTextArea.classList.add('hidden');
        
        
        // Update active textarea reference
        activeTextArea = pasteTextArea;
        
        // Check if we should show format detection for this tab
        const formatDetectedElement = document.getElementById('format-auto-detected');
        const inputValue = pasteTextArea.value.trim();
        if (inputValue.length > 0) {
            // Try to detect format only if there's content in the textarea
            autoDetectFormat();
        } else {
            // Hide format detected indicator if there's no content
            formatDetectedElement.style.display = 'none';
        }
        
        // Save active tab state
        localStorage.setItem('v2xtools_active_tab', 'paste');
        
        updateBytesLen();
        updateButtonStates();
    }
    
    function activateGenerateTab() {
        // Update tab button styles
        pasteTabButton.classList.remove('active');
        generateTabButton.classList.add('active');
        
        // Update tab content visibility
        pasteTabContent.classList.remove('active');
        pasteTabContent.classList.add('hidden');
        generateTabContent.classList.add('active');
        generateTabContent.classList.remove('hidden');
        
        // Show the generate textarea and hide others
        pasteTextArea.classList.add('hidden');
        generateTextArea.classList.remove('hidden');
        minimalTextArea.classList.add('hidden');
        randomTextArea.classList.add('hidden');
        
        
        // Update active textarea reference
        activeTextArea = generateTextArea;
        
        // Hide format detection indicator in this tab
        const formatDetectedElement = document.getElementById('format-auto-detected');
        formatDetectedElement.style.display = 'none';
        
        // Ya no generamos automáticamente al cambiar de pestaña
        // El usuario debe usar el botón Generate
        
        // Save active tab state
        localStorage.setItem('v2xtools_active_tab', 'generate');
        
        updateBytesLen();
        updateButtonStates();
    }
    
    // Legacy activation functions for compatibility
    function activateMinimalTab() {
        activateGenerateTab();
        minimalPayloadCheckbox.checked = true;
        // Ya no generamos automáticamente
    }
    
    function activateRandomTab() {
        activateGenerateTab();
        minimalPayloadCheckbox.checked = false;
        // Ya no generamos automáticamente
    }
    
    // New handler functions for message type changes
    function handleGenerateMessageTypeChange() {
        // Save selected message type
        localStorage.setItem('v2xtools_message_type', generateMessageTypeSelector.value);
        
        // Ya no generamos automáticamente cuando cambia el tipo de mensaje
        // Esperamos a que el usuario haga clic en el botón Generate
    }
    
    // Legacy handler functions for compatibility
    function handleMinimalMessageTypeChange() {
        const messageType = minimalMessageTypeSelector.value;
        generateMinimalPayload(messageType);
    }
    
    function handleRandomMessageTypeChange() {
        const messageType = randomMessageTypeSelector.value;
        generateRandomPayload(messageType);
    }
    
    // New function to update payload based on generate tab selection
    function updateGeneratePayload() {
        console.log('updateGeneratePayload called');
        
        const messageType = generateMessageTypeSelector.value;
        const isMinimal = minimalPayloadCheckbox.checked;
        // Use the new dropdown value instead of radio buttons directly
        const selectedFormat = generateFormatSelect ? generateFormatSelect.value : document.querySelector('input[name="generateSendFormat"]:checked')?.value || "UPER";
        
        console.log('Generate params:', { 
            messageType, 
            isMinimal, 
            selectedFormat 
        });
        
        // Ya no es necesario verificar si messageType está vacío
        // porque ya no hay opción vacía en el dropdown
        
        // Copy value to legacy textareas for compatibility
        if (isMinimal) {
            if (minimalMessageTypeSelector) minimalMessageTypeSelector.value = messageType;
        } else {
            if (randomMessageTypeSelector) randomMessageTypeSelector.value = messageType;
        }
        
        // Siempre generamos el payload mediante una solicitud al backend
        console.log('Generating payload for', messageType, 'with format', selectedFormat, 'minimal:', isMinimal);
        
        // Mostrar mensaje de carga mientras esperamos la respuesta del backend
        generateTextArea.value = "Generating payload...";
        
        // Enviar solicitud al backend
        generateRandomPayloadWithOptions(messageType, selectedFormat, isMinimal);
    }
    
    // Legacy functions for compatibility
    function updateMinimalPayload() {
        const messageType = minimalMessageTypeSelector.value;
        generateMinimalPayload(messageType);
    }
    
    function updateRandomPayload() {
        const messageType = randomMessageTypeSelector.value;
        generateRandomPayload(messageType);
    }
    
    // Generate payloads based on options
    function generateMinimalPayload(messageType) {
        // Use selected format from minimal tab
        const selectedFormat = document.querySelector('input[name="minimalSendFormat"]:checked')?.value || "UPER";
        
        // Use backend request with minimal=true
        if (messageType) {
            // Generate via backend
            generateRandomPayloadWithOptions(messageType, selectedFormat, true);
        }
    }
    
    function generateRandomPayload(messageType) {
        // Use selected format from random tab
        const selectedFormat = document.querySelector('input[name="randomSendFormat"]:checked')?.value || "UPER";
        
        // Generate random payload without minimal flag
        generateRandomPayloadWithOptions(messageType, selectedFormat, false);
    }
    
    // Function that handles both random and minimal payload generation via backend
    function generateRandomPayloadWithOptions(messageType, format, isMinimal) {
        console.log('generateRandomPayloadWithOptions called with:', { messageType, format, isMinimal });
        
        if (!messageType) {
            console.error('No message type provided');
            return;
        }
        
        const midMap = {
            "cam_v2": "2:2",
            "denm_v2": "1:2",
            "cpm_v1": "14:1",
            "spat_v2": "4:2",
            "map_v2": "5:2",
            "ivi_v2": "6:2",
            "srem_v2": "9:2",
            "ssem_v2": "10:2"
        };
        
        const mid = midMap[messageType];
        if (!mid) {
            console.error('No mapping found for message type:', messageType);
            return;
        }
        
        // Construir la URL con el formato y la opción minimal (true o false)
        const url = `/v2xtools/command/random?mid=${mid}&format=${format}&minimal=${isMinimal}`;
        console.log('Fetching from URL:', url);
        
        // Mostrar mensaje de carga en el textarea si estamos en la pestaña Generate
        if (generateTabButton && generateTabButton.classList.contains('active')) {
            generateTextArea.value = "Generating payload...";
        }
        
        fetch(url)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.text();
            })
            .then(data => {
                console.log('Received payload data:', data.substring(0, 50) + '...');
                
                // Always update randomTextArea for compatibility
                randomTextArea.value = data;
                
                // Update generateTextArea if we're in Generate tab
                if (generateTabButton && generateTabButton.classList.contains('active')) {
                    console.log('Updating generateTextArea with payload');
                    generateTextArea.value = data;
                }
                
                updateBytesLen();
                updateButtonStates();
            })
            .catch(error => {
                console.error('Fetch error:', error);
                
                const errorMsg = "Error generating payload: " + error.message;
                randomTextArea.value = errorMsg;
                
                // Update generateTextArea if we're in Generate tab
                if (generateTabButton && generateTabButton.classList.contains('active')) {
                    generateTextArea.value = errorMsg;
                }
                
                updateButtonStates();
            });
    }

    function handleTextAreaInput() {
        updateBytesLen();
        autoDetectFormat();
        updateButtonStates();
    }
    
    function autoDetectFormat() {
        const inputValue = activeTextArea.value.trim();
        const formatDetectedElement = document.getElementById('format-auto-detected');
        const formatDetectedText = document.getElementById('format-auto-detected-text');
        const detectedFormatValue = document.getElementById('detected-format-value');
        
        // Hide the format detected indicator initially
        formatDetectedElement.style.display = 'none';
        
        // Only show and attempt auto-detection when in paste tab
        if (!pasteTabButton.classList.contains('active')) return;
        
        // Return early if there's no input and we're in paste tab
        if (inputValue.length === 0) return;
        
        // Asegurarse de que el indicador tenga la clase de éxito por defecto
        formatDetectedElement.classList.remove('text-warning');
        formatDetectedElement.classList.add('text-success');
        
        // Remove any prefix for detection
        const cleanInput = inputValue.replace(/^(uper:|wer:)/i, '');
        
        let detected = false;
        let detectedFormat = null;
        
        // Check if it's JSON
        if ((inputValue.startsWith('{') && inputValue.endsWith('}')) || 
            (inputValue.startsWith('[') && inputValue.endsWith(']'))) {
            try {
                JSON.parse(inputValue);
                detectedFormat = "JSON";
                detected = true;
            } catch (e) {
                // Not valid JSON, continue checking other formats
            }
        }
        
        // Check if it's XML
        if (!detected && inputValue.startsWith('<') && inputValue.endsWith('>')) {
            detectedFormat = "XML";
            detected = true;
        }
        
        // Check for UPER/WER format (hexadecimal)
        if (!detected) {
            const hexRegex = /^[0-9A-Fa-f]+$/;
            if (hexRegex.test(cleanInput)) {
                // If it has uper: prefix or starts with 02 (common ETSI ITS format identifier)
                if (inputValue.toLowerCase().startsWith('uper:') || cleanInput.startsWith('02')) {
                    detectedFormat = "UPER";
                    detected = true;
                } else if (inputValue.toLowerCase().startsWith('wer:')) {
                    detectedFormat = "WER";
                    detected = true;
                }
            }
        }
        
        // Mostrar el indicador tanto si se detectó como si no
        formatDetectedElement.style.display = 'flex';
        
        if (detected) {
            // Si se detectó, mostrar cuál formato se detectó
            formatDetectedText.innerHTML = `Format detected: <strong id="detected-format-value">${detectedFormat}</strong>`;
            
            // Seleccionar el formato detectado cuando estemos en la pestaña Paste
            const sendFormatRadios = document.querySelectorAll('input[name="sendFormat"]');
            sendFormatRadios.forEach(radio => {
                if (radio.value === detectedFormat) {
                    radio.checked = true;
                }
            });
        } else {
            // Si no se pudo detectar, mostrar un mensaje de advertencia
            formatDetectedElement.classList.remove('text-success');
            formatDetectedElement.classList.add('text-warning');
            formatDetectedText.innerHTML = `Format not detected`;
        }
    }

    // Esta función ya no es necesaria con la nueva estructura de tabs
    function handleMessageTypeChange() {
        // Mantener por compatibilidad, pero no hará nada
        console.log("Legacy handleMessageTypeChange called");
    }

    function updateBytesLen() {
		let inputValue = activeTextArea.value.trim();
    
		// Remove 'uper:' or 'wer:' prefix if present
		inputValue = inputValue.replace(/^(uper:|wer:)/i, '');
		
		// Regular expression to check if the string is a valid hexadecimal
		const hexRegex = /^[0-9A-Fa-f]+$/;
		
		if (hexRegex.test(inputValue)) {
			// Calculate the length, considering each pair of hex characters as one byte
			const len = Math.floor(inputValue.length / 2);
			bytesLen.textContent = `${len} bytes`;
		} else {
			// If not a valid hexadecimal, clear the bytes label
			bytesLen.textContent = '';
		}
	}

    function getCurrentInputFormat() {
        let inputFormat = "Unknown";
        
        // For paste tab, use selected format from radio buttons
        if (pasteTabButton.classList.contains('active')) {
            document.querySelectorAll('input[name="sendFormat"]').forEach(radio => {
                if (radio.checked) inputFormat = radio.value;
            });
        } 
        // For generate tab, use the dropdown format
        else if (generateTabButton.classList.contains('active')) {
            inputFormat = generateFormatSelect.value;
        }
        
        return inputFormat;
    }

    function updateButtonStates() {
        console.log('updateButtonStates called');
        
        const fontSizeDecreaseButton = document.getElementById('fontSizeDecreaseButton');
        const fontSizeIncreaseButton = document.getElementById('fontSizeIncreaseButton');
        const shareButton = document.getElementById('shareButton');
        const downloadButton = document.getElementById('downloadButton');
        const expandButton = document.getElementById('expandButton');
        
        if (!activeTextArea || !clearInputButton || !copyButton || !copyButton1 || !fontSizeDecreaseButton || !fontSizeIncreaseButton || !shareButton || !downloadButton || !expandButton) {
            console.error('Missing DOM elements in updateButtonStates:', {
                activeTextArea: !!activeTextArea,
                clearInputButton: !!clearInputButton,
                copyButton: !!copyButton, 
                copyButton1: !!copyButton1,
                fontSizeDecreaseButton: !!fontSizeDecreaseButton,
                fontSizeIncreaseButton: !!fontSizeIncreaseButton,
                shareButton: !!shareButton,
                downloadButton: !!downloadButton,
                expandButton: !!expandButton
            });
            return;
        }
        
        
        const inputContent = activeTextArea.value.trim();
        
        // Get output content from active tab
        const activeTab = window.TabManager.getActiveTab();
        const outputContent = activeTab ? activeTab.content.trim() : '';
        
        const currentFormat = outputFormatSelect.value;
        
        console.log('States:', {
            inputContentLength: inputContent.length,
            outputContentLength: outputContent.length,
            currentFormat: currentFormat
        });
        
        // Get current input format
        const inputFormat = getCurrentInputFormat();
        const outputFormat = outputFormatSelect.value;
        
        console.log('Format comparison:', {
            inputFormat: inputFormat,
            outputFormat: outputFormat,
            formatsEqual: inputFormat === outputFormat
        });
        
        // Convert button logic - disable if no input or same format
        const shouldDisableConvert = inputContent.length === 0 || inputFormat === outputFormat;
        
        // Mostrar/ocultar el botón activo o inactivo
        if (shouldDisableConvert) {
            convertButton.classList.add('hidden');
            convertButtonDisabled.classList.remove('hidden');
        } else {
            convertButton.classList.remove('hidden');
            convertButtonDisabled.classList.add('hidden');
        }
        
        // Update the last selected format
        lastSelectedFormat = currentFormat;
        
        // Button state logic
        const hasOutput = outputContent.length > 0;
        const hasActiveTab = activeTab !== null;
        console.log('Has output:', hasOutput, 'Has active tab:', hasActiveTab);
        
        // Set button states
        // clearButton ya no existe
        clearInputButton.disabled = inputContent.length === 0; // Solo desactivar si no hay input
        copyButton.disabled = !hasActiveTab; // Disabled when no tab is active
        copyButton1.disabled = inputContent.length === 0; // Solo se desactiva si no hay input
        shareButton.disabled = !(hasActiveTab && hasOutput && inputContent.length > 0); // Requiere tab activo, output y input
        downloadButton.disabled = !(hasActiveTab && hasOutput && inputContent.length > 0); // Same requirements as share
        expandButton.disabled = !hasActiveTab; // Disabled when no tab is active
        
        // Font size buttons
        fontSizeDecreaseButton.disabled = !hasActiveTab; // Disabled when no tab is active
        fontSizeIncreaseButton.disabled = !hasActiveTab; // Disabled when no tab is active
        
        // Disable generate format dropdown when textarea has content (in Generate tab)
        if (generateTabButton.classList.contains('active')) {
            const generateTextAreaContent = generateTextArea.value.trim();
            const hasGenerateContent = generateTextAreaContent.length > 0;
            
            if (hasGenerateContent) {
                generateFormatSelect.disabled = true;
                // generateFormatSelect.classList.add('opacity-50', 'cursor-not-allowed');
            } else {
                generateFormatSelect.disabled = false;
                generateFormatSelect.classList.remove('opacity-50', 'cursor-not-allowed');
            }
        }
        
        // Always enable output format dropdown since we now use tabs
        outputFormatSelect.disabled = false;
        outputFormatSelect.classList.remove('opacity-50', 'cursor-not-allowed');
        
        // Always keep input format select visually enabled
        document.getElementById('generateInputFormatSelect').classList.remove('opacity-50', 'pointer-events-none');

        // Always enable left panel controls since we now use tabs
        enableLeftPanelControls();
        
        console.log('Button states updated:', {
            convertDisabled: shouldDisableConvert,
            clearInputButtonDisabled: clearInputButton.disabled,
            copyButtonDisabled: copyButton.disabled,
            copyButton1Disabled: copyButton1.disabled,
            shareButtonDisabled: shareButton.disabled,
            downloadButtonDisabled: downloadButton.disabled,
            expandButtonDisabled: expandButton.disabled,
            fontSizeDecreaseButtonDisabled: fontSizeDecreaseButton.disabled,
            fontSizeIncreaseButtonDisabled: fontSizeIncreaseButton.disabled
        });
    }
    
    // Make updateButtonStates available globally for TabManager
    window.updateButtonStates = updateButtonStates;
    
    // Download functionality
    function downloadCurrentTab(format = 'html') {
        const activeTab = window.TabManager.getActiveTab();
        const inputContent = activeTextArea.value.trim();
        
        if (!activeTab) {
            showToast("No active tab to download");
            return;
        }
        
        if (!inputContent) {
            showToast("No input content to download");
            return;
        }
        
        const inputFormat = getCurrentInputFormat();
        const tabData = [{
            name: activeTab.customName || `${activeTab.format} ${activeTab.timestamp}`,
            inputFormat: inputFormat,
            outputFormat: activeTab.format,
            inputContent: inputContent,
            outputContent: activeTab.content
        }];
        
        if (format === 'csv') {
            const csvContent = generateCSV(tabData);
            downloadFile(csvContent, 'csv', false);
        } else {
            const htmlContent = generateSimpleHTML(tabData);
            downloadFile(htmlContent, 'html', false);
        }
    }
    
    function downloadAllTabs(format = 'html') {
        const allTabs = Array.from(window.TabManager.tabs.values());
        const inputContent = activeTextArea.value.trim();
        
        if (allTabs.length === 0) {
            showToast("No tabs to download");
            return;
        }
        
        if (!inputContent) {
            showToast("No input content to download");
            return;
        }
        
        const inputFormat = getCurrentInputFormat();
        const tabsData = allTabs.map(tab => ({
            name: tab.customName || `${tab.format} ${tab.timestamp}`,
            inputFormat: inputFormat,
            outputFormat: tab.format,
            inputContent: inputContent,
            outputContent: tab.content
        }));
        
        if (format === 'csv') {
            const csvContent = generateCSV(tabsData);
            downloadFile(csvContent, 'csv', true);
        } else {
            const htmlContent = generateSimpleHTML(tabsData);
            downloadFile(htmlContent, 'html', true);
        }
    }
    
    function generateSimpleHTML(tabsData) {
        const now = new Date();
        const dateTime = now.toISOString().replace(/[:.]/g, '-').slice(0, -5);
        
        let html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>V2X Conversion Data</title>
    <style>
        body { font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; background-color: #f8f9fa; }
        .header { background-color: #3498db; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; }
        .header h1 { margin: 0; font-size: 24px; }
        .header p { margin: 5px 0; opacity: 0.9; }
        .header a { color: #fff; text-decoration: none; font-weight: bold; }
        .tab-container { background-color: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .tab-title { color: #2c3e50; font-size: 20px; margin-bottom: 15px; padding-bottom: 10px; border-bottom: 2px solid #3498db; }
        .content-section { margin-bottom: 20px; }
        .content-label { color: #e67e22; font-size: 16px; font-weight: bold; margin-bottom: 8px; }
        .content-box { background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 4px; padding: 15px; font-family: 'Courier New', monospace; font-size: 12px; line-height: 1.4; overflow-x: auto; word-break: break-word; white-space: pre-wrap; }
        .footer { text-align: center; color: #6c757d; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #dee2e6; }
        .footer a { color: #3498db; text-decoration: none; }
    </style>
</head>
<body>
    <div class="header">
        <h1>V2X Conversion Data</h1>
        <p><strong>Generated:</strong> ${now.toLocaleString()}</p>
        <p><strong>Source:</strong> <a href="https://v2x.tools">v2x.tools</a></p>
    </div>
`;

        tabsData.forEach((tab, index) => {
            html += `
    <div class="tab-container">
        <div class="tab-title">Tab ${index + 1}: ${tab.name}</div>
        
        <div class="content-section">
            <div class="content-label">Input Data (${tab.inputFormat})</div>
            <div class="content-box">${escapeHtmlPreserveFormat(tab.inputContent)}</div>
        </div>
        
        <div class="content-section">
            <div class="content-label">Output Data (${tab.outputFormat})</div>
            <div class="content-box">${escapeHtmlPreserveFormat(tab.outputContent)}</div>
        </div>
    </div>
`;
        });

        html += `
    <div class="footer">
        <p>Generated by <a href="https://v2x.tools">v2x.tools</a></p>
    </div>
</body>
</html>`;

        return html;
    }
    
    function generateCSV(tabsData) {
        let csv = 'Tab Name,Input Format,Input Content,Output Format,Output Content\n';
        
        // Function to normalize content for CSV (remove line breaks and spaces between tags)
        const normalizeContent = (content) => {
            if (!content) return '';
            return content
                .replace(/\n/g, '')            // Remove line breaks
                .replace(/\r/g, '')            // Remove carriage returns
                .replace(/>\s+</g, '><')       // Remove spaces between tags
                .replace(/\s+/g, ' ')          // Replace remaining multiple spaces with single space
                .trim();                       // Remove leading/trailing spaces
        };
        
        tabsData.forEach(tab => {
            // Escape CSV fields by wrapping in quotes and doubling internal quotes
            const escapeCsvField = (field) => {
                if (field == null) return '""';
                const stringField = String(field);
                const escaped = stringField.replace(/"/g, '""');
                return `"${escaped}"`;
            };
            
            // Normalize content before escaping
            const normalizedInputContent = normalizeContent(tab.inputContent);
            const normalizedOutputContent = normalizeContent(tab.outputContent);
            
            csv += `${escapeCsvField(tab.name)},${escapeCsvField(tab.inputFormat)},${escapeCsvField(normalizedInputContent)},${escapeCsvField(tab.outputFormat)},${escapeCsvField(normalizedOutputContent)}\n`;
        });
        
        return csv;
    }
    
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
    
    function escapeHtmlPreserveFormat(text) {
        if (!text) return '';
        // Escape HTML but preserve line breaks and formatting
        const div = document.createElement('div');
        div.textContent = text;
        // Convert line breaks to <br> tags for HTML display
        return div.innerHTML.replace(/\n/g, '<br>');
    }
    
    function downloadFile(content, fileType, isMultiple) {
        const now = new Date();
        const dateTime = now.toISOString().replace(/[:.]/g, '-').slice(0, -5);
        const filename = `v2x.tools_${dateTime}.${fileType}`;
        
        const mimeTypes = {
            'html': 'text/html',
            'csv': 'text/csv'
        };
        
        const blob = new Blob([content], { type: mimeTypes[fileType] || 'text/plain' });
        const url = URL.createObjectURL(blob);
        
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        link.style.display = 'none';
        
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        URL.revokeObjectURL(url);
        
        const scope = isMultiple ? "All tabs" : "Current tab";
        const message = `${scope} downloaded as ${fileType.toUpperCase()}`;
        showToast(message);
    }

    function convertPayload() {
        const userInput = activeTextArea.value.trim();
        const conversionStatus = document.getElementById('conversion-status');

        if (userInput === "") {
            alert("Please enter or generate a payload before converting.");
            return;
        }

        // Determinar el formato a enviar según la pestaña activa
        let formatToSend;
        
        if (pasteTabButton.classList.contains('active')) {
            // En la pestaña Paste, usar el formato detectado o seleccionado
            formatToSend = document.querySelector('input[name="sendFormat"]:checked')?.value || "UPER";
        } else if (generateTabButton.classList.contains('active')) {
            // En la pestaña Generate, usar el formato seleccionado en el dropdown
            formatToSend = generateFormatSelect ? generateFormatSelect.value : document.querySelector('input[name="generateSendFormat"]:checked')?.value || "UPER";
        } else if (minimalTabButton && minimalTabButton.classList.contains('active')) {
            // En la pestaña Minimal (legacy), usar el formato seleccionado en esa pestaña
            formatToSend = document.querySelector('input[name="minimalSendFormat"]:checked')?.value || "UPER";
        } else if (randomTabButton && randomTabButton.classList.contains('active')) {
            // En la pestaña Random (legacy), usar el formato seleccionado en esa pestaña
            formatToSend = document.querySelector('input[name="randomSendFormat"]:checked')?.value || "UPER";
        } else {
            // Valor por defecto
            formatToSend = "UPER";
        }
        
        const formatToReceive = outputFormatSelect.value;
        
        // Verificar si el formato de entrada y salida son iguales
        if (formatToSend === formatToReceive) {
            // No es necesario enviar la solicitud al backend
            outputTextArea.value = userInput;
            
            // Mostrar mensaje de éxito indicando que se usó el mismo contenido
            conversionStatus.innerHTML = `
                <span style="display: flex; align-items: center; color: var(--success-color);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 5px;">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                        <polyline points="22 4 12 14.01 9 11.01"></polyline>
                    </svg>
                    Content copied (same format: ${formatToSend})
                </span>
            `;
            
            // Create a new tab even for same format (input is same as content)
            window.TabManager.createTab(formatToReceive, userInput);
            
            updateButtonStates();
            return;
        }
        
        // Si los formatos son diferentes, enviamos al backend
        outputTextArea.value = "Request sent. Awaiting response...";
        
        // Clear previous status
        conversionStatus.innerHTML = '';
        conversionStatus.className = '';

        disableAllInputs();

        // Fetch original URL
        fetch('', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                textData: userInput,
                sendFormat: formatToSend,
                receiveFormat: formatToReceive
            })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(response.status === 400 ? 'Bad Request: Check your input' : 'Network response was not ok.');
            }
            return response.text();
        })
        .then(text => {
            outputTextArea.value = text;
            updateSyntaxHighlighting();
            
            // Verificar si la respuesta contiene un mensaje de error específico del backend
            if (text.startsWith('Error:') || text.startsWith('Message could not be decoded')) {
                // Mostrar mensaje de error
                conversionStatus.innerHTML = `
                    <span style="display: flex; align-items: center; color: var(--error-color);">
                        <svg xmlns="http://www.w3.org/2000/svg" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 2px;">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="15" y1="9" x2="9" y2="15"></line>
                            <line x1="9" y1="9" x2="15" y2="15"></line>
                        </svg>
                        Conversion failed
                    </span>
                `;
                
                // Track conversion failure
                if (typeof gtag !== 'undefined') {
                    gtag('event', 'conversion_failed', {
                        'custom_parameter_1': formatToSend,
                        'custom_parameter_2': formatToReceive,
                        'value': 0
                    });
                }
            } else {
                // Mostrar mensaje de éxito solo si no hay errores
                conversionStatus.innerHTML = '';
                // Create a new tab for the conversion using TabManager
                window.TabManager.createTab(formatToReceive, text, userInput);
                
                // Track successful conversion
                if (typeof gtag !== 'undefined') {
                    gtag('event', 'conversion_success', {
                        'custom_parameter_1': formatToSend,
                        'custom_parameter_2': formatToReceive,
                        'value': 1
                    });
                }
            }
            
            updateButtonStates();
            enableAllInputs();
        })
        .catch(error => {
            outputTextArea.value = "Error decoding message: " + error.message;
            // Show error status
            conversionStatus.innerHTML = `
                <span style="display: flex; align-items: center; color: var(--error-color);">
                    <svg xmlns="http://www.w3.org/2000/svg" width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right: 2px;">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="15" y1="9" x2="9" y2="15"></line>
                        <line x1="9" y1="9" x2="15" y2="15"></line>
                    </svg>
                    Conversion failed
                </span>
            `;
            
            // Track network/system errors
            if (typeof gtag !== 'undefined') {
                gtag('event', 'conversion_error', {
                    'custom_parameter_1': formatToSend,
                    'custom_parameter_2': formatToReceive,
                    'error_message': error.message,
                    'value': 0
                });
            }
            
            console.error('Error:', error);
            updateButtonStates();
            enableAllInputs();
        });
    }

    
    function clearInput() {
        // Limpiar solo el textarea activo
        activeTextArea.value = '';
        document.getElementById('format-auto-detected').style.display = 'none';
        updateBytesLen();
        updateButtonStates();
    }
    
    // IMPORTANTE: Esta función implementa la lógica para limpiar el textarea de salida.
    // Se llama desde múltiples lugares:
    // 1. El event listener estándar (clearButton.addEventListener)
    // 2. La función reinitializeButtons
    // Además, existe un evento onclick en línea en el HTML que realiza
    // la misma operación directamente, sin llamar a esta función.
    function updateSyntaxHighlighting() {
        const outputContent = outputTextArea.value.trim();
        const outputFormat = outputFormatSelect.value;
        const highlightedOutput = document.getElementById('highlightedOutput');
        const codeElement = highlightedOutput.querySelector('code');
        
        if (!outputContent) {
            codeElement.textContent = '';
            codeElement.className = 'language-none';
            return;
        }
        
        // Set the appropriate language class based on format
        let languageClass = 'language-none';
        if (outputFormat === 'JSON') {
            languageClass = 'language-json';
        } else if (outputFormat === 'XML') {
            languageClass = 'language-xml';
        }
        
        // Update the code content and class
        codeElement.textContent = outputContent;
        codeElement.className = languageClass;
        
        // Apply Prism highlighting
        if (outputFormat === 'JSON' || outputFormat === 'XML') {
            Prism.highlightElement(codeElement);
        }
    }
    
    function clearOutput() {
        console.log('clearOutput function called');
        
        // Simple, clean implementation - get elements directly every time
        const textarea = document.getElementById('outputTextArea');
        if (textarea) {
            textarea.value = '';
            console.log('Output textarea cleared');
        } else {
            console.error('Output textarea not found!');
        }
        
        const statusElement = document.getElementById('conversion-status');
        if (statusElement) {
            statusElement.innerHTML = '';
            console.log('Conversion status cleared');
        } else {
            console.error('Conversion status element not found!');
        }
        
        // Update UI state
        updateButtonStates();
        enableLeftPanelControls();
        
        // Clear syntax highlighting
        updateSyntaxHighlighting();
        
        console.log('Clear output operation completed');
    }

    function copyToClipboard() {
        navigator.clipboard.writeText(outputTextArea.value).then(function() {
            showToast("Copied to clipboard");
        }, function(err) {
            console.error('Could not copy text: ', err);
            showToast("Failed to copy");
        });
    }
	
	function copyInputToClipboard() {
        navigator.clipboard.writeText(activeTextArea.value).then(function() {
            showToast("Copied to clipboard");
        }, function(err) {
            console.error('Could not copy text: ', err);
            showToast("Failed to copy");
        });
    }

    // Esta función ya no utiliza payloads predefinidos, sino que llama al backend
    function selection1(messageType) {
        console.log('selection1 called for messageType:', messageType);
        console.log('DEPRECATED: Now calling generateMinimalPayload instead');
        
        // Usar la función que llama al backend con minimal=true
        generateMinimalPayload(messageType);
    }

    function selection2(messageType) {
        console.log('selection2 called for messageType:', messageType);
        
        // Obtener el formato seleccionado de la pestaña Random
        const selectedFormat = document.querySelector('input[name="randomSendFormat"]:checked')?.value || "UPER";
        
        // Llamar a la función actualizada y pasar minimal=false
        generateRandomPayloadWithOptions(messageType, selectedFormat, false);
    }

    function showToast(message) {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.classList.remove('opacity-0');
        toast.classList.add('opacity-100');
        setTimeout(() => {
            toast.classList.remove('opacity-100');
            toast.classList.add('opacity-0');
        }, 3000);
    }
    

    function disableAllInputs() {
        // Reutilizar la función disableLeftPanelControls que ya tiene toda la lógica para los tabs
        disableLeftPanelControls();
    }

    function enableAllInputs() {
        // Always enable controls since output is now in tabs
        enableLeftPanelControls();
        updateButtonStates();
    }
    
    function disableLeftPanelControls() {
        // Disable tab buttons
        pasteTabButton.disabled = true;
        generateTabButton.disabled = true;
        pasteTabButton.classList.add('opacity-50', 'pointer-events-none');
        generateTabButton.classList.add('opacity-50', 'pointer-events-none');
        
        // Disable message type selector
        generateMessageTypeSelector.disabled = true;
        
        // Disable format dropdown
        generateFormatSelect.disabled = true;
        
        // Disable minimal checkbox and generate button
        minimalPayloadCheckbox.disabled = true;
        generatePayloadButton.disabled = true;
        generatePayloadButton.classList.add('opacity-50', 'pointer-events-none');
        
        // Disable all textareas
        pasteTextArea.disabled = true;
        generateTextArea.disabled = true;
        minimalTextArea.disabled = true;
        randomTextArea.disabled = true;
        
        // Disable input format radios in all tabs
        document.querySelectorAll('input[name="sendFormat"]').forEach(radio => radio.disabled = true);
        document.querySelectorAll('input[name="generateSendFormat"]').forEach(radio => radio.disabled = true);
        
        // Legacy controls for compatibility
        if (minimalMessageTypeSelector) minimalMessageTypeSelector.disabled = true;
        if (randomMessageTypeSelector) randomMessageTypeSelector.disabled = true;
        document.querySelectorAll('input[name="minimalSendFormat"]').forEach(radio => radio.disabled = true);
        document.querySelectorAll('input[name="randomSendFormat"]').forEach(radio => radio.disabled = true);
        
        // Add visual indication for input format sections
        // document.getElementById('generateInputFormatSelect').classList.add('opacity-50', 'pointer-events-none');
    }
    
    function enableLeftPanelControls() {
        // Enable tab buttons
        pasteTabButton.disabled = false;
        generateTabButton.disabled = false;
        pasteTabButton.classList.remove('opacity-50', 'pointer-events-none');
        generateTabButton.classList.remove('opacity-50', 'pointer-events-none');
        
        // Enable message type selector
        generateMessageTypeSelector.disabled = false;
        
        // Enable format dropdown
        generateFormatSelect.disabled = false;
        
        // Enable minimal checkbox and generate button
        minimalPayloadCheckbox.disabled = false;
        generatePayloadButton.disabled = false;
        generatePayloadButton.classList.remove('opacity-50', 'pointer-events-none');
        
        // Enable active textarea only
        pasteTextArea.disabled = !(pasteTabButton.classList.contains('active'));
        generateTextArea.disabled = !(generateTabButton.classList.contains('active'));
        
        // Enable input format radios
        document.querySelectorAll('input[name="sendFormat"]').forEach(radio => radio.disabled = false);
        document.querySelectorAll('input[name="generateSendFormat"]').forEach(radio => radio.disabled = false);
        
        // Legacy controls for compatibility
        if (minimalMessageTypeSelector) minimalMessageTypeSelector.disabled = false;
        if (randomMessageTypeSelector) randomMessageTypeSelector.disabled = false;
        document.querySelectorAll('input[name="minimalSendFormat"]').forEach(radio => radio.disabled = false);
        document.querySelectorAll('input[name="randomSendFormat"]').forEach(radio => radio.disabled = false);
        
        // Remove visual indication for input format sections
        document.getElementById('generateInputFormatSelect').classList.remove('opacity-50', 'pointer-events-none');
    }
    
    // Share current tab function for email modal
    function shareCurrentTab() {
        // Get the input value from active textarea
        const inputContent = activeTextArea.value.trim();
        
        // Get the active tab data for output content
        const activeTab = window.TabManager.getActiveTab();
        
        if (!activeTab) {
            showToast("No conversion output to share");
            return;
        }
        
        const outputContent = activeTab.content.trim();
        
        // Check if both areas have content
        if (!inputContent || !outputContent) {
            showToast("No content to share");
            return;
        }
        
        // Get formats from active tab data
        let inputFormat = getCurrentInputFormat();
        let outputFormat = activeTab.format;
        
        // Generate HTML content for single tab
        const tabData = [{
            name: activeTab.customName || `${activeTab.format} ${activeTab.timestamp}`,
            inputFormat: inputFormat,
            outputFormat: outputFormat,
            inputContent: inputContent,
            outputContent: outputContent
        }];
        const htmlContent = generateSimpleHTML(tabData);
        const csvContent = generateCSV(tabData);
        
        // Store data for the modal
        window.shareData = {
            inputContent: inputContent,
            outputContent: outputContent,
            inputFormat: inputFormat,
            outputFormat: outputFormat,
            htmlContent: htmlContent,
            csvContent: csvContent,
            isMultipleTabsShare: false
        };
        
        // Show the modal
        const modal = document.getElementById('shareModal');
        modal.classList.remove('hidden');
        
        // Clear error/success messages
        document.getElementById('shareError').classList.add('hidden');
        document.getElementById('shareSuccess').classList.add('hidden');
        
        // Use saved email if available
        const savedEmail = localStorage.getItem('v2xtools_last_email') || '';
        document.getElementById('emailInput').value = savedEmail;
        
        // Focus the email input
        document.getElementById('emailInput').focus();
    }
    
    // Share all tabs function for email modal
    function shareAllTabs() {
        const allTabs = Array.from(window.TabManager.tabs.values());
        const inputContent = activeTextArea.value.trim();
        
        if (allTabs.length === 0) {
            showToast("No tabs to share");
            return;
        }
        
        if (!inputContent) {
            showToast("No input content to share");
            return;
        }
        
        const inputFormat = getCurrentInputFormat();
        const tabsData = allTabs.map(tab => ({
            name: tab.customName || `${tab.format} ${tab.timestamp}`,
            inputFormat: inputFormat,
            outputFormat: tab.format,
            inputContent: inputContent,
            outputContent: tab.content
        }));
        
        // Generate both HTML and CSV content for all tabs
        const htmlContent = generateSimpleHTML(tabsData);
        const csvContent = generateCSV(tabsData);
        
        // Store both data for the modal
        window.shareData = {
            htmlContent: htmlContent,
            csvContent: csvContent,
            isMultipleTabsShare: true
        };
        
        // Show the modal
        const modal = document.getElementById('shareModal');
        modal.classList.remove('hidden');
        
        // Clear error/success messages
        document.getElementById('shareError').classList.add('hidden');
        document.getElementById('shareSuccess').classList.add('hidden');
        
        // Use saved email if available
        const savedEmail = localStorage.getItem('v2xtools_last_email') || '';
        document.getElementById('emailInput').value = savedEmail;
        
        // Focus the email input
        document.getElementById('emailInput').focus();
    }
    
    // Setup share modal handlers
    function setupShareModal() {
        const modal = document.getElementById('shareModal');
        const closeBtn = document.getElementById('closeShareModal');
        const cancelBtn = document.getElementById('cancelShareBtn');
        const form = document.getElementById('shareForm');
        
        // Close modal handlers
        function closeModal() {
            modal.classList.add('hidden');
        }
        
        closeBtn.addEventListener('click', closeModal);
        cancelBtn.addEventListener('click', closeModal);
        
        // Close when clicking outside the modal
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeModal();
            }
        });
        
        // Form submission
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const emailInput = document.getElementById('emailInput');
            const recipientEmail = emailInput.value.trim();
            const errorEl = document.getElementById('shareError');
            const successEl = document.getElementById('shareSuccess');
            
            // Basic validation
            if (!recipientEmail) {
                errorEl.textContent = 'Please enter an email address';
                errorEl.classList.remove('hidden');
                return;
            }
            
            // Email format validation
            const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
            if (!emailRegex.test(recipientEmail)) {
                errorEl.textContent = 'Please enter a valid email address';
                errorEl.classList.remove('hidden');
                return;
            }
            
            // Hide error if previously shown
            errorEl.classList.add('hidden');
            
            // Save email to localStorage for future use
            localStorage.setItem('v2xtools_last_email', recipientEmail);
            
            // Prepare data for the server
            let shareData;
            
            // Check if this is a multiple tabs share
            if (window.shareData.isMultipleTabsShare) {
                // For multiple tabs, send HTML content
                shareData = {
                    recipientEmail: recipientEmail,
                    htmlContent: window.shareData.htmlContent,
                    csvContent: window.shareData.csvContent,
                    isHTMLShare: "true",
                    // For compatibility with main contact form
                    name: "V2X All Tabs Share",
                    email: "no-reply@v2x.tools",
                    subject: "V2X All Tabs Share",
                    message: "All tabs shared from V2X Tools"
                };
            } else {
                // Single tab share - use HTML content with CSV attachment
                shareData = {
                    recipientEmail: recipientEmail,
                    htmlContent: window.shareData.htmlContent,
                    csvContent: window.shareData.csvContent,
                    isHTMLShare: "true",
                    // For compatibility with main contact form
                    name: "V2X Conversion Share",
                    email: "no-reply@v2x.tools",
                    subject: "V2X Conversion Share",
                    message: "Shared from V2X Tools"
                };
            }
            
            // Send to server using the contact form endpoint (using the correct endpoint)
            fetch('/api/contact', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(shareData)
            })
            .then(response => response.json())
            .then(data => {
                if (data.status === 'success') {
                    // Show success message
                    successEl.classList.remove('hidden');
                    
                    // Show success message in toast
                    showToast('Email sent successfully');
                    
                    // Close modal immediately
                    closeModal();
                } else {
                    // Show error message
                    errorEl.textContent = data.message || 'Failed to send email';
                    errorEl.classList.remove('hidden');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                errorEl.textContent = 'Network error occurred';
                errorEl.classList.remove('hidden');
            });
        });
    }
    
    // Call setup on load
    setupShareModal();
    
    // Setup F5 warning function
    function setupF5Warning() {
        // Custom F5 handler to show warning modal
        let refreshWarningShown = false;
        let userConfirmedReload = false;
        
        document.addEventListener('keydown', function(e) {
            // Detect F5 key
            if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
                const hasConversionTabs = window.TabManager && window.TabManager.getTabCount() > 0;
                
                if (hasConversionTabs && !refreshWarningShown) {
                    e.preventDefault();
                    showRefreshWarning();
                }
            }
        });
        
        function showRefreshWarning() {
            refreshWarningShown = true;
            
            // Create modal overlay
            const overlay = document.createElement('div');
            overlay.style.cssText = `
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 9999;
            `;
            
            // Create modal
            const modal = document.createElement('div');
            const isDark = document.documentElement.classList.contains('dark');
            modal.style.cssText = `
                background: ${isDark ? '#374151' : '#ffffff'};
                color: ${isDark ? '#f9fafb' : '#333333'};
                padding: 24px;
                border-radius: 8px;
                box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
                max-width: 400px;
                width: 90%;
            `;
            
            modal.innerHTML = `
                <h3 style="margin: 0 0 16px 0; font-size: 18px; font-weight: 600; color: ${isDark ? '#f59e0b' : '#d97706'};">⚠️ Warning</h3>
                <p style="margin: 0 0 20px 0; line-height: 1.5;">You have open conversion tabs that will be lost if you reload the page. Are you sure you want to continue?</p>
                <div style="display: flex; gap: 12px; justify-content: flex-end;">
                    <button id="cancel-refresh" style="
                        padding: 8px 16px;
                        border: 1px solid ${isDark ? '#6b7280' : '#d1d5db'};
                        background: transparent;
                        color: ${isDark ? '#f9fafb' : '#333333'};
                        border-radius: 4px;
                        cursor: pointer;
                    ">Cancel</button>
                    <button id="confirm-refresh" style="
                        padding: 8px 16px;
                        border: none;
                        background: #dc2626;
                        color: white;
                        border-radius: 4px;
                        cursor: pointer;
                    ">Reload Page</button>
                </div>
            `;
            
            overlay.appendChild(modal);
            document.body.appendChild(overlay);
            
            // Button handlers
            document.getElementById('cancel-refresh').onclick = function() {
                document.body.removeChild(overlay);
                refreshWarningShown = false;
            };
            
            document.getElementById('confirm-refresh').onclick = function() {
                userConfirmedReload = true;
                window.location.reload();
            };
            
            // Close on overlay click
            overlay.onclick = function(e) {
                if (e.target === overlay) {
                    document.body.removeChild(overlay);
                    refreshWarningShown = false;
                }
            };
            
            // Close on Escape key
            const escapeHandler = function(e) {
                if (e.key === 'Escape') {
                    document.body.removeChild(overlay);
                    refreshWarningShown = false;
                    document.removeEventListener('keydown', escapeHandler);
                }
            };
            document.addEventListener('keydown', escapeHandler);
        }
        
        // Fallback: Still use beforeunload for other reload methods
        window.addEventListener('beforeunload', function(e) {
            // Don't show warning if user already confirmed reload
            if (userConfirmedReload) {
                return;
            }
            
            const hasConversionTabs = window.TabManager && window.TabManager.getTabCount() > 0;
            if (hasConversionTabs) {
                e.preventDefault();
                e.returnValue = '';
            }
        });
    }
    
    // Call setup functions after all are defined
    setupF5Warning();
    
    // Function to expand output in a new window
    function expandOutput() {
        const outputContent = outputTextArea.value.trim();
        const inputContent = activeTextArea.value.trim();
        
        // Only open new window if there's content
        if (!outputContent) {
            showToast("No content to expand");
            return;
        }
        
        // Get current format of output
        const outputFormat = outputFormatSelect.value;
        
        // Get the active tab information
        const activeTab = window.TabManager.getActiveTab();
        let windowTitle = `V2X Output - ${outputFormat}`;
        
        // If there's an active tab, use its name
        if (activeTab) {
            // Use custom name if available, otherwise use format + timestamp
            windowTitle = activeTab.customName || `${activeTab.format} ${activeTab.timestamp}`;
        }
        
        // Create a new tab in the browser
        const newWindow = window.open('', '_blank');
        
        // Prepare CSS for the new window
        let backgroundColor = '#ffffff';
        let textColor = '#333333';
        
        // Check if current theme is dark mode and adapt expanded window accordingly
        if (document.documentElement.classList.contains('dark')) {
            backgroundColor = '#1f2937'; // Dark mode background
            textColor = '#f9fafb'; // Dark mode text
        }
        
        // Write content to the new window with appropriate styling
        newWindow.document.write(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>${windowTitle}</title>
                <style>
                    body {
                        font-family: 'Roboto Mono', monospace;
                        margin: 0;
                        padding: 20px;
                        background-color: ${backgroundColor};
                        color: ${textColor};
                    }
                    pre {
                        white-space: pre-wrap;
                        word-wrap: break-word;
                        font-family: 'Roboto Mono', monospace;
                        font-size: 14px;
                        line-height: 1.5;
                        margin: 0;
                    }
                    .container {
                        max-width: 100%;
                    }
                    .content-section {
                        margin-bottom: 20px;
                    }
                    h3 {
                        margin-top: 0;
                        margin-bottom: 10px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="content-section">
                        <h3>Input:</h3>
                        <pre>${inputContent.replace(/</g, '&lt;').replace(/>/g, '&gt;') || "(No input content)"}</pre>
                    </div>
                    
                    <div class="content-section">
                        <h3>Output (${outputFormat}):</h3>
                        <pre>${outputContent.replace(/</g, '&lt;').replace(/>/g, '&gt;')}</pre>
                    </div>
                </div>
            </body>
            </html>
        `);
        
        // Make sure to close the document after writing
        newWindow.document.close();
    }

    // Font size management functions
    function initFontSize() {
        console.log('initFontSize called');
        const savedFontSize = localStorage.getItem(FONT_SIZE_STORAGE_KEY);
        const fontSize = savedFontSize ? parseInt(savedFontSize) : DEFAULT_FONT_SIZE;
        applyFontSize(fontSize);
        
        // Set event listeners for font size buttons
        const fontSizeDecreaseButton = document.getElementById('fontSizeDecreaseButton');
        const fontSizeIncreaseButton = document.getElementById('fontSizeIncreaseButton');
        
        console.log('Font size buttons found:', {
            decrease: !!fontSizeDecreaseButton,
            increase: !!fontSizeIncreaseButton
        });
        
        if (fontSizeDecreaseButton) {
            fontSizeDecreaseButton.title = "Decrease font size";
            fontSizeDecreaseButton.addEventListener('click', function() {
                console.log('A- button clicked');
                adjustFontSize('decrease');
            });
            
        }
        
        if (fontSizeIncreaseButton) {
            fontSizeIncreaseButton.title = "Increase font size";
            fontSizeIncreaseButton.addEventListener('click', function() {
                console.log('A+ button clicked');
                adjustFontSize('increase');
            });
            
        }
    }
    
    function adjustFontSize(action) {
        console.log('adjustFontSize called with:', action);
        const currentSize = getCurrentFontSize();
        console.log('Current font size:', currentSize);
        let newSize;
        
        if (action === 'increase') {
            newSize = Math.min(currentSize + 1, MAX_FONT_SIZE);
        } else {
            newSize = Math.max(currentSize - 1, MIN_FONT_SIZE);
        }
        
        console.log('New font size:', newSize);
        applyFontSize(newSize);
        localStorage.setItem(FONT_SIZE_STORAGE_KEY, newSize.toString());
    }
    
    function getCurrentFontSize() {
        const outputTextArea = document.getElementById('outputTextArea');
        const currentSize = window.getComputedStyle(outputTextArea).fontSize;
        return parseInt(currentSize);
    }
    
    function applyFontSize(size) {
        console.log('applyFontSize called with size:', size);
        const outputTextArea = document.getElementById('outputTextArea');
        const highlightedOutput = document.getElementById('highlightedOutput');
        
        // Apply to legacy elements (for compatibility)
        if (outputTextArea) {
            outputTextArea.style.fontSize = `${size}px`;
            console.log('Applied fontSize to outputTextArea:', outputTextArea.style.fontSize);
        } else {
            console.log('outputTextArea not found!');
        }
        
        if (highlightedOutput) {
            highlightedOutput.style.fontSize = `${size}px`;
            console.log('Applied fontSize to highlightedOutput:', highlightedOutput.style.fontSize);
        } else {
            console.log('highlightedOutput not found!');
        }
        
        // Apply to all tab content elements (new tab system)
        const tabContents = document.querySelectorAll('.conv-tab-content');
        tabContents.forEach((tabContent, index) => {
            const textarea = tabContent.querySelector('textarea');
            const pre = tabContent.querySelector('pre');
            
            if (textarea) {
                textarea.style.fontSize = `${size}px`;
                console.log(`Applied fontSize to tab ${index + 1} textarea:`, textarea.style.fontSize);
            }
            
            if (pre) {
                pre.style.fontSize = `${size}px`;
                console.log(`Applied fontSize to tab ${index + 1} pre:`, pre.style.fontSize);
            }
        });
    }
    
    // Function to load saved state
    function loadSavedState() {
        // Load active tab
        const savedTab = localStorage.getItem('v2xtools_active_tab');
        
        if (savedTab === 'generate') {
            activateGenerateTab();
        } else {
            // Default to paste tab
            activatePasteTab();
        }
        
        // Load message type
        const savedMessageType = localStorage.getItem('v2xtools_message_type');
        if (savedMessageType && generateMessageTypeSelector) {
            // Find option that matches saved value
            const options = generateMessageTypeSelector.options;
            for (let i = 0; i < options.length; i++) {
                if (options[i].value === savedMessageType) {
                    generateMessageTypeSelector.selectedIndex = i;
                    break;
                }
            }
        }
    }
    
    // Load saved state already called earlier
    
    // Initialize font size at the end to ensure DOM elements exist
    initFontSize();
    
    
    // Tab management is now handled by TabManager module
});

// Impressum modal functions
function openImpressum() {
    document.getElementById('impressumModal').classList.remove('hidden');
}

function closeImpressum() {
    document.getElementById('impressumModal').classList.add('hidden');
}

// Privacy Policy modal functions
function openPrivacyPolicy() {
    document.getElementById('privacyPolicyModal').classList.remove('hidden');
}

function closePrivacyPolicy() {
    document.getElementById('privacyPolicyModal').classList.add('hidden');
}

// Terms of Use & Disclaimer modal functions
function openTermsDisclaimer() {
    document.getElementById('termsDisclaimerModal').classList.remove('hidden');
}

function closeTermsDisclaimer() {
    document.getElementById('termsDisclaimerModal').classList.add('hidden');
}