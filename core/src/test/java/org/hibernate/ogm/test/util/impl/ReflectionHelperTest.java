/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.util.impl;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;

import org.hibernate.ogm.util.impl.ReflectionHelper;
import org.junit.Test;

/**
 * Unit test for {@link ReflectionHelper}.
 *
 * @author Gunnar Morling
 */
public class ReflectionHelperTest {

	@Test
	public void propertyExistsShouldConsiderField() {
		assertThat( ReflectionHelper.propertyExists( Giraffe.class, "size", ElementType.FIELD ) ).isTrue();
		assertThat( ReflectionHelper.propertyExists( Giraffe.class, "size", ElementType.METHOD ) ).isFalse();
	}

	@Test
	public void propertyExistsShouldConsiderGetter() {
		assertThat( ReflectionHelper.propertyExists( Giraffe.class, "name", ElementType.METHOD ) ).isTrue();
		assertThat( ReflectionHelper.propertyExists( Giraffe.class, "name", ElementType.FIELD ) ).isFalse();
	}

	@Test
	public void propertyExistsShouldConsiderBooleanGetter() {
		assertThat( ReflectionHelper.propertyExists( Giraffe.class, "grownUp", ElementType.METHOD ) ).isTrue();
		assertThat( ReflectionHelper.propertyExists( Giraffe.class, "grownUp", ElementType.FIELD ) ).isFalse();
	}

	@Test
	public void getPropertyNameShouldReturnNameForGetter() throws Exception {
		assertThat( ReflectionHelper.getPropertyName( Giraffe.class.getMethod( "getName" ) ) ).isEqualTo( "name" );
	}

	@Test
	public void getPropertyNameShouldReturnNameForBooleanGetter() throws Exception {
		assertThat( ReflectionHelper.getPropertyName( Giraffe.class.getMethod( "isGrownUp" ) ) ).isEqualTo( "grownUp" );
	}

	@Test
	public void getPropertyNameShouldReturnNullForNonGetterMethod() throws Exception {
		assertThat( ReflectionHelper.getPropertyName( Giraffe.class.getMethod( "setName", String.class ) ) ).isNull();
	}

	public static class Giraffe {

		@SuppressWarnings("unused")
		private int size;

		public String getName() {
			return null;
		}

		public void setName(String name) {
		}

		public boolean isGrownUp() {
			return false;
		}
	}
}
