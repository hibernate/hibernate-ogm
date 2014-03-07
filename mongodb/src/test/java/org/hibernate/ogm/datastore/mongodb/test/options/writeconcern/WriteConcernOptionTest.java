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
package org.hibernate.ogm.datastore.mongodb.test.options.writeconcern;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * Test the {@link WriteConcernOption} used to set the {@link WriteConcernType} in MongoDB.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class WriteConcernOptionTest {

	private MongoDBGlobalContext mongoOptions;
	private AppendableConfigurationContext context;

	@Before
	public void setupBuilder() {
		context = new AppendableConfigurationContext();
		mongoOptions = new MongoDB().getConfigurationBuilder( new ConfigurationContext( context ) );
	}

	@Test
	public void testWriteConcernGivenByTypeOnGlobalLevel() throws Exception {
		mongoOptions
			.writeConcern( WriteConcernType.ERRORS_IGNORED );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcern.ERRORS_IGNORED );
	}

	@Test
	public void testWriteConcernGivenByInstanceOnGlobalLevel() throws Exception {
		ReplicaConfigurableWriteConcern writeConcern = new ReplicaConfigurableWriteConcern( 5 );

		mongoOptions.writeConcern( writeConcern );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( writeConcern );
	}

	@Test
	public void testWriteConcernGivenByTypePriority() throws Exception {
		mongoOptions
			.writeConcern( WriteConcernType.ERRORS_IGNORED )
			.entity( ExampleForMongoDBMapping.class )
				.writeConcern( WriteConcernType.MAJORITY )
				.property( "content", ElementType.FIELD )
					.writeConcern( WriteConcernType.FSYNCED );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcern.ERRORS_IGNORED );

		options = getSource().getEntityOptions( ExampleForMongoDBMapping.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcern.MAJORITY );

		options = getSource().getPropertyOptions( ExampleForMongoDBMapping.class, "content" );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( WriteConcern.FSYNCED );
	}

	@Test
	public void testWriteConcernGivenByInstancePriority() throws Exception {
		mongoOptions
			.writeConcern( new ReplicaConfigurableWriteConcern( 2 ) )
			.entity( ExampleForMongoDBMapping.class )
				.writeConcern( new ReplicaConfigurableWriteConcern( 3 ) )
				.property( "content", ElementType.FIELD )
					.writeConcern( new ReplicaConfigurableWriteConcern( 4 ) );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new ReplicaConfigurableWriteConcern( 2 ) );

		options = getSource().getEntityOptions( ExampleForMongoDBMapping.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new ReplicaConfigurableWriteConcern( 3 ) );

		options = getSource().getPropertyOptions( ExampleForMongoDBMapping.class, "content" );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new ReplicaConfigurableWriteConcern( 4 ) );
	}

	@Test
	public void testWriteConcernGivenByInstanceTakesPrecedenceOverType() throws Exception {
		mongoOptions
			.writeConcern( WriteConcernType.ACKNOWLEDGED )
			.writeConcern( new ReplicaConfigurableWriteConcern( 2 ) )
			.entity( ExampleForMongoDBMapping.class )
				.writeConcern( WriteConcernType.ACKNOWLEDGED )
				.writeConcern( new ReplicaConfigurableWriteConcern( 3 ) )
				.property( "content", ElementType.FIELD )
					.writeConcern( WriteConcernType.ACKNOWLEDGED )
					.writeConcern( new ReplicaConfigurableWriteConcern( 4 ) );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new ReplicaConfigurableWriteConcern( 2 ) );

		options = getSource().getEntityOptions( ExampleForMongoDBMapping.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new ReplicaConfigurableWriteConcern( 3 ) );

		options = getSource().getPropertyOptions( ExampleForMongoDBMapping.class, "content" );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new ReplicaConfigurableWriteConcern( 4 ) );
	}

	private OptionValueSource getSource() {
		return new ProgrammaticOptionValueSource( context );
	}

	@SuppressWarnings("unused")
	private static final class ExampleForMongoDBMapping {
		String content;
	}

	/**
	 * A write concern which allows to specify the number of replicas which need to acknowledge a write.
	 */
	@SuppressWarnings("serial")
	private static class ReplicaConfigurableWriteConcern extends WriteConcern {

		public ReplicaConfigurableWriteConcern(int numberOfRequiredReplicas) {
			super( numberOfRequiredReplicas, 0, false, true, false );
		}
	}
}
