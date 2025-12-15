package com.animeflix.animerecommendation.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "watches")
public class Watch {
    @Id
    private String id;
    private String userName;
    private String aniId;
    private String aniTitle;
    private String epTitle;
    private String image;
    private String epId;
    private Integer epNum;
    private String epid;
    private Integer epnum;
    private Double timeWatched;
    private Double duration;
    private String provider;
    private String nextepId;
    private Integer nextepNum;
    private String subtype;
    private Date createdAt;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAniId() { return aniId; }
    public void setAniId(String aniId) { this.aniId = aniId; }

    public String getAniTitle() { return aniTitle; }
    public void setAniTitle(String aniTitle) { this.aniTitle = aniTitle; }

    public String getEpTitle() { return epTitle; }
    public void setEpTitle(String epTitle) { this.epTitle = epTitle; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getEpId() { return epId; }
    public void setEpId(String epId) { this.epId = epId; }

    public Integer getEpNum() { return epNum; }
    public void setEpNum(Integer epNum) { this.epNum = epNum; }

    public String getEpid() { return epid; }
    public void setEpid(String epid) { this.epid = epid; }

    public Integer getEpnum() { return epnum; }
    public void setEpnum(Integer epnum) { this.epnum = epnum; }

    public Double getTimeWatched() { return timeWatched; }
    public void setTimeWatched(Double timeWatched) { this.timeWatched = timeWatched; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getNextepId() { return nextepId; }
    public void setNextepId(String nextepId) { this.nextepId = nextepId; }

    public Integer getNextepNum() { return nextepNum; }
    public void setNextepNum(Integer nextepNum) { this.nextepNum = nextepNum; }

    public String getSubtype() { return subtype; }
    public void setSubtype(String subtype) { this.subtype = subtype; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
