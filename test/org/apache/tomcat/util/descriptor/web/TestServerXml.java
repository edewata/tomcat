/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.tomcat.util.descriptor.web;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.catalina.core.LoadExternalPropertiesListener;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import org.apache.tomcat.util.descriptor.XmlErrorHandler;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Test case for {@link ServerXml}.
 */
public class TestServerXml {

    public Digester createDigester(File config) throws IOException, SAXException {
        InputSource serverXml = new InputSource(config.getAbsoluteFile().toURI().toString());

        // Initialize the digester
        Digester digester = new Digester();
        digester.setValidating(false);
        digester.setRulesValidation(true);
        HashMap<Class<?>, List<String>> fakeAttributes = new HashMap<>();
        ArrayList<String> attrs = new ArrayList<>();
        attrs.add("className");
        fakeAttributes.put(Object.class, attrs);
        digester.setFakeAttributes(fakeAttributes);
        digester.setUseContextClassLoader(true);

        // Add Server rule to prevent warning
        digester.addObjectCreate("Server",
                "org.apache.catalina.core.StandardServer",
                "className");
        digester.addSetProperties("Server");

        // Parse only LoadExternalPropertiesListener
        digester.addObjectCreate("Server/Listener",
                null,
                "className");
        digester.addSetProperties("Server/Listener");

        XmlErrorHandler handler = new XmlErrorHandler();
        digester.setErrorHandler(handler);
        digester.parse(serverXml);

        Assert.assertEquals(0, handler.getErrors().size());
        Assert.assertEquals(0, handler.getWarnings().size());

        return digester;
    }

    @Before
    public void setup() {
        // Probably better to do all this with mocking, but I'd have to learn how so TODO

        // SetPropertiesRule causes static variables to be set on LoadExternalPropertiesListener
        // Since there are only a few, I'll reset them here before each test run.
        LoadExternalPropertiesListener.setOverwrite(true);
        LoadExternalPropertiesListener.setLoadFirst(false);
        LoadExternalPropertiesListener.setFiles(new String[100]);
        LoadExternalPropertiesListener.setPropertiesLoaded(false);

        // Clear test properties
        System.clearProperty("overwrite.test");
    }

    @Test
    public void testLoadFirst() throws IOException, SAXException {
        createDigester(new File("test/redhat/LoadExternalPropertiesListener/server.xml"));

        Assert.assertEquals(true, LoadExternalPropertiesListener.getLoadFirst());
    }

    @Test
    public void testOverwrite() throws IOException, SAXException {
        createDigester(new File("test/redhat/LoadExternalPropertiesListener/server.xml"));

        Assert.assertEquals(false, LoadExternalPropertiesListener.getOverwrite());
        Assert.assertEquals("1", System.getProperty("overwrite.test"));
    }

    @Test
    public void testFiles() throws IOException, SAXException {
        createDigester(new File("test/redhat/LoadExternalPropertiesListener/server.xml"));

        int counter = 0;
        // Verify that only one file exists in the array
        for (String f : LoadExternalPropertiesListener.getFiles()) {
            if (f != null && !f.isEmpty()) {
                counter++;
            }
        }

        Assert.assertEquals(2, counter);
    }
}
