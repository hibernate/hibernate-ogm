/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.ogm.datastore.infinispanremote.configuration.impl.InfinispanRemoteConfiguration;
import org.hibernate.ogm.datastore.keyvalue.cfg.KeyValueStoreProperties;

/**
 * Properties for configuring the Infinispan Remote datastore via {@code persistence.xml} or
 * {@link StandardServiceRegistryBuilder}.
 * <p>
 * It also contains the properites to configure the Hot Rod client.
 * There are two ways to configure the client:
 * <ul>
 *   <li>Using an external Hot Rod properties file; see {@link InfinispanRemoteProperties#CONFIGURATION_RESOURCE_NAME}
 *   <li>Defining the properties of the client using {@value InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}
 * </ul>
 * <p>
 * When a property of the Hot Rod client is set in the hibernate configuration, it should be prefixed
 * with {@value InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}.
 * For example, the property {@code infinispan.client.hotrod.server_list}
 * becomes {@code hibernate.ogm.infinispan_remote.client.server_list}.
 * <p>
 * Currently, some properties in the Hot Rod client don't have a prefix, in this case it must be added
 * in the hibernate configuration.
 * For example, the property {@code maxActive} becomes {@code hibernate.ogm.infinispan_remote.client.maxActive}
 * <p>
 * Properties with the Hibernate OGM prefix ({@value InfinispanRemoteProperties#HOT_ROD_CLIENT_PREFIX}) will
 * override corresponding properties defined in the external Hot Rod configuration file.
 *
 * @see InfinispanRemoteConfiguration
 *
 * @author Davide D'Alto
 */
public final class InfinispanRemoteProperties implements KeyValueStoreProperties {

	/**
	 * The configuration property to use as key to define a custom configuration resource
	 * for the Hot Rod (Infinispan remote) client.
	 */
	public static final String CONFIGURATION_RESOURCE_NAME = "hibernate.ogm.infinispan_remote.configuration_resource_name";

	/**
	 * Prefix for the Hot Rod (Infinispan remote) client properties.
	 */
	public static final String HOT_ROD_CLIENT_PREFIX = "hibernate.ogm.infinispan_remote.client.";

	/**
	 * You can inject an instance of {@link org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaCapture} into
	 * the configuration properties to capture the generated Protobuf schema.
	 * Useful for testing, or to dump the schema somewhere else.
	 */
	public static final String SCHEMA_CAPTURE_SERVICE = "hibernate.ogm.infinispan_remote.schema_capture_service";

	/**
	 * You can inject an instance of {@link org.hibernate.ogm.datastore.infinispanremote.schema.spi.SchemaOverride} into
	 * the configuration properties to override the Protobuf schema being generated.
	 * This will not affect how entities are encoded, so the alternative schema must be compatible.
	 */
	public static final String SCHEMA_OVERRIDE_SERVICE = "hibernate.ogm.infinispan_remote.schema_override_service";

	/**
	 * The configuration property key to configure the package name to be used in Protobuf generated schemas.
	 */
	public static final String SCHEMA_PACKAGE_NAME = "hibernate.ogm.infinispan_remote.schema_package_name";

	/**
	 * The default package name for Protobuf schemas. Override using SCHEMA_PACKAGE_NAME.
	 * @see #SCHEMA_PACKAGE_NAME
	 */
	public static final String DEFAULT_SCHEMA_PACKAGE_NAME = "HibernateOGMGenerated";

	private InfinispanRemoteProperties() {
	}

}
