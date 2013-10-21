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

import org.hibernate.ogm.datastore.mongodb.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.options.mongodb.WriteConcernOption;
import org.hibernate.ogm.options.mongodb.mapping.impl.MongoDBGlobalOptions;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContext;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernAnnotationTest {

	private OptionsContext optionsContext;
	private MongoDBGlobalOptions mongoOptions;

	@Before
	public void setupBuilder() {
		optionsContext = new OptionsContext();
		mongoOptions = new MongoDBDatastoreProvider().getConfigurationBuilder( new ConfigurationContext( optionsContext ) );
	}


	@Test
	public void testWriteConcernForEntity() throws Exception {
		mongoOptions.entity( EntityWriteConcernExample.class );

		assertThat( optionsContext.getGlobalOptions() ).isEmpty();

		assertThat( optionsContext.getEntityOptions( EntityWriteConcernExample.class ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcernType.ERRORS_IGNORED ) );
	}

	@Test
	public void testWriteConcernForField() throws Exception {
		mongoOptions
			.entity( FieldWriteConcernExample.class );

		assertThat( optionsContext.getGlobalOptions() ).isEmpty();

		assertThat( optionsContext.getPropertyOptions( FieldWriteConcernExample.class, "content" ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcernType.FSYNCED ) );
	}

	@Test
	public void testWriteConcernForMethod() throws Exception {
		mongoOptions
			.entity( MethodWriteConcernExample.class );

		assertThat( optionsContext.getGlobalOptions() ).isEmpty();

		assertThat( optionsContext.getPropertyOptions( MethodWriteConcernExample.class, "content" ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcernType.JOURNALED ) );
	}

	@Test
	public void testWriteConcernAnnotationPriorities() throws Exception {
		mongoOptions
			.entity( AnnotatedClass.class );

		assertThat( optionsContext.getGlobalOptions() ).isEmpty();

		assertThat( optionsContext.getEntityOptions( AnnotatedClass.class ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcernType.ACKNOWLEDGED ) );

		assertThat( optionsContext.getPropertyOptions( AnnotatedClass.class, "title") )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcernType.ERRORS_IGNORED ) );
	}

	@org.hibernate.ogm.options.mongodb.WriteConcern(WriteConcernType.ERRORS_IGNORED)
	private static final class EntityWriteConcernExample {
	}

	private static final class FieldWriteConcernExample {
		@org.hibernate.ogm.options.mongodb.WriteConcern(WriteConcernType.FSYNCED)
		public String content;
	}

	private static final class MethodWriteConcernExample {
		public String content;

		@org.hibernate.ogm.options.mongodb.WriteConcern(WriteConcernType.JOURNALED)
		public String getContent() {
			return content;
		}
	}

	@org.hibernate.ogm.options.mongodb.WriteConcern(WriteConcernType.ACKNOWLEDGED)
	private static final class AnnotatedClass {
		@org.hibernate.ogm.options.mongodb.WriteConcern(WriteConcernType.ERRORS_IGNORED)
		public String title;
	}

}
