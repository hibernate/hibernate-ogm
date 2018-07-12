/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream.multimessage;

import static org.infinispan.protostream.WrappedMessage.WRAPPED_DESCRIPTOR_FULL_NAME;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_DESCRIPTOR_ID;
import static org.infinispan.protostream.WrappedMessage.WRAPPED_MESSAGE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.infinispan.protostream.ImmutableSerializationContext;
import org.infinispan.protostream.RawProtoStreamWriter;
import org.infinispan.protostream.impl.BaseMarshallerDelegate;
import org.infinispan.protostream.impl.ByteArrayOutputStreamEx;
import org.infinispan.protostream.impl.RawProtoStreamWriterImpl;
import org.infinispan.protostream.impl.SerializationContextImpl;

/**
 * Handle the marshalling of a {@link MultiMessage}.
 *
 * The code is copied from :
 * {@link org.infinispan.protostream.WrappedMessage#writeMessage(ImmutableSerializationContext, RawProtoStreamWriter, Object)}
 * {@link org.infinispan.query.remote.client.BaseProtoStreamMarshaller#objectToBuffer(Object, int)}.
 *
 * The only change here is the way we choose the {@link BaseMarshallerDelegate} from {@link org.infinispan.protostream.SerializationContext}:.
 * We use the message type instead of java class to get right marshaller.
 *
 */
public class MultiMessageExtension {

	public static byte[] toWrappedByteArray(ImmutableSerializationContext ctx, MultiMessage t) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		MultiMessageExtension.writeMessage( ctx, RawProtoStreamWriterImpl.newInstance( baos ), t );
		return baos.toByteArray();
	}

	private static void writeMessage(ImmutableSerializationContext ctx, RawProtoStreamWriter out, MultiMessage t) throws IOException {
		if ( t == null ) {
			return;
		}

		// This is either an unknown primitive type or a message type. Try to use a message marshaller.
		BaseMarshallerDelegate marshallerDelegate = ( (SerializationContextImpl) ctx ).getMarshallerDelegate( t.getMessageType() );
		ByteArrayOutputStreamEx buffer = new ByteArrayOutputStreamEx();
		RawProtoStreamWriter nestedOut = RawProtoStreamWriterImpl.newInstance( buffer );
		marshallerDelegate.marshall( null, t, null, nestedOut );
		nestedOut.flush();

		String typeName = marshallerDelegate.getMarshaller().getTypeName();
		Integer typeId = ctx.getTypeIdByName( typeName );
		if ( typeId == null ) {
			out.writeString( WRAPPED_DESCRIPTOR_FULL_NAME, typeName );
		}
		else {
			out.writeInt32( WRAPPED_DESCRIPTOR_ID, typeId );
		}
		out.writeBytes( WRAPPED_MESSAGE, buffer.getByteBuffer() );
		out.flush();
	}
}
