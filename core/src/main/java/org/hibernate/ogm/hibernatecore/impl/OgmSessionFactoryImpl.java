/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.persistence.SynchronizationType;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.engine.jndi.spi.JndiService;
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.SessionFactoryRegistry.ObjectFactoryImpl;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.engine.spi.OgmSessionBuilderImplementor;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
import org.hibernate.ogm.exception.NotSupportedException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmSessionFactoryImpl extends SessionFactoryDelegatingImpl implements OgmSessionFactoryImplementor {

	private static final IdentifierGenerator UUID_GENERATOR = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();

	private final String uuid;

	public OgmSessionFactoryImpl(SessionFactoryImplementor delegate) {
		super( delegate );

		try {
			uuid = (String) UUID_GENERATOR.generate( null, null );
		}
		catch (Exception e) {
			throw new AssertionFailure( "Could not generate UUID" );
		}

		SessionFactoryRegistry.INSTANCE.addSessionFactory(
				uuid,
				delegate.getName(),
				delegate.getSessionFactoryOptions().isSessionFactoryNameAlsoJndiName(),
				this,
				delegate.getServiceRegistry().getService( JndiService.class )
		);
	}

	@Override
	public OgmSession openTemporarySession() throws HibernateException {
		return new OgmSessionImpl( this, (EventSource) delegate().openTemporarySession() );
	}

	@Override
	public OgmSessionBuilderImplementor withOptions() {
		return new OgmSessionBuilderDelegator( delegate().withOptions(), this );
	}

	@Override
	public OgmSession openSession() throws HibernateException {
		final Session session = delegate().openSession();
		return new OgmSessionImpl( this, (EventSource) session );
	}

	@Override
	public OgmSession getCurrentSession() throws HibernateException {
		final Session session = delegate().getCurrentSession();
		return new OgmSessionImpl( this, (EventSource) session );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		throw new NotSupportedException( "OGM-18", "Stateless session is not implemented in OGM" );
	}

	@Override
	public StatelessSession openStatelessSession() {
		throw new NotSupportedException( "OGM-18", "Stateless session is not implemented in OGM" );
	}

	@Override
	public StatelessSession openStatelessSession(Connection connection) {
		throw new NotSupportedException( "OGM-18", "Stateless session is not implemented in OGM" );
	}

	@Override
	public Reference getReference() throws NamingException {
		return new Reference(
				getClass().getName(),
				new StringRefAddr( "uuid", uuid ),
				ObjectFactoryImpl.class.getName(),
				null
		);
	}

	@Override
	public EntityManager createEntityManager() {
		return new OgmSessionImpl( this, (EventSource) delegate().createEntityManager() );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EntityManager createEntityManager(Map map) {
		return new OgmSessionImpl( this, (EventSource) delegate().createEntityManager( map ) );
	}

	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType) {
		return new OgmSessionImpl( this, (EventSource) delegate().createEntityManager( synchronizationType ) );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
		return new OgmSessionImpl( this, (EventSource) delegate().createEntityManager( synchronizationType, map ) );
	}

	@Override
	public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public EntityGraph findEntityGraphByName(String name) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@Override
	public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
		throw new IllegalStateException( "Hibernate OGM does not support entity graphs" );
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		if ( type.isAssignableFrom( SessionFactory.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( SessionFactoryImplementor.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( SessionFactoryImpl.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( OgmSessionFactory.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( OgmSessionFactoryImplementor.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( OgmSessionFactoryImpl.class ) ) {
			return type.cast( this );
		}

		if ( type.isAssignableFrom( EntityManagerFactory.class ) ) {
			return type.cast( this );
		}

		throw new PersistenceException( "Hibernate cannot unwrap EntityManagerFactory as '" + type.getName() + "'" );
	}
}
