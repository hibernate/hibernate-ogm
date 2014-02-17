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
package org.hibernate.ogm.datastore.ehcache.test.serialization;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableKey;
import org.hibernate.ogm.grid.RowKey;
import org.junit.Test;

/**
 * Test for the serialization of {@link SerializableKey}.
 *
 * @author Gunnar Morling
 */
public class KeySerializationTest {

	@Test
	public void shouldSerializeAndDeserializeRowKey() throws Exception {
		String[] columnNames = { "foo", "bar", "baz" };
		Object[] values = { 123, "Hello", 456L };

		// given
		SerializableKey key = new SerializableKey( new RowKey( "Foobar", columnNames, values ) );

		// when
		byte[] bytes = marshall( key );
		SerializableKey unmarshalledKey = unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getClass() ).isEqualTo( SerializableKey.class );
		assertThat( unmarshalledKey.getTable() ).isEqualTo( key.getTable() );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}

	private byte[] marshall(SerializableKey object) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );

		try {
			oos.writeObject( object );
		}
		finally {
			try {
				oos.close();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		return baos.toByteArray();
	}

	SerializableKey unmarshall(byte[] bytes) throws Exception {
		InputStream is = new ByteArrayInputStream( bytes );
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			SerializableKey object = (SerializableKey) ois.readObject();
			return object;
		}
		finally {
			try {
				ois.close();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}
}
