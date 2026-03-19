#!/bin/bash

# V2X.tools Restart Script
# This script restarts the V2X.tools service on AWS after deployment

# Load shared configuration
source "$(dirname "$0")/config.sh"

log_info "Restarting V2X.tools service on AWS ($AWS_HOST)..."

# Stop the service
log_warning "Stopping service..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl stop ${SERVICE_NAME}"

# Wait a moment
sleep 2

# Start the service
log_info "Starting service..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl start ${SERVICE_NAME}"

# Check status
log_info "Checking service status..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl status ${SERVICE_NAME} --no-pager"

echo ""
log_success "Restart complete!"
echo ""
echo "V2X.tools is running on: http://${AWS_HOST}:${PORT}"
echo ""
echo "To check logs:"
echo "  ./logs.sh"