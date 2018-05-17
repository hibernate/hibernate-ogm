/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;

/**
 * @author Fabio Massimo Ercoli
 */
@Embeddable
public class ProjectKey implements Serializable {

	private Integer fiscalYear;
	private String businessUnit;
	private Integer projectSerialNumber;

	public ProjectKey() {
	}

	public ProjectKey(Integer fiscalYear, String businessUnit, Integer projectSerialNumber) {
		this.fiscalYear = fiscalYear;
		this.businessUnit = businessUnit;
		this.projectSerialNumber = projectSerialNumber;
	}

	public Integer getFiscalYear() {
		return fiscalYear;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ProjectKey that = (ProjectKey) o;
		return Objects.equals( fiscalYear, that.fiscalYear ) &&
				Objects.equals( businessUnit, that.businessUnit ) &&
				Objects.equals( projectSerialNumber, that.projectSerialNumber );
	}

	@Override
	public int hashCode() {
		return Objects.hash( fiscalYear, businessUnit, projectSerialNumber );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "ProjectKey{" );
		sb.append( "fiscalYear=" ).append( fiscalYear );
		sb.append( ", businessUnit='" ).append( businessUnit ).append( '\'' );
		sb.append( ", projectSerialNumber=" ).append( projectSerialNumber );
		sb.append( '}' );
		return sb.toString();
	}
}
