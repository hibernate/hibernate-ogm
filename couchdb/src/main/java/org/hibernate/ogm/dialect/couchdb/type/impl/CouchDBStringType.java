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
package org.hibernate.ogm.dialect.couchdb.type.impl;

import java.util.Comparator;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.type.StringType;
import org.hibernate.type.VersionType;

/**
 * A {@link StringType} which implemenets the {@link VersionType} contract and thus supports optimistic locking done by
 * the datastore.
 *
 * @author Gunnar Morling
 */
public class CouchDBStringType extends StringType implements VersionType<String> {

	@Override
	public String seed(SessionImplementor session) {
		return null;
	}

	@Override
	public String next(String current, SessionImplementor session) {
		return current;
	}

	@Override
	public Comparator<String> getComparator() {
		return ComparableComparator.INSTANCE;
	}
}
