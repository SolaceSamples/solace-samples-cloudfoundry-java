---
layout: tutorials
title: Secure Session
summary: A sample showing how to connect with the Solace Messaging service using Transport Level Security (TLS).
icon: I_secure_session.svg
---

## Overview

This tutorial is part of a series of tutorials which aims to introduce users to Solace Messaging in Pivotal Cloud Foundry. Solace Messaging in Pivotal Cloud Foundry is delivered as a tile on the [Pivotal Network]({{ site.links-ext-pivotal }}){:target="_blank"}. you can see the [Solace Messaging for Pivotal Cloud Foundry Documentation]({{ site.links-ext-pivotal-solace }}){:target="_blank"} for full details.


This tutorial is based on the [Spring Cloud]({{ site.baseurl }}/spring-cloud) tutorial, adding a demonstration on how to connect to the Solace Messaging service using TLS.

## Goals

The goal of this tutorial is to demonstrate how to connect to the Solace Messaging service instance using TLS. This tutorial will show you how to establish a connection to the solace messaging service using both self-signed and Certificate Authority approved certificates.

## Assumptions

This tutorial assumes the following:

* You have completed the [Spring Cloud]({{ site.baseurl }}/spring-cloud) tutorial.
* You are familiar with [Transport Level Security]({{ site.links-ext-tls }}){:target="_blank"} concepts, including how to create or obtain certificates.


## Working with a Trusted Certificate

In order to use Transport Level Security, your Solace Message Router needs to be configured for TLS with an installed servcer certificate.
The instructions for doing that are described in the Pivotal/Solace Messaging documentation -
see [Tile Installation and Configuration]({{ site.links-ext-tls-server }}){:target="_blank"},
look for the step named Configure Message Routers RSA certificate.

This section assumes that you have a Solace Messaging Service Instance properly configured for TLS access with a certificate that has been purchased from a certificate authority. Instructions for working with self-signed certificates are below.

## Code Walk Through - Trusted Certificates

when we initialize the connection to the service, we can specify whether to use TLS or not. to use TLS, we replaced this line from the Spring Cloud tutorial:

```java
properties.setProperty(JCSMPProperties.HOST, solaceServiceCredentials.getSmfHost());
```

with this:

```java
properties.setProperty(JCSMPProperties.HOST, solaceServiceCredentials.getSmfTlsHost());
```

When you use the TLS host, you can also specify whether the client should validate the TLS certificate.
This should always be done in production environments. We do this by setting the `JCSMPProperties`:

```java
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, true);
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, true);
properties.setProperty(JCSMPProperties.SSL_TRUST_STORE, "path-to-trust-store");
properties.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, "changeit");
```
In Cloud Foundry, the path to the trust store is
`/home/vcap/app/.java-buildpack/open_jdk_jre/lib/security/cacerts`
and the password is the default JRE keystore password, `changeit`.

That is all that is required when using a Certificate Authority issued certificate.
The JRE's trusted store comes pre-configured with certificates that are sufficient
to validated your CA-issued certificate.

## Working with a Self-signed Certificate

You can install your Solace Messaging service for PCF with a self-signed certificate. For instructions on how to do this see the [Solace Messaging Documentation]({{ site.links-ext-tls-server }}){:target="_blank"}.

With self-signed certificates you have two choices. Either you can have the client validate the self-signed certificate,
or not. For testing purposes, you can choose to not validate the certificate simply by
setting the following properties to false:

```java
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, false);
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, false);
```

Note that this is insecure and should never be done in production. This provides you with a TLS encrypted connection to the Solace Message Router but no validation of the identity so you're still exposed to a variety of attacks.

It is also possible to validate the self-signed certificate. This provides an environment that is a little closer to production. To do this, you must provide a certificate to the client-side trust store. This is tricky because the trust store is bundled along with the JRE together with the Cloud Foundry app. It must be added after the application is deployed but before it connects with the Solace Messaging service. Follow these steps:

1. Copy the certificate (the *.pem file) to the directory secure-app/src/main/resources.
1. Edit the file. Remove the private key section and just leave the lines starting with -----BEGIN CERTIFICATE----- and ending with ----- END CERTIFICATE-----.
1. In the CertificateUtil class, change the `CERTIFICATE_FILE_NAME` to match your certificate's file name.
1. In the SolaceController class, enable certificate validation and tell the app to install it.

```java
private static final boolean INSTALL_CERTIFICATE = true;
// and set these further down...
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, true);
properties.setproperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, true);
```

When you set `INSTALL_CERTIFICATE` to `true`, it instructs the SolaceController class to install the certificate in the JRE's trusted store before trying to connect to the service.

The way it works is this. When you copy the certificate into the src/main/resources file, it gets packaged with the application. The application can then read the file from its local file system at runtime.

The standard location for a trusted store, relative to a JRE directory, is in a file at lib/security/cacerts.

The JRE is provided at runtime by Cloud Foundry, and from the point of view of an application the path to the jre is
```
/home/vcap/app/.java-buildpack/open_jdk_jre
```

The SolaceController class reads the certificate and installs it in the trusted store when the application starts running, but before it tries to connect to the service.

Note that we only need to do this when we want the client to validate a self-signed certificate. There should never be a need to do this kind of thing in a production system.

## Building

The full source code for this example is available in [GitHub]({{ site.repository }}){:target="_blank"}. To build, just clone and use gradle. Here is an example:

```
git clone {{ site.repository }}
cd {{ site.baseurl | remove: '/'}}
./gradlew build
```

## Cloud Foundry Setup

The sample application specifies a dependency on a service instance named `solace-messaging-sample-instance` in its manifiest (See `secure-session/manifest.yml`).  This must be an instance of the Solace Messaging Service which can be created with this command:

```
cf create-service solace-messaging shared solace-messaging-sample-instance
```

## Deploying

To deploy this tutorial's application you first need to go inside it's project directory and then push the application:

```
cd secure-session
cf push
```

This will push the application and will give the application the name specified by the manifest: `solace-sample-secure-session`.

## Trying Out The Application

As described above, the sample application has a simple REST interface that allows you to:

* Subscribe
* Send a message
* Receive a message
* Unsubscribe

In order to interact with the application you need to determine the application's URL.  These shell commands can be used to quickly find out the URL:

```
export APP_NAME=solace-sample-secure-session
export APP_URL=`cf apps | grep $APP_NAME | grep started | awk '{ print $6}'`
echo "The application URL is: ${APP_URL}"
```

To demonstrate the application we will make the application send a message to itself.  Then we will read the message back to confirm the successful delivery of the message :

```
# Subscribes the application to the topic "test"
curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"subscription": "test"}' http://$APP_URL/subscription

# Send message with topic "test" and this content: "TEST_MESSAGE"
curl -X POST -H "Content-Type: application/json;charset=UTF-8" -d '{"topic": "test", "body": "Test Message"}' http://$APP_URL/message

# The message should have been asynchronously received by the application.  Check that the message was indeed received:
curl -X GET http://$APP_URL/message

# Unsubscribe the application from the topic "test"
curl -X DELETE http://$APP_URL/subscription/test
```

## Common Problems

If you see:

```
JCSMPTransportException: Error communicating with the router, ConnectException: Connection Refused
```

this can happen if a TLS server certificate was not configured in the Solace Messaging for PCF tile in which case the Solace Message Router will reject incoming TLS connections.

If you see:
```
CertificateException: Path does not chain with any of the trust anchors
```

it is because the application could not validate the certificate it received from the Solace Message Router. This can happen when you install a self-signed certificate on the Solace Message Router, but didn't package it with the app, or didn't set INSTALL_CERTIFICATE to true in the sample.

