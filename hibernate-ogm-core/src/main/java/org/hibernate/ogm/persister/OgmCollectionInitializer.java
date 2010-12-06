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
package org.hibernate.ogm.persister;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Emmanuel Bernard
 */
public class OgmCollectionInitializer implements CollectionInitializer {
	private final EntityPersister elementPersister;
	private OgmCollectionPersister ogmCollectionPersister;

	public OgmCollectionInitializer(OgmCollectionPersister ogmCollectionPersister) {
		this.ogmCollectionPersister = ogmCollectionPersister;
		if ( ogmCollectionPersister.isOneToMany() ) {
			elementPersister = ogmCollectionPersister.getElementPersister();
		}
		else {
			throw new NotYetImplementedException( "Non oneToMany collections not yet implemented" );
		}
	}

	@Override
	public void initialize(Serializable id, SessionImplementor session) throws HibernateException {

	}
}
