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
package org.hibernate.ogm.test.descriptor;

import static org.fest.assertions.Assertions.assertThat;

import org.hibernate.ogm.hibernatecore.impl.OgmSessionFactory;
import org.hibernate.ogm.type.StringDateTypeDescriptor;
import org.junit.Test;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class StringDateTypeDescriptorTest {

	@Test
	public void testDescriptorName() throws Exception {
		assertThat( StringDateTypeDescriptor.INSTANCE.getName() ).as( StringDateTypeDescriptor.class.getSimpleName() )
				.isEqualTo( "string_date" );
	}

	@Test
	public void testColumnSpanForNull() throws Exception {
		assertThat( StringDateTypeDescriptor.INSTANCE.getColumnSpan( null ) ).as( "Column span for null" )
				.isEqualTo( 1 );
	}

	@Test
	public void testColumnSpanForOgmSessionFactory() throws Exception {
		assertThat( StringDateTypeDescriptor.INSTANCE.getColumnSpan( new OgmSessionFactory( null ) ) )
				.as( "Column span" ).isEqualTo( 1 );
	}
}
