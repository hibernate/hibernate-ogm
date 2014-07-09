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
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;

/**
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class OgmSharedSessionBuilderDelegator implements SharedSessionBuilder {
	private final SharedSessionBuilder builder;
	private final OgmSessionFactory factory;

	public OgmSharedSessionBuilderDelegator(SharedSessionBuilder builder, OgmSessionFactory factory) {
		this.builder = builder;
		this.factory = factory;
	}

	@Override
	public SharedSessionBuilder interceptor() {
		builder.interceptor();
		return this;
	}

	@Override
	public SharedSessionBuilder connection() {
		builder.connection();
		return this;
	}

	@Override
	public SharedSessionBuilder connectionReleaseMode() {
		builder.connectionReleaseMode();
		return this;
	}

	@Override
	public SharedSessionBuilder autoJoinTransactions() {
		builder.autoJoinTransactions();
		return this;
	}

	@Override
	@Deprecated
	public SharedSessionBuilder autoClose() {
		builder.autoClose();
		return this;
	}

	@Override
	public SharedSessionBuilder flushBeforeCompletion() {
		builder.flushBeforeCompletion();
		return this;
	}

	@Override
	public SharedSessionBuilder transactionContext() {
		builder.transactionContext();
		return this;
	}

	@Override
	public OgmSession openSession() {
		return new OgmSessionImpl( factory, (EventSource) builder.openSession() );
	}

	@Override
	public SharedSessionBuilder interceptor(Interceptor interceptor) {
		builder.interceptor( interceptor );
		return this;
	}

	@Override
	public SharedSessionBuilder noInterceptor() {
		builder.noInterceptor();
		return this;
	}

	@Override
	public SharedSessionBuilder connection(Connection connection) {
		builder.connection( connection );
		return this;
	}

	@Override
	public SharedSessionBuilder connectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		builder.connectionReleaseMode( connectionReleaseMode );
		return this;
	}

	@Override
	public SharedSessionBuilder autoJoinTransactions(boolean autoJoinTransactions) {
		builder.autoJoinTransactions( autoJoinTransactions );
		return this;
	}

	@Override
	public SharedSessionBuilder autoClose(boolean autoClose) {
		builder.autoClose( autoClose );
		return this;
	}

	@Override
	public SharedSessionBuilder flushBeforeCompletion(boolean flushBeforeCompletion) {
		builder.flushBeforeCompletion( flushBeforeCompletion );
		return this;
	}

	@Override
	public SessionBuilder tenantIdentifier(String tenantIdentifier) {
		builder.tenantIdentifier( tenantIdentifier );
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
