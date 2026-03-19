#!/bin/bash

# Setup V2X.tools Telegram Monitoring
source config.sh

log_info "Setting up Telegram monitoring for v2x.tools..."

# Copy configuration file to server
log_info "📁 Copying configuration files..."
scp -i "$AWS_KEY" ../../v2x.tools/webTools/config.properties "$AWS_USER@$AWS_HOST:$REMOTE_DIR/"

# Create environment file on server
log_info "🔧 Creating environment configuration..."
ssh -i "$AWS_KEY" "$AWS_USER@$AWS_HOST" << 'EOF'
# Create environment file for v2x.tools
cat > /home/ec2-user/apps/v2x.tools/v2xtools.env << 'ENVEOF'
# V2X.tools Environment Configuration

# Telegram Bot Configuration
TELEGRAM_CHAT_ID=20088993
TELEGRAM_BOT_TOKEN=6905485918:AAGkBzlywwxtM2aENMfHSlfK5AaWP-urrfQ

# Monitoring Configuration
MONITORING_ENABLED=true
MONITORING_TELEGRAM_ENABLED=true

# Server Configuration
SERVER_PORT=8080
ENVEOF

chmod 600 /home/ec2-user/apps/v2x.tools/v2xtools.env
echo "✅ Environment file created"
EOF

# Update systemd service to load environment variables
log_info "⚙️ Updating systemd service..."
ssh -i "$AWS_KEY" "$AWS_USER@$AWS_HOST" << 'EOF'
sudo tee /etc/systemd/system/v2xtools.service > /dev/null << 'SERVICEEOF'
[Unit]
Description=V2X.tools Web Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/home/ec2-user/apps/v2x.tools
EnvironmentFile=/home/ec2-user/apps/v2x.tools/v2xtools.env
ExecStart=/usr/bin/java -jar v2x.tools-1.8-jar-with-dependencies.jar --port 8080
Restart=always
RestartSec=10
StandardOutput=append:/home/ec2-user/apps/v2x.tools/v2xtools.log
StandardError=append:/home/ec2-user/apps/v2x.tools/v2xtools.log

[Install]
WantedBy=multi-user.target
SERVICEEOF

# Reload systemd and restart service
sudo systemctl daemon-reload
echo "✅ Systemd service updated"
EOF

# Test Telegram bot configuration
log_info "📱 Testing Telegram bot..."
ssh -i "$AWS_KEY" "$AWS_USER@$AWS_HOST" << 'EOF'
# Test if we can send a message
curl -s -X POST "https://api.telegram.org/bot6905485918:AAGkBzlywwxtM2aENMfHSlfK5AaWP-urrfQ/sendMessage" \
  -d chat_id=20088993 \
  -d text="🧪 V2X.tools monitoring setup test - $(date)" \
  -d parse_mode=HTML

if [ $? -eq 0 ]; then
  echo "✅ Telegram test message sent"
else
  echo "❌ Failed to send Telegram test message"
fi
EOF

log_success "Monitoring setup complete!"
log_info "Next steps:"
echo "  1. Run ./restart.sh to restart with monitoring enabled"
echo "  2. Check ./logs.sh to verify monitoring is working"
echo "  3. Test an API call to see if you receive notifications"
echo ""
echo "Test API call:"
echo "  curl -X POST http://51.21.218.242:2001/api/v2x/uper/json -d '020000'"