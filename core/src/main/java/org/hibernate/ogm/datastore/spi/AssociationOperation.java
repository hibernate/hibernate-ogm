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
package org.hibernate.ogm.datastore.spi;

import org.hibernate.ogm.grid.RowKey;

/**
 * Operation applied to the association.
 * A RowKey is provided and when it makes sense a Tuple
 * (eg DELETE or PUT_NULL do not have tuples)
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */

public class AssociationOperation {
	private final RowKey key;
	private final Tuple value;
	private final AssociationOperationType type;

	public AssociationOperation(RowKey key, Tuple value, AssociationOperationType type) {
		this.key = key;
		this.value = value;
		this.type = type;
	}

	public RowKey getKey() {
		return key;
	}

	public Tuple getValue() {
		return value;
	}

	public AssociationOperationType getType() {
		return type;
	}
}
