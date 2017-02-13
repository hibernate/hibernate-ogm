/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.orientdbremote;

import org.hibernate.ogm.datastore.orientdb.OrientDBDialect;
import org.hibernate.ogm.datastore.orientdb.logging.impl.Log;
import org.hibernate.ogm.datastore.orientdb.logging.impl.LoggerFactory;
import org.hibernate.ogm.datastore.orientdbremote.impl.RemoteOrientDBDatastoreProvider;
import org.hibernate.ogm.model.key.spi.AssociationKey;
import org.hibernate.ogm.model.key.spi.AssociationKeyMetadata;
import org.hibernate.ogm.model.key.spi.RowKey;
import org.hibernate.ogm.model.spi.Association;
import org.hibernate.ogm.model.spi.Tuple;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * Implementation of dialect for Remote OrientDB
 * <p>
 * A {@link Tuple} is saved as a {@link ODocument} where the columns are converted into properties of the node.<br>
 * In the version, an {@link Association} is stored like relation DBMS and identified by the {@link AssociationKey} and
 * the {@link RowKey}. The type of the relationship is the value returned by
 * {@link AssociationKeyMetadata#getCollectionRole()}.
 * <p>
 * If the value of a property is set to null the property will be removed (OrientDB does not allow to store null
 * values).
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */
@SuppressWarnings("serial")
public class RemoteOrientDBDialect extends OrientDBDialect {

	private static final Log log = LoggerFactory.getLogger();

	public RemoteOrientDBDialect(RemoteOrientDBDatastoreProvider provider) {
		super( provider );
	}

}
