/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.options.associationstorage;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.document.options.AssociationStorage;
import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.document.options.spi.AssociationStorageOption;
import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link AssociationStorage} annotation used to set the {@link AssociationStorageType} in MongoDB.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class AssociationStorageAnnotationTest {

	private AnnotationOptionValueSource source;

	@Before
	public void setupContexts() {
		source = new AnnotationOptionValueSource();
	}

	@Test
	public void testAssociationStorageMappingOptionOnField() throws Exception {
		OptionsContainer fieldOptions = source.getPropertyOptions( EntityAnnotatedOnField.class, "field" );
		assertThat( fieldOptions.getUnique( AssociationStorageOption.class ) ).isEqualTo( AssociationStorageType.IN_ENTITY );
	}

	@Test
	public void testAssociationStorageMappingOptionOnMethod() throws Exception {
		OptionsContainer methodOptions = source.getPropertyOptions( EntityAnnotatedOnMethod.class, "method" );
		assertThat( methodOptions.getUnique( AssociationStorageOption.class ) ).isEqualTo( AssociationStorageType.ASSOCIATION_DOCUMENT );
	}

	private static final class EntityAnnotatedOnField {

		@AssociationStorage(AssociationStorageType.IN_ENTITY)
		public String field;

	}

	private static final class EntityAnnotatedOnMethod {

		public String method;

		@AssociationStorage(AssociationStorageType.ASSOCIATION_DOCUMENT)
		public String getMethod() {
			return method;
		}

	}
}
