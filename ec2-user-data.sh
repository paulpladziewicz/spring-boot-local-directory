#!/bin/bash

# Update the instance
sudo yum update -y

# Install necessary software
sudo yum install -y nginx
sudo yum install -y java-21-amazon-corretto-headless

# Download the required files
aws s3 cp s3://westmichigansoftware/deployments/fremontmi-1.0.4.jar /home/ec2-user/fremontmi-1.0.4.jar
sudo aws s3 cp s3://westmichigansoftware/deployments/nginx.conf /etc/nginx/nginx.conf

# Start the application
sudo bash -c 'nohup java -jar /home/ec2-user/fremontmi-1.0.4.jar > /var/log/app.log 2>&1 &'

# Start and enable NGINX
sudo systemctl start nginx
sudo systemctl enable nginx