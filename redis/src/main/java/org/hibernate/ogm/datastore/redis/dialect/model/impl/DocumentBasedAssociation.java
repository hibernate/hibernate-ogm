/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.redis.dialect.model.impl;

import java.util.Collections;
import java.util.List;

import org.hibernate.ogm.datastore.redis.dialect.value.Association;
import org.hibernate.ogm.datastore.redis.dialect.value.StructuredValue;


/**
 * A {@link RedisAssociation} backed by an {@link Association}.
 *
 * @author Gunnar Morling
 */
class DocumentBasedAssociation extends RedisAssociation {

	private final Association document;

	public DocumentBasedAssociation(Association document) {
		this.document = document;
	}

	@Override
	public Object getRows() {
		return document.getRows();
	}

	@Override
	public void setRows(Object rows) {
		if ( rows instanceof List ) {
			document.setRows( (List) rows );
		}
		else {
			document.setRows( Collections.singletonList( rows ) );
		}
	}

	@Override
	public StructuredValue getOwningDocument() {
		return document;
	}
}
