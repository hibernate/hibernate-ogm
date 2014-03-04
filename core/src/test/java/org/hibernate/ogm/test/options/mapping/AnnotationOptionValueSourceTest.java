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
package org.hibernate.ogm.test.options.mapping;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.options.navigation.impl.OptionsContainer;
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
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
