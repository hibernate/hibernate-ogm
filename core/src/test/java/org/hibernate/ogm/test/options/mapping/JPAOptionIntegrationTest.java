/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.utils.TestHelper.dropSchemaAndDatabase;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.jpa.impl.OgmEntityManagerFactory;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.options.spi.OptionsService;
import org.hibernate.ogm.options.spi.OptionsService.OptionsServiceContext;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.ForceExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.mapping.model.Microwave;
import org.hibernate.ogm.test.options.mapping.model.Refrigerator;
import org.hibernate.ogm.utils.PackagingRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for specifying the name of an {@link org.hibernate.ogm.cfg.OptionConfigurator} type in persistence.xml.
 *
 * @author Gunnar Morling
 */
public class JPAOptionIntegrationTest {

	@Rule
	public PackagingRule packaging = new PackagingRule( "persistencexml/jpajtastandalone-options.xml", Refrigerator.class );

	@Test
	public void shouldApplyOptionConfiguratorSpecifiedInPersistenceXml() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory( "jpajtastandalone-options" );
		OptionsServiceContext optionsContext = getOptionsContext( emf );

		OptionsContext refrigatorOptions = optionsContext.getEntityOptions( Refrigerator.class );
		assertThat( refrigatorOptions.getUnique( ForceExampleOption.class ) ).isTrue();

		OptionsContext microwaveOptions = optionsContext.getEntityOptions( Microwave.class );
		assertThat( microwaveOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "test" );

		OptionsContext temperatureOptions = optionsContext.getPropertyOptions( Refrigerator.class, "temperature" );
		assertThat( temperatureOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Embedded" );

		dropSchemaAndDatabase( emf );
		emf.close();
	}

	private OptionsServiceContext getOptionsContext(EntityManagerFactory emf) {
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) ( (OgmEntityManagerFactory) emf ).getSessionFactory();
		return sessionFactory.getServiceRegistry().getService( OptionsService.class ).context();
	}
}
