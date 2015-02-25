/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;

/**
 * Test entity containing the data types each data store needs to handle.
 *
 * @author Nicolas Helleringer
 * @author Hardy Ferentschik
 */
@Entity
public class Bookmark {

	public enum Classifier {
		HOME, WORK
	}

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;

	// basic types
	private String description;
	private Character delimiter;
	private Integer stockCount;
	private Short urlPort;
	private Long userId;
	private Float visitRatio;
	private Double taxPercentage;
	private Boolean favourite;
	private Byte displayMask;

	// byte arrays
	@Lob
	private byte[] lob;
	private byte[] data;

	// enum type
	@Enumerated(EnumType.STRING)
	private Classifier classifier;

	@Enumerated(EnumType.ORDINAL)
	private Classifier classifierAsOrdinal;

	// Date/time types
	@Temporal(TemporalType.DATE)
	private Date creationDate;

	@Temporal(TemporalType.TIME)
	private Date updateTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date destructionDate;

	@Temporal(TemporalType.DATE)
	private Calendar creationCalendar;

	@Temporal(TemporalType.TIMESTAMP)
	private Calendar destructionCalendar;

	// "special" types
	private UUID serialNumber;
	private URL url;
	private BigDecimal siteWeight;
	private BigInteger visitCount;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Classifier getClassifier() {
		return classifier;
	}

	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	public Classifier getClassifierAsOrdinal() {
		return classifierAsOrdinal;
	}

	public void setClassifierAsOrdinal(Classifier classifierAsOrdinal) {
		this.classifierAsOrdinal = classifierAsOrdinal;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public URL getUrl() {
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public BigDecimal getSiteWeight() {
		return siteWeight;
	}

	public void setSiteWeight(BigDecimal siteWeight) {
		this.siteWeight = siteWeight;
	}

	public BigInteger getVisitCount() {
		return visitCount;
	}

	public void setVisitCount(BigInteger visitCount) {
		this.visitCount = visitCount;
	}

	public Boolean isFavourite() {
		return favourite;
	}

	public void setFavourite(Boolean favourite) {
		this.favourite = favourite;
	}

	public Boolean getFavourite() {
		return favourite;
	}

	public Byte getDisplayMask() {
		return displayMask;
	}

	public void setDisplayMask(Byte displayMask) {
		this.displayMask = displayMask;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date date) {
		this.creationDate = date;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Date getDestructionDate() {
		return destructionDate;
	}

	public void setDestructionDate(Date destructionDate) {
		this.destructionDate = destructionDate;
	}

	public Calendar getCreationCalendar() {
		return creationCalendar;
	}

	public void setCreationCalendar(Calendar creationCalendar) {
		this.creationCalendar = creationCalendar;
	}

	// not supported by core today: nobody misses it apparently ;)
	// @Temporal(TemporalType.TIME)
	// public Calendar getUpdateCalendar() { return updateCalendar; }
	// public void setUpdateCalendar(Calendar updateCalendar) { this.updateCalendar = updateCalendar; }
	// private Calendar updateCalendar;

	public Calendar getDestructionCalendar() {
		return destructionCalendar;
	}

	public void setDestructionCalendar(Calendar destructionCalendar) {
		this.destructionCalendar = destructionCalendar;
	}

	public byte[] getLob() {
		return lob;
	}

	public void setLob(byte[] lob) {
		this.lob = lob;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public UUID getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(UUID serialNumber) {
		this.serialNumber = serialNumber;
	}

	public Integer getStockCount() {
		return stockCount;
	}

	public void setStockCount(Integer stockCount) {
		this.stockCount = stockCount;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Double getTaxPercentage() {
		return taxPercentage;
	}

	public void setTaxPercentage(Double taxPercentage) {
		this.taxPercentage = taxPercentage;
	}

	public Float getVisitRatio() {
		return visitRatio;
	}

	public void setVisitRatio(Float visitRatio) {
		this.visitRatio = visitRatio;
	}

	public Short getUrlPort() {
		return urlPort;
	}

	public void setUrlPort(Short urlPort) {
		this.urlPort = urlPort;
	}

	public Character getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(Character delimiter) {
		this.delimiter = delimiter;
	}
}
