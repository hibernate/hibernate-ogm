/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.integration.infinispan;

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
 * Test for the combination of Hibernate OGM and Hibernate Search modules on WildFly.
 *
 * @author Sanne Grinovero
 */
@RunWith(Arquillian.class)
public class SearchIntegrationIT extends MagiccardsDatabaseScenario {

	@Deployment
	public static Archive<?> createTestArchive() {
		WebArchive webArchive = ShrinkWrap
				.create( WebArchive.class, "modules-magic-searchit.war" )
				.addClasses( MagicCard.class, MagicCardsCollectionBean.class, SearchIntegrationIT.class, MagiccardsDatabaseScenario.class );
		String persistenceXml = persistenceXml().exportAsString();
		webArchive.addAsResource( new StringAsset( persistenceXml ), "META-INF/persistence.xml" );
		ModulesHelper.addModulesDependencyDeclaration( webArchive, "org.hibernate.ogm:${hibernate-ogm.module.slot} services, org.hibernate.ogm.infinispan:${hibernate-ogm.module.slot} services" );
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
					.createProperty().name( "hibernate.ogm.datastore.provider" ).value( "infinispan" ).up()
					.createProperty().name( "hibernate.ogm.infinispan.configuration_resourcename" ).value( "infinispan.xml" ).up()
					.createProperty().name( "hibernate.transaction.jta.platform" ).value( "JBossAS" ).up();
		return descriptor;
	}

}
