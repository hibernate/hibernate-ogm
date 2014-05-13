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
import org.hibernate.SessionBuilder;
import org.hibernate.SessionEventListener;
import org.hibernate.SharedSessionBuilder;
import org.hibernate.event.spi.EventSource;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.OgmSessionFactory;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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
