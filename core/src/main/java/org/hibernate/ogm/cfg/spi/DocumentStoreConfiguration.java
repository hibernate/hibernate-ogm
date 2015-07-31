/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.cfg.spi;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.cfg.impl.HostParser;
import org.hibernate.ogm.util.configurationreader.impl.Validators;
import org.hibernate.ogm.util.configurationreader.spi.ConfigurationPropertyReader;

/**
 * Provides access to properties common to different document datastores.
 *
 * @author Gunnar Morling
 */
public abstract class DocumentStoreConfiguration {

	/**
	 * The default host to connect to in case the {@link OgmProperties#HOST} property is not set
	 */
	private static final String DEFAULT_HOST = "localhost";

	private final Hosts hosts;
	private final String databaseName;
	private final String username;
	private final String password;
	private final boolean createDatabase;

	public DocumentStoreConfiguration(ConfigurationPropertyReader propertyReader, int defaultPort) {
		String host = propertyReader.property( OgmProperties.HOST, String.class )
				.withDefault( DEFAULT_HOST )
				.getValue();

		Integer port =  propertyReader.property( OgmProperties.PORT, Integer.class )
				.withValidator( Validators.PORT )
				.withDefault( null )
				.getValue();

		hosts = HostParser.parse( host, port, defaultPort );

		this.databaseName = propertyReader.property( OgmProperties.DATABASE, String.class )
				.required()
				.getValue();

		this.username = propertyReader.property( OgmProperties.USERNAME, String.class ).getValue();
		this.password = propertyReader.property( OgmProperties.PASSWORD, String.class ).getValue();

		this.createDatabase = propertyReader.property( OgmProperties.CREATE_DATABASE, boolean.class )
				.withDefault( false )
				.getValue();
	}

	/**
	 * @see OgmProperties#HOST
	 * @see OgmProperties#PORT
	 * @return The host name of the data store instance
	 */
	public Hosts getHosts() {
		return hosts;
	}

	/**
	 * @see OgmProperties#DATABASE
	 * @return the name of the database to connect to
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * @see OgmProperties#USERNAME
	 * @return The user name to be used for connecting with the data store
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @see OgmProperties#PASSWORD
	 * @return The password to be used for connecting with the data store
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @see OgmProperties#CREATE_DATABASE
	 * @return whether to create the database to connect to if not existent or not
	 */
	public boolean isCreateDatabase() {
		return createDatabase;
	}
}
