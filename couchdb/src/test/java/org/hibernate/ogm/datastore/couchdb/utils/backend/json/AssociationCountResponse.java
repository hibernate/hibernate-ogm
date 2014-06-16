/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.couchdb.utils.backend.json;

/**
 * Represents the result returned by the association count list functions.
 *
 * @author Gunnar Morling
 */
public class AssociationCountResponse {

	private long associationDocumentCount;
	private long inEntityAssociationCount;

	public long getAssociationDocumentCount() {
		return associationDocumentCount;
	}

	public void setAssociationDocumentCount(long associationDocumentCount) {
		this.associationDocumentCount = associationDocumentCount;
	}

	public long getInEntityAssociationCount() {
		return inEntityAssociationCount;
	}

	public void setInEntityAssociationCount(long inEntityAssociationCount) {
		this.inEntityAssociationCount = inEntityAssociationCount;
	}

	@Override
	public String toString() {
		return "AssociationCountResponse [associationDocumentCount=" + associationDocumentCount + ", inEntityAssociationCount=" + inEntityAssociationCount
				+ "]";
	}
}
