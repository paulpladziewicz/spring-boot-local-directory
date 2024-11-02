#!/bin/bash

# Log user data output for debugging
exec > >(tee /var/log/user-data.log | logger -t user-data) 2>&1

# Update the instance
sudo yum update -y

# Install necessary software
sudo yum install -y nginx
sudo yum install -y java-21-amazon-corretto-headless

# Download the required files
aws s3 cp s3://westmichigansoftware/deployments/fremontmi-2.0.3.jar /home/ec2-user/fremontmi-2.0.3.jar
sudo aws s3 cp s3://westmichigansoftware/deployments/nginx.conf /etc/nginx/nginx.conf

# Check if port 8080 is already in use, and kill the process if it is
if sudo lsof -i :8080; then
    sudo fuser -k 8080/tcp
fi

# Start the application
sudo bash -c 'nohup java -jar /home/ec2-user/fremontmi-2.0.3.jar > /var/log/app.log 2>&1 &'

# Start and enable NGINX
sudo systemctl start nginx
sudo systemctl enable nginx