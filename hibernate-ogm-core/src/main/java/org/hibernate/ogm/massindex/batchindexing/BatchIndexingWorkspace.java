/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.massindex.batchindexing;

import java.util.concurrent.CountDownLatch;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.EntityKeyMetadata;
import org.hibernate.ogm.persister.OgmEntityPersister;
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
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class BatchIndexingWorkspace implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final SearchFactoryImplementor searchFactory;
	private final SessionFactory sessionFactory;

	private final Class<?> indexedType;

	// progress monitor
	private final MassIndexerProgressMonitor monitor;

	// loading options
	private final CacheMode cacheMode;

	private final BatchBackend batchBackend;

	private final GridDialect gridDialect;

	private final CountDownLatch endAllSignal;

	public BatchIndexingWorkspace(GridDialect gridDialect, SearchFactoryImplementor searchFactoryImplementor,
			SessionFactory sessionFactory, Class<?> entityType, CacheMode cacheMode, CountDownLatch endAllSignal,
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

	private EntityKeyMetadata metadata(SessionFactory sessionFactory, Class<?> indexedType) {
		OgmEntityPersister persister = (OgmEntityPersister) ( (SessionFactoryImplementor) sessionFactory ).getEntityPersister( indexedType.getName() );
		return new EntityKeyMetadata( persister.getTableName(), persister.getRootTableIdentifierColumnNames() );
	}

	public void run() {
		ErrorHandler errorHandler = searchFactory.getErrorHandler();
		try {
			final EntityKeyMetadata keyMetadata = metadata( sessionFactory, indexedType );
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
