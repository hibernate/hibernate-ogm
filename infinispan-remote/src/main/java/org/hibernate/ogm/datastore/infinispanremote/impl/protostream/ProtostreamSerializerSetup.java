/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.lang.invoke.MethodHandles;

import org.hibernate.ogm.datastore.infinispanremote.impl.schema.SequenceTableDefinition;
import org.hibernate.ogm.datastore.infinispanremote.impl.sequences.SequenceIdMarshaller;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.Log;
import org.hibernate.ogm.datastore.infinispanremote.logging.impl.LoggerFactory;

import org.infinispan.protostream.DescriptorParserException;

public class ProtostreamSerializerSetup {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private ProtostreamSerializerSetup() {
		//Not to be constructed
	}

	public static void registerEntityMarshaller(MainOgmCoDec delegate, OgmProtoStreamMarshaller marshaller) throws DescriptorParserException {
		IdMessageMarshaller idM = new IdMessageMarshaller( delegate );
		PayloadMessageMarshaller valueM = new PayloadMessageMarshaller( delegate );
		marshaller.getSerializationContext().registerMarshaller( idM );
		marshaller.getSerializationContext().registerMarshaller( valueM );
	}

	public static void registerSequenceMarshaller(SequenceTableDefinition std, OgmProtoStreamMarshaller marshaller) {
		SequenceIdMarshaller idM = new SequenceIdMarshaller( std );
		marshaller.getSerializationContext().registerMarshaller( idM );
	}
}
