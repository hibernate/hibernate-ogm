/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.datastore.ogm.orientdb.dialect.impl;

import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.EntityKeyMetadata;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;

/**
 * @author Sergey Chernolyas (sergey.chernolyas@gmail.com)
 */
public class OrientDBAssociationQueries extends QueriesBase {

	private static final Log log = LoggerFactory.make();

	private final EntityKeyMetadata ownerEntityKeyMetadata;
	private final AssociationKeyMetadata associationKeyMetadata;

	public OrientDBAssociationQueries(EntityKeyMetadata ownerEntityKeyMetadata, AssociationKeyMetadata associationKeyMetadata) {
		this.ownerEntityKeyMetadata = ownerEntityKeyMetadata;
		this.associationKeyMetadata = associationKeyMetadata;
	}
}
