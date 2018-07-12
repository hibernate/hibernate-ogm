/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.util.Objects;

import org.hibernate.ogm.datastore.infinispanremote.impl.AssociationCacheOperation;
import org.hibernate.ogm.datastore.infinispanremote.impl.CacheOperation;
import org.hibernate.ogm.datastore.infinispanremote.impl.ProtoStreamMappingAdapter;
import org.hibernate.ogm.datastore.infinispanremote.impl.ProtostreamAssociationMappingAdapter;
import org.hibernate.ogm.datastore.infinispanremote.impl.VersionedAssociation;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.spi.Tuple;

import org.infinispan.protostream.DescriptorParserException;

public final class ProtoDataMapper implements ProtoStreamMappingAdapter, ProtostreamAssociationMappingAdapter {

	private final MainOgmCoDec delegate;

	public ProtoDataMapper(MainOgmCoDec delegate) throws DescriptorParserException {
		this.delegate = Objects.requireNonNull( delegate );
	}

	@Override
	public ProtostreamPayload createValuePayload(Tuple tuple) {
		return delegate.createValuePayload( tuple );
	}

	@Override
	public ProtostreamAssociationPayload createAssociationPayload(EntityKey key, VersionedAssociation tuple, TupleContext tupleContext) {
		return delegate.createAssociationPayload( key, tuple, tupleContext );
	}

	@Override
	public ProtostreamId createIdPayload(String[] columnNames, Object[] columnValues) {
		return delegate.createIdPayload( columnNames, columnValues );
	}

	@Override
	public <T> T withinCacheEncodingContext(CacheOperation<T> function) {
		return (T) function.doOnCache( delegate.getLinkedCache() );
	}

	@Override
	public <T> T withinCacheEncodingContext(AssociationCacheOperation<T> function) {
		return (T) function.doOnCache( delegate.getLinkedCache() );
	}

	@Override
	public String convertColumnNameToFieldName(String string) {
		return delegate.convertColumnNameToFieldName( string );
	}

	@Override
	public String toString() {
		return "ProtoDataMapper[cacheName='" + delegate.getLinkedCache().getName() + "']";
	}

	@Override
	public String[] listIdColumnNames() {
		return delegate.listIdColumnNames();
	}
}
