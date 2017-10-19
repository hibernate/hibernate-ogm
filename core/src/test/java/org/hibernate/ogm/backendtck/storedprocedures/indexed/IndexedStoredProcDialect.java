/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.storedprocedures.indexed;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.ogm.dialect.query.spi.ClosableIterator;
import org.hibernate.ogm.dialect.spi.AssociationContext;
import org.hibernate.ogm.dialect.spi.AssociationTypeContext;
import org.hibernate.ogm.dialect.spi.BaseGridDialect;
import org.hibernate.ogm.dialect.spi.ModelConsumer;
import org.hibernate.ogm.dialect.spi.NextValueRequest;
import org.hibernate.ogm.dialect.spi.OperationContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.dialect.spi.TupleTypeContext;
import org.hibernate.ogm.dialect.storedprocedure.spi.StoredProcedureAwareGridDialect;
import org.hibernate.ogm.entityentry.impl.TuplePointer;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKey;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
public class IndexedStoredProcDialect extends BaseGridDialect implements StoredProcedureAwareGridDialect {

	public static final Map<String, IndexedStoredProcedure> FUNCTIONS = new HashMap<>(  );

	private static final Log log = LoggerFactory.make();
	private IndexedStoredProcProvider provider;

	public IndexedStoredProcDialect(IndexedStoredProcProvider provider) {
		this.provider = provider;
	}

	@Override
	public boolean supportsNamedPosition() {
		return false;
	}

	@Override
	public ClosableIterator<Tuple> callStoredProcedure(String storedProcedureName, Object[] params, TupleContext tupleContext) {
		log.infof( "Try to call procedure %s",storedProcedureName );
		return FUNCTIONS.get( storedProcedureName ).execute( params );
	}

	@Override
	public ClosableIterator<Tuple> callStoredProcedure(String storedProcedureName, Map<String, Object> params,
			TupleContext tupleContext) {
		throw new UnsupportedOperationException( "The dialect is not support named parameters!" );
	}

	@Override
	public Tuple getTuple(EntityKey key, OperationContext tupleContext) {
		return null;
	}

	@Override
	public Tuple createTuple(EntityKey key, OperationContext tupleContext) {
		return null;
	}

	@Override
	public void insertOrUpdateTuple(EntityKey key, TuplePointer tuplePointer, TupleContext tupleContext) {
	}

	@Override
	public void removeTuple(EntityKey key, TupleContext tupleContext) {
	}

	@Override
	public Association getAssociation(AssociationKey key, AssociationContext associationContext) {
		return null;
	}

	@Override
	public Association createAssociation(AssociationKey key, AssociationContext associationContext) {
		return null;
	}

	@Override
	public void insertOrUpdateAssociation(AssociationKey key, Association association, AssociationContext associationContext) {
	}

	@Override
	public void removeAssociation(AssociationKey key, AssociationContext associationContext) {
	}

	@Override
	public boolean isStoredInEntityStructure(AssociationKeyMetadata associationKeyMetadata, AssociationTypeContext associationTypeContext) {
		return false;
	}

	@Override
	public Number nextValue(NextValueRequest request) {
		return null;
	}

	@Override
	public void forEachTuple(ModelConsumer consumer, TupleTypeContext tupleTypeContext, EntityKeyMetadata entityKeyMetadata) {
	}
}
