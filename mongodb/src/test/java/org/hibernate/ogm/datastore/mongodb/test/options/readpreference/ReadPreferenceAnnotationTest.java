/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.datastore.mongodb.test.options.readpreference;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.datastore.mongodb.options.ReadPreference;
import org.hibernate.ogm.datastore.mongodb.options.ReadPreferenceType;
import org.hibernate.ogm.datastore.mongodb.options.impl.ReadPreferenceOption;
import org.hibernate.ogm.options.navigation.impl.WritableOptionsServiceContext;
import org.hibernate.ogm.options.spi.OptionsContainer;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for setting the {@link ReadPreferenceOption} via annotations.
 *
 * @author Gunnar Morling
 */
public class ReadPreferenceAnnotationTest {

	private WritableOptionsServiceContext optionsContext;

	@Before
	public void setupBuilder() {
		optionsContext = new WritableOptionsServiceContext();
	}

	@Test
	public void shouldObtainReadPreferenceOptionFromAnnotation() throws Exception {
		OptionsContainer options = optionsContext.getEntityOptions( MyEntity.class );
		assertThat( options.getUnique( ReadPreferenceOption.class ) ).isEqualTo( com.mongodb.ReadPreference.secondaryPreferred() );
	}

	@ReadPreference(ReadPreferenceType.SECONDARY_PREFERRED)
	private static final class MyEntity {
	}
}
