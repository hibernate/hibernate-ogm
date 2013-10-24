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
package org.hibernate.ogm.jpa.impl;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.jpa.HibernateQuery;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.spi.AbstractEntityManagerImpl;

/**
 * Hibernate OGM implementation of both {@link HibernateQuery} and {@link TypedQuery}
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class OgmNativeQuery<X> extends QueryImpl<X> implements HibernateQuery, TypedQuery<X> {

	public OgmNativeQuery(org.hibernate.Query query, EntityManager em) {
		super( query, convert( em ) );
	}

	private static AbstractEntityManagerImpl convert(EntityManager em) {
		if ( AbstractEntityManagerImpl.class.isInstance( em ) ) {
			return (AbstractEntityManagerImpl) em;
		}
		throw new IllegalStateException( String.format( "Unknown entity manager type [%s]", em.getClass().getName() ) );
	}

}
