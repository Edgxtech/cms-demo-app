# Building: docker build -t edgx/python-yaml-env -f python-yaml-env.Dockerfile .

# Use python:3.9-slim as the base image
FROM python:3.9-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    gcc \
    python3-dev \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /scripts

# Copy requirements file (optional, for better dependency management)
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the scripts and app directories
#COPY ../scripts/setup_accounts.py /scripts/setup_accounts.py
#COPY . /app