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
package org.hibernate.ogm.datastore.spi;

import java.util.List;

import org.hibernate.ogm.grid.AssociationKeyMetadata;
import org.hibernate.ogm.options.spi.OptionsContext;

/**
 * A context containing static information about a given tuple type, i.e. information which is constant between dialect
 * operations for the tuple with one and the same key.
 *
 * @author Gunnar Morling
 */
public class TupleTypeContext {

	private final List<String> selectableColumns;
	private final OptionsContext optionsContext;
	private final List<AssociationKeyMetadata> embeddedAssociations;

	public TupleTypeContext(List<String> selectableColumns, OptionsContext optionsContext, List<AssociationKeyMetadata> embeddedAssociations) {
		this.selectableColumns = selectableColumns;
		this.optionsContext = optionsContext;
		this.embeddedAssociations = embeddedAssociations;
	}

	/**
	 * Returns the mapped columns of the given entity. May be used by a dialect to only load those columns instead of
	 * the complete document/record.
	 */
	public List<String> getSelectableColumns() {
		return selectableColumns;
	}

	/**
	 * Returns a context object providing access to the options effectively applying for a given entity or property.
	 */
	public OptionsContext getOptionsContext() {
		return optionsContext;
	}

	public List<AssociationKeyMetadata> getEmbeddedAssociations() {
		return embeddedAssociations;
	}

	@Override
	public String toString() {
		return "TupleTypeContext [selectableColumns=" + selectableColumns + ", optionsContext=" + optionsContext + ", embeddedAssociations="
				+ embeddedAssociations + "]";
	}
}
