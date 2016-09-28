/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.neo4j.remote.common.request.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Collects a group of {@link RemoteStatement}.
 *
 * @author Davide D'Alto
 */
public class RemoteStatements implements Iterable<RemoteStatement> {

	private final List<RemoteStatement> statements = new ArrayList<>();

	@Override
	public Iterator<RemoteStatement> iterator() {
		return Collections.unmodifiableCollection( statements ).iterator();
	}

	public RemoteStatements addStatement(RemoteStatement statement) {
		statements.add( statement );
		return this;
	}
}
