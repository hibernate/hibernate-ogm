/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.ogm.options.navigation.impl.ConfigurationContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.WriteConcern;

/**
 * Test the {@link WriteConcernOption} used to set the {@link WriteConcernType} in MongoDB.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class WriteConcernOptionTest {

	private MongoDBGlobalContext mongoOptions;
	private AppendableConfigurationContext context;

	@Before
	public void setupBuilder() {
		context = new AppendableConfigurationContext();
		mongoOptions = new MongoDB().getConfigurationBuilder( new ConfigurationContextImpl( context ) );
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
