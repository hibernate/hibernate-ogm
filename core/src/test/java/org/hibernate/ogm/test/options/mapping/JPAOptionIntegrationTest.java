/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
 * Test for specifying the name of an {@link org.hibernate.ogm.cfg.spi.OptionConfigurator} type in persistence.xml.
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
