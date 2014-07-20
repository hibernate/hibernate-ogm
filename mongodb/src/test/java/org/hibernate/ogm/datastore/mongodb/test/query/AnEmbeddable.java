/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.query;

import javax.persistence.Embeddable;

@Embeddable
public class AnEmbeddable {

	String embeddedString;

	public AnEmbeddable() {
	}

	public AnEmbeddable(String embeddedString) {
		this.embeddedString = embeddedString;
	}

}
