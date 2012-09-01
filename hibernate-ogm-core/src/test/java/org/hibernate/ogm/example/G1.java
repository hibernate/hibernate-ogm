
package org.hibernate.ogm.example;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

@Entity
public class G1 {
	private int e1;
	private String e2;
	private G2 g2;
	
	public G1(int e1, String e2) {
		this.e1 = e1;
		this.e2 = e2;
	}
	
	@Id
	@Column(name = "e1")
	public int getE1() { return e1; }
	public void setE1(int value) { e1 = value; }
	
	@Column(name = "e2")
	public String getE2() { return e2; }
	public void setE2(String value) { e2 = value; }
	
	@OneToOne(fetch=FetchType.EAGER)
	public G2 getG2() {return this.g2;}
	public void setG2(G2 g2) {this.g2 = g2;}
}
