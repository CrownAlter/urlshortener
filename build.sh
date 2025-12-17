#!/usr/bin/env bash
# Build script for Render deployment

set -e

echo "Building URL Shortener Application..."

# Make Maven wrapper executable
chmod +x mvnw

# Clean and build the application
./mvnw clean package -DskipTests

echo "Build completed successfully!"
