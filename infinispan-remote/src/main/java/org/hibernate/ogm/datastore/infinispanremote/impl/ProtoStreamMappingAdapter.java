/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl;

import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamId;
import org.hibernate.ogm.datastore.infinispanremote.impl.protostream.ProtostreamPayload;
import org.hibernate.ogm.model.spi.Tuple;

public interface ProtoStreamMappingAdapter {

	ProtostreamPayload createValuePayload(Tuple tuple);

	ProtostreamId createIdPayload(String[] columnNames, Object[] columnValues);

	<T> T withinCacheEncodingContext(CacheOperation<T> function);

	boolean columnSetExceedsIdRequirements(String[] associationIdColumns);

	String[] listIdColumnNames();

}
