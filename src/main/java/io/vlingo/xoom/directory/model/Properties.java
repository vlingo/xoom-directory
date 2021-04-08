// Copyright © 2012-2021 VLINGO LABS. All rights reserved.
//
// This Source Code Form is subject to the terms of the
// Mozilla Public License, v. 2.0. If a copy of the MPL
// was not distributed with this file, You can obtain
// one at https://mozilla.org/MPL/2.0/.

package io.vlingo.xoom.directory.model;

import java.io.IOException;

public final class Properties {
  public static final Properties instance;

  private static final String propertiesFile = "/xoom-directory.properties";

  static {
    instance = open();
  }

  private final java.util.Properties properties;

  public static Properties open() {
    final java.util.Properties properties = new java.util.Properties();

    try {
      properties.load(Properties.class.getResourceAsStream(propertiesFile));
    } catch (IOException e) {
      throw new IllegalStateException("Must provide properties file on classpath: " + propertiesFile);
    }

    return new Properties(properties);
  }

  protected static Properties openForTest(java.util.Properties properties) {
    return new Properties(properties);
  }
  
  public String directoryGroupAddress() {
    final String address = getString("directory.group.address", "");
    if (address.isEmpty()) throw new IllegalStateException("Must define a directory group address in properties file.");
    return address;
  }
  
  public int directoryGroupPort() {
    final int port = getInteger("directory.group.port", -1);
    if (port == -1) throw new IllegalStateException("Must define a directory group port in properties file.");
    return port;
  }

  public int directoryIncomingPort() {
    final int port = getInteger("directory.incoming.port", -1);
    if (port == -1) throw new IllegalStateException("Must define a directory incoming port in properties file.");
    return port;
  }

  public int directoryMessageBufferSize() {
    final int messageSize = getInteger("directory.message.buffer.size", 32767);
    return messageSize;
  }

  public int directoryMessageProcessingInterval() {
    final int interval = getInteger("directory.message.processing.interval", 100);
    return interval;
  }

  public final int directoryMessagePublishingInterval() {
    final int interval = getInteger("directory.message.publishing.interval", 5000);
    return interval;
  }

  public final int directoryUnregisteredServiceNotifications() {
    final int notifications = getInteger("directory.unregistered.service.notifications", 20);
    return notifications;
  }

  public final Boolean getBoolean(final String key, final Boolean defaultValue) {
    final String value = getString(key, defaultValue.toString());
    return Boolean.parseBoolean(value);
  }

  public final Integer getInteger(final String key, final Integer defaultValue) {
    final String value = getString(key, defaultValue.toString());
    return Integer.parseInt(value);
  }

  public final String getString(final String key, final String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  public void validateRequired(final String nodeName) {
    // assertions in each accessor

    directoryGroupAddress();
    
    directoryGroupPort();
    
    directoryIncomingPort();
  }

  private Properties(java.util.Properties properties) {
    this.properties = properties;
  }
}
