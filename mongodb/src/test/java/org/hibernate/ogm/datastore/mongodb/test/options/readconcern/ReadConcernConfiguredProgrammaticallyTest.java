/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readconcern;

import com.mongodb.ReadConcern;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.options.ReadConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadConcernOption;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.ElementType;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for setting the {@link ReadConcernOption} programmatically.
 *
 * @author Aleksandr Mylnikov
 */
public class ReadConcernConfiguredProgrammaticallyTest {

	private MongoDBGlobalContext mongoOptions;
	private AppendableConfigurationContext context;

	@Before
	public void setupBuilder() {
		context = new AppendableConfigurationContext();
		mongoOptions = new MongoDB().getConfigurationBuilder( new ConfigurationContextImpl( context ) );
	}

	@Test
	public void testReadConcernGivenOnGlobalLevel() throws Exception {
		mongoOptions.readConcern( ReadConcernType.LOCAL );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( ReadConcernOption.class ) ).isEqualTo( ReadConcern.LOCAL );
	}

	@Test
	public void testReadConcernGivenOnEntityLevel() throws Exception {
		mongoOptions
				.entity( MyEntity.class )
				.readConcern( ReadConcernType.MAJORITY );

		OptionsContainer options = getSource().getEntityOptions( MyEntity.class );
		assertThat( options.getUnique( ReadConcernOption.class ) ).isEqualTo( ReadConcern.MAJORITY );
	}

	@Test
	public void testReadConcernGivenOnPropertyLevel() throws Exception {
		mongoOptions
				.entity( MyEntity.class )
				.property( "content", ElementType.FIELD )
				.readConcern( ReadConcernType.LINEARIZABLE );

		OptionsContainer options = getSource().getPropertyOptions( MyEntity.class, "content" );
		assertThat( options.getUnique( ReadConcernOption.class ) ).isEqualTo( ReadConcern.LINEARIZABLE );
	}

	private OptionValueSource getSource() {
		return new ProgrammaticOptionValueSource( context );
	}

	@SuppressWarnings("unused")
	private static final class MyEntity {
		String content;
	}

}
