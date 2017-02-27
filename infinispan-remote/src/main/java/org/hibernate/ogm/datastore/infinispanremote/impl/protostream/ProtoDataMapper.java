/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.impl.protostream;

import java.io.IOException;
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
import org.infinispan.protostream.SerializationContext;

public final class ProtoDataMapper implements ProtoStreamMappingAdapter, ProtostreamAssociationMappingAdapter {

	private final SerializationContext serContext;
	private final MainOgmCoDec delegate;
	private final OgmProtoStreamMarshaller marshaller;

	public ProtoDataMapper(MainOgmCoDec delegate, SerializationContext serContext, OgmProtoStreamMarshaller marshaller) throws DescriptorParserException, IOException {
		this.marshaller = marshaller;
		this.delegate = Objects.requireNonNull( delegate );
		this.serContext = Objects.requireNonNull( serContext );
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
		try {
			marshaller.setCurrentSerializationContext( serContext );
			return (T) function.doOnCache( delegate.getLinkedCache() );
		}
		finally {
			marshaller.setCurrentSerializationContext( null );
		}
	}

	@Override
	public <T> T withinCacheEncodingContext(AssociationCacheOperation<T> function) {
		try {
			marshaller.setCurrentSerializationContext( serContext );
			return (T) function.doOnCache( delegate.getLinkedCache() );
		}
		finally {
			marshaller.setCurrentSerializationContext( null );
		}
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
