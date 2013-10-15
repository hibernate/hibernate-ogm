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

import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContext;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.examples.annotations.NameExample;
import org.hibernate.ogm.test.options.mapping.SampleMappingModel.SampleGlobalContext;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ProxyFactoryWithAnnotationTest {

	@Test
	public void testAnnotatedEntity() throws Exception {
		OptionsContext context = new OptionsContext();

		assertThat( context.getEntityOptions( Example.class ) )
			.hasSize( 1 )
			.contains( new NameExampleOption( "Batman" ) );
	}

	@Test
	public void testAnnotationIsOverridenByAPI() throws Exception {
		OptionsContext context = new OptionsContext();
		ConfigurationContext configurationContext = new ConfigurationContext( context );

		SampleGlobalContext sampleMapping = new SampleMappingFactory().createMapping( configurationContext );
		sampleMapping
			.entity( Example.class )
				.name( "Name replaced" );

		assertThat( context.getEntityOptions( Example.class ) )
			.hasSize( 1 )
			.contains( new NameExampleOption( "Name replaced" ) );
	}

	@NameExample("Batman")
	private static final class Example {
	}
}
