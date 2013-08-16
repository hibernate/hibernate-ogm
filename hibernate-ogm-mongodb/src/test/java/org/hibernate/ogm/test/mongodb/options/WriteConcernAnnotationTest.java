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
package org.hibernate.ogm.test.mongodb.options;

import static org.fest.assertions.Assertions.assertThat;
import static org.hibernate.ogm.test.utils.OptionContainerHelper.options;

import org.hibernate.ogm.options.mongodb.WriteConcernOption;
import org.hibernate.ogm.options.mongodb.mapping.MongoDBMappingServiceFactory;
import org.hibernate.ogm.options.mongodb.mapping.MongoDBMapping.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationProxyFactory;
import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernAnnotationTest {

	@Test
	public void testWriteConcernForEntity() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = factory.createMappingContext();
		MongoDBGlobalContext mapping = ConfigurationProxyFactory.get( factory.getMappingType(), context );
		mapping.entity( EntityWriteConcernExample.class );

		assertThat( options( context ) )
			.isEmpty();

		assertThat( options( context, EntityWriteConcernExample.class ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.NONE ) );
	}

	@Test
	public void testWriteConcernForField() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = factory.createMappingContext();
		MongoDBGlobalContext mapping = ConfigurationProxyFactory.get( factory.getMappingType(), context );
		mapping
			.entity( FieldWriteConcernExample.class );

		assertThat( options( context ) )
			.isEmpty();

		assertThat( options( context, FieldWriteConcernExample.class, "content" ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.FSYNCED ) );
	}

	@Test
	public void testWriteConcernAnnotationPriorities() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = factory.createMappingContext();
		MongoDBGlobalContext mapping = ConfigurationProxyFactory.get( factory.getMappingType(), context );
		mapping
			.entity( AnnotatedClass.class );

		assertThat( options( context ) )
			.isEmpty();

		assertThat( options( context, AnnotatedClass.class ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.REPLICA_ACKNOWLEDGED ) );

		assertThat( options( context, AnnotatedClass.class, "title" ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.NONE ) );
	}

	@org.hibernate.ogm.options.mongodb.WriteConcern("NONE")
	private static final class EntityWriteConcernExample {
	}

	private static final class FieldWriteConcernExample {
		@org.hibernate.ogm.options.mongodb.WriteConcern("FSYNCED")
		public String content;
	}

	@org.hibernate.ogm.options.mongodb.WriteConcern("REPLICA_ACKNOWLEDGED")
	private static final class AnnotatedClass {
		@org.hibernate.ogm.options.mongodb.WriteConcern("NONE")
		public String title;
	}

}
