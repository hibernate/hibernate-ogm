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

	static <T, E extends AdvancedExternalizer<T>> ExternalizerTestHelper<T, E> getInstance( E externalizer ) {
		return new ExternalizerTestHelper<T, E>(externalizer);
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
