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
package org.hibernate.ogm.options.mongodb;

import org.hibernate.ogm.datastore.mongodb.AssociationStorage;
import org.hibernate.ogm.options.UniqueOption;

/**
 * @author Davide D'Alto <davide@hibernate.org>
 */
public class AssociationStorageOption extends UniqueOption<AssociationStorageOption> {

	private final AssociationStorage associationStorage;

	public AssociationStorageOption(AssociationStorage associationStorage) {
		this.associationStorage = associationStorage;
	}

	public AssociationStorage getAssociationStorage() {
		return associationStorage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ( ( associationStorage == null ) ? 0 : associationStorage.hashCode() );
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals( obj ) ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		AssociationStorageOption other = (AssociationStorageOption) obj;
		if ( associationStorage != other.associationStorage ) {
			return false;
		}
		return true;
	}

}
