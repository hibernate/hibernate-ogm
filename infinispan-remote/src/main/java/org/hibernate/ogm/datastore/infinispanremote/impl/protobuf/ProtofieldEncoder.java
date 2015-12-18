/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protobuf;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller.ProtoStreamWriter;

@FunctionalInterface
public interface ProtofieldEncoder<T> {

	void encode(ProtoStreamWriter outProtobuf, T value) throws IOException;

}
