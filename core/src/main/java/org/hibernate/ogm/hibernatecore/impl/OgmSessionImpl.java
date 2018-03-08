/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.lang.invoke.MethodHandles;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.StoredProcedureQuery;

import org.hibernate.Criteria;
import org.hibernate.Filter;
import org.hibernate.HibernateException;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.ScrollMode;
import org.hibernate.Session;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionDelegatorBaseImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.jdbc.Work;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.datastore.spi.DatastoreConfiguration;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
import org.hibernate.ogm.exception.NotSupportedException;
import org.hibernate.ogm.options.navigation.GlobalContext;
import org.hibernate.ogm.storedprocedure.impl.NoSQLProcedureCallMemento;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureCallMemento;
import org.hibernate.procedure.internal.NoSQLProcedureCallImpl;
import org.hibernate.query.Query;
import org.hibernate.query.spi.NamedQueryRepository;
import org.hibernate.query.spi.ScrollableResultsImplementor;

/**
 * An OGM specific session implementation which delegates most of the work to the underlying Hibernate ORM {@code Session},
 * except queries which are redirected to the OGM engine.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSessionImpl extends SessionDelegatorBaseImpl implements OgmSession, EventSource {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private final OgmSessionFactoryImpl factory;

	public OgmSessionImpl(OgmSessionFactory factory, EventSource delegate) {
		super( delegate );
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

	/**
	 * In ORM 5.2, it is not possible anymore to call getTransaction() in a JTA environment anymore.
	 *
	 * A lot of our shared tests rely on this and we want to keep them working for Neo4j.
	 */
	@Override
	public Transaction getTransaction() {
		return ( (SharedSessionContractImplementor) delegate ).accessTransaction();
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
	public ScrollableResultsImplementor scroll(Criteria criteria, ScrollMode scrollMode) {
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
		final ProcedureCallMemento memento = factory.getNamedQueryRepository().getNamedProcedureCallMemento( name );
		if ( memento == null ) {
			throw new IllegalArgumentException(
					"Could not find named stored procedure call with that registration name : " + name
			);
		}
		return new NoSQLProcedureCallImpl( this, new NoSQLProcedureCallMemento( memento ) );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName) {
		return new NoSQLProcedureCallImpl( this, procedureName );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, Class... resultClasses) {
		return new NoSQLProcedureCallImpl( this, procedureName, resultClasses );
	}

	@Override
	public ProcedureCall createStoredProcedureCall(String procedureName, String... resultSetMappings) {
		return new NoSQLProcedureCallImpl( this, procedureName, resultSetMappings );
	}

	@Override
	public StoredProcedureQuery createNamedStoredProcedureQuery(String name) {
		checkOpen();
		NamedQueryRepository namedQueryRepository = getSessionFactory().getNamedQueryRepository();
		ProcedureCallMemento memento =  namedQueryRepository.getNamedProcedureCallMemento( name );

		if ( memento == null ) {
			throw new IllegalArgumentException( "No @NamedStoredProcedureQuery was found with that name : " + name );
		}

		NoSQLProcedureCallMemento nosqlMemento = new NoSQLProcedureCallMemento( memento );
		return nosqlMemento.makeProcedureCall( this );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName) {
		checkOpen();
		return createStoredProcedureCall( procedureName );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, Class... resultClasses) {
		checkOpen();
		return createStoredProcedureCall( procedureName, resultClasses );
	}

	@Override
	public StoredProcedureQuery createStoredProcedureQuery(String procedureName, String... resultSetMappings) {
		checkOpen();
		return createStoredProcedureCall( procedureName, resultSetMappings );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public SharedSessionBuilder sessionWithOptions() {
		return new OgmSharedSessionBuilderDelegator( delegate.sessionWithOptions(), factory );
	}

	public <G extends GlobalContext<?, ?>, D extends DatastoreConfiguration<G>> G configureDatastore(Class<D> datastoreType) {
		throw new UnsupportedOperationException( "OGM-343 Session specific options are not currently supported" );
	}

	@Override
	public void removeOrphanBeforeUpdates(String entityName, Object child) {
		delegate.removeOrphanBeforeUpdates( entityName, child );
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

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clazz) {
		checkOpen();

		if ( Session.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( SessionImplementor.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( SharedSessionContractImplementor.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( OgmSession.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}
		if ( EntityManager.class.isAssignableFrom( clazz ) ) {
			return (T) this;
		}

		throw new PersistenceException( "Hibernate OGM cannot unwrap " + clazz );
	}
}
