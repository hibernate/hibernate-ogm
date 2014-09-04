/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.loader.impl;

import org.hibernate.loader.CollectionAliases;
import org.hibernate.ogm.persister.impl.OgmCollectionPersister;

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
