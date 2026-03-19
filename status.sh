#!/bin/bash

# V2X.tools Status Script
# This script checks the status of V2X.tools service on AWS

# Load shared configuration
source "$(dirname "$0")/config.sh"

echo "📊 Checking V2X.tools service status on AWS..."
echo ""

# Check service status
echo "🔍 Service Status:"
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl status ${SERVICE_NAME} --no-pager"

echo ""
echo "💾 Memory Usage:"
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "ps aux | grep java | grep v2x.tools | grep -v grep"

echo ""
echo "🌐 Port Status:"
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo netstat -tlnp | grep :${PORT} || echo 'Port ${PORT} not in use'"

echo ""
echo "📝 Recent Logs (last 10 lines):"
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo journalctl -u ${SERVICE_NAME} --no-pager -n 10"

echo ""
echo "📁 Files in app directory:"
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "ls -la ${REMOTE_DIR}/"

echo ""
echo "📂 Web files:"
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "ls -la ${REMOTE_DIR}/web/ | head -10"

echo ""
echo "🔗 Service URL: http://${AWS_HOST}:${PORT}"
echo ""
echo "To view live logs: ./logs.sh"