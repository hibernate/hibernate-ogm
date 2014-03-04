/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.ReadPreference;

/**
 * Test for setting the {@link ReadPreferenceOption} programmatically.
 *
 * @author Gunnar Morling
 */
public class ReadPreferenceConfiguredProgrammaticallyTest {

	private ProgrammaticOptionValueSource source;
	private MongoDBGlobalContext mongoOptions;

	@Before
	public void setupBuilder() {
		source = new ProgrammaticOptionValueSource();
		mongoOptions = new MongoDB().getConfigurationBuilder( new ConfigurationContext( source ) );
	}

	@Test
	public void testReadPreferenceGivenOnGlobalLevel() throws Exception {
		mongoOptions.readPreference( ReadPreferenceType.SECONDARY );

		OptionsContainer options = source.getGlobalOptions();
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( ReadPreference.secondary() );
	}

	@Test
	public void testReadPreferenceGivenOnEntityLevel() throws Exception {
		mongoOptions
			.entity( MyEntity.class )
				.readPreference( ReadPreferenceType.SECONDARY_PREFERRED );

		OptionsContainer options = source.getEntityOptions( MyEntity.class );
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( ReadPreference.secondaryPreferred() );
	}

	@Test
	public void testReadPreferenceGivenOnPropertyLevel() throws Exception {
		mongoOptions
			.entity( MyEntity.class )
				.property( "content", ElementType.FIELD )
				.readPreference( ReadPreferenceType.NEAREST );

		OptionsContainer options = source.getPropertyOptions( MyEntity.class, "content" );
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( ReadPreference.nearest() );
	}

	@SuppressWarnings("unused")
	private static final class MyEntity {
		String content;
	}
}
