/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.readconcern;

import com.mongodb.ReadConcern;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadConcernOption;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for setting the {@link ReadConcernOption} via annotations.
 *
 * @author Aleksandr Mylnikov
 */
public class ReadConcernAnnotationTest {

	private AnnotationOptionValueSource source;

	@Before
	public void setupBuilder() {
		source = new AnnotationOptionValueSource();
	}

	@Test
	public void shouldObtainReadConcernOptionFromAnnotation() {
		OptionsContainer options = source.getEntityOptions( GolfPlayer.class );
		assertThat( options.getUnique( ReadConcernOption.class ) ).isEqualTo( ReadConcern.MAJORITY );
	}

}
