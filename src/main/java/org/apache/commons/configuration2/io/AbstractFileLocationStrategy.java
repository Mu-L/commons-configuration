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

package org.apache.commons.configuration2.io;

import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.ex.ConfigurationDeniedException;
import org.apache.commons.io.build.AbstractSupplier;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstracts services for FileLocationStrategy implementations.
 * <p>
 * Note that some FileLocationStrategy implementation use URLs internally to encode file locations.
 * </p>
 * <p>
 * As of version 2.15.0, by default, the only URL schemes allowed are {@code file} and {@code jar}. To override this default, you can either use the system
 * property {@code org.apache.commons.configuration2.io.FileLocationStrategy.schemes} or build a subclass of {@link AbstractFileLocationStrategy}.
 * </p>
 * <strong>Using System Properties</strong>
 * <p>
 * The system property {@code org.apache.commons.configuration2.io.FileLocationStrategy.schemes} String value must be a comma-separated list of schemes,
 * where the default is {@code "file,jar"}, and the complete list is {@code "file,http,https,jar"}.
 * </p>
 * <strong>Using a Builder</strong>
 * <p>
 * The root builder for {@link AbstractFileLocationStrategy} is {@link AbstractBuilder} where you define allowed schemes and hosts through its setter
 * methods.
 * </p>
 * <p>
 * For example, to programatically enable the shemes "file", "http", "https", and "jar" for all strategies, you write:
 * </p>
 * <pre>{@code
 * final PropertiesConfiguration pc = new PropertiesConfiguration();
 *      pc.setIncludeListener(PropertiesConfiguration.NOOP_INCLUDE_LISTENER);
 *      final FileHandler handler = new FileHandler(pc);
 *      final CombinedLocationStrategy.Builder builder = new CombinedLocationStrategy.Builder()
 *              .setSchemes(new TreeSet<>(Arrays.asList("file", "http", "https", "jar")));
 *      // @formatter:off
 *      handler.setLocationStrategy(builder.setSubStrategies(Arrays.asList(
 *              new ProvidedURLLocationStrategy(builder),
 *              new FileSystemLocationStrategy(builder),
 *              new AbsoluteNameLocationStrategy(builder),
 *              new BasePathLocationStrategy(builder),
 *              new HomeDirectoryLocationStrategy.Builder().setEvaluateBasePath(true).getUnchecked(),
 *              new HomeDirectoryLocationStrategy.Builder().setEvaluateBasePath(false).getUnchecked(),
 *              new ClasspathLocationStrategy(builder)))
 *              .get());
 *      // @formatter:on
 *      handler.setBasePath(TEST_BASE_PATH);
 *      handler.setFileName("include-load-url-host-unknown-exception.properties");
 *      handler.load();
 * }</pre>
 *
 *
 * @since 2.15.0
 * @see FileLocationStrategy
 */
public abstract class AbstractFileLocationStrategy implements FileLocationStrategy {

    /**
     * Builds new instances for subclasses.
     * <p>
     * As of version 2.15.0, by default, the only URL schemes allowed are {@code file} and {@code jar}. To override this default, you can either use the system
     * property {@code org.apache.commons.configuration2.io.FileLocationStrategy.schemes} or build a subclass of {@link AbstractFileLocationStrategy}.
     * </p>
     * <strong>Using System Properties</strong>
     * <p>
     * The system property {@code org.apache.commons.configuration2.io.FileLocationStrategy.schemes} String value must be a comma-separated list of schemes,
     * where the default is {@code "file,jar"}, and the complete list is {@code "file,http,https,jar"}.
     * </p>
     * <strong>Using a Builder</strong>
     * <p>
     * The root builder for {@link AbstractFileLocationStrategy} is {@link AbstractBuilder} where you define allowed schemes and hosts through its setter
     * methods.
     * </p>
     * <p>
     * See {@link AbstractFileLocationStrategy} learn how to grant an deny URL schemes and hosts.
     * </p>
     *
     * @param <T> The type of {@link FileLocationStrategy} to build.
     * @param <B> The builder type.
     */
    public abstract static class AbstractBuilder<T extends FileLocationStrategy, B extends AbstractBuilder<T, B>> extends AbstractSupplier<T, B> {

        /**
         * Enabled URL-based hosts, empty means all are enabled. Host are case-insensitive.
         */
        private Set<Pattern> hosts = Collections.emptySet();
        /**
         * Enabled URL-based schemes, empty means all are enabled. Schemes are case-insensitive.
         */
        private Set<String> schemes = Collections.emptySet();

        /**
         * Constructs a new instance for subclasses.
         */
        public AbstractBuilder() {
            // empty
        }

        Set<Pattern> getHosts() {
            return hosts;
        }

        Set<String> getSchemes() {
            return schemes;
        }

        /**
         * Sets enabled URL-based hosts, empty means all are enabled. URL hosts are case-insensitive.
         *
         * @param hosts enabled URL-based hosts.
         * @return {@code this} instance.
         */
        public B setHosts(final Set<Pattern> hosts) {
            this.hosts = hosts != null ? hosts : Collections.emptySet();
            return asThis();
        }

        /**
         * Sets enabled URL-based hosts, empty means all are enabled. URL hosts are case-insensitive.
         *
         * @param hosts Regular expressions enabled URL-based hosts.
         * @return {@code this} instance.
         */
        public B setHostsRegEx(final Set<String> hosts) {
            return setHosts(hosts.stream().map(e -> Pattern.compile(e, Pattern.CASE_INSENSITIVE)).collect(Collectors.toSet()));
        }

        /**
         * Sets enabled URL-based schemes, empty means all are enabled. URL schemes are case-insensitive.
         *
         * @param schemes enabled URL-based schemes, the default null means all schemes are allowed.
         * @return {@code this} instance.
         */
        public B setSchemes(final Set<String> schemes) {
            this.schemes = schemes != null ? schemes : Collections.emptySet();
            return asThis();
        }
    }

    /**
     * Builds new instances of T.
     *
     * @param <T> The type of {@link FileLocationStrategy} to build.
     */
    public static class StrategyBuilder<T extends FileLocationStrategy> extends AbstractBuilder<T, StrategyBuilder<T>> {

        /**
         * Either set this or implement get().
         */
        private final Function<StrategyBuilder<T>, T> function;

        /**
         * Constructs a new instance for subclasses.
         *
         * @param function Builds an instance of T.
         */
        public StrategyBuilder(final Function<StrategyBuilder<T>, T> function) {
            this.function = Objects.requireNonNull(function, "function");
        }

        @Override
        public T get() {
            return function.apply(asThis());
        }
    }

    /**
     * Default schemes.
     */
    private static final String DEFAULT_SCHEMES = "file,jar";
    /**
     * The system property key {@code org.apache.commons.configuration2.io.FileLocationStrategy.schemes}.
     * <p>
     * If absent, defaults to {@code "file,jar"}.
     * </p>
     * <p>
     * For complete functionality, use {@code "file,http,https,jar"}.
     * </p>
     */
    private static final String KEY_SCHEMES = "org.apache.commons.configuration2.io.FileLocationStrategy.schemes";

    private static Set<String> getSchemesProperty() {
        final Set<String> set = new LinkedHashSet<>();
        final String[] split = System.getProperty(KEY_SCHEMES, DEFAULT_SCHEMES).split(",");
        Collections.addAll(set, split);
        return set;
    }

    /**
     * Enabled URL-based hosts, empty means all are enabled. Host are case-insensitive.
     */
    private final Set<Pattern> hosts;
    /**
     * Enabled URL-based schemes, empty means all are enabled. Schemes are case-insensitive.
     */
    private final Set<String> schemes;

    /**
     * Constructs a new instance where the enabled URL schemes are read the system property
     * {@code "org.apache.commons.configuration2.io.FileLocationStrategy.schemes"}.
     * <p>
     * If absent, defaults to {@code "file,jar"}.
     * </p>
     * <p>
     * For complete functionality, use {@code "file,http,https,jar"}.
     * </p>
     */
    AbstractFileLocationStrategy() {
        this(getSchemesProperty());
    }

    AbstractFileLocationStrategy(final AbstractBuilder<?, ?> builder) {
        Objects.requireNonNull(builder, "builder");
        this.schemes = builder.schemes;
        this.hosts = builder.hosts != null ? builder.hosts : Collections.emptySet();
    }

    AbstractFileLocationStrategy(final Set<String> schemes) {
        this.schemes = schemes;
        this.hosts = Collections.emptySet();
    }

    URL check(final URL url) {
        if (url != null) {
            checkScheme("scheme", url, url.getProtocol(), schemes);
            checkHost("host", url, url.getHost(), hosts);
        }
        return url;
    }

    void checkHost(final String type, final URL url, final String value, final Set<Pattern> validSet) {
        if (!validSet.isEmpty() && StringUtils.isNotEmpty(value)) {
            hosts.stream().filter(p -> p.matcher(StringUtils.toRootLowerCase(value)).matches()).findFirst()
                    .orElseThrow(() -> new ConfigurationDeniedException(String.format("URL %s is not enabled: %s; must be one of %s", type, value, validSet)));
        }
    }

    void checkScheme(final String type, final URL url, final String value, final Set<String> validSet) {
        if (!validSet.isEmpty() && value != null && !validSet.contains(StringUtils.toRootLowerCase(value))) {
            throw new ConfigurationDeniedException(String.format(
                    "URL %s \"%s\" is not enabled, must be one of %s, override defaults with the system property \"%s\", complete set: \"file,http,https,jar\"",
                    type, value, validSet, KEY_SCHEMES));
        }
    }

    /**
     * Gets the enabled hosts.
     *
     * @return the enabled hosts.
     */
    Set<Pattern> getHosts() {
        return hosts;
    }

    /**
     * Gets the enabled schemes.
     *
     * @return the enabled schemes.
     */
    Set<String> getSchemes() {
        return schemes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [schemes=" + schemes + ", hosts=" + hosts + "]";
    }
}
