/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013-2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.test.options;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.options.context.impl.ConfigurationContext;
import org.hibernate.ogm.options.context.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.junit.Test;

/**
 * Test the {@link AssociationStorageOption} used to set the {@link AssociationStorageType} in MongoDB.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AssociationStorageOptionTest {

	@Test
	public void testAssociationStorageMappingOption() throws Exception {
		WritableOptionsServiceContext optionsContext = new WritableOptionsServiceContext();
		ConfigurationContext context = new ConfigurationContext( optionsContext );

		new MongoDB().getConfigurationBuilder( context )
			.entity( ExampleForMongoDBMapping.class )
				.property( "content", ElementType.FIELD )
					.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		OptionsContainer options = optionsContext.getPropertyOptions( ExampleForMongoDBMapping.class, "content" );
		assertThat( options.getUnique( AssociationStorageOption.class ) ).isEqualTo( AssociationStorageType.ASSOCIATION_DOCUMENT );
	}

	@SuppressWarnings("unused")
	private static final class ExampleForMongoDBMapping {
		String content;
	}
}
