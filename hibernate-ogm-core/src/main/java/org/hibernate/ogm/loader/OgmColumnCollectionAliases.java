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
package org.hibernate.ogm.loader;

import org.hibernate.loader.CollectionAliases;
import org.hibernate.ogm.persister.OgmCollectionPersister;

/**
 * Return the column value for each element. ie we don't return any aliasing
 *
 * @author Emmanuel Bernard
 */
public class OgmColumnCollectionAliases implements CollectionAliases {
	private final String[] keyAliases;
	private final String[] indexAliases;
	private final String[] elementAliases;
	private final String identifierAlias;

	public OgmColumnCollectionAliases(OgmCollectionPersister persister) {
		keyAliases = persister.getKeyColumnNames();
		indexAliases = persister.getIndexColumnNames();
		elementAliases = persister.getElementColumnNames();
		identifierAlias = persister.getIdentifierColumnName();
	}

	@Override
	public String[] getSuffixedKeyAliases() {
		return keyAliases;
	}

	@Override
	public String[] getSuffixedIndexAliases() {
		return indexAliases;
	}

	@Override
	public String[] getSuffixedElementAliases() {
		return elementAliases;
	}

	@Override
	public String getSuffixedIdentifierAlias() {
		return identifierAlias;
	}

	@Override
	public String getSuffix() {
		return "";
	}

}
