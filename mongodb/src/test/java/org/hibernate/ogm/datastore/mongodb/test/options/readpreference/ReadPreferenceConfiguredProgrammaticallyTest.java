/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.datastore.mongodb.options.navigation.MongoDBGlobalContext;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContextImpl;
import org.hibernate.ogm.options.navigation.source.impl.OptionValueSource;
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

	private MongoDBGlobalContext mongoOptions;
	private AppendableConfigurationContext context;

	@Before
	public void setupBuilder() {
		context = new AppendableConfigurationContext();
		mongoOptions = new MongoDB().getConfigurationBuilder( new ConfigurationContextImpl( context ) );
	}

	@Test
	public void testReadPreferenceGivenOnGlobalLevel() throws Exception {
		mongoOptions.readPreference( ReadPreferenceType.SECONDARY );

		OptionsContainer options = getSource().getGlobalOptions();
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( ReadPreference.secondary() );
	}

	@Test
	public void testReadPreferenceGivenOnEntityLevel() throws Exception {
		mongoOptions
			.entity( MyEntity.class )
				.readPreference( ReadPreferenceType.SECONDARY_PREFERRED );

		OptionsContainer options = getSource().getEntityOptions( MyEntity.class );
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( ReadPreference.secondaryPreferred() );
	}

	@Test
	public void testReadPreferenceGivenOnPropertyLevel() throws Exception {
		mongoOptions
			.entity( MyEntity.class )
				.property( "content", ElementType.FIELD )
				.readPreference( ReadPreferenceType.NEAREST );

		OptionsContainer options = getSource().getPropertyOptions( MyEntity.class, "content" );
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( ReadPreference.nearest() );
	}

	private OptionValueSource getSource() {
		return new ProgrammaticOptionValueSource( context );
	}

	@SuppressWarnings("unused")
	private static final class MyEntity {
		String content;
	}
}
