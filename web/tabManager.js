// tabManager.js - Maneja completamente el panel derecho (tabs)

class TabManager {
    constructor() {
        this.tabCounter = 0;
        this.activeTabId = null;
        this.tabs = new Map(); // Store tab data
        this.initialized = false;
    }

    init() {
        if (this.initialized) return;
        
        // Wait for DOM to be ready
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this._initializeDOM());
        } else {
            this._initializeDOM();
        }
        
        this.initialized = true;
    }

    _initializeDOM() {
        // Get containers
        this.tabsContainer = document.querySelector('.flex.items-end.border-b .flex');
        this.tabsOuterContainer = document.querySelector('.flex.items-end.border-b'); // Parent container for height adjustment
        this.contentContainer = document.querySelector('.flex-1.min-h-\\[180px\\].md\\:min-h-\\[100px\\].relative');
        this.contextMenu = document.getElementById('tab-context-menu');
        
        if (!this.tabsContainer || !this.contentContainer || !this.tabsOuterContainer) {
            console.error('TabManager: Required containers not found');
            return;
        }

        // Initialize context menu
        this._initContextMenu();

        // TabManager initialized and ready
    }

    _adjustTabContainerHeight() {
        if (!this.tabsOuterContainer || !this.tabsContainer) return;
        
        // Check if horizontal scroll is needed by comparing outer container with inner content
        const hasHorizontalScroll = this.tabsContainer.scrollWidth > this.tabsOuterContainer.clientWidth;
        
        if (hasHorizontalScroll) {
            // Need scroll, use larger height
            this.tabsOuterContainer.classList.remove('h-10');
            this.tabsOuterContainer.classList.add('h-14');
        } else {
            // No scroll needed, use smaller height
            this.tabsOuterContainer.classList.remove('h-14');
            this.tabsOuterContainer.classList.add('h-10');
        }
    }

    _initContextMenu() {
        if (!this.contextMenu) {
            console.warn('Context menu element not found');
            return;
        }

        // Handle context menu item clicks
        this.contextMenu.addEventListener('click', (e) => {
            const action = e.target.dataset.action;
            if (action === 'close' && this.contextTargetTabId) {
                this.closeTab(this.contextTargetTabId);
            } else if (action === 'close-all') {
                this.clearAllTabs();
            } else if (action === 'close-others' && this.contextTargetTabId) {
                this._closeOtherTabs(this.contextTargetTabId);
            } else if (action === 'rename' && this.contextTargetTabId) {
                this._renameTab(this.contextTargetTabId);
            }
            this._hideContextMenu();
        });

        // Hide context menu when clicking elsewhere
        document.addEventListener('click', (e) => {
            if (!e.target.closest('#tab-context-menu')) {
                this._hideContextMenu();
            }
        });

        // Prevent context menu on the document when right-clicking tabs
        document.addEventListener('contextmenu', (e) => {
            if (e.target.closest('.conversion-tab')) {
                e.preventDefault();
            }
        });
    }


    // PUBLIC API: Create a new tab with content
    createTab(format, content, originalInput = null) {
        if (!this.tabsContainer || !this.contentContainer) {
            console.error('TabManager not initialized properly');
            return null;
        }

        this.tabCounter++;
        const tabId = `conv-tab-${this.tabCounter}`;
        
        // Generate timestamp
        const timestamp = new Date().toLocaleTimeString('en-US', { 
            hour12: false, 
            hour: '2-digit', 
            minute: '2-digit', 
            second: '2-digit' 
        });

        // Store tab data
        this.tabs.set(tabId, {
            id: tabId,
            format: format,
            content: content,
            originalInput: originalInput,
            timestamp: timestamp,
            number: this.tabCounter
        });

        // Create tab UI
        this._createTabElement(tabId, format, timestamp);
        this._createContentElement(tabId, content, format, originalInput);
        
        // Activate the new tab
        this._activateTab(tabId);
        
        // Adjust container height based on scroll need
        setTimeout(() => this._adjustTabContainerHeight(), 10);

        return tabId;
    }

    _createTabElement(tabId, format, timestamp) {
        // Deactivate all existing tabs first
        this._deactivateAllTabs();

        const tabElement = document.createElement('div');
        tabElement.id = tabId;
        tabElement.className = 'conversion-tab px-3 py-2 border border-gray-300 dark:border-gray-600 border-b-0 rounded-t-md bg-white dark:bg-gray-800 cursor-pointer mr-1 relative -mb-px flex items-center min-w-[120px] flex-shrink-0 active';
        tabElement.setAttribute('data-tab-id', tabId);
        
        tabElement.innerHTML = `
            <span class="text-sm font-medium text-primary dark:text-gray-100 flex-1 whitespace-nowrap overflow-hidden text-ellipsis">${format} ${timestamp}</span>
            <button class="conv-tab-close ml-3 text-gray-400 hover:text-gray-700 dark:hover:text-gray-300 text-base w-4 h-4 flex items-center justify-center" data-tab-id="${tabId}">×</button>
        `;

        // Add event listeners
        tabElement.addEventListener('click', (e) => {
            if (!e.target.closest('.conv-tab-close')) {
                this._activateTab(tabId);
            }
        });

        // Add double-click for rename
        tabElement.addEventListener('dblclick', (e) => {
            if (!e.target.closest('.conv-tab-close')) {
                this._renameTab(tabId);
            }
        });

        // Add right-click context menu
        tabElement.addEventListener('contextmenu', (e) => {
            e.preventDefault();
            this._showContextMenu(e, tabId);
        });

        const closeBtn = tabElement.querySelector('.conv-tab-close');
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            this.closeTab(tabId);
        });

        // Insert at the beginning (leftmost position) - newest tabs on the left
        this.tabsContainer.insertBefore(tabElement, this.tabsContainer.firstChild);
    }

    _createContentElement(tabId, content, format, originalInput) {
        // Hide all existing content
        this._hideAllContent();

        const contentElement = document.createElement('div');
        contentElement.id = `${tabId}-content`;
        contentElement.className = 'conv-tab-content absolute inset-0';
        
        // Create layout with input textarea and horizontal divider if original input exists
        if (originalInput) {
            contentElement.innerHTML = `
                <!-- Input area (top) -->
                <div class="input-area absolute top-0 left-0 right-0" style="height: 30%;">
                    <div class="text-xs text-gray-600 dark:text-gray-400 px-2 py-1 bg-gray-100 dark:bg-gray-700 border-b border-gray-200 dark:border-gray-600">
                        📄 Original Input (${originalInput.length} chars)
                    </div>
                    <textarea class="w-full h-full resize-none font-mono p-2 border-l border-r border-border dark:border-gray-600 bg-gray-50 dark:bg-gray-800 leading-snug font-['Roboto_Mono'] text-xs" readonly style="padding-top: 8px;">${this._escapeHtml(originalInput)}</textarea>
                </div>
                
                <!-- Horizontal divider -->
                <div class="horizontal-divider absolute left-0 right-0 h-2 cursor-ns-resize bg-gray-200 dark:bg-gray-600 hover:bg-blue-300 dark:hover:bg-blue-600 transition-colors flex items-center justify-center" style="top: 30%;">
                    <div class="w-6 h-0.5 bg-gray-400 dark:bg-gray-500"></div>
                </div>
                
                <!-- Invisible drag zone at top for when input is hidden -->
                <div class="top-drag-zone absolute top-0 left-0 right-0 h-3 cursor-ns-resize z-20" style="display: none;"></div>
                
                <!-- Output area (bottom) -->
                <div class="output-area absolute bottom-0 left-0 right-0" style="top: calc(30% + 8px);">
                    <textarea class="absolute inset-0 resize-none font-mono p-2 border border-border dark:border-gray-600 rounded-md bg-gray-50 leading-snug font-['Roboto_Mono'] font-normal" style="opacity: 0; pointer-events: none;" readonly>${content}</textarea>
                    <pre class="absolute inset-0 overflow-auto font-mono p-2 border border-border dark:border-gray-600 rounded-md bg-gray-50 dark:bg-gray-800 leading-snug font-['Roboto_Mono'] font-normal m-0"><code class="language-${this._getLanguageClass(format)}">${this._escapeHtml(content)}</code></pre>
                </div>
            `;
        } else {
            // No input, just output (original behavior)
            contentElement.innerHTML = `
                <textarea class="absolute inset-0 resize-none font-mono p-2 border border-border dark:border-gray-600 rounded-md bg-gray-50 leading-snug font-['Roboto_Mono'] font-normal" style="opacity: 0; pointer-events: none;" readonly>${content}</textarea>
                <pre class="absolute inset-0 overflow-auto font-mono p-2 border border-border dark:border-gray-600 rounded-md bg-gray-50 dark:bg-gray-800 leading-snug font-['Roboto_Mono'] font-normal m-0"><code class="language-${this._getLanguageClass(format)}">${this._escapeHtml(content)}</code></pre>
            `;
        }

        this.contentContainer.appendChild(contentElement);

        // Add horizontal divider functionality if original input exists
        if (originalInput) {
            this._setupHorizontalDivider(contentElement, tabId);
        }

        // Apply syntax highlighting if Prism is available
        if (window.Prism) {
            Prism.highlightElement(contentElement.querySelector('code'));
        }
        
        // Apply saved font size to the new tab content
        this._applySavedFontSize(contentElement);
    }

    _getLanguageClass(format) {
        switch(format.toLowerCase()) {
            case 'json': return 'json';
            case 'xml': return 'xml';
            default: return 'none';
        }
    }

    _escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    _deactivateAllTabs() {
        document.querySelectorAll('.conversion-tab').forEach(tab => {
            tab.classList.remove('active', 'bg-white', 'dark:bg-gray-800');
            tab.classList.add('bg-gray-300', 'dark:bg-gray-700');
            const span = tab.querySelector('span');
            if (span) {
                span.classList.remove('text-primary', 'dark:text-gray-100', 'font-medium');
                span.classList.add('text-gray-600', 'dark:text-gray-300');
            }
        });
    }

    _hideAllContent() {
        document.querySelectorAll('.conv-tab-content').forEach(content => {
            content.style.display = 'none';
        });
    }

    _activateTab(tabId) {
        this._deactivateAllTabs();
        this._hideAllContent();

        // Activate the selected tab
        const tabElement = document.getElementById(tabId);
        const contentElement = document.getElementById(`${tabId}-content`);

        if (tabElement) {
            tabElement.classList.add('active', 'bg-white', 'dark:bg-gray-800');
            tabElement.classList.remove('bg-gray-300', 'dark:bg-gray-700');
            const span = tabElement.querySelector('span');
            if (span) {
                span.classList.add('text-primary', 'dark:text-gray-100', 'font-medium');
                span.classList.remove('text-gray-600', 'dark:text-gray-300');
            }
        }

        if (contentElement) {
            contentElement.style.display = 'block';
        }

        this.activeTabId = tabId;
    }

    // PUBLIC API: Close a specific tab
    closeTab(tabId) {
        const tabElement = document.getElementById(tabId);
        const contentElement = document.getElementById(`${tabId}-content`);
        const wasActive = this.activeTabId === tabId;

        // Remove from DOM
        if (tabElement) tabElement.remove();
        if (contentElement) contentElement.remove();

        // Remove from internal storage
        this.tabs.delete(tabId);

        // Handle active tab logic
        if (wasActive) {
            const remainingTabs = Array.from(this.tabs.keys());
            if (remainingTabs.length > 0) {
                // Activate the last remaining tab
                this._activateTab(remainingTabs[remainingTabs.length - 1]);
            } else {
                // No more tabs
                this.activeTabId = null;
                
                // Update button states when no tabs remain
                if (typeof window.updateButtonStates === 'function') {
                    window.updateButtonStates();
                }
            }
        }
        
        // Adjust container height after closing tab
        setTimeout(() => this._adjustTabContainerHeight(), 10);
    }

    // PUBLIC API: Get number of active tabs
    getTabCount() {
        return this.tabs.size;
    }

    // PUBLIC API: Get active tab info
    getActiveTab() {
        if (!this.activeTabId) return null;
        return this.tabs.get(this.activeTabId);
    }

    // PUBLIC API: Clear all tabs
    clearAllTabs() {
        const tabIds = Array.from(this.tabs.keys());
        
        // Remove all tabs from DOM and storage
        tabIds.forEach(tabId => {
            const tabElement = document.getElementById(tabId);
            const contentElement = document.getElementById(`${tabId}-content`);
            
            // Remove from DOM
            if (tabElement) tabElement.remove();
            if (contentElement) contentElement.remove();
            
            // Remove from internal storage
            this.tabs.delete(tabId);
        });
        
        // Reset active tab
        this.activeTabId = null;
        
        // Hide all content without showing test tabs
        this._hideAllContent();
    }

    // Context menu methods
    _showContextMenu(event, tabId) {
        if (!this.contextMenu) return;
        
        this.contextTargetTabId = tabId;
        
        // Position the menu at the cursor
        this.contextMenu.style.left = `${event.pageX}px`;
        this.contextMenu.style.top = `${event.pageY}px`;
        this.contextMenu.style.display = 'block';
        
        // Ensure menu stays within viewport
        const rect = this.contextMenu.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        
        if (rect.right > viewportWidth) {
            this.contextMenu.style.left = `${event.pageX - rect.width}px`;
        }
        
        if (rect.bottom > viewportHeight) {
            this.contextMenu.style.top = `${event.pageY - rect.height}px`;
        }
    }

    _hideContextMenu() {
        if (this.contextMenu) {
            this.contextMenu.style.display = 'none';
            this.contextTargetTabId = null;
        }
    }

    _closeOtherTabs(keepTabId) {
        const allTabIds = Array.from(this.tabs.keys());
        const tabsToClose = allTabIds.filter(id => id !== keepTabId);
        
        // Close all other tabs
        tabsToClose.forEach(tabId => this.closeTab(tabId));
        
        // Ensure the kept tab is active
        if (this.tabs.has(keepTabId)) {
            this._activateTab(keepTabId);
        }
    }

    _renameTab(tabId) {
        const tabData = this.tabs.get(tabId);
        if (!tabData) return;

        const currentName = `${tabData.format} ${tabData.timestamp}`;
        const newName = prompt('Enter new tab name:', currentName);
        
        if (newName && newName.trim() && newName.trim() !== currentName) {
            // Update tab data
            tabData.customName = newName.trim();
            
            // Update tab visual
            const tabElement = document.getElementById(tabId);
            if (tabElement) {
                const spanElement = tabElement.querySelector('span');
                if (spanElement) {
                    spanElement.textContent = newName.trim();
                }
            }
        }
    }

    _applySavedFontSize(contentElement) {
        // Get saved font size from localStorage (same key as v2xConverter.js)
        const FONT_SIZE_STORAGE_KEY = 'outputTextAreaFontSize';
        const DEFAULT_FONT_SIZE = 14;
        
        const savedFontSize = localStorage.getItem(FONT_SIZE_STORAGE_KEY);
        const fontSize = savedFontSize ? parseInt(savedFontSize) : DEFAULT_FONT_SIZE;
        
        // Apply font size to textarea and pre elements in this tab
        const textarea = contentElement.querySelector('textarea');
        const pre = contentElement.querySelector('pre');
        
        if (textarea) {
            textarea.style.fontSize = `${fontSize}px`;
        }
        
        if (pre) {
            pre.style.fontSize = `${fontSize}px`;
        }
    }

    _setupHorizontalDivider(contentElement, tabId) {
        const divider = contentElement.querySelector('.horizontal-divider');
        const topDragZone = contentElement.querySelector('.top-drag-zone');
        const inputArea = contentElement.querySelector('.input-area');
        const outputArea = contentElement.querySelector('.output-area');
        
        if (!divider || !inputArea || !outputArea || !topDragZone) return;
        
        let isResizing = false;
        let startY = 0;
        let startHeight = 0;
        
        // Load saved height from localStorage
        const savedHeight = localStorage.getItem(`v2x-input-height-${tabId}`);
        if (savedHeight) {
            const height = parseFloat(savedHeight);
            this._updateDividerPosition(inputArea, outputArea, divider, topDragZone, height);
        }
        
        const startDrag = (e) => {
            isResizing = true;
            startY = e.clientY;
            
            const containerRect = contentElement.getBoundingClientRect();
            const inputRect = inputArea.getBoundingClientRect();
            
            if (inputArea.style.display === 'none') {
                startHeight = 0;
            } else {
                startHeight = (inputRect.height / containerRect.height) * 100;
            }
            
            document.body.style.cursor = 'ns-resize';
            document.body.style.userSelect = 'none';
            
            e.preventDefault();
        };
        
        divider.addEventListener('mousedown', startDrag);
        topDragZone.addEventListener('mousedown', startDrag);
        
        document.addEventListener('mousemove', (e) => {
            if (!isResizing) return;
            
            const containerRect = contentElement.getBoundingClientRect();
            const deltaY = e.clientY - startY;
            const deltaPercent = (deltaY / containerRect.height) * 100;
            
            const newHeight = startHeight + deltaPercent;
            
            // Allow hiding input completely (0% to 80%)
            const clampedHeight = Math.max(0, Math.min(80, newHeight));
            
            this._updateDividerPosition(inputArea, outputArea, divider, topDragZone, clampedHeight);
        });
        
        document.addEventListener('mouseup', () => {
            if (isResizing) {
                isResizing = false;
                document.body.style.cursor = '';
                document.body.style.userSelect = '';
                
                // Save height to localStorage
                const currentHeight = parseFloat(inputArea.style.height);
                localStorage.setItem(`v2x-input-height-${tabId}`, currentHeight.toString());
            }
        });
    }
    
    _updateDividerPosition(inputArea, outputArea, divider, topDragZone, heightPercent) {
        inputArea.style.height = `${heightPercent}%`;
        divider.style.top = `${heightPercent}%`;
        
        if (heightPercent === 0) {
            // Hide input area and main divider completely
            inputArea.style.display = 'none';
            divider.style.display = 'none';
            // Show top drag zone for restoration
            topDragZone.style.display = 'block';
            // Position output at top
            outputArea.style.top = '0px';
        } else {
            // Show input area and main divider
            inputArea.style.display = 'block';
            divider.style.display = 'flex';
            // Hide top drag zone
            topDragZone.style.display = 'none';
            // Position output below input + divider
            outputArea.style.top = `calc(${heightPercent}% + 8px)`;
        }
    }
}

// Create global instance
window.TabManager = new TabManager();

// Auto-initialize when script loads
window.TabManager.init();