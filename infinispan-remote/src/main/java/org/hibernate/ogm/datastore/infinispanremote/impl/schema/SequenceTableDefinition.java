/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.schema;

import java.io.IOException;
import java.util.List;

import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.LongProtofieldAccessor;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.ProtofieldAccessor;
import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.StringProtofieldAccessor;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.SequenceId;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;

import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FieldDescriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;

public final class SequenceTableDefinition implements ProtobufEntryExporter {

	private final String messageName;
	private final String idMessageName;
	private final String keyColumnName;
	private final String valueColumnName;
	private final LongProtofieldAccessor valueEncoder;
	private final StringProtofieldAccessor sequenceNameEncoder;
	private final String protobufPackageName;

	public SequenceTableDefinition(IdSourceKeyMetadata idSourceKeyMetadata, String protobufPackageName) {
		this.protobufPackageName = protobufPackageName;
		this.messageName = inferMessageName( idSourceKeyMetadata );
		this.idMessageName = SanitationUtils.toProtobufIdName( messageName );
		this.keyColumnName = SanitationUtils.convertNameSafely( idSourceKeyMetadata.getKeyColumnName() );
		this.valueColumnName = SanitationUtils.convertNameSafely( idSourceKeyMetadata.getValueColumnName() );
		this.sequenceNameEncoder = new StringProtofieldAccessor( 1,
				keyColumnName, false, idSourceKeyMetadata.getKeyColumnName() );
		this.valueEncoder = new LongProtofieldAccessor( 2,
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

	private static void exportMessage(String messageName, ProtofieldAccessor<?> encoder, StringBuilder sb) {
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

	public boolean isDescribedIn(FileDescriptor fileDescriptor) {
		boolean idMessageIsDescribed = false;
		boolean messageIsDescribed = false;

		for ( Descriptor descriptor : fileDescriptor.getMessageTypes() ) {
			if ( descriptor.getName().equals( idMessageName ) ) {
				if ( idMessageIsDescribedIn( descriptor ) ) {
					idMessageIsDescribed = true;
				}
			}
			if ( descriptor.getName().equals( messageName ) ) {
				if ( messageIsDescribedIn( descriptor ) ) {
					messageIsDescribed = true;
				}
			}
		}

		// both key and value types must be described
		return idMessageIsDescribed && messageIsDescribed;
	}

	private boolean idMessageIsDescribedIn(Descriptor descriptor) {
		List<FieldDescriptor> fields = descriptor.getFields();
		if ( fields.size() != 1 ) {
			return false;
		}

		FieldDescriptor fieldDescriptor = fields.get( 0 );
		return fieldDescriptor.getName().equals( keyColumnName ) || !fieldDescriptor.getTypeName().equals( sequenceNameEncoder.getProtobufTypeName() );
	}

	private boolean messageIsDescribedIn(Descriptor descriptor) {
		List<FieldDescriptor> fields = descriptor.getFields();
		if ( fields.size() != 1 ) {
			return false;
		}

		FieldDescriptor fieldDescriptor = fields.get( 0 );
		return fieldDescriptor.getName().equals( valueColumnName ) || !fieldDescriptor.getTypeName().equals( valueEncoder.getProtobufTypeName() );
	}
}
