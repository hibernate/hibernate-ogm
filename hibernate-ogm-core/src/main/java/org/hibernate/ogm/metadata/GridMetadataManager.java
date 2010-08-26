/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.metadata;

import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.CacheManager;
import org.infinispan.manager.DefaultCacheManager;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.ogm.cfg.impl.Version;
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
	private CacheContainer manager;
	private final TypeTranslator typeTranslator;
	private GridDialect gridDialect;

	public GridMetadataManager() {
		Version.touch();
		typeTranslator = new TypeTranslator();
		gridDialect = new InfinispanDialect();
	}

	@Override
	public void sessionFactoryCreated(SessionFactory factory) {
		manager = new DefaultCacheManager( );
		manager.start();
	}

	//TODO abstract to other grids
	public CacheContainer getCacheContainer() { return manager; }

	//TODO move to a *Implementor interface
	public TypeTranslator getTypeTranslator() { return typeTranslator; }

	public GridDialect getGridDialect() { return gridDialect; }

	@Override
	public void sessionFactoryClosed(SessionFactory factory) {
		manager.stop();
	}
}
