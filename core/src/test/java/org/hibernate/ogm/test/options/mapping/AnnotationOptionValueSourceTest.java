/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.options.container.impl.OptionsContainer;
import org.hibernate.ogm.options.navigation.source.impl.AnnotationOptionValueSource;
import org.hibernate.ogm.test.options.examples.EmbedExampleOption;
import org.hibernate.ogm.test.options.examples.NameExampleOption;
import org.hibernate.ogm.test.options.examples.annotations.EmbedExample;
import org.hibernate.ogm.test.options.examples.annotations.NameExample;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the retrieval of options specified via Java annotations.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class AnnotationOptionValueSourceTest {

	private AnnotationOptionValueSource source;

	@Before
	public void setupContext() {
		source = new AnnotationOptionValueSource();
	}

	@Test
	public void testAnnotatedEntity() throws Exception {
		OptionsContainer entityOptions = source.getEntityOptions( Example.class );
		assertThat( entityOptions.getUnique( NameExampleOption.class ) ).isEqualTo( "Batman" );
	}

	@Test
	public void testAnnotationGivenOnPropertyCanBeRetrievedFromOptionsContext() {
		OptionsContainer propertyOptions = source.getPropertyOptions( Example.class, "exampleProperty" );
		assertThat( propertyOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Test" );
	}

	@Test
	public void testAnnotationGivenOnBooleanPropertyCanBeRetrievedFromOptionsContext() {
		OptionsContainer propertyOptions = source.getPropertyOptions( Example.class, "helpful" );
		assertThat( propertyOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Another Test" );
	}

	@Test
	public void testAnnotationGivenOnPrivateFieldCanBeRetrievedFromOptionsContext() {
		OptionsContainer propertyOptions = source.getPropertyOptions( Example.class, "anotherProperty" );
		assertThat( propertyOptions.getUnique( EmbedExampleOption.class ) ).isEqualTo( "Yet Another Test" );
	}

	@NameExample( "Batman" )
	private static final class Example {

		@EmbedExample("Yet Another Test")
		private int anotherProperty;

		@EmbedExample("Test")
		public String getExampleProperty() {
			return null;
		}

		@EmbedExample("Another Test")
		public boolean isHelpful() {
			return false;
		}
	}
}
