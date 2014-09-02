/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg;

/**
 * Common properties for configuring NoSql datastores via {@code persistence.xml} or
 * {@link OgmConfiguration}.
 * <p>
 * Note that not all properties are supported by all datastores; refer to the documentation of the specific dialect to
 * find out the supported configuration properties.
 * <p>
 * This interface should not be implemented by client code, only its constants are intended to be referenced.
 *
 * @author Gunnar Morling
 */
public interface OgmProperties {

	/**
	 * Name of the configuration option for specifying an {@link OptionConfigurator} when bootstrapping Hibernate OGM.
	 * Supported value types are:
	 * <ul>
	 * <li>{@link String}: the fully qualified name of an {@code OptionConfigurator} type</li>
	 * <li>{@link Class}: the class object representing an {@code OptionConfigurator} type</li>
	 * <li>{@code OptionConfigurator}: a configurator instance</li>
	 * </ul>
	 */
	String OPTION_CONFIGURATOR = "hibernate.ogm.option.configurator";

	String GRID_DIALECT = "hibernate.ogm.datastore.grid_dialect";

	/**
	 * Property for setting the datastore provider. Can take the following values:
	 * <ul>
	 * <li>a {@code DatastoreProvider} instance</li>
	 * <li>a {@code DatastoreProvider} class</li>
	 * <li>a string representing the {@code DatastoreProvider} class</li>
	 * <li>a string representing one of the datastore provider shortcuts (case-insensitive; a constant with the name to
	 * be used can be found on the public identifier type of your chosen grid dialect, e.g.
	 * {@code MongoDB#DATASTORE_PROVIDER_NAME})</li>
	 * </ul>
	 * If the property is not set, Infinispan is used by default.
	 */
	String DATASTORE_PROVIDER = "hibernate.ogm.datastore.provider";

	/**
	 * Property for setting the host name to connect to. Accepts {@code String}.
	 */
	String HOST = "hibernate.ogm.datastore.host";

	/**
	 * Property for setting the port number of the database to connect to. Accepts {@code int}.
	 */
	String PORT = "hibernate.ogm.datastore.port";

	/**
	 * Property for setting the name of the database to connect to. Accepts {@code String}.
	 */
	String DATABASE = "hibernate.ogm.datastore.database";

	/**
	 * Property for setting the user name to connect with. Accepts {@code String}.
	 */
	String USERNAME = "hibernate.ogm.datastore.username";

	/**
	 * Property for setting the password to connect with. Accepts {@code String}.
	 */
	String PASSWORD = "hibernate.ogm.datastore.password";

	/**
	 * Property for specifying whether the database to connect to should be created when it doesn't exist. Default to
	 * false. The user defined with {@link #USERNAME} must have the privileges for the creation of a new database. The
	 * database will have the name defined with {@link #DATABASE}. Accepts "true" or "false".
	 */
	String CREATE_DATABASE = "hibernate.ogm.datastore.create_database";
}
