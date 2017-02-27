/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamAssociationPayload;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;

public interface ProtostreamAssociationMappingAdapter {

	ProtostreamAssociationPayload createAssociationPayload(EntityKey key, VersionedAssociation tuple, TupleContext tupleContext);

	ProtostreamId createIdPayload(String[] columnNames, Object[] columnValues);

	<T> T withinCacheEncodingContext(AssociationCacheOperation<T> function);

	String convertColumnNameToFieldName(String string);

}
