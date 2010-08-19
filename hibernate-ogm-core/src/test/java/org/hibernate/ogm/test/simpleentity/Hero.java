package org.hibernate.ogm.test.simpleentity;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Persister( impl = OgmEntityPersister.class )
public class Hero {
	@Id
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;
}
