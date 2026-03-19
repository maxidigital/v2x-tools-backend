#!/bin/bash

# V2X.tools localhost.run Script
# This script creates SSH tunnel using localhost.run for V2X.tools service

# Load shared configuration
source "$(dirname "$0")/config.sh"

echo "🌐 Starting localhost.run tunnel for V2X.tools..."
echo ""

# Create localhost.run start script on server
cat << 'EOF' > localhost_run_v2x.sh
#!/bin/bash
# Start localhost.run tunnel for v2x.tools on port 8080
echo "Starting localhost.run tunnel for port 8080..."

# Kill any existing localhost.run process
pkill -f "ssh -R.*localhost.run"

# Start localhost.run in background with custom subdomain
nohup ssh -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -R v2xtools:80:localhost:8080 ssh.localhost.run > /home/ec2-user/apps/v2x.tools/localhost_run.log 2>&1 &
echo $! > /home/ec2-user/apps/v2x.tools/localhost_run.pid

echo "localhost.run started with PID $(cat /home/ec2-user/apps/v2x.tools/localhost_run.pid)"
echo ""
echo "Waiting for tunnel to establish..."
sleep 8

# Show the public URL from log
echo "Public URL:"
grep -E "https://.*localhost.run" /home/ec2-user/apps/v2x.tools/localhost_run.log | tail -1
EOF

# Upload script to server
scp -i ${AWS_KEY} localhost_run_v2x.sh ${AWS_USER}@${AWS_HOST}:/home/ec2-user/apps/v2x.tools/
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "chmod +x /home/ec2-user/apps/v2x.tools/localhost_run_v2x.sh"
rm localhost_run_v2x.sh

# Kill old serveo process if exists
echo "🧹 Cleaning up old tunnels..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "pkill -f 'ssh.*serveo.net' 2>/dev/null || true"

# Execute localhost.run on server
echo "🚀 Starting localhost.run on server..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "/home/ec2-user/apps/v2x.tools/localhost_run_v2x.sh"

echo ""
echo "✅ localhost.run tunnel established!"
echo ""
echo "To check the public URL:"
echo "  ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} 'grep -E \"https://.*localhost.run\" /home/ec2-user/apps/v2x.tools/localhost_run.log | tail -1'"
echo ""
echo "To view localhost.run logs:"
echo "  ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} 'tail -f /home/ec2-user/apps/v2x.tools/localhost_run.log'"
echo ""
echo "To stop localhost.run:"
echo "  ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} 'kill \$(cat /home/ec2-user/apps/v2x.tools/localhost_run.pid)'"