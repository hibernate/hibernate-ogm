/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.associations.onetoone;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Mark Paluch
 */
@Embeddable
public class MediaId implements Serializable {
	private String vendor;
	private String type;

	public MediaId() {
	}

	public MediaId(String vendor, String type) {
		this.vendor = vendor;
		this.type = type;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof MediaId ) ) {
			return false;
		}

		MediaId mediaId = (MediaId) o;

		if ( vendor != null ? !vendor.equals( mediaId.vendor ) : mediaId.vendor != null ) {
			return false;
		}
		return !( type != null ? !type.equals( mediaId.type ) : mediaId.type != null );

	}

	@Override
	public int hashCode() {
		int result = vendor != null ? vendor.hashCode() : 0;
		result = 31 * result + ( type != null ? type.hashCode() : 0 );
		return result;
	}
}
