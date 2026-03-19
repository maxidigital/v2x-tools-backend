/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: ["./web/**/*.{html,js}"],
  theme: {
    extend: {
      colors: {
        primary: '#3498db',
        secondary: '#2c3e50',
        background: '#ecf0f1',
        textColor: '#333',
        border: '#bdc3c7',
        success: '#2ecc71',
        error: '#e74c3c',
      }
    },
  },
  plugins: [],
}