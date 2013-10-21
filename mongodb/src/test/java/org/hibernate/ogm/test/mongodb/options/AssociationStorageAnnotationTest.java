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

import org.hibernate.ogm.datastore.mongodb.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.impl.MongoDBDatastoreProvider;
import org.hibernate.ogm.options.mongodb.AssociationStorage;
import org.hibernate.ogm.options.mongodb.AssociationStorageOption;
import org.hibernate.ogm.options.mongodb.mapping.spi.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link AssociationStorage} annotation used to set the {@link AssociationStorageType} in MongoDB.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AssociationStorageAnnotationTest {

	private OptionsContext optionsContext;
	private ConfigurationContext context;

	@Before
	public void setupContexts() {
		optionsContext = new OptionsContext();
		context = new ConfigurationContext( optionsContext );
	}

	@Test
	public void testAssociationStorageMappingOptionOnField() throws Exception {
		MongoDBGlobalContext mapping = new MongoDBDatastoreProvider().getConfigurationBuilder( context );
		mapping
			.entity( EntityAnnotatedOnField.class );

		assertThat( optionsContext.getPropertyOptions( EntityAnnotatedOnField.class, "field" ) )
			.hasSize( 1 )
			.contains( new AssociationStorageOption( AssociationStorageType.IN_ENTITY ) );
	}

	@Test
	public void testAssociationStorageMappingOptionOnMethod() throws Exception {
		MongoDBGlobalContext mapping = new MongoDBDatastoreProvider().getConfigurationBuilder( context );
		mapping
			.entity( EntityAnnotatedOnMethod.class );

		assertThat( optionsContext.getPropertyOptions( EntityAnnotatedOnMethod.class, "method" ) )
			.hasSize( 1 )
			.contains( new AssociationStorageOption( AssociationStorageType.GLOBAL_COLLECTION ) );
	}

	private static final class EntityAnnotatedOnField {

		@AssociationStorage(AssociationStorageType.IN_ENTITY)
		public String field;

	}

	private static final class EntityAnnotatedOnMethod {

		public String method;

		@AssociationStorage(AssociationStorageType.GLOBAL_COLLECTION)
		public String getMethod() {
			return method;
		}

	}
}
