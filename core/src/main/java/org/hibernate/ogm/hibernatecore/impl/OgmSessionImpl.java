/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.engine.query.spi.sql.NativeSQLQuerySpecification;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerGroup;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.loader.nativeloader.impl.BackendCustomQuery;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.query.Query;
import org.hibernate.resource.transaction.backend.jta.internal.JtaTransactionCoordinatorImpl;

/**
 * An OGM specific session implementation which delegates most of the work to the underlying Hibernate ORM {@code Session},
 * except queries which are redirected to the OGM engine.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSessionImpl extends SessionDelegatorBaseImpl implements OgmSession, EventSource {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final EventSource delegate;
	private final OgmSessionFactoryImpl factory;

	public OgmSessionImpl(OgmSessionFactory factory, EventSource delegate) {
		super( delegate );
		this.delegate = delegate;
		this.factory = (OgmSessionFactoryImpl) factory;
	}

	//Overridden methods
	@Override
	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	@Override
	public OgmSessionFactoryImplementor getSessionFactory() {
		return factory;
	}

	@Override
	public Criteria createCriteria(Class persistentClass) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Criteria createCriteria(Class persistentClass, String alias) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Criteria createCriteria(String entityName) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Criteria createCriteria(String entityName, String alias) {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-23", "Criteria queries are not supported yet" );
	}

	@Override
	public Query createFilter(Object collection, String queryString) throws HibernateException {
		//TODO plug the Lucene engine
		throw new NotSupportedException( "OGM-24", "filters are not supported yet" );
	}

	@Override
	public Filter enableFilter(String filterName) {
		throw new NotSupportedException( "OGM-25", "filters are not supported yet" );
	}

	@Override
	public void disableFilter(String filterName) {
		throw new NotSupportedException( "OGM-25", "filters are not supported yet" );
	}

	@Override
	public void doWork(Work work) throws HibernateException {
		throw new IllegalStateException( "Hibernate OGM does not support SQL Connections hence no Work" );
	}

	@Override
	public ProcedureCall getNamedProcedureCall(String name) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		throw new NotSupportedException( "OGM-359", "Stored procedures are not supported yet" );
	}

	@Override
	public List<?> listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) throws HibernateException {
		checkOpen();
		checkTransactionSynchStatus();

		if ( log.isTraceEnabled() ) {
			log.tracev( "NoSQL query: {0}", customQuery.getSQL() );
		}

		BackendCustomLoader loader = new BackendCustomLoader( (BackendCustomQuery<?>) customQuery, getFactory() );
		autoFlushIfRequired( loader.getQuerySpaces() );

		return loader.list( getDelegate(), queryParameters );
	}

	/**
	 * detect in-memory changes, determine if the changes are to tables named in the query and, if so, complete
	 * execution the flush
	 * <p>
	 * NOTE: Copied as-is from {@link SessionImpl}. We need it here as
	 * {@link #listCustomQuery(CustomQuery, QueryParameters)} needs to be customized (which makes use of auto flushes)
	 * to work with our custom loaders.
	 */
	private boolean autoFlushIfRequired(Set<String> querySpaces) throws HibernateException {
		if ( ! isTransactionInProgress() ) {
			// do not auto-flush while outside a transaction
			return false;
		}
		AutoFlushEvent event = new AutoFlushEvent( querySpaces, getDelegate() );
		for ( AutoFlushEventListener listener : listeners( EventType.AUTO_FLUSH ) ) {
			listener.onAutoFlush( event );
		}
		return event.isFlushRequired();
	}

	private <T> Iterable<T> listeners(EventType<T> type) {
		return eventListenerGroup( type ).listeners();
	}

	private <T> EventListenerGroup<T> eventListenerGroup(EventType<T> type) {
		return factory.getServiceRegistry().getService( EventListenerRegistry.class ).getEventListenerGroup( type );
	}

	@Override
	public List<?> list(NativeSQLQuerySpecification spec, QueryParameters queryParameters) throws HibernateException {
		return listCustomQuery(
				factory.getQueryPlanCache().getNativeSQLQueryPlan( spec ).getCustomQuery(),
				queryParameters
		);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new OgmSharedSessionBuilderDelegator( delegate.sessionWithOptions(), factory );
	}

	// Copied from org.hibernate.internal.AbstractSharedSessionContract.checkOpen() to mimic same behaviour
	@Override
	public void checkOpen(boolean markForRollbackIfClosed) {
		if ( isClosed() ) {
			if ( markForRollbackIfClosed && delegate.getTransactionCoordinator().isTransactionActive() ) {
				markForRollbackOnly();
			}
			throw new IllegalStateException( "Session/EntityManager is closed" );
		}
	}

	// Copied from org.hibernate.internal.AbstractSharedSessionContract.checkTransactionSynchStatus() to mimic same behaviour
	private void checkTransactionSynchStatus() {
		pulseTransactionCoordinator();
		delayedAfterCompletion();
	}

	// Copied from org.hibernate.internal.AbstractSharedSessionContract.pulseTransactionCoordinator() to mimic same behaviour
	private void pulseTransactionCoordinator() {
		if ( !isClosed() ) {
			delegate.getTransactionCoordinator().pulse();
		}
	}

	// Copied from org.hibernate.internal.AbstractSharedSessionContract.delayedAfterCompletion() to mimic same behaviour
	protected void delayedAfterCompletion() {
		if ( delegate.getTransactionCoordinator() instanceof JtaTransactionCoordinatorImpl ) {
			( (JtaTransactionCoordinatorImpl) delegate.getTransactionCoordinator() ).getSynchronizationCallbackCoordinator()
					.processAnyDelayedAfterCompletion();
		}
	}

	public <G extends GlobalContext<?, ?>, D extends DatastoreConfiguration<G>> G configureDatastore(Class<D> datastoreType) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		delegate.removeOrphanBeforeUpdates( entityName, child );
	}

	/**
	 * Returns the underlying ORM session to which most work is delegated.
	 *
	 * @return the underlying session
	 */
	@Override
	public EventSource getDelegate() {
		return delegate;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public NaturalIdLoadAccess byNaturalId(Class entityClass) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public NaturalIdLoadAccess byNaturalId(String entityName) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(Class entityClass) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SimpleNaturalIdLoadAccess bySimpleNaturalId(String entityName) {
		throw new UnsupportedOperationException( "OGM-589 - Natural id look-ups are not yet supported" );
	}
}
