/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
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

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.event.internal.DefaultFlushEventListener;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.AutoFlushEventListener;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.FlushEvent;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.util.impl.CoreLogCategories;
import org.hibernate.ogm.util.impl.Log;

/**
 * @author Guillaume Scheibel <guillaume.scheibel@gmail.com>
 */
public class OgmEventListener implements FlushEventListener, AutoFlushEventListener {
	private GridDialect gridDialect;
	private static final Log log = Logger.getMessageLogger(
			Log.class, CoreLogCategories.DATASTORE_ACCESS.toString()
	);
	private Object delegate;

	public OgmEventListener(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
	}

	@Override
	public void onFlush(FlushEvent event) throws HibernateException {
		log.trace( "FlushEvent - begin" );
		gridDialect.prepareBatch();
		( (FlushEventListener) delegate ).onFlush( event );
		gridDialect.executeBatch();
		log.trace( "FlushEvent - end" );
	}

	@Override
	public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
		log.trace( "AutoFlushEvent - begin" );
		gridDialect.prepareBatch();
		( (AutoFlushEventListener) delegate ).onAutoFlush( event );
		gridDialect.executeBatch();
		log.trace( "AutoFlushEvent - end" );
	}

	public void setDelegate(Object delegate) {
		this.delegate = delegate;
	}
}
