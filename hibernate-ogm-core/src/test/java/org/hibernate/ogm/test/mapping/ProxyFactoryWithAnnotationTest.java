/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.test.mapping;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.OptionContainerHelper.options;

import org.hibernate.ogm.mapping.impl.ConfigurationProxyFactory;
import org.hibernate.ogm.mapping.impl.MappingContext;
import org.hibernate.ogm.test.mapping.option.NameExampleOption;
import org.junit.Test;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class ProxyFactoryWithAnnotationTest {
	@Test
	public void testAnnotatedEntity() throws Exception {
		SampleMappingFactory factory = new SampleMappingFactory();
		MappingContext context = factory.createMappingContext();
		SampleMappingModel.SampleMapping sampleMapping = ConfigurationProxyFactory.get( factory.getMappingType(), context );
		sampleMapping.entity( Example.class );

		assertThat( options( context, Example.class ) )
			.hasSize( 1 )
			.contains( new NameExampleOption( "Batman" ) );
	}

	@Test
	public void testAnnotationIsOverridenByAPI() throws Exception {
		SampleMappingFactory factory = new SampleMappingFactory();
		MappingContext context = factory.createMappingContext();
		SampleMappingModel.SampleMapping sampleMapping = ConfigurationProxyFactory.get( factory.getMappingType(), context );
		sampleMapping
			.entity( Example.class )
				.name( "Name replaced" );

		assertThat( options( context, Example.class ) )
			.hasSize( 1 )
			.contains( new NameExampleOption( "Name replaced" ) );
	}

	@org.hibernate.ogm.test.mapping.annotation.NameExample("Batman")
	private static final class Example {
	}

}
