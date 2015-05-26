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
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.runner.RunWith;

import static org.hibernate.ogm.test.integration.jboss.util.CassandraConfigurationHelper.setCassandraPort;
import static org.hibernate.ogm.test.integration.jboss.util.CassandraConfigurationHelper.setCassandraHostName;

/**
 * Test for the Hibernate OGM module in WildFly using Cassandra.
 * The class name has to finish with "ITCassandra" for the test to be enabled in the right profile.
 *
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class ModuleMemberRegistrationITCassandra extends ModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
				.Builder( ModuleMemberRegistrationITCassandra.class)
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate:ogm services, org.hibernate.ogm.cassandra services, org.hibernate.search.orm:${hibernate-search.module.slot} services" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() {
		PersistenceDescriptor descriptor = Descriptors.create( PersistenceDescriptor.class );
		Properties<PersistenceUnit<PersistenceDescriptor>> properties = descriptor
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( Member.class.getName() )
				.getOrCreateProperties()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.createProperty().name( "hibernate.ogm.datastore.database" ).value( "ogm_test_database" ).up()
					.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "cassandra_experimental" ).up();

		setCassandraHostName( properties );
		setCassandraPort( properties );

		return descriptor;
	}

}
