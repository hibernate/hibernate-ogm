/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

@Indexed
@Entity
public class WithEmbedded {

	@Field
	@Id
	Long id;

	@Embedded
	AnEmbeddable anEmbeddable;

	public WithEmbedded() {
	}

	public WithEmbedded(Long id, AnEmbeddable anEmbeddable) {
		this.id = id;
		this.anEmbeddable = anEmbeddable;
	}

	public AnEmbeddable getAnEmbeddable() {
		return anEmbeddable;
	}

}
