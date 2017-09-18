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
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import java.lang.invoke.MethodHandles;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class BatchIndexingWorkspace implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ExtendedSearchIntegrator searchIntegrator;
	private final SessionFactoryImplementor sessionFactory;

	private final IndexedTypeIdentifier indexedTypeIdentifier;

	// progress monitor
	private final MassIndexerProgressMonitor monitor;

	// loading options
	private final CacheMode cacheMode;

	private final BatchBackend batchBackend;

	private final GridDialect gridDialect;

	private final CountDownLatch endAllSignal;

	private final String tenantId;

	public BatchIndexingWorkspace(GridDialect gridDialect, SearchIntegrator search,
			SessionFactoryImplementor sessionFactory, IndexedTypeIdentifier indexedTypeIdentifier, CacheMode cacheMode, CountDownLatch endAllSignal,
			MassIndexerProgressMonitor monitor, BatchBackend backend, String tenantId) {
		this.gridDialect = gridDialect;
		this.indexedTypeIdentifier = indexedTypeIdentifier;
		this.tenantId = tenantId;
		this.searchIntegrator = search.unwrap( ExtendedSearchIntegrator.class );
		this.sessionFactory = sessionFactory;
		this.cacheMode = cacheMode;
		this.endAllSignal = endAllSignal;
		this.batchBackend = backend;
		this.monitor = monitor;
	}

	@Override
	public void run() {
		ErrorHandler errorHandler = searchIntegrator.getErrorHandler();
		try {
			OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( indexedTypeIdentifier.getPojoType() );
			final EntityKeyMetadata keyMetadata = new DefaultEntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );

			final SessionAwareRunnable consumer = new TupleIndexer( indexedTypeIdentifier, monitor, sessionFactory, searchIntegrator, cacheMode, batchBackend, errorHandler, tenantId );
			gridDialect.forEachTuple( new OptionallyWrapInJTATransaction( sessionFactory, errorHandler, consumer ), persister.getTupleTypeContext(), keyMetadata );
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
