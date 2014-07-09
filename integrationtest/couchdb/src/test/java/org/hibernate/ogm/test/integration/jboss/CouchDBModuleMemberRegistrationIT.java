/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.jboss;

import org.hibernate.ogm.test.integration.jboss.model.Member;
import org.hibernate.ogm.test.integration.jboss.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.runner.RunWith;

/**
 * Test the hibernate OGM module in JBoss AS using CouchDB
 *
 * @author Andrea Boriero &lt;dreborier@gmail.com&gt;
 */
@RunWith(Arquillian.class)
public class CouchDBModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	public static final String ENVIRONMENT_COUCHDB_HOSTNAME = "COUCHDB_HOSTNAME";
	public static final String ENVIRONMENT_COUCHDB_PORT = "COUCHDB_PORT";

	public static final String DEFAULT_HOSTNAME = "localhost";
	public static final String DEFAULT_PORT = "5984";

	private static String couchDBHostName;
	private static String couchDBPortNumber;

	static {
		setHostName();
		setPortNumber();
	}

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment.Builder( CouchDBModuleMemberRegistrationIT.class )
				.persistenceXml( persistenceXml() )
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.createDeployment();
	}

	private static void setHostName() {
		couchDBHostName = System.getenv( ENVIRONMENT_COUCHDB_HOSTNAME );
		if ( isNull( couchDBHostName ) ) {
			couchDBHostName = DEFAULT_HOSTNAME;
		}
	}

	private static void setPortNumber() {
		couchDBPortNumber = System.getenv( ENVIRONMENT_COUCHDB_PORT );
		if ( isNull( couchDBPortNumber ) ) {
			couchDBPortNumber = DEFAULT_PORT;
		}
	}

	private static boolean isNull(String value) {
		return value == null || value.length() == 0 || value.toLowerCase().equals( "null" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
					.version( "2.0" )
					.createPersistenceUnit()
						.name( "primary" )
						.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
						.clazz( Member.class.getName() )
						.getOrCreateProperties()
							.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "couchdb" ).up()
							.createProperty().name( "hibernate.ogm.datastore.host" ).value( couchDBHostName ).up()
							.createProperty().name( "hibernate.ogm.datastore.port" ).value( couchDBPortNumber ).up()
							.createProperty().name( "hibernate.ogm.datastore.database" ).value( "ogm_test_database" ).up()
							.createProperty().name( "hibernate.ogm.datastore.create_database" ).value( "true" ).up()
							.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.up().up();
	}

}
