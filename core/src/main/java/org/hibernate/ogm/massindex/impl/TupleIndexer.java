/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.massindex.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.loader.impl.OgmLoadingContext;
import org.hibernate.ogm.loader.impl.TupleBasedEntityLoader;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.persister.impl.OgmEntityPersister;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.hcore.util.impl.HibernateHelper;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;
import org.hibernate.search.orm.loading.impl.HibernateSessionLoadingInitializer;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeMap;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.logging.impl.Log;
import java.lang.invoke.MethodHandles;

/**
 * Component of batch-indexing pipeline, using chained producer-consumers.
 * <p>
 * This Runnable will consume {@link Tuple} objects taken one-by-one and it will create an {@link AddLuceneWork} for the
 * corresponding entity.
 *
 * @author Sanne Grinovero
 * @author Davide D'Alto
 */
public class TupleIndexer implements SessionAwareRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionFactoryImplementor sessionFactory;
	private final IndexedTypeMap<EntityIndexBinding> entityIndexBindings;
	private final MassIndexerProgressMonitor monitor;
	private final CacheMode cacheMode;
	private final BatchBackend backend;
	private final ErrorHandler errorHandler;
	private final IndexedTypeIdentifier indexedTypeIdentifier;
	private final ServiceManager serviceManager;
	private final String tenantId;

	public TupleIndexer(IndexedTypeIdentifier indexedTypeIdentifier, MassIndexerProgressMonitor monitor,
			SessionFactoryImplementor sessionFactory, ExtendedSearchIntegrator searchIntegrator,
			CacheMode cacheMode, BatchBackend backend, ErrorHandler errorHandler, String tenantId) {
		this.indexedTypeIdentifier = indexedTypeIdentifier;
		this.monitor = monitor;
		this.sessionFactory = sessionFactory;
		this.cacheMode = cacheMode;
		this.backend = backend;
		this.errorHandler = errorHandler;
		this.tenantId = tenantId; //can be null when multi-tenancy isn't being used
		this.entityIndexBindings = searchIntegrator.getIndexBindings();
		serviceManager = searchIntegrator.getServiceManager();
	}

	private void index(Session session, Object entity) {
		try {
			final InstanceInitializer sessionInitializer = new HibernateSessionLoadingInitializer(
					(SessionImplementor) session );
			final ConversionContext contextualBridge = new ContextualExceptionBridgeHelper();

			// trick to attach the objects to session:
			session.buildLockRequest( LockOptions.NONE ).lock( entity );
			index( entity, session, sessionInitializer, contextualBridge );
			monitor.documentsBuilt( 1 );
			session.clear();
		}
		catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
	}

	private void index(Object entity, Session session, InstanceInitializer sessionInitializer,
			ConversionContext conversionContext) throws InterruptedException {
		Class<?> clazz = HibernateHelper.getClass( entity );
		EntityIndexBinding entityIndexBinding = entityIndexBindings.get( clazz );
		// it might be possible to receive not-indexes subclasses of the currently indexed type;
		// being not-indexed, we skip them.
		// FIXME for improved performance: avoid loading them in an early phase.
		if ( entityIndexBinding != null ) {
			EntityIndexingInterceptor<?> interceptor = entityIndexBinding.getEntityIndexingInterceptor();
			if ( isNotSkippable( interceptor, entity ) ) {
				Serializable id = session.getIdentifier( entity );
				AddLuceneWork addWork = createAddLuceneWork( tenantId, entity, sessionInitializer, conversionContext, id,
						entityIndexBinding );
				backend.enqueueAsyncWork( addWork );
			}
		}
	}

	private AddLuceneWork createAddLuceneWork(String tenantIdentifier, Object entity, InstanceInitializer sessionInitializer,
			ConversionContext conversionContext, Serializable id, EntityIndexBinding entityIndexBinding) {
		DocumentBuilderIndexedEntity docBuilder = entityIndexBinding.getDocumentBuilder();
		String idInString = idInString( conversionContext, id, docBuilder.getTypeIdentifier(), docBuilder );
		// depending on the complexity of the object graph going to be indexed it's possible
		// that we hit the database several times during work construction.

		return docBuilder.createAddWork( tenantIdentifier, docBuilder.getTypeIdentifier(), entity, id, idInString, sessionInitializer, conversionContext );
	}

	private String idInString(ConversionContext conversionContext, Serializable id, IndexedTypeIdentifier typeIdentifier,
			DocumentBuilderIndexedEntity docBuilder) {
		conversionContext.pushProperty( docBuilder.getIdPropertyName() );
		try {
			String idInString = conversionContext.setConvertedTypeId( typeIdentifier ).twoWayConversionContext( docBuilder.getIdBridge() )
					.objectToString( id );
			return idInString;
		}
		finally {
			conversionContext.popProperty();
		}
	}

	private boolean isNotSkippable(EntityIndexingInterceptor interceptor, Object entity) {
		if ( interceptor == null ) {
			return true;
		}
		else {
			return !isSkippable( interceptor.onAdd( entity ) );
		}
	}

	private boolean isSkippable(IndexingOverride indexingOverride) {
		switch ( indexingOverride ) {
		case REMOVE:
		case SKIP:
			return true;
		default:
			return false;
		}
	}

	private Transaction beginTransaction(Session session) throws ClassNotFoundException, NoSuchMethodException,
			IllegalAccessException, InvocationTargetException {
		Transaction transaction = Helper.getTransactionAndMarkForJoin( session, serviceManager );
		transaction.begin();
		return transaction;
	}

	private Session openSession(Session upperSession) {
		Session session = upperSession;
		if ( upperSession == null ) {
			session = sessionFactory.openSession();
		}
		initSession( session );
		return session;
	}

	private void initSession(Session session) {
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		session.setDefaultReadOnly( true );
	}

	private void close(Session upperSession, Session session) {
		if ( upperSession == null ) {
			session.close();
		}
	}

	@Override
	public void run(Session upperSession, Tuple tuple) {
		if ( upperSession == null ) {
			runInNewTransaction( upperSession, tuple );
		}
		else {
			runIndexing( upperSession, tuple );
		}
	}

	/*
	 * Index using the existing session without opening new transactions
	 */
	private void runIndexing(Session upperSession, Tuple tuple) {
		initSession( upperSession );
		try {
			index( upperSession, entity( upperSession, tuple ) );
		}
		catch (Throwable e) {
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage(), e );
		}
		finally {
			log.debug( "finished" );
		}
	}

	private void runInNewTransaction(Session upperSession, Tuple tuple) {
		Session session = openSession( upperSession );
		try {
			Transaction transaction = beginTransaction( session );
			index( session, entity( session, tuple ) );
			transaction.commit();
		}
		catch ( Throwable e ) {
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage(), e );
		}
		finally {
			close( upperSession, session );
			log.debug( "finished" );
		}
	}

	private Object entity(Session session, Tuple tuple) {
		SessionImplementor sessionImplementor = (SessionImplementor) session;
		OgmEntityPersister persister = (OgmEntityPersister) sessionFactory.getMetamodel().entityPersister( indexedTypeIdentifier.getPojoType() );

		TupleBasedEntityLoader loader = (TupleBasedEntityLoader) persister.getAppropriateLoader( LockOptions.READ, sessionImplementor );

		OgmLoadingContext ogmLoadingContext = new OgmLoadingContext();
		ogmLoadingContext.setTuples( Collections.singletonList( tuple ) );
		List<Object> entities = loader.loadEntitiesFromTuples( sessionImplementor, LockOptions.NONE, ogmLoadingContext );

		return entities.get( 0 );
	}
}
