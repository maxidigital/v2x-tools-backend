# Botpoison Setup for V2X.tools

## Configuration Required

Before the contact form will work, you need to:

1. **Get your Botpoison keys:**
   - Go to https://botpoison.com/
   - Create a free account
   - Create a new project for v2x.tools
   - Add these domains: v2x.tools, www.v2x.tools, localhost

2. **Update the public key in the frontend:**
   - Edit `/web/contact.html`
   - Find line: `const BOTPOISON_PUBLIC_KEY = 'YOUR_PUBLIC_KEY_HERE';`
   - Replace with your actual public key

3. **Update the secret key in the backend:**
   - Edit `/src/main/java/main/handlers/ContactFormHandler.java`
   - Find line: `private static final String BOTPOISON_SECRET_KEY = "YOUR_SECRET_KEY_HERE";`
   - Replace with your actual secret key

4. **Deploy the changes:**
   ```bash
   ./deploy.sh
   ```

## How it works

1. When the page loads, Botpoison runs invisible challenges in the background
2. It analyzes user behavior to determine if they're human
3. When the form is submitted, the solution is sent to your server
4. Your server verifies the solution with Botpoison's API
5. Only verified humans can submit the form

## Benefits over reCAPTCHA

- No annoying "click the traffic lights" puzzles
- Works invisibly in the background
- Better user experience
- GDPR compliant
- Free for up to 2,500 verifications/month