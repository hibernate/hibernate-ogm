/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.testcase.util;

import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;

public final class CassandraConfigurationHelper {

	public static final String CASSANDRA_HOSTNAME = System.getenv( "CASSANDRA_HOSTNAME" );
	public static final String CASSANDRA_PORT = System.getenv( "CASSANDRA_PORT" );

	private CassandraConfigurationHelper() {
		//not to be created
	}

	public static void setCassandraPort(Properties<PersistenceUnit<PersistenceDescriptor>> properties) {
		if ( CASSANDRA_PORT != null ) {
			properties.createProperty().name( "hibernate.ogm.datastore.port" ).value( CASSANDRA_PORT );
		}
	}

	public static void setCassandraHostName(Properties<PersistenceUnit<PersistenceDescriptor>> properties) {
		if ( CASSANDRA_HOSTNAME != null ) {
			properties.createProperty().name( "hibernate.ogm.datastore.host" ).value( CASSANDRA_HOSTNAME );
		}
	}

}
