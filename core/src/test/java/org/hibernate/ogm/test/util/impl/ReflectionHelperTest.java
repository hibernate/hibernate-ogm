/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
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
