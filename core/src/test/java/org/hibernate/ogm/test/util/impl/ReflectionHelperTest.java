/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.util.impl;

import java.lang.annotation.ElementType;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.hibernate.ogm.util.impl.ReflectionHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Unit test for {@link ReflectionHelper}.
 *
 * @author Gunnar Morling
 */
public class ReflectionHelperTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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

	@Test
	public void introspectShouldReturnFieldValues() throws Exception {
		Subject subject = new Subject();
		subject.setField( "value" );
		Map<String, Object> res = ReflectionHelper.introspect( subject );
		assertThat( res ).isEqualTo( Collections.singletonMap( "field", "value" ) );
	}

	@Test
	public void setField() throws Exception {
		Subject subject = new Subject();
		ReflectionHelper.setField( subject, "field", "value" );
		assertThat( subject.getField() ).isEqualTo( "value" );
	}

	@Test
	public void setNonExistingFields() throws Exception {
		thrown.expect( NoSuchMethodException.class );
		ReflectionHelper.setField( new Subject(), "nonExistingField", "value" );
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

	public static class Subject {

		private String field;

		public Subject() {
		}

		public Subject(String field) {
			this.field = field;
		}

		public String getField() {
			return field;
		}

		public void setField(String field) {
			this.field = field;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Subject subject = (Subject) o;
			return Objects.equals( field, subject.field );
		}

		@Override
		public int hashCode() {
			return Objects.hash( field );
		}
	}
}
