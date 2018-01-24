/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.wildfly.neo4j.remote.http;

import static org.hibernate.ogm.test.integration.wildfly.neo4j.remote.RemoteNeo4jEnvironmentVariables.getNeo4jHost;
import static org.hibernate.ogm.test.integration.wildfly.neo4j.remote.RemoteNeo4jEnvironmentVariables.getNeo4jPassword;
import static org.hibernate.ogm.test.integration.wildfly.neo4j.remote.RemoteNeo4jEnvironmentVariables.getNeo4jUsername;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.test.integration.wildfly.neo4j.Neo4jModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.wildfly.neo4j.remote.controller.RemoteNeo4jResourceLocalCleaner;
import org.hibernate.ogm.test.integration.wildfly.testcase.model.Member;
import org.hibernate.ogm.test.integration.wildfly.testcase.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnitTransactionType;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module using Neo4j
 *
 * @author Davide D'Alto
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
@RunWith(Arquillian.class)
public class HttpNeo4jResourceLocalModuleMemberRegistrationIT extends Neo4jModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() throws Exception {
		return new ModuleMemberRegistrationDeployment
				.Builder( HttpNeo4jResourceLocalModuleMemberRegistrationIT.class )
					.addClasses( Neo4jModuleMemberRegistrationScenario.class, RemoteNeo4jResourceLocalCleaner.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.neo4j:${hibernate-ogm.module.slot} services" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() throws Exception {
		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.transactionType( PersistenceUnitTransactionType._RESOURCE_LOCAL )
				.provider( HibernateOgmPersistence.class.getName() )
				.getOrCreateProperties();
		PersistenceDescriptor persistenceDescriptor = propertiesContext
				.createProperty().name( Neo4jProperties.DATASTORE_PROVIDER ).value( Neo4j.HTTP_DATASTORE_PROVIDER_NAME ).up()
				.createProperty().name( Neo4jProperties.HOST ).value( getNeo4jHost() ).up()
				.createProperty().name( Neo4jProperties.USERNAME ).value( getNeo4jUsername() ).up()
				.createProperty().name( Neo4jProperties.PASSWORD ).value( getNeo4jPassword() ).up()
				.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${hibernate-search.module.slot}" ).up()
				.up().up();
		return persistenceDescriptor;
	}

	@Test
	public void shouldFindPersistedMemberByIdWithNativeQuery() throws Exception {
		Member newMember = memberRegistration.getNewMember();
		newMember.setName( "Giovanni Doe" );
		memberRegistration.register();

		String nativeQuery = "MATCH (n:Member {id: " + newMember.getId() + "}) RETURN n";
		Member found = memberRegistration.findWithNativeQuery( nativeQuery );

		assertNotNull( "Expected at least one result using a native query", found );
		assertEquals( "Native query hasn't found a new member", newMember.getName(), found.getName() );
	}
}
