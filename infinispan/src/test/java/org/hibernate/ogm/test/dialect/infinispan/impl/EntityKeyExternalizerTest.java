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
package org.hibernate.ogm.test.dialect.infinispan.impl;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.hibernate.ogm.dialect.infinispan.impl.EntityKeyExternalizer;
import org.hibernate.ogm.grid.EntityKey;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.grid.Key;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link EntityKeyExternalizer}.
 *
 * @author Gunnar Morling
 */
public class EntityKeyExternalizerTest {

	private ExternalizerTestHelper<EntityKey, EntityKeyExternalizer> externalizerHelper;

	@Before
	public void setupMarshallerFactory() {
		externalizerHelper = ExternalizerTestHelper.getInstance( EntityKeyExternalizer.INSTANCE );
	}

	@Test
	public void shouldSerializeAndDeserializeEntityKey() throws Exception {
		String[] columnNames = { "foo", "bar", "baz" };
		EntityKeyMetadata keyMetadata = new EntityKeyMetadata( "Foobar", columnNames );
		Object[] values = { 123, "Hello", 456L };

		// given
		EntityKey key = new EntityKey( keyMetadata, values );

		// when
		byte[] bytes = externalizerHelper.marshall( key );
		Key unmarshalledKey = externalizerHelper.unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getClass() ).isEqualTo( EntityKey.class );
		assertThat( unmarshalledKey.getTable() ).isEqualTo( key.getTable() );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}
}
