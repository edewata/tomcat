/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.core;


import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.digester.Digester;

import java.io.*;
import java.util.Enumeration;
import java.util.Properties;


public class LoadExternalPropertiesListener implements LifecycleListener {

    private static final Log log = LogFactory.getLog(LoadExternalPropertiesListener.class);

    private static boolean propertiesLoaded = false;

    // ---------------------------------------------- Properties
    protected static boolean overwrite = true;
    protected static boolean loadFirst = false;
    protected static String[] files = new String[100];

    // ---------------------------------------------- LifecycleListener Methods

    /**
     * Primary entry point for startup and shutdown events.
     *
     * @param event The event that has occurred
     */
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())) {
            loadProperties();
        }
    }

    // ---------------------------------------------- Getter/Setter Methods

    public static boolean isPropertiesLoaded() {
        return propertiesLoaded;
    }

    public static void setPropertiesLoaded(boolean propertiesLoaded) {
        LoadExternalPropertiesListener.propertiesLoaded = propertiesLoaded;
    }

    public static boolean getOverwrite() {
        return overwrite;
    }

    public static void setOverwrite(boolean overwrite) {
        LoadExternalPropertiesListener.overwrite = overwrite;
    }

    public static boolean getLoadFirst() {
        return loadFirst;
    }

    public static void setLoadFirst(boolean loadFirst) {
        LoadExternalPropertiesListener.loadFirst = loadFirst;
    }

    public static String[] getFiles() {
        return files;
    }

    public static void setFiles(String[] files) {
        LoadExternalPropertiesListener.files = files;
    }

    /**
     * Method to add files to an array of files.
     *
     * @param index Where to put the file in the array.
     * @param name File path.
     */
    public static void addFiles(Integer index, String name) {
        // Check to see if the index is > 99
        if (index > 99) {
            log.warn("file." + index + " is larger than 99 and is not set.");
            return;
        }

        LoadExternalPropertiesListener.files[index] = name;
    }

    /**
     * Method that takes one parameter so that I can use IntrospectionUtils to call it :)
     *
     * @param force Resets propertiesLoaded boolean and causes properties to be loaded again.
     */
    public void loadProperties(Boolean force) {
        if (force && propertiesLoaded) {
            log.warn("Forcing properties to be set again.");
            propertiesLoaded = false;
        }
        loadProperties();
    }

    /**
     * Where the magic happens. This method takes the list of files (set in files variable) and
     * loads properties from each individual file. The 'propertiesLoaded' variable keeps it from
     * happening twice in the event that 'loadFirst' causes the properties to be loaded before the
     * BEFORE_INIT_EVENT. It also checks overwrite so that it doesn't overwrite the variables when
     * expected.
     */
    public void loadProperties() {
        // If loadFirst is present, property files will be loaded before the Digester continues
        // parsing the server.xml
        if (propertiesLoaded) {
            log.warn("Property files from LoadExternalPropertiesListener have already been loaded.");
            return;
        }

        InputStream is;
        Properties properties;

        for (String file : LoadExternalPropertiesListener.files) {
            // It is possible to have empty file names, if they use file.1 and file.3, but not file.2
            // so we should handle that case
            if (file == null || file.isEmpty()) {
                continue;
            }

            File propsFile = new File(file);
            try {
                is = new FileInputStream(propsFile);
            } catch (FileNotFoundException ex) {
                log.warn(ex.getMessage());
                continue;
            }

            try {
                properties = new Properties();
                properties.load(is);
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
                continue;
            } finally {
                try {
                    is.close();
                } catch (IOException ex) {
                    log.warn("Could not close " + propsFile.getName());
                }
            }

            log.debug("Loading properties from " + file);
            // Register the properties as system properties
            Enumeration<?> enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                String value = properties.getProperty(name);
                if (value != null) {

                    if (System.getProperty(name) != null && !System.getProperty(name).isEmpty()) {
                        if(!overwrite) {
                            log.warn("Overwrite is false and property '" + name +
                                    "' already exists. Not setting '" + name + "' to '" + value +
                                    "'.");
                        } else {
                            log.debug("Property '" + name + "' is being overwritten by value '" +
                                value + "'.");
                            System.setProperty(name, value);
                        }
                    } else {
                        System.setProperty(name, value);
                    }
                }
            }
        }

        if (Boolean.getBoolean("org.apache.tomcat.util.digester.REPLACE_SYSTEM_PROPERTIES")) {
            Digester.replaceSystemProperties();
        }

        propertiesLoaded = true;
    }
}
