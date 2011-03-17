/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.metadata;

import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.ogm.cfg.impl.Version;
import org.hibernate.ogm.datastore.infinispan.impl.CacheManagerServiceProvider;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.dialect.infinispan.InfinispanDialect;
import org.hibernate.ogm.type.TypeTranslator;

/**
 * Start and stop the Infinispan CacheManager with the SearchFactory
 * TODO abstract that to other grids
 *
 * @author Emmanuel Bernard
 */
public class GridMetadataManager implements SessionFactoryObserver {
	private CacheManagerServiceProvider manager;
	private final TypeTranslator typeTranslator;
	private GridDialect gridDialect;

	public GridMetadataManager() {
		Version.touch();
		typeTranslator = new TypeTranslator();
		gridDialect = new InfinispanDialect();
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor ) factory;
		manager = new CacheManagerServiceProvider();
		manager.start( factoryImplementor.getProperties() );
	}

	//TODO abstract to other grids
	public CacheContainer getCacheContainer() { return manager.getService(); }

	//TODO move to a *Implementor interface
	public TypeTranslator getTypeTranslator() { return typeTranslator; }

	public GridDialect getGridDialect() { return gridDialect; }

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		manager.stop();
	}
}
