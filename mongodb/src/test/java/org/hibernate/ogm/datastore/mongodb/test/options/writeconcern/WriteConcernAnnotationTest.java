/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.writeconcern;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.mongodb.options.WriteConcern;
import org.hibernate.ogm.datastore.mongodb.options.WriteConcernType;
import org.hibernate.ogm.datastore.mongodb.options.impl.WriteConcernOption;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class WriteConcernAnnotationTest {

	private AnnotationOptionValueSource source;

	@Before
	public void setupBuilder() {
		source = new AnnotationOptionValueSource();
	}

	@Test
	public void testWriteConcernForEntity() throws Exception {
		OptionsContainer options = source.getEntityOptions( EntityWriteConcernExample.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( com.mongodb.WriteConcern.ERRORS_IGNORED );
	}

	@Test
	public void testWriteConcernByTypeForEntity() throws Exception {
		OptionsContainer options = source.getEntityOptions( EntityWriteConcernUsingTypeExample.class );
		assertThat( options.getUnique( WriteConcernOption.class ) ).isEqualTo( new MultipleDataCenters() );
	}

	@WriteConcern(WriteConcernType.ERRORS_IGNORED)
	private static final class EntityWriteConcernExample {
	}

	@WriteConcern(value = WriteConcernType.CUSTOM, type = MultipleDataCenters.class)
	private static final class EntityWriteConcernUsingTypeExample {
	}

	@SuppressWarnings("serial")
	public static class MultipleDataCenters extends com.mongodb.WriteConcern {

		public MultipleDataCenters() {
			super( "MultipleDataCenters", 0, false, true, false );
		}
	}
}
