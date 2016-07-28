# Overview

A Sample Cloud Application Using Solace Provided Messaging Services.

This application is a modified copy of the sample application provided in this Blog [Getting Started using Solace Messaging in Cloud Foundry](http://dev.solacesystems.com/blog/solace-in-cloud-foundry/)


## Building

./gradlew assemble

## Installing the sample application.

cf push

## Determine the application url
export APP_URL=`cf apps | grep sample-cloud-app | grep started  | awk '{ print $6}'`
echo "Application URL is $APP_URL"

The Required Solace Messaging Services can be obtained from the Cloud Foundry Market Place.

cf marketplace

cf create-service solmessaging vmr-shared SharedSampleConnection

## Bind the service to the application
cf bind-service sample-cloud-app SharedSampleConnection

## Examine application environment and confirm binding
cf env sample-cloud-app SharedSampleConnection

## Restage the applcation to ensure your env variable changes take effect
cf restage sample-cloud-app

Subscribe to topic "test"
curl -sX POST -H "Authorization: Basic c29sYWNlZGVtbzpzb2xhY2VkZW1v" -H "Content-Type: application/json;charset=UTF-8" -d '{"subscription": "test"}' http://$APP_URL/subscription 

Send message with topic "test"
curl -sX POST -H "Authorization: Basic c29sYWNlZGVtbzpzb2xhY2VkZW1v" -H "Content-Type: application/json;charset=UTF-8" -d '{"topic": "test", "body": "TEST_MESSAGE2"}' http://$APP_URL/message 

The message is received asynchrously, check for the last message.
curl -sX GET -H "Authorization: Basic c29sYWNlZGVtbzpzb2xhY2VkZW1v"  http://$APP_URL/message 

