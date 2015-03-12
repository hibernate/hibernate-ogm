/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.queries;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;

@Indexed
@Entity
public class WithEmbedded {

	@Id
	private Long id;

	@Embedded
	@IndexedEmbedded
	private AnEmbeddable anEmbeddable;

	@Embedded
	@IndexedEmbedded
	private AnEmbeddable yetAnotherEmbeddable;

	@ElementCollection
	@IndexedEmbedded
	private List<EmbeddedCollectionItem> anEmbeddedCollection;

	@ElementCollection
	@IndexedEmbedded
	private List<EmbeddedCollectionItem> anotherEmbeddedCollection;

	public WithEmbedded() {
	}

	public WithEmbedded(Long id, AnEmbeddable anEmbeddable) {
		this.id = id;
		this.anEmbeddable = anEmbeddable;
	}

	public Long getId() {
		return id;
	}

	public void setAnEmbeddable(AnEmbeddable anEmbeddable) {
		this.anEmbeddable = anEmbeddable;
	}

	public AnEmbeddable getAnEmbeddable() {
		return anEmbeddable;
	}

	public AnEmbeddable getYetAnotherEmbeddable() {
		return yetAnotherEmbeddable;
	}

	public void setYetAnotherEmbeddable(AnEmbeddable yetAnotherEmbeddable) {
		this.yetAnotherEmbeddable = yetAnotherEmbeddable;
	}

	public List<EmbeddedCollectionItem> getAnEmbeddedCollection() {
		return anEmbeddedCollection;
	}

	public void setAnEmbeddedCollection(List<EmbeddedCollectionItem> anEmbeddedCollection) {
		this.anEmbeddedCollection = anEmbeddedCollection;
	}

	public List<EmbeddedCollectionItem> getAnotherEmbeddedCollection() {
		return anotherEmbeddedCollection;
	}

	public void setAnotherEmbeddedCollection(List<EmbeddedCollectionItem> anotherEmbeddedCollection) {
		this.anotherEmbeddedCollection = anotherEmbeddedCollection;
	}
}
