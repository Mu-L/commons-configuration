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

package org.apache.commons.configuration2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.configuration2.convert.ListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;

/**
 * <p>
 * {@code CompositeConfiguration} allows you to add multiple {@code Configuration} objects to an aggregated
 * configuration. If you add Configuration1, and then Configuration2, any properties shared will mean that the value
 * defined by Configuration1 will be returned. If Configuration1 doesn't have the property, then Configuration2 will be
 * checked. You can add multiple different types or the same type of properties file.
 * </p>
 * <p>
 * When querying properties the order in which child configurations have been added is relevant. To deal with property
 * updates, a so-called <em>in-memory configuration</em> is used. Per default, such a configuration is created
 * automatically. All property writes target this special configuration. There are constructors which allow you to
 * provide a specific in-memory configuration. If used that way, the in-memory configuration is always the last one in
 * the list of child configurations. This means that for query operations all other configurations take precedence.
 * </p>
 * <p>
 * Alternatively it is possible to mark a child configuration as in-memory configuration when it is added. In this case
 * the treatment of the in-memory configuration is slightly different: it remains in the list of child configurations at
 * the position it was added, i.e. its priority for property queries can be defined by adding the child configurations
 * in the correct order.
 * </p>
 * <p>
 * This configuration class uses a {@code Synchronizer} to control concurrent access. While all methods for reading and
 * writing configuration properties make use of this {@code Synchronizer} per default, the methods for managing the list
 * of child configurations and the in-memory configuration
 * ({@code addConfiguration(), getNumberOfConfigurations(), removeConfiguration(),
 * getConfiguration(), getInMemoryConfiguration()}) are guarded, too. Because most methods for accessing configuration
 * data delegate to the list of child configurations, the thread-safety of a {@code CompositeConfiguration} object also
 * depends on the {@code Synchronizer} objects used by these children.
 * </p>
 */
public class CompositeConfiguration extends AbstractConfiguration implements Cloneable {

    /** List holding all the configuration */
    private List<Configuration> configList = new LinkedList<>();

    /**
     * Configuration that holds in memory stuff. Inserted as first so any setProperty() override anything else added.
     */
    private Configuration inMemoryConfiguration;

    /**
     * Stores a flag whether the current in-memory configuration is also a child configuration.
     */
    private boolean inMemoryConfigIsChild;

    /**
     * Creates an empty CompositeConfiguration object which can then be added some other Configuration files
     */
    public CompositeConfiguration() {
        clear();
    }

    /**
     * Create a CompositeConfiguration with an empty in memory configuration and adds the collection of configurations
     * specified.
     *
     * @param configurations the collection of configurations to add
     */
    public CompositeConfiguration(final Collection<? extends Configuration> configurations) {
        this(new BaseConfiguration(), configurations);
    }

    /**
     * Creates a CompositeConfiguration object with a specified <em>in-memory configuration</em>. This configuration will
     * store any changes made to the {@code CompositeConfiguration}. Note: Use this constructor if you want to set a special
     * type of in-memory configuration. If you have a configuration which should act as both a child configuration and as
     * in-memory configuration, use {@link #addConfiguration(Configuration, boolean)} with a value of <strong>true</strong> instead.
     *
     * @param inMemoryConfiguration the in memory configuration to use
     */
    public CompositeConfiguration(final Configuration inMemoryConfiguration) {
        this.configList.clear();
        this.inMemoryConfiguration = inMemoryConfiguration;
        this.configList.add(inMemoryConfiguration);
    }

    /**
     * Creates a CompositeConfiguration with a specified <em>in-memory configuration</em>, and then adds the given
     * collection of configurations.
     *
     * @param inMemoryConfiguration the in memory configuration to use
     * @param configurations the collection of configurations to add
     * @see #CompositeConfiguration(Configuration)
     */
    public CompositeConfiguration(final Configuration inMemoryConfiguration, final Collection<? extends Configuration> configurations) {
        this(inMemoryConfiguration);
        if (configurations != null) {
            configurations.forEach(this::addConfiguration);
        }
    }

    /**
     * Add a configuration.
     *
     * @param config the configuration to add
     */
    public void addConfiguration(final Configuration config) {
        addConfiguration(config, false);
    }

    /**
     * Adds a child configuration and optionally makes it the <em>in-memory configuration</em>. This means that all future
     * property write operations are executed on this configuration. Note that the current in-memory configuration is
     * replaced by the new one. If it was created automatically or passed to the constructor, it is removed from the list of
     * child configurations! Otherwise, it stays in the list of child configurations at its current position, but it passes
     * its role as in-memory configuration to the new one.
     *
     * @param config the configuration to be added
     * @param asInMemory <strong>true</strong> if this configuration becomes the new <em>in-memory</em> configuration, <strong>false</strong>
     *        otherwise
     * @since 1.8
     */
    public void addConfiguration(final Configuration config, final boolean asInMemory) {
        syncWrite(() -> {
            if (!configList.contains(config)) {
                if (asInMemory) {
                    replaceInMemoryConfiguration(config);
                    inMemoryConfigIsChild = true;
                }
                if (!inMemoryConfigIsChild) {
                    // As the inMemoryConfiguration contains all manually added
                    // keys, we must make sure that it is always last. "Normal", non
                    // composed configurations add their keys at the end of the
                    // configuration and we want to mimic this behavior.
                    configList.add(configList.indexOf(inMemoryConfiguration), config);
                } else {
                    // However, if the in-memory configuration is a regular child,
                    // only the order in which child configurations are added is relevant
                    configList.add(config);
                }
                if (config instanceof AbstractConfiguration) {
                    ((AbstractConfiguration) config).setThrowExceptionOnMissing(isThrowExceptionOnMissing());
                }
            }
        }, false);
    }

    /**
     * Add a configuration to the start of the list of child configurations.
     *
     * @param config the configuration to add
     * @since 2.3
     */
    public void addConfigurationFirst(final Configuration config) {
        addConfigurationFirst(config, false);
    }

    /**
     * Adds a child configuration to the start of the collection and optionally makes it the <em>in-memory
     * configuration</em>. This means that all future property write operations are executed on this configuration. Note
     * that the current in-memory configuration is replaced by the new one. If it was created automatically or passed to the
     * constructor, it is removed from the list of child configurations! Otherwise, it stays in the list of child
     * configurations at its current position, but it passes its role as in-memory configuration to the new one.
     *
     * @param config the configuration to be added
     * @param asInMemory <strong>true</strong> if this configuration becomes the new <em>in-memory</em> configuration, <strong>false</strong>
     *        otherwise
     * @since 2.3
     */
    public void addConfigurationFirst(final Configuration config, final boolean asInMemory) {
        syncWrite(() -> {
            if (!configList.contains(config)) {
                if (asInMemory) {
                    replaceInMemoryConfiguration(config);
                    inMemoryConfigIsChild = true;
                }
                configList.add(0, config);
                if (config instanceof AbstractConfiguration) {
                    ((AbstractConfiguration) config).setThrowExceptionOnMissing(isThrowExceptionOnMissing());
                }
            }
        }, false);
    }

    /**
     * Add this property to the in-memory Configuration.
     *
     * @param key The Key to add the property to.
     * @param token The Value to add.
     */
    @Override
    protected void addPropertyDirect(final String key, final Object token) {
        inMemoryConfiguration.addProperty(key, token);
    }

    /**
     * Adds the value of a property to the given list. This method is used by {@code getList()} for gathering property
     * values from the child configurations.
     *
     * @param dest the list for collecting the data
     * @param config the configuration to query
     * @param key the key of the property
     */
    private void appendListProperty(final List<Object> dest, final Configuration config, final String key) {
        final Object value = interpolate(config.getProperty(key));
        if (value != null) {
            if (value instanceof Collection) {
                final Collection<?> col = (Collection<?>) value;
                dest.addAll(col);
            } else {
                dest.add(value);
            }
        }
    }

    /**
     * Removes all child configurations and reinitializes the <em>in-memory configuration</em>. <strong>Attention:</strong>
     * A new in-memory configuration is created; the old one is lost.
     */
    @Override
    protected void clearInternal() {
        configList.clear();
        // recreate the in memory configuration
        inMemoryConfiguration = new BaseConfiguration();
        ((BaseConfiguration) inMemoryConfiguration).setThrowExceptionOnMissing(isThrowExceptionOnMissing());
        ((BaseConfiguration) inMemoryConfiguration).setListDelimiterHandler(getListDelimiterHandler());
        configList.add(inMemoryConfiguration);
        inMemoryConfigIsChild = false;
    }

    @Override
    protected void clearPropertyDirect(final String key) {
        configList.forEach(config -> config.clearProperty(key));
    }

    /**
     * Returns a copy of this object. This implementation will create a deep clone, i.e. all configurations contained in
     * this composite will also be cloned. This only works if all contained configurations support cloning; otherwise a
     * runtime exception will be thrown. Registered event handlers won't get cloned.
     *
     * @return the copy
     * @since 1.3
     */
    @Override
    public Object clone() {
        try {
            final CompositeConfiguration copy = (CompositeConfiguration) super.clone();
            copy.configList = new LinkedList<>();
            copy.inMemoryConfiguration = ConfigurationUtils.cloneConfiguration(getInMemoryConfiguration());
            copy.configList.add(copy.inMemoryConfiguration);

            configList.forEach(config -> {
                if (config != getInMemoryConfiguration()) {
                    copy.addConfiguration(ConfigurationUtils.cloneConfiguration(config));
                }
            });

            copy.cloneInterpolator(this);
            return copy;
        } catch (final CloneNotSupportedException cnex) {
            // cannot happen
            throw new ConfigurationRuntimeException(cnex);
        }
    }

    @Override
    protected boolean containsKeyInternal(final String key) {
        return configList.stream().anyMatch(config -> config.containsKey(key));
    }

    /**
     * Tests whether this configuration contains one or more matches to this value. This operation stops at first
     * match but may be more expensive than the containsKey method.
     * @since 2.11.0
     */
    @Override
    protected boolean containsValueInternal(final Object value) {
        return configList.stream().anyMatch(config -> config.containsValue(value));
    }

    /**
     * Gets the configuration at the specified index.
     *
     * @param index The index of the configuration to retrieve
     * @return the configuration at this index
     */
    public Configuration getConfiguration(final int index) {
        return syncRead(() -> configList.get(index), false);
    }

    /**
     * Gets the &quot;in memory configuration&quot;. In this configuration changes are stored.
     *
     * @return the in memory configuration
     */
    public Configuration getInMemoryConfiguration() {
        return syncReadValue(inMemoryConfiguration, false);
    }

    @Override
    protected Iterator<String> getKeysInternal() {
        final Set<String> keys = new LinkedHashSet<>();
        configList.forEach(config -> config.getKeys().forEachRemaining(keys::add));
        return keys.iterator();
    }

    @Override
    protected Iterator<String> getKeysInternal(final String key) {
        final Set<String> keys = new LinkedHashSet<>();
        configList.forEach(config -> config.getKeys(key).forEachRemaining(keys::add));
        return keys.iterator();
    }

    @Override
    protected Iterator<String> getKeysInternal(final String key, final String delimiter) {
        final Set<String> keys = new LinkedHashSet<>();
        configList.forEach(config -> config.getKeys(key, delimiter).forEachRemaining(keys::add));
        return keys.iterator();
    }

    @Override
    public List<Object> getList(final String key, final List<?> defaultValue) {
        final List<Object> list = new ArrayList<>();

        // add all elements from the first configuration containing the requested key
        final Iterator<Configuration> it = configList.iterator();
        while (it.hasNext() && list.isEmpty()) {
            final Configuration config = it.next();
            if (config != inMemoryConfiguration && config.containsKey(key)) {
                appendListProperty(list, config, key);
            }
        }

        // add all elements from the in memory configuration
        appendListProperty(list, inMemoryConfiguration, key);

        if (list.isEmpty()) {
            // This is okay because we just return this list to the caller
            @SuppressWarnings("unchecked")
            final List<Object> resultList = (List<Object>) defaultValue;
            return resultList;
        }

        final ListIterator<Object> lit = list.listIterator();
        while (lit.hasNext()) {
            lit.set(interpolate(lit.next()));
        }

        return list;
    }

    /**
     * Gets the number of configurations.
     *
     * @return the number of configuration
     */
    public int getNumberOfConfigurations() {
        return syncRead(configList::size, false);
    }

    /**
     * Reads property from underlying composite
     *
     * @param key key to use for mapping
     * @return object associated with the given configuration key.
     */
    @Override
    protected Object getPropertyInternal(final String key) {
        return configList.stream().filter(config -> config.containsKey(key)).findFirst().map(config -> config.getProperty(key)).orElse(null);
    }

    /**
     * Gets the configuration source, in which the specified key is defined. This method will iterate over all existing
     * child configurations and check whether they contain the specified key. The following constellations are possible:
     * <ul>
     * <li>If exactly one child configuration contains the key, this configuration is returned as the source configuration.
     * This may be the <em>in memory configuration</em> (this has to be explicitly checked by the calling application).</li>
     * <li>If none of the child configurations contain the key, <strong>null</strong> is returned.</li>
     * <li>If the key is contained in multiple child configurations or if the key is <strong>null</strong>, a
     * {@code IllegalArgumentException} is thrown. In this case the source configuration cannot be determined.</li>
     * </ul>
     *
     * @param key the key to be checked
     * @return the source configuration of this key
     * @throws IllegalArgumentException if the source configuration cannot be determined
     * @since 1.5
     */
    public Configuration getSource(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null!");
        }

        Configuration source = null;
        for (final Configuration conf : configList) {
            if (conf.containsKey(key)) {
                if (source != null) {
                    throw new IllegalArgumentException("The key " + key + " is defined by multiple sources!");
                }
                source = conf;
            }
        }

        return source;
    }

    @Override
    public String[] getStringArray(final String key) {
        final List<Object> list = getList(key);

        // transform property values into strings
        final String[] tokens = new String[list.size()];

        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = String.valueOf(list.get(i));
        }

        return tokens;
    }

    @Override
    protected boolean isEmptyInternal() {
        return configList.stream().allMatch(Configuration::isEmpty);
    }

    /**
     * Remove a configuration. The in memory configuration cannot be removed.
     *
     * @param config The configuration to remove
     */
    public void removeConfiguration(final Configuration config) {
        syncWrite(() -> {
            // Make sure that you can't remove the inMemoryConfiguration from
            // the CompositeConfiguration object
            if (!config.equals(inMemoryConfiguration)) {
                configList.remove(config);
            }
        }, false);
    }

    /**
     * Replaces the current in-memory configuration by the given one.
     *
     * @param config the new in-memory configuration
     */
    private void replaceInMemoryConfiguration(final Configuration config) {
        if (!inMemoryConfigIsChild) {
            // remove current in-memory configuration
            configList.remove(inMemoryConfiguration);
        }
        inMemoryConfiguration = config;
    }

    /**
     * {@inheritDoc} This implementation ensures that the in memory configuration is correctly initialized.
     */
    @Override
    public void setListDelimiterHandler(final ListDelimiterHandler listDelimiterHandler) {
        if (inMemoryConfiguration instanceof AbstractConfiguration) {
            ((AbstractConfiguration) inMemoryConfiguration).setListDelimiterHandler(listDelimiterHandler);
        }
        super.setListDelimiterHandler(listDelimiterHandler);
    }
}
