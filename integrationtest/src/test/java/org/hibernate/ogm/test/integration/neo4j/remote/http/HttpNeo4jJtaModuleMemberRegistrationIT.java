/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.neo4j.remote.http;

import static org.hibernate.ogm.test.integration.neo4j.remote.RemoteNeo4jEnvironmentVariables.getNeo4jHost;
import static org.hibernate.ogm.test.integration.neo4j.remote.RemoteNeo4jEnvironmentVariables.getNeo4jPassword;
import static org.hibernate.ogm.test.integration.neo4j.remote.RemoteNeo4jEnvironmentVariables.getNeo4jUsername;

import org.hibernate.ogm.datastore.neo4j.Neo4j;
import org.hibernate.ogm.datastore.neo4j.Neo4jProperties;
import org.hibernate.ogm.jpa.HibernateOgmPersistence;
import org.hibernate.ogm.test.integration.neo4j.Neo4jModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.neo4j.remote.controller.RemoteNeo4jJtaCleaner;
import org.hibernate.ogm.test.integration.testcase.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnitTransactionType;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module using Neo4j
 *
 * @author Davide D'Alto
 * @author Guillaume Scheibel &lt;guillaume.scheibel@gmail.com&gt;
 */
@RunWith(Arquillian.class)
public class HttpNeo4jJtaModuleMemberRegistrationIT extends Neo4jModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() throws Exception {
		return new ModuleMemberRegistrationDeployment
				.Builder( HttpNeo4jJtaModuleMemberRegistrationIT.class )
					.addClasses( Neo4jModuleMemberRegistrationScenario.class, RemoteNeo4jJtaCleaner.class )
				.persistenceXml( persistenceXml() )
				.manifestDependencies( "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.neo4j:${hibernate-ogm.module.slot} services" )
				.createDeployment();
	}

	private static PersistenceDescriptor persistenceXml() throws Exception {
		Properties<PersistenceUnit<PersistenceDescriptor>> propertiesContext = Descriptors.create( PersistenceDescriptor.class )
				.version( "2.0" )
				.createPersistenceUnit()
				.name( "primary" )
				.transactionType( PersistenceUnitTransactionType._JTA )
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
}
