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

import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.options.mongodb.AssociationStorageOption;
import org.hibernate.ogm.options.mongodb.mapping.impl.MongoDBMappingServiceFactory;
import org.hibernate.ogm.options.mongodb.mapping.spi.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AssociationStorageAnnotationTest {

	@Test
	public void testAssociationStorageMappingOption() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = new MappingContext();
		MongoDBGlobalContext mapping = factory.createMapping( context );
		mapping
			.entity( EntityAnnotated.class );

		assertThat( options( context, EntityAnnotated.class, "content" ) )
			.hasSize( 1 )
			.contains( new AssociationStorageOption( AssociationStorage.IN_ENTITY ) );
	}

	private static final class EntityAnnotated {
		@org.hibernate.ogm.options.mongodb.AssociationStorage("IN_ENTITY")
		public String content;
	}

}
