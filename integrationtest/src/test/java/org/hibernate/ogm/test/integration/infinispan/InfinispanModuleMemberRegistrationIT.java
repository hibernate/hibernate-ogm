/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.infinispan;

import org.hibernate.ogm.test.integration.testcase.ModuleMemberRegistrationScenario;
import org.hibernate.ogm.test.integration.testcase.model.Member;
import org.hibernate.ogm.test.integration.testcase.util.ModuleMemberRegistrationDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module in WildFly using Infinispan.
 *
 * @author Davide D'Alto
 */
@RunWith(Arquillian.class)
public class InfinispanModuleMemberRegistrationIT extends ModuleMemberRegistrationScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		return new ModuleMemberRegistrationDeployment
			.Builder( InfinispanModuleMemberRegistrationIT.class )
			.persistenceXml( persistenceXml() )
			.manifestDependencies( "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.infinispan:${hibernate-ogm.module.slot} services" )
			.createDeployment()
			.addAsResource( "infinispan.xml", "infinispan.xml" );
	}

	private static PersistenceDescriptor persistenceXml() {
		return Descriptors.create( PersistenceDescriptor.class )
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( Member.class.getName() ).getOrCreateProperties()
				.createProperty().name( "jboss.as.jpa.providerModule" ).value( "application" ).up()
				.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "infinispan" ).up()
				.createProperty().name( "hibernate.ogm.infinispan.configuration_resourcename" ).value( "infinispan.xml" ).up()
				.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
				.createProperty().name( "hibernate.transaction.jta.platform" ).value( "JBossAS" ).up()
			.up().up();
	}

}
