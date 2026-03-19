#!/bin/bash

# V2X.tools Configuration
# Shared configuration for all deployment scripts

# AWS Configuration
AWS_HOST="16.16.217.157"
AWS_USER="ec2-user"
AWS_KEY="/home/maxi/.ssh/myec2key.pem"

# Application Configuration
REMOTE_DIR="/home/ec2-user/apps/v2x.tools"
JAR_NAME="v2x.tools-1.8-jar-with-dependencies.jar"
SERVICE_NAME="v2xtools"
PORT="2001"  # Port for v2x.tools service

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}