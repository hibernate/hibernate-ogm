/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.util.impl;

import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.dialect.batch.spi.OperationsQueue;
import org.hibernate.ogm.dialect.spi.TransactionContext;
import org.hibernate.ogm.dialect.spi.TupleContext;
import org.hibernate.ogm.model.key.spi.AssociatedEntityKeyMetadata;
import org.hibernate.ogm.model.spi.EntityMetadataInformation;
import org.hibernate.ogm.options.spi.OptionsContext;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;

/**
 * @author Davide D'Alto
 */
public class TupleContextHelper {

	private static final Log LOG = LoggerFactory.make();

	/**
	 * Given a {@link SessionImplementor} returns the {@link TupleContext} associated to an entity.
	 *
	 * @param session the current session
	 * @param metadata the {@link EntityMetadataInformation} of the entity associated to the TupleContext
	 * @return the TupleContext associated to the current session for the entity specified
	 */
	public static TupleContext tupleContext(SessionImplementor session, EntityMetadataInformation metadata) {
		if ( metadata != null ) {
			OgmEntityPersister persister = (OgmEntityPersister) session.getFactory().getEntityPersister( metadata.getTypeName() );
			return persister.getTupleContext( session );
		}
		else if ( session != null ) {
			// We are not dealing with a single entity but we might still need the transactionContext
			TransactionContext transactionContext = TransactionContextHelper.transactionContext( session );
			TupleContext tupleContext = new OnlyWithTransactionContext( transactionContext );
			return tupleContext;
		}
		else {
			return null;
		}
	}

	/**
	 * This {@link TupleContext} can be used for those use case where we don't necessarily have a tuple context but we
	 * still need a {@link TransactionContext}.
	 */
	private static class OnlyWithTransactionContext implements TupleContext {

		private final TransactionContext transactionContext;

		private OnlyWithTransactionContext(TransactionContext transactionContext) {
			this.transactionContext = transactionContext;
		}

		@Override
		public TransactionContext getTransactionContext() {
			return transactionContext;
		}

		@Override
		public boolean isPartOfAssociation(String column) {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public List<String> getSelectableColumns() {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public String getRole(String column) {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public OptionsContext getOptionsContext() {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public OperationsQueue getOperationsQueue() {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public AssociatedEntityKeyMetadata getAssociatedEntityKeyMetadata(String column) {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public Map<String, String> getAllRoles() {
			throw LOG.tupleContextNotAvailable();
		}

		@Override
		public Map<String, AssociatedEntityKeyMetadata> getAllAssociatedEntityKeyMetadata() {
			throw LOG.tupleContextNotAvailable();
		}
	}
}
