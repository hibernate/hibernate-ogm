package org.hibernate.ogm.test.simpleentity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity @Persister( impl = OgmEntityPersister.class)
public class Helicopter {
	@Id @GeneratedValue(generator = "uuid") @GenericGenerator( name="uuid", strategy = "uuid2")
	public String getUUID() { return uuid; }
	public void setUUID(String uuid) { this.uuid = uuid; }
	private String uuid;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	private String name;
}
