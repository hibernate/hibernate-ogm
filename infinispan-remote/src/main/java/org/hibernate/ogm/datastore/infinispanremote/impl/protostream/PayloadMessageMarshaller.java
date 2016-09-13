/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.io.IOException;
import java.util.Objects;

import org.infinispan.protostream.MessageMarshaller;

public class PayloadMessageMarshaller implements MessageMarshaller<ProtostreamPayload> {

	private final MainOgmCoDec ogmEncoder;

	public PayloadMessageMarshaller(MainOgmCoDec ogmEncoder) {
		this.ogmEncoder = Objects.requireNonNull( ogmEncoder );
	}

	@Override
	public Class<ProtostreamPayload> getJavaClass() {
		return ProtostreamPayload.class;
	}

	@Override
	public String getTypeName() {
		return ogmEncoder.getProtobufTypeName();
	}

	@Override
	public ProtostreamPayload readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
		return ogmEncoder.readPayloadFrom( reader );
	}

	@Override
	public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, ProtostreamPayload payload) throws IOException {
		ogmEncoder.writePayloadTo( writer, payload );
	}

}
