package org.hibernate.ogm.test.simpleentity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity @Persister( impl = OgmEntityPersister.class)
public class Hypothesis {

	@Id @GeneratedValue
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	private String id;

	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	private String description;
}
