/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.spi.GridDialect;
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
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class OgmMassIndexer implements MassIndexer {

	private static final Log log = LoggerFactory.make();

	private final SearchFactoryImplementor searchFactory;
	private final SessionFactoryImplementor sessionFactory;
	private final GridDialect gridDialect;

	private MassIndexerProgressMonitor monitor;
	private CacheMode cacheMode = CacheMode.IGNORE;
	private boolean optimizeOnFinish = true;
	private boolean optimizeAfterPurge = true;
	private boolean purgeAllOnStart = true;
	private int typesToIndexInParallel = 1;

	private final Set<Class<?>> rootEntities;

	public OgmMassIndexer(GridDialect gridDialect, SearchFactoryImplementor searchFactory, SessionFactoryImplementor sessionFactory, Class<?>... entities) {
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
		log.unsupportedIndexerConfigurationOption( "threadsToLoadObjects" );
		return this;
	}

	@Override
	public MassIndexer batchSizeToLoadObjects(int batchSize) {
		log.unsupportedIndexerConfigurationOption( "batchSizeToLoadObjects" );
		return this;
	}

	@Override
	public MassIndexer threadsForSubsequentFetching(int numberOfThreads) {
		log.unsupportedIndexerConfigurationOption( "threadForSubsequentFetching" );
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
		log.unsupportedIndexerConfigurationOption( "limitIndexedObjectsTo" );
		return this;
	}

	@Override
	public MassIndexer idFetchSize(int idFetchSize) {
		log.unsupportedIndexerConfigurationOption( "idFetchSize" );
		return this;
	}

	@Override
	public MassIndexer typesToIndexInParallel(int threadsToIndexObjects) {
		atLeastOneValidation( threadsToIndexObjects );
		this.typesToIndexInParallel = Math.min( threadsToIndexObjects, rootEntities.size() );
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
		return new BatchCoordinator( gridDialect, rootEntities, searchFactory, sessionFactory, typesToIndexInParallel, cacheMode, optimizeOnFinish,
				purgeAllOnStart, optimizeAfterPurge, monitor );
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
