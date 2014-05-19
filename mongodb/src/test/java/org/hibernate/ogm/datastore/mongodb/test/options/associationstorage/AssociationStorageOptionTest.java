/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.associationstorage;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.impl.AssociationStorageOption;
import org.hibernate.ogm.datastore.mongodb.MongoDB;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.impl.AppendableConfigurationContext;
import org.hibernate.ogm.options.navigation.impl.ConfigurationContext;
import org.hibernate.ogm.options.navigation.source.impl.ProgrammaticOptionValueSource;
import org.junit.Test;

/**
 * Test the {@link AssociationStorageOption} used to set the {@link AssociationStorageType} in MongoDB.
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AssociationStorageOptionTest {

	@Test
	public void testAssociationStorageMappingOption() throws Exception {
		AppendableConfigurationContext context = new AppendableConfigurationContext();
		ConfigurationContext configurationContext = new ConfigurationContext( context );

		new MongoDB().getConfigurationBuilder( configurationContext )
			.entity( ExampleForMongoDBMapping.class )
				.property( "content", ElementType.FIELD )
					.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		OptionsContainer options = new ProgrammaticOptionValueSource( context ).getPropertyOptions( ExampleForMongoDBMapping.class, "content" );
		assertThat( options.getUnique( AssociationStorageOption.class ) ).isEqualTo( AssociationStorageType.ASSOCIATION_DOCUMENT );
	}

	@SuppressWarnings("unused")
	private static final class ExampleForMongoDBMapping {
		String content;
	}
}
