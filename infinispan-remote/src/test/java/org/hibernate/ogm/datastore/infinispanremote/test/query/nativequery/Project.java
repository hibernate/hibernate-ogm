/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.query.nativequery;

import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.SqlResultSetMappings;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Fabio Massimo Ercoli
 */
@Entity
@Table(name = "Plan")
@NamedNativeQueries(
		@NamedNativeQuery(name = "sunQuery", query = "from HibernateOGMGenerated.Plan where title = 'Sun'", resultSetMapping = "titleMapping")
)
@SqlResultSetMappings({
		@SqlResultSetMapping(name = "titleMapping", columns = @ColumnResult(name = "title")),
		@SqlResultSetMapping(
				name = "multiValueMapping",
				columns = {
						@ColumnResult(name = "title"),
						@ColumnResult(name = "description"),
						@ColumnResult(name = "key.fiscalYear")
				}
		)
})
public class Project {

	public enum Status {
		STARTED, COMPLETED, ABORTED
	}

	@Id
	private ProjectKey key;

	@Column(name = "title")
	private String name;

	private String description;

	@Temporal(TemporalType.DATE)
	private Date start;

	@Temporal(TemporalType.DATE)
	private Date end;

	private Status status;

	public Project() {
	}

	public Project(Integer fiscalYear, String businessUnit, Integer projectSerialNumber, String name, String description) {
		this.key = new ProjectKey( fiscalYear, businessUnit, projectSerialNumber );
		this.name = name;
		this.description = description;
		this.start = new Date();
		this.status = Status.STARTED;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Integer getFiscalYear() {
		return key.getFiscalYear();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		Project project = (Project) o;
		return Objects.equals( key, project.key ) &&
				Objects.equals( name, project.name ) &&
				Objects.equals( description, project.description ) &&
				Objects.equals( start, project.start ) &&
				Objects.equals( end, project.end ) &&
				status == project.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash( key, name, description, start, end, status );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "Project{" );
		sb.append( "key=" ).append( key );
		sb.append( ", name='" ).append( name ).append( '\'' );
		sb.append( ", description='" ).append( description ).append( '\'' );
		sb.append( ", start=" ).append( start );
		sb.append( ", end=" ).append( end );
		sb.append( ", status=" ).append( status );
		sb.append( '}' );
		return sb.toString();
	}
}
