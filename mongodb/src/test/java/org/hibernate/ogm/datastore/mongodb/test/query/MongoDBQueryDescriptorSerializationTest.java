/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query;

import static org.fest.assertions.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor;
import org.hibernate.ogm.datastore.mongodb.query.impl.MongoDBQueryDescriptor.Operation;
import org.junit.Test;

import com.mongodb.BasicDBObject;

/**
 * Tests the serialization and de-serialization of {@link MongoDBQueryDescriptor}.
 *
 * @author Gunnar Morling
 */
public class MongoDBQueryDescriptorSerializationTest {

	@Test
	public void canSerializeAndDeserialize() throws Exception {
		MongoDBQueryDescriptor descriptor = new MongoDBQueryDescriptor(
				"test",
				Operation.FIND,
				new BasicDBObject( "foo", "bar" ),
				new BasicDBObject( "foo", 1 ),
				new BasicDBObject( "bar", 1 )
		);

		byte[] bytes = serialize( descriptor );
		MongoDBQueryDescriptor deserializedDescriptor = deserialize( bytes );

		assertThat( deserializedDescriptor.getCollectionName() ).isEqualTo( descriptor.getCollectionName() );
		assertThat( deserializedDescriptor.getOperation() ).isEqualTo( descriptor.getOperation() );
		assertThat( deserializedDescriptor.getCriteria() ).isEqualTo( descriptor.getCriteria() );
		assertThat( deserializedDescriptor.getProjection() ).isEqualTo( descriptor.getProjection() );
		assertThat( deserializedDescriptor.getOrderBy() ).isEqualTo( descriptor.getOrderBy() );
	}

	private byte[] serialize(MongoDBQueryDescriptor descriptor) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream( outputStream );
		objectOutputStream.writeObject( descriptor );
		objectOutputStream.close();
		return outputStream.toByteArray();
	}

	private MongoDBQueryDescriptor deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		ObjectInputStream inputStream = new ObjectInputStream( new ByteArrayInputStream( bytes ) );
		MongoDBQueryDescriptor deserializedDescriptor = (MongoDBQueryDescriptor) inputStream.readObject();
		return deserializedDescriptor;
	}
}
