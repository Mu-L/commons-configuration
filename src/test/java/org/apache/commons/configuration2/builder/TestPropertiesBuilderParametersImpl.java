/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.apache.commons.configuration2.ConfigurationConsumer;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.PropertiesConfigurationLayout;
import org.apache.commons.configuration2.beanutils.BeanHelper;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@code PropertiesBuilderParametersImpl}.
 */
public class TestPropertiesBuilderParametersImpl {
    /** The parameters object to be tested. */
    private PropertiesBuilderParametersImpl params;

    @BeforeEach
    public void setUp() throws Exception {
        params = new PropertiesBuilderParametersImpl();
    }

    /**
     * Tests whether properties can be set using BeanUtils.
     */
    @Test
    void testBeanPropertiesAccess() throws Exception {
        final PropertiesConfiguration.IOFactory factory = mock(PropertiesConfiguration.IOFactory.class);
        BeanHelper.setProperty(params, "IOFactory", factory);
        BeanHelper.setProperty(params, "throwExceptionOnMissing", Boolean.TRUE);
        BeanHelper.setProperty(params, "fileName", "test.properties");
        assertEquals("test.properties", params.getFileHandler().getFileName());
        final Map<String, Object> paramsMap = params.getParameters();
        assertEquals(Boolean.TRUE, paramsMap.get("throwExceptionOnMissing"));
        assertSame(factory, params.getParameters().get("IOFactory"));
    }

    /**
     * Tests whether properties can be inherited.
     */
    @Test
    void testInheritFrom() {
        final PropertiesConfiguration.IOFactory factory = mock(PropertiesConfiguration.IOFactory.class);
        @SuppressWarnings("unchecked")
        final ConfigurationConsumer<ConfigurationException> includeListener = mock(ConfigurationConsumer.class);
        params.setIOFactory(factory).setIncludeListener(includeListener).setIncludesAllowed(false).setLayout(new PropertiesConfigurationLayout())
            .setThrowExceptionOnMissing(true);
        final PropertiesBuilderParametersImpl params2 = new PropertiesBuilderParametersImpl();

        params2.inheritFrom(params.getParameters());
        final Map<String, Object> parameters = params2.getParameters();
        assertEquals(Boolean.TRUE, parameters.get("throwExceptionOnMissing"));
        assertEquals(includeListener, parameters.get("includeListener"));
        assertEquals(factory, parameters.get("IOFactory"));
        assertEquals(Boolean.FALSE, parameters.get("includesAllowed"));
        assertNull(parameters.get("layout"));
    }

    /**
     * Tests whether the include listener can be set.
     */
    @Test
    void testSetIncludeListener() {
        @SuppressWarnings("unchecked")
        final ConfigurationConsumer<ConfigurationException> includeListener = mock(ConfigurationConsumer.class);
        assertSame(params, params.setIncludeListener(includeListener));
        assertSame(includeListener, params.getParameters().get("includeListener"));
    }

    /**
     * Tests whether the IncludeListener property can be correctly set.
     */
    @Test
    void testSetIncludeListenerProperty() throws ConfigurationException {
        final ConfigurationConsumer<ConfigurationException> includeListener = PropertiesConfiguration.DEFAULT_INCLUDE_LISTENER;
        final ConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
            .configure(params.setIncludeListener(includeListener));

        final PropertiesConfiguration config = builder.getConfiguration();
        assertEquals(includeListener, config.getIncludeListener());
    }

    /**
     * Tests whether the includesAllowed property can be set.
     */
    @Test
    void testSetIncludesAllowed() {
        assertSame(params, params.setIncludesAllowed(true));
        assertEquals(Boolean.TRUE, params.getParameters().get("includesAllowed"));
    }

    /**
     * Tests whether the IO factory can be set.
     */
    @Test
    void testSetIOFactory() {
        final PropertiesConfiguration.IOFactory factory = mock(PropertiesConfiguration.IOFactory.class);
        assertSame(params, params.setIOFactory(factory));
        assertSame(factory, params.getParameters().get("IOFactory"));
    }

    /**
     * Tests whether the IOFactory property can be correctly set. This test is related to CONFIGURATION-648.
     */
    @Test
    void testSetIOFactoryProperty() throws ConfigurationException {
        final PropertiesConfiguration.IOFactory factory = new PropertiesConfiguration.DefaultIOFactory();
        final ConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
            .configure(params.setIOFactory(factory));

        final PropertiesConfiguration config = builder.getConfiguration();
        assertEquals(factory, config.getIOFactory());
    }

    /**
     * Tests whether the layout object can be set.
     */
    @Test
    void testSetLayout() {
        final PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout();
        assertSame(params, params.setLayout(layout));
        assertSame(layout, params.getParameters().get("layout"));
    }
}
