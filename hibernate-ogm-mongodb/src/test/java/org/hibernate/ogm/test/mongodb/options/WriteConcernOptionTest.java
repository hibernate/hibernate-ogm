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
import org.hibernate.ogm.options.mongodb.mapping.impl.MongoDBMappingServiceFactory;
import org.hibernate.ogm.options.mongodb.mapping.spi.MongoDBGlobalContext;
import org.hibernate.ogm.options.navigation.impl.MappingContext;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernOptionTest {

	@Test
	public void testGetter() throws Exception {
		WriteConcernOption option = new WriteConcernOption( WriteConcern.REPLICA_ACKNOWLEDGED );
		assertThat( option.getWriteConcern() ).isEqualTo( WriteConcern.REPLICA_ACKNOWLEDGED );
	}

	@Test
	public void testWriteConcernMappingOption() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = new MappingContext();
		MongoDBGlobalContext mapping = factory.createMapping( context );
		mapping.writeConcern( WriteConcern.ERRORS_IGNORED );

		assertThat( options( context ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.ERRORS_IGNORED ) );
	}

	@Test
	public void testWriteConcernedContextPriority() throws Exception {
		MongoDBMappingServiceFactory factory = new MongoDBMappingServiceFactory();
		MappingContext context = new MappingContext();
		MongoDBGlobalContext mapping = factory.createMapping( context );
		mapping
			.writeConcern( WriteConcern.ERRORS_IGNORED )
			.entity( ExampleForMongoDBMapping.class )
				.writeConcern( WriteConcern.MAJORITY );

		assertThat( options( context ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.ERRORS_IGNORED) );

		assertThat( options( context, ExampleForMongoDBMapping.class ) )
			.hasSize( 1 )
			.contains( new WriteConcernOption( WriteConcern.MAJORITY ) );
	}

	@SuppressWarnings("unused")
	private static final class ExampleForMongoDBMapping {
		String content;
	}

}
