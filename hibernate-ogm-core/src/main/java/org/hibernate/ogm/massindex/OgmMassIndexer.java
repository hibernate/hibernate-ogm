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
package org.hibernate.ogm.massindex;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.massindex.batchindexing.BatchCoordinator;
import org.hibernate.ogm.massindex.batchindexing.Executors;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jmx.IndexingProgressMonitor;

/**
 * {@link MassIndexer} that can be register in Hibernate Search to index existing data stores.
 *
 * @see org.hibernate.search.spi.MassIndexerFactory
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OgmMassIndexer implements MassIndexer {

	private static final Log log = LoggerFactory.make();

	private final SearchFactoryImplementor searchFactory;
	private final SessionFactory sessionFactory;
	private final GridDialect gridDialect;

	private int threadsToLoad = 2;
	private int batchSizeToLoad = 10;
	private int threadForSubsequentFetching = 4;
	private MassIndexerProgressMonitor monitor;
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeOnFinish = true;
	private boolean optimizeAfterPurge = true;
	private boolean purgeAllOnStart = true;
	private long maximumIndexedObjects = 10;
	private int idFetchSize = 100;

	private final Set<Class<?>> rootEntities;

	public OgmMassIndexer(GridDialect gridDialect, SearchFactoryImplementor searchFactory, SessionFactory sessionFactory, Class<?>... entities) {
		this.gridDialect = gridDialect;
		this.searchFactory = searchFactory;
		this.sessionFactory = sessionFactory;
		this.rootEntities = toRootEntities( searchFactory, entities );
		this.monitor = createMonitor( searchFactory );
	}

	private MassIndexerProgressMonitor createMonitor(SearchFactoryImplementor searchFactory) {
		return searchFactory.isJMXEnabled() ? new IndexingProgressMonitor() : new SimpleIndexingProgressMonitor();
	}

	@Override
	public MassIndexer threadsToLoadObjects(int numberOfThreads) {
		atLeastOneValidation( numberOfThreads );
		this.threadsToLoad = numberOfThreads;
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		this.batchSizeToLoad = batchSize;
		return this;
	}

	@Override
	public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
		atLeastOneValidation( numberOfThreads );
		this.threadForSubsequentFetching = numberOfThreads;
		return this;
	}

	@Override
	@Deprecated
	public MassIndexer threadsForIndexWriter(int numberOfThreads) {
		return this;
	}

	@Override
	public MassIndexer progressMonitor(MassIndexerProgressMonitor monitor) {
		this.monitor = monitor;
		return this;
	}

	@Override
	public MassIndexer cacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public MassIndexer optimizeOnFinish(boolean optimize) {
		this.optimizeOnFinish = optimize;
		return this;
	}

	@Override
	public MassIndexer optimizeAfterPurge(boolean optimize) {
		this.optimizeAfterPurge = optimize;
		return this;
	}

	@Override
	public MassIndexer purgeAllOnStart(boolean purgeAll) {
		this.purgeAllOnStart = purgeAll;
		return this;
	}

	@Override
	public MassIndexer limitIndexedObjectsTo(long maximum) {
		this.maximumIndexedObjects = maximum;
		return this;
	}

	@Override
	public MassIndexer idFetchSize(int idFetchSize) {
		this.idFetchSize = idFetchSize;
		return this;
	}

	@Override
	public Future<?> start() {
		ExecutorService executor = Executors.newFixedThreadPool( 1, "batch coordinator" );
		try {
			return executor.submit( createCoordinator() );
		}
		finally {
			executor.shutdown();
		}
	}

	@Override
	public void startAndWait() throws InterruptedException {
		BatchCoordinator coordinator = createCoordinator();
		coordinator.run();
	}

	protected BatchCoordinator createCoordinator() {
		return new BatchCoordinator( gridDialect, rootEntities, searchFactory, sessionFactory, threadsToLoad,
				threadForSubsequentFetching, cacheMode, batchSizeToLoad, maximumIndexedObjects, optimizeOnFinish,
				purgeAllOnStart, optimizeAfterPurge, monitor, idFetchSize );
	}

	private void atLeastOneValidation(int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "numberOfThreads must be at least 1" );
		}
	}

	/**
	 * From the set of classes a new set is built containing all indexed
	 * subclasses, but removing then all subtypes of indexed entities.
	 *
	 * @return a new set of entities
	 */
	private static Set<Class<?>> toRootEntities(SearchFactoryImplementor searchFactoryImplementor, Class<?>... selection) {
		Set<Class<?>> entities = new HashSet<Class<?>>();
		// first build the "entities" set containing all indexed subtypes of "selection".
		for ( Class<?> entityType : selection ) {
			Set<Class<?>> targetedClasses = searchFactoryImplementor
					.getIndexedTypesPolymorphic( new Class[] { entityType } );
			if ( targetedClasses.isEmpty() ) {
				String msg = entityType.getName() + " is not an indexed entity or a subclass of an indexed entity";
				throw new IllegalArgumentException( msg );
			}
			entities.addAll( targetedClasses );
		}
		Set<Class<?>> cleaned = new HashSet<Class<?>>();
		Set<Class<?>> toRemove = new HashSet<Class<?>>();
		// now remove all repeated types to avoid duplicate loading by polymorphic query loading
		for ( Class<?> type : entities ) {
			boolean typeIsOk = true;
			for ( Class<?> existing : cleaned ) {
				if ( existing.isAssignableFrom( type ) ) {
					typeIsOk = false;
					break;
				}
				if ( type.isAssignableFrom( existing ) ) {
					toRemove.add( existing );
				}
			}
			if ( typeIsOk ) {
				cleaned.add( type );
			}
		}
		cleaned.removeAll( toRemove );
		log.debugf( "Targets for indexing job: %s", cleaned );
		return cleaned;
	}

}
