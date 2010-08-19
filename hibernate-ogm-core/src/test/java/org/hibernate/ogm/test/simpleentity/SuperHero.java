package org.hibernate.ogm.test.simpleentity;

import javax.persistence.Entity;

import org.hibernate.annotations.Persister;
import org.hibernate.ogm.persister.OgmEntityPersister;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Persister( impl = OgmEntityPersister.class )
public class SuperHero extends Hero {
	public String getSpecialPower() { return specialPower; }
	public void setSpecialPower(String specialPower) { this.specialPower = specialPower; }
	private String specialPower;
}
