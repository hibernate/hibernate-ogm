/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.mongodb.options.ReadPreference;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for setting the {@link ReadPreferenceOption} via annotations.
 *
 * @author Gunnar Morling
 */
public class ReadPreferenceAnnotationTest {

	private AnnotationOptionValueSource source;

	@Before
	public void setupBuilder() {
		source = new AnnotationOptionValueSource();
	}

	@Test
	public void shouldObtainReadPreferenceOptionFromAnnotation() throws Exception {
		OptionsContainer options = source.getEntityOptions( MyEntity.class );
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( com.mongodb.ReadPreference.secondaryPreferred() );
	}

	@ReadPreference(ReadPreferenceType.SECONDARY_PREFERRED)
	private static final class MyEntity {
	}
}
