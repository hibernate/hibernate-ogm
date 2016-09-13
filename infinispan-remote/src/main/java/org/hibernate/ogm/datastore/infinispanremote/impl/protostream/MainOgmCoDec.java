/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.io.IOException;

import org.hibernate.ogm.datastore.infinispanremote.impl.VersionedAssociation;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;
import org.infinispan.client.hotrod.RemoteCache;

public interface MainOgmCoDec {

	RemoteCache getLinkedCache();

	ProtostreamId readProtostreamId(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException;

	void writeIdTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, ProtostreamId id) throws IOException;

	ProtostreamPayload readPayloadFrom(org.infinispan.protostream.MessageMarshaller.ProtoStreamReader reader) throws IOException;

	void writePayloadTo(org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter writer, ProtostreamPayload payload) throws IOException;

	ProtostreamId createIdPayload(String[] columnNames, Object[] columnValues);

	ProtostreamPayload createValuePayload(Tuple tuple);

	ProtostreamAssociationPayload createAssociationPayload(EntityKey key, VersionedAssociation tuple, TupleContext tupleContext);

	String getProtobufTypeName();

	String getIdProtobufTypeName();

	String convertColumnNameToFieldName(String string);

	boolean columnSetExceedsIdRequirements(String[] columnNames);

	String[] listIdColumnNames();

}
