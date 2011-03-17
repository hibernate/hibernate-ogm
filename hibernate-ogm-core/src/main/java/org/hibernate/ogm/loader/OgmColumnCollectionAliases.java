/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2010-2011 Red Hat Inc. and/or its affiliates and other contributors
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
