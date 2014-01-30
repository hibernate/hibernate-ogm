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
package org.hibernate.ogm.dialect.batch;

import org.hibernate.ogm.datastore.spi.AssociationContext;
import org.hibernate.ogm.grid.AssociationKey;

/**
 * An operation representing the removal of an association
 *
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class RemoveAssociationOperation implements Operation {

	private final AssociationKey associationKey;
	private final AssociationContext context;

	public RemoveAssociationOperation(AssociationKey associationKey, AssociationContext context) {
		this.associationKey = associationKey;
		this.context = context;
	}

	public AssociationKey getAssociationKey() {
		return associationKey;
	}

	public AssociationContext getContext() {
		return context;
	}

}
