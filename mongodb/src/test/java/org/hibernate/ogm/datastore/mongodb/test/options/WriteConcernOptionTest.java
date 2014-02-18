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

import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.context.MongoDBGlobalContext;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.context.impl.ConfigurationContext;
import org.hibernate.ogm.options.context.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link WriteConcernOption} used to set the {@link WriteConcernType} in MongoDB.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernOptionTest {

	private WritableOptionsServiceContext optionsContext;
	private MongoDBGlobalContext mongoOptions;

	@Before
	public void setupBuilder() {
		optionsContext = new WritableOptionsServiceContext();
		mongoOptions = new MongoDB().getConfigurationBuilder( new ConfigurationContext( optionsContext ) );
	}

	@Test
	public void testWriteConcernMappingOption() throws Exception {
		mongoOptions
			.writeConcern( WriteConcernType.ERRORS_IGNORED );

		OptionsContainer options = optionsContext.getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.ERRORS_IGNORED );
	}

	@Test
	public void testWriteConcernedContextPriority() throws Exception {
		mongoOptions
			.writeConcern( WriteConcernType.ERRORS_IGNORED )
			.entity( ExampleForMongoDBMapping.class )
				.writeConcern( WriteConcernType.MAJORITY );

		OptionsContainer options = optionsContext.getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.ERRORS_IGNORED );

		options = optionsContext.getEntityOptions( ExampleForMongoDBMapping.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcernType.MAJORITY );
	}

	@SuppressWarnings("unused")
	private static final class ExampleForMongoDBMapping {
		String content;
	}
}
