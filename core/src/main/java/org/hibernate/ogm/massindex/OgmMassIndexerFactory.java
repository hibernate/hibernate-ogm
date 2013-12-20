/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.ogm.massindex;

import java.util.Properties;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.spi.MassIndexerFactory;

/**
 * {@link MassIndexerFactory} that can be used to register the {@link OgmMassIndexer} to Hibernate Search.
 *
 * @see org.hibernate.search.hcore.impl.MassIndexerFactoryIntegrator#MASS_INDEXER_FACTORY_CLASSNAME
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OgmMassIndexerFactory implements MassIndexerFactory {

	@Override
	public void initialize(Properties properties) {
	}

	@Override
	public MassIndexer createMassIndexer(SearchFactoryImplementor searchFactory, SessionFactoryImplementor sessionFactory,
			Class<?>... entities) {
		return new OgmMassIndexer( sessionFactory.getServiceRegistry().getService( GridDialect.class ), searchFactory, sessionFactory, entities );
	}
}
