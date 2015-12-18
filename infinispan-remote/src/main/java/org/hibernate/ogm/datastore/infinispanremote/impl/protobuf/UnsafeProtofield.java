/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.io.IOException;
import java.util.Objects;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamMappedField;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.ProtobufFieldExporter;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.ProtobufTypeExporter;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamReader;
import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

/**
 * Catching all IOException cases makes usage of lambdas inconvenient.
 * Wrap all ProtofieldWriter instances with this for convenience.
 */
final class UnsafeProtofield<T> implements ProtobufFieldExporter, ProtobufTypeExporter, ProtostreamMappedField<T> {

	private final ProtofieldWriter<T> delegate;

	UnsafeProtofield(ProtofieldWriter<T> delegate) {
		Objects.requireNonNull( delegate );
		this.delegate = delegate;
	}

	@Override
	public void writeTo(ProtoStreamWriter outProtobuf, T value) {
		try {
			delegate.writeTo( outProtobuf, value );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public T read(ProtoStreamReader reader) {
		try {
			return delegate.read( reader );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@Override
	public String getColumnName() {
		return delegate.getColumnName();
	}

	@Override
	public String getProtobufName() {
		return delegate.getProtobufName();
	}

	@Override
	public void exportProtobufFieldDefinition(StringBuilder sb) {
		delegate.exportProtobufFieldDefinition( sb );
	}

	@Override
	public void collectTypeDefinitions(TypeDeclarationsCollector typesDefCollector) {
		delegate.collectTypeDefinitions( typesDefCollector );
	}

}
