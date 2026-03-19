#!/bin/bash

# V2X.tools Deployment Script
# This script builds and deploys V2X.tools to AWS

# Load shared configuration
source "$(dirname "$0")/config.sh"

echo "🚀 Starting V2X.tools deployment..."

# Build the application
echo "📦 Building application..."
mvn clean package
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

# Check if JAR was created
if [ ! -f "target/${JAR_NAME}" ]; then
    echo "❌ JAR file not found: target/${JAR_NAME}"
    exit 1
fi

echo "✅ Build successful"

# Create remote directory if it doesn't exist
echo "📁 Creating remote directory..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "mkdir -p ${REMOTE_DIR}/web"

# Stop the service before deployment
echo "⏹️  Stopping existing service..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl stop ${SERVICE_NAME} 2>/dev/null || true"

# Copy JAR file
echo "📤 Uploading JAR file..."
scp -i ${AWS_KEY} target/${JAR_NAME} ${AWS_USER}@${AWS_HOST}:${REMOTE_DIR}/

# Copy web files
echo "📤 Uploading web files..."
scp -i ${AWS_KEY} -r web/* ${AWS_USER}@${AWS_HOST}:${REMOTE_DIR}/web/

# Copy credentials (if they exist)
echo "📤 Uploading credentials..."
if [ -d "../credentials" ]; then
    ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "mkdir -p ${REMOTE_DIR}/../credentials"
    scp -i ${AWS_KEY} ../credentials/*.json ${AWS_USER}@${AWS_HOST}:${REMOTE_DIR}/../credentials/ 2>/dev/null || true
fi

# Create start script
echo "📝 Creating start script..."
cat << 'EOF' > start.sh
#!/bin/bash
cd /home/ec2-user/apps/v2x.tools
java -jar v2x.tools-1.8-jar-with-dependencies.jar > v2xtools.log 2>&1 &
echo $! > v2xtools.pid
echo "V2X.tools started with PID $(cat v2xtools.pid)"
EOF

scp -i ${AWS_KEY} start.sh ${AWS_USER}@${AWS_HOST}:${REMOTE_DIR}/
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "chmod +x ${REMOTE_DIR}/start.sh"
rm start.sh

# Create stop script
echo "📝 Creating stop script..."
cat << 'EOF' > stop.sh
#!/bin/bash
cd /home/ec2-user/apps/v2x.tools
if [ -f v2xtools.pid ]; then
    PID=$(cat v2xtools.pid)
    if ps -p $PID > /dev/null; then
        kill $PID
        echo "V2X.tools stopped (PID $PID)"
        rm v2xtools.pid
    else
        echo "Process not running"
        rm v2xtools.pid
    fi
else
    echo "PID file not found"
fi
EOF

scp -i ${AWS_KEY} stop.sh ${AWS_USER}@${AWS_HOST}:${REMOTE_DIR}/
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "chmod +x ${REMOTE_DIR}/stop.sh"
rm stop.sh

# Create systemd service file
echo "📝 Creating systemd service..."
cat << EOF > v2xtools.service
[Unit]
Description=V2X.tools Web Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=${REMOTE_DIR}
ExecStart=/usr/bin/java -jar ${REMOTE_DIR}/${JAR_NAME}
Restart=on-failure
RestartSec=10

# Environment variables
Environment="JAVA_OPTS=-Xms256m -Xmx512m"
Environment="PORT=${PORT}"

[Install]
WantedBy=multi-user.target
EOF

# Upload and install service
scp -i ${AWS_KEY} v2xtools.service ${AWS_USER}@${AWS_HOST}:/tmp/
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo mv /tmp/v2xtools.service /etc/systemd/system/ && sudo systemctl daemon-reload"
rm v2xtools.service

# Start the service
echo "▶️  Starting V2X.tools service..."
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl enable ${SERVICE_NAME} && sudo systemctl start ${SERVICE_NAME}"

# Check status
sleep 2
ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} "sudo systemctl status ${SERVICE_NAME} --no-pager"

echo "✅ Deployment complete!"
echo ""
echo "V2X.tools is now running on: http://${AWS_HOST}:${PORT}"
echo ""
echo "To check logs:"
echo "  Service: ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} 'sudo journalctl -u ${SERVICE_NAME} -f'"
echo "  Manual:  ssh -i ${AWS_KEY} ${AWS_USER}@${AWS_HOST} 'tail -f ${REMOTE_DIR}/v2xtools.log'"
echo ""
echo "To manage the service:"
echo "  Start:   ./start.sh"
echo "  Stop:    ./stop.sh"
echo "  Restart: ./restart.sh"
echo "  Status:  ./status.sh"
echo "  Logs:    ./logs.sh"