/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.sequences;

import java.io.IOException;

import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.infinispan.protostream.MessageMarshaller;

public final class SequenceIdMarshaller implements MessageMarshaller<SequenceId> {

	private final String typeName;
	private final SequenceTableDefinition std;

	public SequenceIdMarshaller(SequenceTableDefinition std) {
		this.typeName = std.getQualifiedIdMessageName();
		this.std = std;
	}

	@Override
	public Class<? extends SequenceId> getJavaClass() {
		return SequenceId.class;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public SequenceId readFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
		return std.readSequenceId( reader );
	}

	@Override
	public void writeTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, SequenceId t) throws IOException {
		std.writeSequenceId( writer, t );
	}

}
