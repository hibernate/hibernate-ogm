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

import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.ogm.dialect.BatchableGridDialect;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * Contains the methods to signal to a {@link BatchableGridDialect} when to prepare for the execution of batch
 * operations and when to execute them
 *
 * @author Davide D'Alto <davide@hibernate.org>
 * @param <L> the type of the delegate
 */
abstract class BatchManagerEventListener<L, E extends AbstractEvent> {

	private static final Log log = LoggerFactory.make();

	private final BatchableGridDialect gridDialect;

	private L delegate;

	/**
	 * @param gridDialect the dialect that can execute batch operations
	 */
	public BatchManagerEventListener(BatchableGridDialect gridDialect) {
		this.gridDialect = gridDialect;
	}

	void onEvent(E event) {
		try {
			log.tracef( "%s %s", event.getClass(), " - begin" );
			gridDialect.prepareBatch();
			delegate( delegate, event );
			gridDialect.executeBatch();
			log.tracef( "%s %s", event.getClass(), " - end" );
		}
		finally {
			gridDialect.clearBatch();
		}
	}

	abstract void delegate(L delegate, E event);

	public void setDelegate(L delegate) {
		this.delegate = delegate;
	}

}
