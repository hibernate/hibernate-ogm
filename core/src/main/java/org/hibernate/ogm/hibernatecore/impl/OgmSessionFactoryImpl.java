/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.sql.Connection;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.engine.spi.SessionFactoryDelegatingImpl;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.engine.spi.OgmSessionBuilderImplementor;
import org.hibernate.ogm.engine.spi.OgmSessionFactoryImplementor;
import org.hibernate.ogm.exception.NotSupportedException;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmSessionFactoryImpl extends SessionFactoryDelegatingImpl implements OgmSessionFactoryImplementor {

	public OgmSessionFactoryImpl(SessionFactoryImplementor delegate) {
		super( delegate );
	}

	@Override
	public OgmSession openTemporarySession() throws HibernateException {
		return new OgmSessionImpl( this, (EventSource) getDelegate().openTemporarySession() );
	}

	@Override
	public OgmSessionBuilderImplementor withOptions() {
		return new OgmSessionBuilderDelegator( getDelegate().withOptions(), this );
	}

	@Override
	public OgmSession openSession() throws HibernateException {
		final Session session = getDelegate().openSession();
		return new OgmSessionImpl( this, (EventSource) session );
	}

	@Override
	public OgmSession getCurrentSession() throws HibernateException {
		final Session session = getDelegate().getCurrentSession();
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
		//Expect Hibernate Core to use one StringRefAddr based address
		String uuid = String.valueOf( getDelegate().getReference().get( 0 ).getContent() );
		return new Reference(
				OgmSessionFactoryImpl.class.getName(),
				new StringRefAddr( "uuid", uuid ),
				OgmSessionFactoryObjectFactory.class.getName(),
				null
				);
	}
}
