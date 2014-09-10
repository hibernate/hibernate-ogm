/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.impl;

import java.util.concurrent.CountDownLatch;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class BatchIndexingWorkspace implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final SearchFactoryImplementor searchFactory;
	private final SessionFactoryImplementor sessionFactory;

	private final Class<?> indexedType;

	// progress monitor
	private final MassIndexerProgressMonitor monitor;

	// loading options
	private final CacheMode cacheMode;

	private final BatchBackend batchBackend;

	private final GridDialect gridDialect;

	private final CountDownLatch endAllSignal;

	public BatchIndexingWorkspace(GridDialect gridDialect, SearchFactoryImplementor searchFactoryImplementor,
			SessionFactoryImplementor sessionFactory, Class<?> entityType, CacheMode cacheMode, CountDownLatch endAllSignal,
			MassIndexerProgressMonitor monitor, BatchBackend backend) {
		this.gridDialect = gridDialect;
		this.indexedType = entityType;
		this.searchFactory = searchFactoryImplementor;
		this.sessionFactory = sessionFactory;
		this.cacheMode = cacheMode;
		this.endAllSignal = endAllSignal;
		this.batchBackend = backend;
		this.monitor = monitor;
	}

	private EntityKeyMetadata getEntityKeyMetadata() {
		OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getEntityPersister( indexedType.getName() );
		return new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
	}

	@Override
	public void run() {
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		try {
			final EntityKeyMetadata keyMetadata = getEntityKeyMetadata();
			final SessionAwareRunnable consumer = new TupleIndexer( indexedType, monitor, sessionFactory, searchFactory, cacheMode, batchBackend, errorHandler );
			gridDialect.forEachTuple( new OptionallyWrapInJTATransaction( sessionFactory, errorHandler, consumer ), keyMetadata );
		}
		catch ( RuntimeException re ) {
			// being this an async thread we want to make sure everything is somehow reported
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage(), re );
		}
		finally {
			endAllSignal.countDown();
		}
	}
}
