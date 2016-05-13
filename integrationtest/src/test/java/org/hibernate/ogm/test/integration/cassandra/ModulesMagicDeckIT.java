/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.cassandra;

import static org.hibernate.ogm.test.integration.testcase.util.CassandraConfigurationHelper.setCassandraHostName;
import static org.hibernate.ogm.test.integration.testcase.util.CassandraConfigurationHelper.setCassandraPort;

import org.hibernate.ogm.test.integration.testcase.MagiccardsDatabaseScenario;
import org.hibernate.ogm.test.integration.testcase.controller.MagicCardsCollectionBean;
import org.hibernate.ogm.test.integration.testcase.model.MagicCard;
import org.hibernate.ogm.test.integration.testcase.util.ModulesHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceDescriptor;
import org.jboss.shrinkwrap.descriptor.api.persistence20.PersistenceUnit;
import org.jboss.shrinkwrap.descriptor.api.persistence20.Properties;
import org.junit.runner.RunWith;

/**
 * Test for the Hibernate OGM module in WildFly using Cassandra.
 * The class name has to finish with "ITCassandra" for the test to be enabled in the right profile.
 * <p>
 * At time of writing, the Cassandra GridDialect implementation is not ready to handle associations
 * so we'll use an over simplified model.
 *
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class ModulesMagicDeckIT extends MagiccardsDatabaseScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, "modules-magic-cassandra.war" )
				.addClasses( MagicCard.class, MagicCardsCollectionBean.class, ModulesMagicDeckIT.class, MagiccardsDatabaseScenario.class );
		String persistenceXml = persistenceXml().exportAsString();
		persistenceXml = ModulesHelper.injectVariables( persistenceXml );
		webArchive.addAsResource( new StringAsset( persistenceXml ), "META-INF/persistence.xml" );
		ModulesHelper.addModulesDependencyDeclaration( webArchive, "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.cassandra:${hibernate-ogm.module.slot} services" );
		return webArchive;
	}

	private static PersistenceDescriptor persistenceXml() {
		PersistenceDescriptor descriptor = Descriptors.create( PersistenceDescriptor.class );
		Properties<PersistenceUnit<PersistenceDescriptor>> properties = descriptor
			.version( "2.0" )
			.createPersistenceUnit()
				.name( "primary" )
				.provider( "org.hibernate.ogm.jpa.HibernateOgmPersistence" )
				.clazz( MagicCard.class.getName() )
				.getOrCreateProperties()
					.createProperty().name( "jboss.as.jpa.providerModule" ).value( "application" ).up()
					.createProperty().name( "hibernate.search.default.directory_provider" ).value( "ram" ).up()
					.createProperty().name( "hibernate.ogm.datastore.database" ).value( "ogm_test_database" ).up()
					.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "cassandra_experimental" ).up()
					.createProperty().name( "hibernate.transaction.jta.platform" ).value( "JBossAS" ).up()
					.createProperty().name( "wildfly.jpa.hibernate.search.module" ).value( "org.hibernate.search.orm:${hibernate-search.module.slot}" ).up();

		setCassandraHostName( properties );
		setCassandraPort( properties );

		return descriptor;
	}

}
