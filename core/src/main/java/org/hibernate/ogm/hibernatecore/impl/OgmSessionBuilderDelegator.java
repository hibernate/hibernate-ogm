/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.hibernatecore.impl;

import java.sql.Connection;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.hibernate.engine.spi.SessionOwner;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;
import org.hibernate.ogm.OgmSessionFactory.OgmSessionBuilderImplementor;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSessionBuilderDelegator implements OgmSessionBuilderImplementor {
	private final SessionBuilderImplementor builder;
	private final OgmSessionFactory factory;


	public OgmSessionBuilderDelegator(SessionBuilderImplementor sessionBuilder, OgmSessionFactory factory) {
		this.builder = sessionBuilder;
		this.factory = factory;
	}

	@Override
	public OgmSession openSession() {
		Session session = builder.openSession();
		return new OgmSessionImpl( factory, (EventSource) session );
	}

	@Override
	public SessionBuilder interceptor(Interceptor interceptor) {
		builder.interceptor( interceptor );
		return this;
	}

	@Override
	public SessionBuilder noInterceptor() {
		builder.noInterceptor();
		return this;
	}

	@Override
	public SessionBuilder connection(Connection connection) {
		builder.connection( connection );
		return this;
	}

	@Override
	public SessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		builder.connectionReleaseMode( connectionReleaseMode );
		return this;
	}

	@Override
	public SessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		builder.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	@Deprecated
	public SessionBuilder autoClose(boolean autoClose) {
		builder.autoClose( autoClose );
		return this;
	}

	@Override
	public SessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
		builder.flushBeforeCompletion( flushBeforeCompletion );
		return this;
	}

	@Override
	public SessionBuilder tenantIdentifier(String tenantIdentifier) {
		builder.tenantIdentifier( tenantIdentifier );
		return this;
	}

	@Override
	public SessionBuilder owner(SessionOwner sessionOwner) {
		builder.owner( sessionOwner );
		return this;
	}

	@Override
	public SessionBuilder eventListeners(SessionEventListener... listeners) {
		builder.eventListeners( listeners );
		return this;
	}

	@Override
	public SessionBuilder clearEventListeners() {
		builder.clearEventListeners();
		return this;
	}
}
