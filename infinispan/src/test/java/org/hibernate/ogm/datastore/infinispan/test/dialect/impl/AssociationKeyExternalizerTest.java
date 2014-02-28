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
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.datastore.infinispan.dialect.impl.AssociationKeyExternalizer;
import org.hibernate.ogm.grid.AssociationKey;
import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.grid.Key;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link AssociationKeyExternalizer}.
 *
 * @author Gunnar Morling
 */
public class AssociationKeyExternalizerTest {

	private ExternalizerTestHelper<AssociationKey, AssociationKeyExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( AssociationKeyExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeAssociationKey() throws Exception {
		String[] columnNames = { "foo", "bar", "baz" };
		AssociationKeyMetadata keyMetadata = new AssociationKeyMetadata( "Foobar", columnNames, null );
		Object[] values = { 123, "Hello", 456L };

		// given
		AssociationKey key = new AssociationKey( keyMetadata, values, null, null, null );

		// when
		byte[] bytes = externalizerHelper.marshall( key );
		Key unmarshalledKey = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getClass() ).isEqualTo( AssociationKey.class );
		assertThat( unmarshalledKey.getTable() ).isEqualTo( key.getTable() );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}
}
