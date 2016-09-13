/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.schema;

import java.io.IOException;

import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.LongProtofieldWriter;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.ProtofieldWriter;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.StringProtofieldWriter;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.SequenceId;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

public final class SequenceTableDefinition implements ProtobufEntryExporter {

	private final String messageName;
	private final String idMessageName;
	private final String keyColumnName;
	private final String valueColumnName;
	private final LongProtofieldWriter valueEncoder;
	private final StringProtofieldWriter sequenceNameEncoder;
	private final String protobufPackageName;

	public SequenceTableDefinition(IdSourceKeyMetadata idSourceKeyMetadata, String protobufPackageName) {
		this.protobufPackageName = protobufPackageName;
		this.messageName = inferMessageName( idSourceKeyMetadata );
		this.idMessageName = SanitationUtils.toProtobufIdName( messageName );
		this.keyColumnName = SanitationUtils.convertNameSafely( idSourceKeyMetadata.getKeyColumnName() );
		this.valueColumnName = SanitationUtils.convertNameSafely( idSourceKeyMetadata.getValueColumnName() );
		this.sequenceNameEncoder = new StringProtofieldWriter( 1,
				keyColumnName, false, idSourceKeyMetadata.getKeyColumnName() );
		this.valueEncoder = new LongProtofieldWriter( 2,
				valueColumnName, false, idSourceKeyMetadata.getValueColumnName() );
	}

	private static String inferMessageName(IdSourceKeyMetadata idSourceKeyMetadata) {
		return SanitationUtils.convertNameSafely( idSourceKeyMetadata.getName() );
	}

	@Override
	public void exportProtobufEntry(StringBuilder sb) {
		exportMessage( idMessageName, sequenceNameEncoder, sb );
		exportMessage( messageName, valueEncoder, sb );
	}

	private static void exportMessage(String messageName, ProtofieldWriter<?> encoder, StringBuilder sb) {
		sb.append( "\nmessage " ).append( messageName ).append( " {" );
		encoder.exportProtobufFieldDefinition( sb );
		sb.append( "\n}\n" );
	}

	public String getName() {
		return messageName;
	}

	public SequenceId readSequenceId(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException {
		String read = sequenceNameEncoder.read( reader );
		return new SequenceId( read );
	}

	public void writeSequenceId(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, SequenceId t) throws IOException {
		sequenceNameEncoder.writeTo( writer, t.getSegmentName() );
	}

	public String getQualifiedIdMessageName() {
		return SanitationUtils.qualify( idMessageName, protobufPackageName );
	}

}
