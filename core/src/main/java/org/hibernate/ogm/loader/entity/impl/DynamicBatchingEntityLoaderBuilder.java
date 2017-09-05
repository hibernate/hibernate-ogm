/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.entity.impl;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.entity.EntityJoinWalker;
import org.hibernate.loader.entity.EntityLoader;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.loader.spi.AfterLoadAction;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.loader.impl.TupleBasedEntityLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.jboss.logging.Logger;

/**
 * DO NOT UPDATE: Copy of ORM code that will be backported
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
class DynamicBatchingEntityLoaderBuilder extends BatchingEntityLoaderBuilder {
	public static final DynamicBatchingEntityLoaderBuilder INSTANCE = new DynamicBatchingEntityLoaderBuilder();

	private static final Logger log = Logger.getLogger( DynamicBatchingEntityLoaderBuilder.class );


	@Override
	protected UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockMode lockMode,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
		return new DynamicBatchingEntityLoader( persister, batchSize, lockMode, factory, influencers, innerEntityLoaderBuilder );
	}

	@Override
	protected UniqueEntityLoader buildBatchingLoader(
			OuterJoinLoadable persister,
			int batchSize,
			LockOptions lockOptions,
			SessionFactoryImplementor factory,
			LoadQueryInfluencers influencers,
			BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
		return new DynamicBatchingEntityLoader(
				persister,
				batchSize,
				lockOptions,
				factory,
				influencers,
				innerEntityLoaderBuilder );
	}

	public static class DynamicBatchingEntityLoader extends BatchingEntityLoader implements TupleBasedEntityLoader {
		private final int maxBatchSize;
		private final TupleBasedEntityLoader singleKeyLoader;
		private final BatchableEntityLoader dynamicLoader;

		public DynamicBatchingEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockMode lockMode,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers,
				BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
			super( persister );
			this.maxBatchSize = maxBatchSize;
			this.singleKeyLoader = innerEntityLoaderBuilder.buildLoader( persister, 1, lockMode, factory, loadQueryInfluencers );
			this.dynamicLoader = innerEntityLoaderBuilder.buildDynamicLoader( persister, maxBatchSize, lockMode, factory, loadQueryInfluencers );
		}

		public DynamicBatchingEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockOptions lockOptions,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers,
				BatchableEntityLoaderBuilder innerEntityLoaderBuilder) {
			super( persister );
			this.maxBatchSize = maxBatchSize;
			this.singleKeyLoader = innerEntityLoaderBuilder.buildLoader( persister, 1, lockOptions, factory, loadQueryInfluencers );
			this.dynamicLoader = innerEntityLoaderBuilder.buildDynamicLoader( persister, maxBatchSize, lockOptions, factory, loadQueryInfluencers );
		}

		@Override
		public Object load(Serializable id, Object optionalObject, SharedSessionContractImplementor session, LockOptions lockOptions) {
			final Serializable[] batch = session.getPersistenceContext()
					.getBatchFetchQueue()
					.getEntityBatch( persister(), id, maxBatchSize, persister().getEntityMode() );

			final int numberOfIds = ArrayHelper.countNonNull( batch );
			if ( numberOfIds <= 1 ) {
				return singleKeyLoader.load( id, optionalObject, session );
			}

			final Serializable[] idsToLoad = new Serializable[numberOfIds];
			System.arraycopy( batch, 0, idsToLoad, 0, numberOfIds );

			return doBatchLoad( id, dynamicLoader, session, idsToLoad, optionalObject, lockOptions );
		}

		@Override
		public List<Object> loadEntitiesFromTuples(SharedSessionContractImplementor session, LockOptions lockOptions, OgmLoadingContext ogmContext) {
			return singleKeyLoader.loadEntitiesFromTuples( session, lockOptions, ogmContext );
		}
	}

	public static class DynamicEntityLoader extends EntityLoader {
		// todo : see the discussion on org.hibernate.loader.collection.DynamicBatchingCollectionInitializerBuilder.DynamicBatchingCollectionLoader

		private final String sqlTemplate;
		private final String alias;

		public DynamicEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockOptions lockOptions,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			this( persister, maxBatchSize, lockOptions.getLockMode(), factory, loadQueryInfluencers );
		}

		public DynamicEntityLoader(
				OuterJoinLoadable persister,
				int maxBatchSize,
				LockMode lockMode,
				SessionFactoryImplementor factory,
				LoadQueryInfluencers loadQueryInfluencers) {
			super( persister, -1, lockMode, factory, loadQueryInfluencers );

			EntityJoinWalker walker = new EntityJoinWalker(
					persister,
					persister.getIdentifierColumnNames(),
					-1,
					lockMode,
					factory,
					loadQueryInfluencers
			) {
				@Override
				protected StringBuilder whereString(String alias, String[] columnNames, int batchSize) {
					return StringHelper.buildBatchFetchRestrictionFragment(
							alias,
							columnNames,
							getFactory().getDialect()
					);
				}
			};

			initFromWalker( walker );
			this.sqlTemplate = walker.getSQLString();
			this.alias = walker.getAlias();
			postInstantiate();

			if ( LOG.isDebugEnabled() ) {
				LOG.debugf(
						"SQL-template for dynamic entity [%s] batch-fetching [%s] : %s",
						entityName,
						lockMode,
						sqlTemplate
				);
			}
		}

		@Override
		protected boolean isSingleRowLoader() {
			return false;
		}

		public List doEntityBatchFetch(
				SessionImplementor session,
				QueryParameters queryParameters,
				Serializable[] ids) {
			final String sql = StringHelper.expandBatchIdPlaceholder(
					sqlTemplate,
					ids,
					alias,
					persister.getKeyColumnNames(),
					getFactory().getDialect()
			);

			try {
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				boolean defaultReadOnlyOrig = persistenceContext.isDefaultReadOnly();
				if ( queryParameters.isReadOnlyInitialized() ) {
					// The read-only/modifiable mode for the query was explicitly set.
					// Temporarily set the default read-only/modifiable setting to the query's setting.
					persistenceContext.setDefaultReadOnly( queryParameters.isReadOnly() );
				}
				else {
					// The read-only/modifiable setting for the query was not initialized.
					// Use the default read-only/modifiable from the persistence context instead.
					queryParameters.setReadOnly( persistenceContext.isDefaultReadOnly() );
				}
				persistenceContext.beforeLoad();
				List results;
				try {
					try {
						results = doTheLoad( sql, queryParameters, session );
					}
					finally {
						persistenceContext.afterLoad();
					}
					persistenceContext.initializeNonLazyCollections();
					log.debug( "Done batch load" );
					return results;
				}
				finally {
					// Restore the original default
					persistenceContext.setDefaultReadOnly( defaultReadOnlyOrig );
				}
			}
			catch ( SQLException sqle ) {
				throw session.getFactory().getServiceRegistry().getService( JdbcServices.class ).getSqlExceptionHelper().convert(
						sqle,
						"could not load an entity batch: " + MessageHelper.infoString(
								getEntityPersisters()[0],
								ids,
								session.getFactory()
						),
						sql
				);
			}
		}

		private List doTheLoad(String sql, QueryParameters queryParameters, SessionImplementor session) throws SQLException {
			final RowSelection selection = queryParameters.getRowSelection();
			final int maxRows = LimitHelper.hasMaxRows( selection ) ?
					selection.getMaxRows() :
					Integer.MAX_VALUE;

			final List<AfterLoadAction> afterLoadActions = new ArrayList<AfterLoadAction>();
			final SqlStatementWrapper wrapper = executeQueryStatement( sql, queryParameters, false, afterLoadActions, session );
			final ResultSet rs = wrapper.getResultSet();
			final Statement st = wrapper.getStatement();
			try {
				return processResultSet( rs, queryParameters, session, false, null, maxRows, afterLoadActions );
			}
			finally {
				session.getJdbcCoordinator().getResourceRegistry().release( st );
				session.getJdbcCoordinator().afterStatementExecution();
			}
		}
	}
}
