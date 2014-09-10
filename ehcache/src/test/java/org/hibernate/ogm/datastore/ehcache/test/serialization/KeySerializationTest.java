/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

import org.hibernate.ogm.datastore.ehcache.dialect.impl.SerializableRowKey;
import org.hibernate.ogm.model.key.spi.RowKey;
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
		SerializableRowKey key = new SerializableRowKey( new RowKey( columnNames, values ) );

		// when
		byte[] bytes = marshall( key );
		SerializableRowKey unmarshalledKey = unmarshall( bytes );

		// then
		assertThat( unmarshalledKey.getClass() ).isEqualTo( SerializableRowKey.class );
		assertThat( unmarshalledKey.getColumnNames() ).isEqualTo( key.getColumnNames() );
		assertThat( unmarshalledKey.getColumnValues() ).isEqualTo( key.getColumnValues() );

		assertTrue( key.equals( unmarshalledKey ) );
		assertTrue( unmarshalledKey.equals( key ) );
		assertThat( unmarshalledKey.hashCode() ).isEqualTo( key.hashCode() );
	}

	private byte[] marshall(SerializableRowKey object) throws Exception {
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

	SerializableRowKey unmarshall(byte[] bytes) throws Exception {
		InputStream is = new ByteArrayInputStream( bytes );
		ObjectInputStream ois = new ObjectInputStream( is );
		try {
			SerializableRowKey object = (SerializableRowKey) ois.readObject();
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
