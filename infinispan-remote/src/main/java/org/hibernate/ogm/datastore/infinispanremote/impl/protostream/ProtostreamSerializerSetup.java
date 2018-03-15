/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.io.IOException;

import org.hibernate.ogm.datastore.infinispanremote.impl.protobuf.SchemaDefinitions;
import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.SequenceIdMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.infinispan.protostream.DescriptorParserException;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.impl.SerializationContextImpl;

public class ProtostreamSerializerSetup {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private ProtostreamSerializerSetup() {
		//Not to be constructed
	}

	public static SerializationContext buildSerializationContext(
			SchemaDefinitions sd, MainOgmCoDec delegate, OgmProtoStreamMarshaller marshaller) throws DescriptorParserException, IOException {
		Configuration cfg = Configuration.builder().setLogOutOfSequenceReads( true ).build();
		SerializationContextImpl serContext = new SerializationContextImpl( cfg );
		IdMessageMarshaller idM = new IdMessageMarshaller( delegate );
		PayloadMessageMarshaller valueM = new PayloadMessageMarshaller( delegate );
		try {
			serContext.registerProtoFiles( sd.asFileDescriptorSource() );
		}
		catch (DescriptorParserException | IOException e) {
			throw log.errorAtProtobufParsing( e );
		}
		// register the marshallers to local serialization context (used for marshalling)
		serContext.registerMarshaller( idM );
		serContext.registerMarshaller( valueM );

		// register the marshallers to global serialization context (used for unmarshalling)
		marshaller.getSerializationContext().registerMarshaller( idM );
		marshaller.getSerializationContext().registerMarshaller( valueM );
		return serContext;
	}

	public static SerializationContext buildSerializationContextForSequences(
			SchemaDefinitions sd, SequenceTableDefinition std) {
		Configuration cfg = Configuration.builder().setLogOutOfSequenceReads( true ).build();
		SerializationContextImpl serContext = new SerializationContextImpl( cfg );
		try {
			serContext.registerProtoFiles( sd.asFileDescriptorSource() );
		}
		catch (DescriptorParserException | IOException e) {
			throw log.errorAtProtobufParsing( e );
		}
		SequenceIdMarshaller idM = new SequenceIdMarshaller( std );
		serContext.registerMarshaller( idM );
		return serContext;
	}

}
