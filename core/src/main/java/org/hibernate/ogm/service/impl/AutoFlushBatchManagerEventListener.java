/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.service.impl;

import org.hibernate.HibernateException;
import org.hibernate.event.service.spi.DuplicationStrategy;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.ogm.dialect.BatchOperationsDelegator;

/**
 * Prepares and executes batched operations when an {@link AutoFlushEvent} is caught
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class AutoFlushBatchManagerEventListener extends BatchManagerEventListener<AutoFlushEventListener, AutoFlushEvent> implements AutoFlushEventListener {

	public AutoFlushBatchManagerEventListener(BatchOperationsDelegator gridDialect) {
		super( gridDialect );
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		onEvent( event );
	}

	@Override
	void delegate(AutoFlushEventListener delegate, AutoFlushEvent event) {
		delegate.onAutoFlush( event );
	}

	/**
	 * Replace the original {@link AutoFlushEventListener} and use it as delegate
	 */
	public static class AutoFlushDuplicationStrategy implements DuplicationStrategy {

		@Override
		public boolean areMatch(Object listener, Object original) {
			boolean match = original instanceof AutoFlushEventListener && listener instanceof AutoFlushBatchManagerEventListener;
			if ( match ) {
				( (AutoFlushBatchManagerEventListener) listener ).setDelegate( (AutoFlushEventListener) original );
			}
			return match;
		}

		@Override
		public Action getAction() {
			return Action.REPLACE_ORIGINAL;
		}

	}

}
