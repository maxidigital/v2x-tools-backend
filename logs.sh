#!/bin/bash

# V2X.tools Logs Script
# This script shows live logs from V2X.tools service on AWS

# Load shared configuration
source "$(dirname "$0")/config.sh"

echo "📝 Connecting to V2X.tools logs on AWS..."
echo "Press Ctrl+C to exit"
echo ""

# Follow logs in real time
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo journalctl -u ${SERVICE_NAME} -f"