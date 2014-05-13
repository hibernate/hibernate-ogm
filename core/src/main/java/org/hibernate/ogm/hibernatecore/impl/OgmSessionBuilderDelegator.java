/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
import org.hibernate.ogm.OgmSessionFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmSessionBuilderDelegator implements SessionBuilderImplementor {
	private final SessionBuilderImplementor builder;
	private final OgmSessionFactory factory;


	public OgmSessionBuilderDelegator(SessionBuilderImplementor sessionBuilder, OgmSessionFactory factory) {
		this.builder = sessionBuilder;
		this.factory = factory;
	}

	@Override
	public Session openSession() {
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
