/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.configurationreader.spi;

import java.util.Map;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.ogm.cfg.OgmConfiguration;
import org.hibernate.ogm.util.configurationreader.impl.SimplePropertyReaderContext;

/**
 * Provides a safe access to configuration values as typically configured via {@link OgmConfiguration} or
 * {@code persistence.xml}.
 * <p>
 * Values can be given in two ways:
 * <ul>
 * <li>as literal value e.g. a {@code String}, {@code int}, {@code enum} or {@code java.net.URL}. These values can
 * either be specified as instance of the target type or as String which can be converted into the target type (in case
 * of URLs this string can either represent a class path element, an URL or a file system path).</li>
 * <li>as an implementation type of the target type. In this case, values can be specified in the following
 * representations:
 * <ul>
 * <li>as instance of the expected target type</li>
 * <li>as {@link Class}, representing a sub-type of the expected target type</li>
 * <li>as string, representing the FQN of a sub-type of the expected target type</li>
 * <li>as string, representing a short name as resolvable via a given {@link ShortNameResolver}</li>
 * </ul>
 * If specified as class name, short name or class object, the specified type will be instantiated using its default
 * constructor.</li>
 * </ul>
 * <p>
 *
 * @author Gunnar Morling
 */
public class ConfigurationPropertyReader {

	private final Map<?, ?> properties;
	private final ClassLoaderService classLoaderService;

	public ConfigurationPropertyReader(Configuration configuration) {
		this( configuration.getProperties(), null );
	}

	public ConfigurationPropertyReader(Map<?, ?> properties) {
		this( properties, null );
	}

	public ConfigurationPropertyReader(Configuration configuration, ClassLoaderService classLoaderService) {
		this( configuration.getProperties(), classLoaderService );
	}

	public ConfigurationPropertyReader(Map<?, ?> properties, ClassLoaderService classLoaderService) {
		this.properties = properties;
		this.classLoaderService = classLoaderService;
	}

	/**
	 * Returns a context for retrieving the specified property. The returned context allows to customize the value
	 * retrieval logic, e.g. by setting a default value or marking the property as required.
	 *
	 * @param propertyName the name of the property to retrieve
	 * @param targetType the target type of the property
	 * @return a context for retrieving the specified property
	 */
	public <T> PropertyReaderContext<T> property(String propertyName, Class<T> targetType) {
		return new SimplePropertyReaderContext<T>( classLoaderService, properties, propertyName, targetType );
	}
}
