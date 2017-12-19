/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;

/**
 * Helper for externalizer tests. Uses the "River" serialization protocol.
 *
 * @author Gunnar Morling
 */
class ExternalizerTestHelper<T, E extends AdvancedExternalizer<T>> {

	private final MarshallerFactory marshallerFactory;
	private final E externalizer;

	private ExternalizerTestHelper(E externalizer) {
		marshallerFactory = Marshalling.getProvidedMarshallerFactory( "river" );
		this.externalizer = externalizer;
	}

	static <T, E extends AdvancedExternalizer<T>> ExternalizerTestHelper<T, E> getInstance(E externalizer) {
		return new ExternalizerTestHelper<T, E>( externalizer );
	}

	byte[] marshall(T object) throws Exception {
		Marshaller marshaller = getMarshaller();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try {
			marshaller.start( Marshalling.createByteOutput( baos ) );
			externalizer.writeObject( marshaller, object );
			marshaller.finish();
		}
		finally {
			try {
				baos.close();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}

		return baos.toByteArray();
	}

	T unmarshall(byte[] bytes) throws Exception {
		Unmarshaller unmarshaller = getUnmarshaller();
		InputStream is = new ByteArrayInputStream( bytes );
		try {
			unmarshaller.start( Marshalling.createByteInput( is ) );
			T object = externalizer.readObject( unmarshaller );
			unmarshaller.finish();

			return object;
		}
		finally {
			try {
				is.close();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}

	private Marshaller getMarshaller() throws IOException {
		MarshallingConfiguration configuration = new MarshallingConfiguration();
		configuration.setVersion( 3 );

		Marshaller marshaller = marshallerFactory.createMarshaller( configuration );
		return marshaller;
	}

	private Unmarshaller getUnmarshaller() throws IOException {
		MarshallingConfiguration configuration = new MarshallingConfiguration();
		return marshallerFactory.createUnmarshaller( configuration );
	}
}
